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

/**
 * Contiene la logica applicativa di autenticazione.
 *
 * Il controller riceverà le richieste HTTP, mentre questo service applica
 * le regole di business: normalizzazione email, hash password, controllo
 * duplicati e verifica credenziali.
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registra un nuovo utente tecnico dell'applicazione.
     *
     * In questa fase non è una registrazione pubblica: serve a creare utenti
     * ADMIN e OPERATORI per testare login e permessi.
     */
    @Transactional
    public AuthResponse register (RegisterRequest request) {

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
                request.role(),
                true,
                0L,
                Instant.now()
        );

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    /**
     * Verifica le credenziali dell'utente.
     *
     * Il messaggio di errore resta generico per non rivelare se il problema
     * riguarda email inesistente, account disattivato o password errata.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if(!user.isActive()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
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

        if(updateRows == 0) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if(!user.isActive()) {
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

}
