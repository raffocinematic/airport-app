package it.raffo.backend.auth;

import it.raffo.backend.users.Role;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Role role,
        // dopo il login, il frontend dovrà ricevere il token JWT e sapere quanto tempo è valido
        String accessToken,
        long expiresInSeconds
) {
}
