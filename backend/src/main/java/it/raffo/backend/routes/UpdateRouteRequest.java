package it.raffo.backend.routes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateRouteRequest(
        @NotNull
        @Positive
        Long destinationCityId,

        @NotNull
        Boolean returnEnabled
) {
}