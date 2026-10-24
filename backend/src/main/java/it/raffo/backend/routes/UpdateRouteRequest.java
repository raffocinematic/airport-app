package it.raffo.backend.routes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record UpdateRouteRequest(
        @NotBlank
        @Size(max = 100)
        String destinationCity,

        @NotBlank
        @Pattern(
                regexp = "IT|GB",
                message = "Destination country must be IT or GB"
        )
        String destinationCountryCode,

        @NotNull
        Boolean returnEnabled
) {

    public UpdateRouteRequest {
        if (destinationCity != null) {
            destinationCity = destinationCity.trim();
        }

        if (destinationCountryCode != null) {
            destinationCountryCode = destinationCountryCode
                    .trim()
                    .toUpperCase(Locale.ROOT);
        }
    }
}