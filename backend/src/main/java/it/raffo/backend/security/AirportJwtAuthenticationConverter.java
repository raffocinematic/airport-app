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
        UUID userId = UUID.fromString(jwt.getSubject());

        // Ogni richiesta autenticata rilegge l'utente dal DB: cosi' ruoli, stato e revoche sono aggiornati.
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid token");
        }

        // authVersion invalida i token emessi prima di logout, cambio password o future modifiche di sicurezza.
        Long tokenAuthVersion = jwt.getClaim("authVersion");

        if (tokenAuthVersion == null || tokenAuthVersion.longValue() != user.getAuthVersion()) {
            throw new BadCredentialsException("Invalid token");
        }

        // L'authority viene dal DB, non dal claim role, per non fidarci di dati potenzialmente vecchi nel token.
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
    }
}
