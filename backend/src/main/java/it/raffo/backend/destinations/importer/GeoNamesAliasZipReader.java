package it.raffo.backend.destinations.importer;

import it.raffo.backend.routes.RouteCountry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Legge gli alias da un archivio GeoNames.
 * Consegna soltanto quelli delle città accettate nel catalogo.
 */
public class GeoNamesAliasZipReader {

    private final GeoNamesAliasParser parser;

    public GeoNamesAliasZipReader(GeoNamesAliasParser parser) {
        this.parser = Objects.requireNonNull(
                parser,
                "parser must not be null"
        );
    }

    /**
     * Restituisce il numero di alias consegnati al consumer.
     * acceptedCityIds deve contenere gli ID delle città ammesse.
     */
    public long read(
            Path archivePath,
            RouteCountry country,
            Set<Long> acceptedCityIds,
            Consumer<GeoNamesAlias> consumer
    ) throws IOException {
        Objects.requireNonNull(
                archivePath,
                "archivePath must not be null"
        );
        Objects.requireNonNull(country, "country must not be null");
        Objects.requireNonNull(
                acceptedCityIds,
                "acceptedCityIds must not be null"
        );
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

                    Optional<GeoNamesAlias> parsed;

                    try {
                        parsed = parser.parse(line);
                    } catch (IllegalArgumentException exception) {
                        throw new IOException(
                                "Invalid GeoNames alias in "
                                        + archivePath
                                        + ", entry " + entryName
                                        + ", line " + lineNumber
                                        + ": " + exception.getMessage(),
                                exception
                        );
                    }

                    if (parsed.isEmpty()) {
                        continue;
                    }

                    GeoNamesAlias alias = parsed.get();

                    if (!acceptedCityIds.contains(alias.cityId())) {
                        continue;
                    }

                    consumer.accept(alias);
                    acceptedCount++;
                }

                return acceptedCount;
            }
        }
    }
}