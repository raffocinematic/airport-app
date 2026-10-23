package it.raffo.backend.routes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateRouteRequest(
        @NotBlank
        @Size(max = 100)
        String destinationCity,

        @NotBlank
        @Pattern(regexp = "IT|GB",
                message = "Destination country must be IT or GB"
        )
        String destinationCountryCode,

        @NotNull
        Boolean returnEnabled
) {

    // blocco eseguito quando viene costruito il record, modifica i parametri prima che siano assegnati
    // ai suoi campi
    public CreateRouteRequest {
        if (destinationCity != null) {
            destinationCity = destinationCity.trim();
        }

        if (destinationCountryCode != null) {
            destinationCountryCode = destinationCountryCode
                    .trim()
                    // così la conversione è indipendente dalla lingua configurata sul pc.
                    .toUpperCase(Locale.ROOT);

        }
    }
}
