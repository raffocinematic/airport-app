package it.raffo.backend.destinations.importer;

import it.raffo.backend.destinations.DestinationNameNormalizer;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Interpreta una riga degli alias GeoNames.
 * Conserva nomi italiani, inglesi e senza lingua dichiarata.
 */
public class GeoNamesAliasParser {

    private static final int EXPECTED_COLUMNS = 10;

    private static final Set<String> ALLOWED_LANGUAGES =
            Set.of("", "it", "en");

    public Optional<GeoNamesAlias> parse(String line) {
        Objects.requireNonNull(line, "line must not be null");

        String[] fields = line.split("\t", -1);

        if (fields.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "Expected 10 columns, found " + fields.length
            );
        }

        String language = fields[2]
                .strip()
                .toLowerCase(Locale.ROOT);

        // Include anche varianti linguistiche come en-GB.
        String baseLanguage = language.split("-", 2)[0];

        if (!ALLOWED_LANGUAGES.contains(baseLanguage)) {
            return Optional.empty();
        }

        boolean preferredName = readFlag(fields[4]);
        boolean shortName = readFlag(fields[5]);
        boolean colloquial = readFlag(fields[6]);
        boolean historic = readFlag(fields[7]);

        if (colloquial || historic) {
            return Optional.empty();
        }

        long sourceId = Long.parseLong(fields[0]);
        long cityId = Long.parseLong(fields[1]);
        String name = fields[3].strip();

        String normalizedName =
                DestinationNameNormalizer.normalize(name);

        if (sourceId <= 0 || cityId <= 0) {
            throw new IllegalArgumentException(
                    "Alias identifiers must be positive"
            );
        }

        if (name.isEmpty()
                || name.length() > 400
                || normalizedName.isEmpty()
                || normalizedName.length() > 400) {
            throw new IllegalArgumentException(
                    "Invalid alias name"
            );
        }

        return Optional.of(new GeoNamesAlias(
                sourceId,
                cityId,
                language,
                name,
                normalizedName,
                preferredName,
                shortName
        ));
    }

    private static boolean readFlag(String value) {
        if ("1".equals(value)) {
            return true;
        }

        if (value.isEmpty() || "0".equals(value)) {
            return false;
        }

        throw new IllegalArgumentException(
                "Invalid GeoNames flag: " + value
        );
    }
}