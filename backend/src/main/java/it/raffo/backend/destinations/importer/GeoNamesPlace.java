package it.raffo.backend.destinations.importer;

import it.raffo.backend.routes.RouteCountry;

import java.math.BigDecimal;

/**
 * Dati di una località letti dal file GeoNames.
 * I codici amministrativi verranno successivamente risolti in nomi.
 */
public record GeoNamesPlace(
        long id,
        String name,
        String asciiName,
        RouteCountry countryCode,
        String admin1Code,
        String admin2Code,
        BigDecimal latitude,
        BigDecimal longitude
) {
}