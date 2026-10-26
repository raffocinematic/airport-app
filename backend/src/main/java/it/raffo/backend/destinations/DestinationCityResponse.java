package it.raffo.backend.destinations;

import it.raffo.backend.routes.RouteCountry;

/**
 * Dati della città necessari al frontend per mostrare
 * e selezionare una destinazione del catalogo.
 */
public record DestinationCityResponse(
        Long id,
        String name,
        RouteCountry countryCode,
        String admin1Name,
        String admin2Name
) {
}