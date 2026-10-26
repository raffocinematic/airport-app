package it.raffo.backend.destinations.importer;

import it.raffo.backend.destinations.DestinationNameNormalizer;
import it.raffo.backend.routes.RouteCountry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Anteprima manuale dei file GeoNames.
 * Legge gli archivi e stampa conteggi e località di esempio.
 */
public class GeoNamesPreview {

    private static final Set<String> EXAMPLE_NAMES = Set.of(
            "firenze", "florence",
            "roma", "rome",
            "milano", "milan",
            "napoli", "naples",
            "londra", "london"
    );

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: GeoNamesPreview <places-directory>"
            );
        }

        Path placesDirectory = Path.of(args[0])
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(placesDirectory)) {
            throw new IOException(
                    "Directory not found: " + placesDirectory
            );
        }

        GeoNamesPlaceParser parser = new GeoNamesPlaceParser();
        GeoNamesPlaceZipReader reader =
                new GeoNamesPlaceZipReader(parser);

        for (RouteCountry country : RouteCountry.values()) {
            Path archivePath = placesDirectory.resolve(
                    country.name() + ".zip"
            );

            System.out.println("Reading: " + archivePath);

            long count = reader.read(
                    archivePath,
                    country,
                    GeoNamesPreview::printExample
            );

            if (count == 0) {
                throw new IOException(
                        "No accepted places in " + archivePath
                );
            }

            System.out.printf(
                    "Accepted places for %s: %d%n%n",
                    country,
                    count
            );
        }
    }

    private static void printExample(GeoNamesPlace place) {
        String normalizedName =
                DestinationNameNormalizer.normalize(place.name());

        String normalizedAsciiName =
                DestinationNameNormalizer.normalize(place.asciiName());

        if (EXAMPLE_NAMES.contains(normalizedName)
                || EXAMPLE_NAMES.contains(normalizedAsciiName)) {
            System.out.printf(
                    "%s | %d | %s | admin1=%s | admin2=%s%n",
                    place.countryCode(),
                    place.id(),
                    place.name(),
                    place.admin1Code(),
                    place.admin2Code()
            );
        }
    }
}