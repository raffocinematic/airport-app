package it.raffo.backend.destinations.importer;

import it.raffo.backend.routes.RouteCountry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Coordina la lettura dei file GeoNames.
 * Costruisce il catalogo completo in memoria.
 */
public class GeoNamesCatalogLoader {

    private final GeoNamesPlaceZipReader placeReader;
    private final GeoNamesAliasZipReader aliasReader;

    public GeoNamesCatalogLoader(
            GeoNamesPlaceZipReader placeReader,
            GeoNamesAliasZipReader aliasReader
    ) {
        this.placeReader = Objects.requireNonNull(
                placeReader,
                "placeReader must not be null"
        );
        this.aliasReader = Objects.requireNonNull(
                aliasReader,
                "aliasReader must not be null"
        );
    }

    public List<GeoNamesCatalogCity> load(
            Path rawDirectory
    ) throws IOException {
        Objects.requireNonNull(
                rawDirectory,
                "rawDirectory must not be null"
        );

        GeoNamesAdminNames adminNames =
                GeoNamesAdminNames.load(rawDirectory);

        List<GeoNamesCatalogCity> result = new ArrayList<>();

        // Un ID GeoNames deve comparire una sola volta nel catalogo.
        Set<Long> seenCityIds = new HashSet<>();

        for (RouteCountry country : RouteCountry.values()) {
            Map<Long, GeoNamesPlace> places = new HashMap<>();
            Map<Long, GeoNamesCityNames> names = new HashMap<>();

            Path placeArchive = rawDirectory
                    .resolve("places")
                    .resolve(country.name() + ".zip");

            long placeCount = placeReader.read(
                    placeArchive,
                    country,
                    place -> {
                        if (!seenCityIds.add(place.id())) {
                            throw new IllegalArgumentException(
                                    "Duplicate GeoNames city ID "
                                            + place.id()
                                            + " in " + placeArchive
                            );
                        }

                        places.put(place.id(), place);
                        names.put(
                                place.id(),
                                new GeoNamesCityNames(place)
                        );
                    }
            );

            if (placeCount == 0) {
                throw new IOException(
                        "No accepted places in " + placeArchive
                );
            }

            Path aliasArchive = rawDirectory
                    .resolve("aliases")
                    .resolve(country.name() + ".zip");

            aliasReader.read(
                    aliasArchive,
                    country,
                    places.keySet(),
                    alias -> names.get(alias.cityId()).add(alias)
            );

            // I nomi vengono scelti dopo aver letto tutti gli alias.
            for (GeoNamesPlace place : places.values()) {
                GeoNamesCityNames cityNames = names.get(place.id());

                result.add(new GeoNamesCatalogCity(
                        place.id(),
                        cityNames.displayName(),
                        place.countryCode(),
                        adminNames.admin1Name(place),
                        adminNames.admin2Name(place),
                        place.latitude(),
                        place.longitude(),
                        cityNames.aliasesByNormalizedName()
                ));
            }
        }

        result.sort(
                Comparator.comparingLong(GeoNamesCatalogCity::id)
        );

        return List.copyOf(result);
    }
}