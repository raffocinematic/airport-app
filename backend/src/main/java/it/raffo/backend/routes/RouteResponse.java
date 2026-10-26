package it.raffo.backend.routes;

import java.time.Instant;
import java.util.UUID;

// Espone l'identità della destinazione insieme ai dati utili all'interfaccia.
public record RouteResponse(
        UUID id,
        String originCity,
        Long destinationCityId,
        String destinationCity,
        RouteCountry destinationCountryCode,
        String destinationAdmin1Name,
        String destinationAdmin2Name,
        RouteType routeType,
        boolean returnEnabled,
        boolean active,
        Instant createdAt
) {
}