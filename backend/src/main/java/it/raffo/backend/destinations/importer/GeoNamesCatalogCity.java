package it.raffo.backend.destinations.importer;

import it.raffo.backend.routes.RouteCountry;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Città con tutti i dati necessari al salvataggio nel catalogo.
 * Gli alias sono indicizzati per nome normalizzato.
 */
public record GeoNamesCatalogCity(
        long id,
        String name,
        RouteCountry countryCode,
        String admin1Name,
        String admin2Name,
        BigDecimal latitude,
        BigDecimal longitude,
        Map<String, String> aliasesByNormalizedName
) {

    public GeoNamesCatalogCity {
        aliasesByNormalizedName =
                Map.copyOf(aliasesByNormalizedName);
    }
}