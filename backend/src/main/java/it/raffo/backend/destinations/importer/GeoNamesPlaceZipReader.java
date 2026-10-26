package it.raffo.backend.destinations.importer;

import it.raffo.backend.routes.RouteCountry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Legge le località direttamente da un archivio GeoNames.
 * Ogni località ammessa viene consegnata al consumer.
 */
public class GeoNamesPlaceZipReader {

    private final GeoNamesPlaceParser parser;

    public GeoNamesPlaceZipReader(GeoNamesPlaceParser parser) {
        this.parser = Objects.requireNonNull(
                parser,
                "parser must not be null"
        );
    }

    /**
     * Restituisce il numero di località consegnate al consumer.
     * Un errore di parsing interrompe la lettura.
     */
    public long read(
            Path archivePath,
            RouteCountry country,
            Consumer<GeoNamesPlace> consumer
    ) throws IOException {
        Objects.requireNonNull(
                archivePath,
                "archivePath must not be null"
        );
        Objects.requireNonNull(country, "country must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        String entryName = country.name() + ".txt";

        try (ZipFile zip = new ZipFile(
                archivePath.toFile(),
                StandardCharsets.UTF_8
        )) {
            ZipEntry entry = zip.getEntry(entryName);

            if (entry == null || entry.isDirectory()) {
                throw new IOException(
                        "Missing file " + entryName
                                + " in archive " + archivePath
                );
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            zip.getInputStream(entry),
                            StandardCharsets.UTF_8
                    )
            )) {
                long lineNumber = 0;
                long acceptedCount = 0;
                String line;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    Optional<GeoNamesPlace> parsed;

                    try {
                        parsed = parser.parse(line, country);
                    } catch (IllegalArgumentException exception) {
                        throw new IOException(
                                "Invalid GeoNames data in "
                                        + archivePath
                                        + ", entry " + entryName
                                        + ", line " + lineNumber
                                        + ": " + exception.getMessage(),
                                exception
                        );
                    }

                    if (parsed.isPresent()) {
                        consumer.accept(parsed.get());
                        acceptedCount++;
                    }
                }

                return acceptedCount;
            }
        }
    }
}