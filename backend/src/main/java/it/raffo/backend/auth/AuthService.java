package it.raffo.backend.auth;

import it.raffo.backend.users.AppUser;
import it.raffo.backend.users.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import it.raffo.backend.common.DuplicateResourceException;
import it.raffo.backend.common.InvalidCredentialsException;
import it.raffo.backend.common.InvalidPasswordException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import it.raffo.backend.users.Role;

/**
 * Contiene la logica applicativa di autenticazione.
 * <p>
 * Il controller riceverà le richieste HTTP, mentre questo service applica
 * le regole di business: normalizzazione email, hash password, controllo
 * duplicati e verifica credenziali.
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TransactionTemplate changePasswordTransaction;
    private static final String EMAIL_UNIQUE_CONSTRAINT = "app_users_email_key";

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PlatformTransactionManager transactionManager) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;

        this.changePasswordTransaction =
                new TransactionTemplate(transactionManager);

        this.changePasswordTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Registra un nuovo utente tecnico dell'applicazione. Come OPERATORE
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Normalizziamo l'email per evitare duplicati logici come "Mario@Test.it" e "mario@test.it ".
        String normalizedEmail = normalizeEmail(request.email());

        // Controllo applicativo sul duplicato; il vincolo unique sul DB resta la protezione finale.
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        AppUser user = new AppUser(
                UUID.randomUUID(),
                request.firstName().trim(),
                request.lastName().trim(),
                normalizedEmail,
                // Salviamo solo l'hash della password, mai la password in chiaro.
                passwordEncoder.encode(request.password()),
                Role.OPERATORI,
                true,
                0L,
                Instant.now()
        );

        AppUser savedUser;

        try {
            savedUser = appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(
                    exception,
                    EMAIL_UNIQUE_CONSTRAINT
            )) {
                throw new DuplicateResourceException(
                        "Email already exists",
                        exception
                );
            }

            throw exception;
        }

        return toResponse(savedUser);
    }

    /**
     * Verifica le credenziali dell'utente.
     * <p>
     * Il messaggio di errore resta generico per non rivelare se il problema
     * riguarda email inesistente, account disattivato o password errata.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return toResponse(user);
    }

    /**
     * Revoca i token precedenti incrementando authVersion nel database.
     * La transazione comprende l'aggiornamento e il relativo commit.
     */
    @Transactional
    public void logout(UUID userId) {
        int updateRows = appUserRepository.incrementAuthVersion(userId);

        if (updateRows == 0) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    public AuthResponse changePassword(
            UUID userId,
            long expectedAuthVersion,
            ChangePasswordRequest request
    ) {
        AppUser updatedUser = changePasswordTransaction.execute(status -> {
            AppUser user = appUserRepository.findById(userId)
                    .orElseThrow(() ->
                            new InvalidCredentialsException("Invalid credentials")
                    );

            if (!user.isActive()
                    || user.getAuthVersion() != expectedAuthVersion) {
                throw new InvalidCredentialsException("Invalid credentials");
            }

            String currentPassword = request.currentPassword();
            String currentPasswordHash = user.getPasswordHash();

            if (currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                    || !passwordEncoder.matches(
                    currentPassword, currentPasswordHash
            )) {
                throw new InvalidCredentialsException("Invalid credentials");
            }

            validateNewPassword(currentPassword, request.newPassword());

            String newPasswordHash =
                    passwordEncoder.encode(request.newPassword());

            int updatedRows = appUserRepository.changePasswordIfUnchanged(
                    userId,
                    expectedAuthVersion,
                    currentPasswordHash,
                    newPasswordHash
            );

            if (updatedRows == 0) {
                throw new InvalidCredentialsException("Invalid credentials");
            }

            return appUserRepository.findById(userId)
                    .orElseThrow(() ->
                            new InvalidCredentialsException("Invalid credentials")
                    );
        });

        return toResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return new MeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );

    }
    // -------------------------------------------------------------------------------------------------

    /**
     * Porta l'email in una forma stabile prima di cercarla o salvarla.
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Converte l'entità JPA in DTO di risposta, evitando di esporre passwordHash.
     */
    private AuthResponse toResponse(AppUser user) {
        return new AuthResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                jwtService.generateAccessToken(user),
                jwtService.expiresInSeconds()
        );
    }

    private void validateNewPassword(String currentPassword, String newPassword) {

        int passwordBytes =
                newPassword.getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new InvalidPasswordException(
                    "New password must not exceed 72 UTF-8 bytes"
            );
        }

        if (newPassword.equals(currentPassword)) {
            throw new InvalidPasswordException(
                    "New password must be different from current password"
            );
        }

    }

    /**
     * Un errore del DB può attraversare diversi livelli ed essere racchiusa in altre eccezioni.
     * Per questo partiamo dalla eccezione ricevuta e percorriamo le sue cause.
     */
    private boolean isUniqueConstraintViolation(
            DataIntegrityViolationException exception,
            String expectedConstraintName
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && expectedConstraintName.equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            cause = cause.getCause();
        }
        return false;
    }

}
