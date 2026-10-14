package it.raffo.backend.auth;

import it.raffo.backend.users.AppUser;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Instant;

@Service
public class JwtService {

    private static final long EXPIRES_IN_SECONDS = 900;

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateAccessToken (AppUser user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("airport-app")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(EXPIRES_IN_SECONDS))
                //identifica l'utente
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                // servirà per autorizzare ADMIN/OPERATORI
                .claim("role", user.getRole().name())
                // servirà più avanti per invalidare token vecchi
                .claim("authVersion", user.getAuthVersion())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return EXPIRES_IN_SECONDS;
    }
}
