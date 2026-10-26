package it.raffo.backend.destinations.importer;

import it.raffo.backend.routes.RouteCountry;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Interpreta una riga del file delle località GeoNames.
 * Seleziona le categorie ammesse e valida i dati da importare.
 */
public class GeoNamesPlaceParser {

    private static final int EXPECTED_COLUMNS = 19;

    private static final Set<String> ALLOWED_FEATURE_CODES = Set.of(
            "PPL",
            "PPLA",
            "PPLA2",
            "PPLA3",
            "PPLA4",
            "PPLA5",
            "PPLC"
    );

    public Optional<GeoNamesPlace> parse(
            String line,
            RouteCountry expectedCountry
    ) {
        Objects.requireNonNull(line, "line must not be null");
        Objects.requireNonNull(
                expectedCountry,
                "expectedCountry must not be null"
        );

        // Il limite negativo conserva anche le colonne finali vuote.
        String[] fields = line.split("\t", -1);

        if (fields.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "Expected 19 columns, found " + fields.length
            );
        }

        // Il paese della riga deve corrispondere all'archivio letto.
        if (!expectedCountry.name().equals(fields[8])) {
            throw new IllegalArgumentException(
                    "Unexpected country code: " + fields[8]
            );
        }

        String featureClass = fields[6];
        String featureCode = fields[7];

        if (!"P".equals(featureClass)
                || !ALLOWED_FEATURE_CODES.contains(featureCode)) {
            return Optional.empty();
        }

        long id = Long.parseLong(fields[0]);
        String name = fields[1].strip();
        BigDecimal latitude = new BigDecimal(fields[4]);
        BigDecimal longitude = new BigDecimal(fields[5]);

        if (id <= 0 || name.isEmpty() || name.length() > 200) {
            throw new IllegalArgumentException(
                    "Invalid place identifier or name"
            );
        }

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException(
                    "Coordinates outside the allowed range"
            );
        }

        return Optional.of(new GeoNamesPlace(
                id,
                name,
                fields[2].strip(),
                expectedCountry,
                fields[10],
                fields[11],
                latitude,
                longitude
        ));
    }
}