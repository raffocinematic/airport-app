package it.raffo.backend.destinations.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Salva il catalogo geografico tramite operazioni JDBC a blocchi.
 * Le città mantengono gli ID GeoNames.
 * Gli alias vengono sincronizzati per le città importate.
 */
@Service
public class GeoNamesCatalogWriter {

    private static final int BATCH_SIZE = 500;

    private static final String UPSERT_CITY = """
            INSERT INTO destination_cities (
                id,
                name,
                country_code,
                admin1_name,
                admin2_name,
                latitude,
                longitude
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                country_code = EXCLUDED.country_code,
                admin1_name = EXCLUDED.admin1_name,
                admin2_name = EXCLUDED.admin2_name,
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude
            """;

    private static final String DELETE_CITY_ALIASES = """
            DELETE FROM destination_city_aliases
            WHERE city_id IN (:cityIds)
            """;

    private static final String INSERT_ALIAS = """
            INSERT INTO destination_city_aliases (
                city_id,
                name,
                normalized_name
            )
            VALUES (?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public GeoNamesCatalogWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "jdbcTemplate must not be null"
        );

        this.namedJdbcTemplate =
                new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * Restituisce il numero di alias scritti.
     * Tutte le operazioni appartengono alla stessa transazione.
     */
    @Transactional
    public long write(List<GeoNamesCatalogCity> cities) {
        Objects.requireNonNull(cities, "cities must not be null");

        if (cities.isEmpty()) {
            throw new IllegalArgumentException(
                    "The catalog must contain at least one city"
            );
        }

        jdbcTemplate.batchUpdate(
                UPSERT_CITY,
                cities,
                BATCH_SIZE,
                (statement, city) -> {
                    statement.setLong(1, city.id());
                    statement.setString(2, city.name());
                    statement.setString(
                            3,
                            city.countryCode().name()
                    );
                    statement.setString(4, city.admin1Name());
                    statement.setString(5, city.admin2Name());
                    statement.setBigDecimal(6, city.latitude());
                    statement.setBigDecimal(7, city.longitude());
                }
        );

        long aliasCount = 0;

        for (int start = 0; start < cities.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, cities.size());

            List<GeoNamesCatalogCity> cityBatch =
                    cities.subList(start, end);

            List<Long> cityIds = cityBatch.stream()
                    .map(GeoNamesCatalogCity::id)
                    .toList();

            namedJdbcTemplate.update(
                    DELETE_CITY_ALIASES,
                    Map.of("cityIds", cityIds)
            );

            List<AliasRow> aliasRows = new ArrayList<>();

            for (GeoNamesCatalogCity city : cityBatch) {
                for (Map.Entry<String, String> alias
                        : city.aliasesByNormalizedName().entrySet()) {
                    aliasRows.add(new AliasRow(
                            city.id(),
                            alias.getValue(),
                            alias.getKey()
                    ));
                }
            }

            jdbcTemplate.batchUpdate(
                    INSERT_ALIAS,
                    aliasRows,
                    BATCH_SIZE,
                    (statement, alias) -> {
                        statement.setLong(1, alias.cityId());
                        statement.setString(2, alias.name());
                        statement.setString(
                                3,
                                alias.normalizedName()
                        );
                    }
            );

            aliasCount += aliasRows.size();
        }

        return aliasCount;
    }

    private record AliasRow(
            long cityId,
            String name,
            String normalizedName
    ) {
    }
}