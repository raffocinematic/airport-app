package it.raffo.backend.destinations.importer;

/**
 * Nome alternativo letto da GeoNames.
 * sourceId identifica l'alias nella fonte;
 * cityId identifica la località a cui appartiene.
 */
public record GeoNamesAlias(
        long sourceId,
        long cityId,
        String language,
        String name,
        String normalizedName,
        boolean preferredName,
        boolean shortName
) {
}