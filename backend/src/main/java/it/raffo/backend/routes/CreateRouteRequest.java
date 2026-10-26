package it.raffo.backend.routes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * La destinazione viene scelta dal catalogo tramite il suo ID.
 */
public record CreateRouteRequest(
        @NotNull
        @Positive
        Long destinationCityId,

        @NotNull
        Boolean returnEnabled
) {
}