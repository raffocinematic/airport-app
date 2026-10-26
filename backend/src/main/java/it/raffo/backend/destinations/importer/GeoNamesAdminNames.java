package it.raffo.backend.destinations.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Risolve i codici amministrativi GeoNames nei rispettivi nomi.
 * I file vengono caricati una volta e consultati tramite mappe.
 */
public class GeoNamesAdminNames {

    private final Map<String, String> admin1Names;
    private final Map<String, String> admin2Names;

    private GeoNamesAdminNames(
            Map<String, String> admin1Names,
            Map<String, String> admin2Names
    ) {
        this.admin1Names = Map.copyOf(admin1Names);
        this.admin2Names = Map.copyOf(admin2Names);
    }

    public static GeoNamesAdminNames load(
            Path rawDirectory
    ) throws IOException {
        Map<String, String> admin1Names = readNames(
                rawDirectory.resolve("admin1CodesASCII.txt")
        );

        Map<String, String> admin2Names = readNames(
                rawDirectory.resolve("admin2Codes.txt")
        );

        return new GeoNamesAdminNames(admin1Names, admin2Names);
    }

    public String admin1Name(GeoNamesPlace place) {
        if (place.admin1Code().isBlank()) {
            return null;
        }

        String key = place.countryCode().name()
                + "." + place.admin1Code();

        return admin1Names.get(key);
    }

    public String admin2Name(GeoNamesPlace place) {
        if (place.admin1Code().isBlank()
                || place.admin2Code().isBlank()) {
            return null;
        }

        String key = place.countryCode().name()
                + "." + place.admin1Code()
                + "." + place.admin2Code();

        return admin2Names.get(key);
    }

    private static Map<String, String> readNames(
            Path file
    ) throws IOException {
        Map<String, String> names = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(
                file,
                StandardCharsets.UTF_8
        )) {
            long lineNumber = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split("\t", -1);

                if (fields.length != 4
                        || fields[0].isBlank()
                        || fields[1].isBlank()
                        || fields[1].strip().length() > 200) {
                    throw new IOException(
                            "Invalid administrative data in "
                                    + file
                                    + ", line " + lineNumber
                    );
                }

                String key = fields[0].strip();
                String name = fields[1].strip();

                String previous = names.putIfAbsent(key, name);

                if (previous != null) {
                    throw new IOException(
                            "Duplicate administrative code "
                                    + key
                                    + " in " + file
                                    + ", line " + lineNumber
                    );
                }
            }
        }

        return names;
    }
}