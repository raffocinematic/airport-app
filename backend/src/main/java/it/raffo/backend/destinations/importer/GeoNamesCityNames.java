package it.raffo.backend.destinations.importer;

import it.raffo.backend.destinations.DestinationNameNormalizer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Raccoglie i nomi di una singola città.
 * Sceglie il nome principale e deduplica gli alias normalizzati.
 */
public class GeoNamesCityNames {

    // Prima nomi non abbreviati, poi preferiti.
    // Nome e ID rendono stabile la scelta in caso di parità.
    private static final Comparator<GeoNamesAlias> ITALIAN_NAME_ORDER =
            Comparator.comparing(GeoNamesAlias::shortName)
                    .thenComparing(alias -> !alias.preferredName())
                    .thenComparing(GeoNamesAlias::name)
                    .thenComparingLong(GeoNamesAlias::sourceId);

    private final long cityId;
    private final String originalName;

    private final Map<String, String> aliasesByNormalizedName =
            new HashMap<>();

    private GeoNamesAlias italianName;

    public GeoNamesCityNames(GeoNamesPlace place) {
        Objects.requireNonNull(place, "place must not be null");

        this.cityId = place.id();
        this.originalName = place.name();

        // Ogni città deve essere ricercabile anche senza alias aggiuntivi.
        addSearchName(place.name());
        addSearchName(place.asciiName());
    }

    public void add(GeoNamesAlias alias) {
        Objects.requireNonNull(alias, "alias must not be null");

        if (alias.cityId() != cityId) {
            throw new IllegalArgumentException(
                    "Alias belongs to a different city: "
                            + alias.cityId()
            );
        }

        addSearchName(alias.name());

        boolean italian = alias.language().equals("it")
                || alias.language().startsWith("it-");

        // Il nome principale deve rispettare varchar(200).
        if (!italian || alias.name().length() > 200) {
            return;
        }

        if (italianName == null
                || ITALIAN_NAME_ORDER.compare(alias, italianName) < 0) {
            italianName = alias;
        }
    }

    public String displayName() {
        return italianName != null
                ? italianName.name()
                : originalName;
    }

    /**
     * Restituisce una copia non modificabile:
     * chiave = nome normalizzato, valore = nome leggibile.
     */
    public Map<String, String> aliasesByNormalizedName() {
        Map<String, String> result =
                new HashMap<>(aliasesByNormalizedName);

        String name = displayName();

        // Per la chiave del nome principale conserviamo la sua grafia.
        result.put(
                DestinationNameNormalizer.normalize(name),
                name
        );

        return Map.copyOf(result);
    }

    private void addSearchName(String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String name = value.strip();
        String normalizedName =
                DestinationNameNormalizer.normalize(name);

        if (name.length() > 400
                || normalizedName.isEmpty()
                || normalizedName.length() > 400) {
            throw new IllegalArgumentException(
                    "Invalid search name for city " + cityId
            );
        }

        String existing = aliasesByNormalizedName.get(normalizedName);

        // A parità di chiave, scegliamo una grafia in modo deterministico.
        if (existing == null || name.compareTo(existing) < 0) {
            aliasesByNormalizedName.put(normalizedName, name);
        }
    }
}