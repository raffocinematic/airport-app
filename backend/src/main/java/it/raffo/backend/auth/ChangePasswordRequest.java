package it.raffo.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dto che descrive il Json che il client invierà all'endpoint
 *
 * @param currentPassword
 * @param newPassword
 */
public record ChangePasswordRequest(
        @NotBlank
        @Size(max = 128)
        String currentPassword,

        @NotBlank
        @Size(min = 15, max = 72)
        String newPassword
) {
}
