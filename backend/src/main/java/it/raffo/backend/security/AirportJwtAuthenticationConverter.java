package it.raffo.backend.security;

import it.raffo.backend.users.AppUser;
import it.raffo.backend.users.AppUserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class AirportJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository appUserRepository;

    public AirportJwtAuthenticationConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Il subject del token contiene l'UUID stabile dell'utente.
        UUID userId = extractUserId(jwt);

        // Ogni richiesta autenticata rilegge l'utente dal DB: cosi' ruoli, stato e revoche sono aggiornati.
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid token");
        }

        // authVersion invalida i token emessi prima di logout, cambio password o future modifiche di sicurezza.
        Long tokenAuthVersion = extractAuthVersion(jwt);

        if (tokenAuthVersion != user.getAuthVersion()) {
            throw new BadCredentialsException("Invalid token");
        }

        // L'authority viene dal DB, non dal claim role, per non fidarci di dati potenzialmente vecchi nel token.
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
    }

    // ---------------------------------------------------------------------------------------------

    private UUID extractUserId(Jwt jwt) {
        // così controlliamo il tipo effettivo del valore
        Object subjectClaim = jwt.getClaims().get("sub");

        if (!(subjectClaim instanceof String subject) || subject.isBlank()) {
            throw new BadCredentialsException("Invalid token");
        }

        try {
            return UUID.fromString(subject); // -> qui tenta la conversione
            // catch trasforma il formato non valido in un errore di autenticazione, conservando la causa per la diagnosi interna
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid token", exception);
        }
    }

    private long extractAuthVersion(Jwt jwt) {
        Object claim = jwt.getClaims().get("authVersion");

        if(claim instanceof Long version && version >= 0) {
            return version;
        }

        if (claim instanceof Integer version && version >=0) {
            return version.longValue();
        }

        throw new BadCredentialsException("Invalid token");
    }
}
