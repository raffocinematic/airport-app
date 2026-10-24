package it.raffo.backend.routes;

import java.time.Instant;
import java.util.UUID;

public record RouteResponse(
        UUID id,
        String originCity,
        String destinationCity,
        RouteCountry destinationCountryCode,
        RouteType routeType,
        boolean returnEnabled,
        boolean active,
        Instant createdAt
) {
}