package it.raffo.backend.auth;

import it.raffo.backend.users.Role;

import java.util.UUID;

/**
 * Me response serve solo a dire "questo token appartiene a questo utente"
 *
 * @param userId
 * @param email
 * @param role
 */
public record MeResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Role role
) {
}
