package tools.geonames;

import it.raffo.backend.destinations.importer.GeoNamesAliasParser;
import it.raffo.backend.destinations.importer.GeoNamesAliasZipReader;
import it.raffo.backend.destinations.importer.GeoNamesCatalogCity;
import it.raffo.backend.destinations.importer.GeoNamesCatalogLoader;
import it.raffo.backend.destinations.importer.GeoNamesCatalogWriter;
import it.raffo.backend.destinations.importer.GeoNamesChecksumVerifier;
import it.raffo.backend.destinations.importer.GeoNamesPlaceParser;
import it.raffo.backend.destinations.importer.GeoNamesPlaceZipReader;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Comando manuale per importare il catalogo GeoNames.
 * Verifica e prepara i dati prima di avviare il salvataggio.
 */
public class GeoNamesImportCommand {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: GeoNamesImportCommand <geonames-directory>"
            );
        }

        Path geonamesDirectory = Path.of(args[0])
                .toAbsolutePath()
                .normalize();

        GeoNamesChecksumVerifier verifier =
                new GeoNamesChecksumVerifier();

        verifier.verify(geonamesDirectory);

        System.out.println("Verifica SHA-256 completata.");

        GeoNamesCatalogLoader loader = new GeoNamesCatalogLoader(
                new GeoNamesPlaceZipReader(
                        new GeoNamesPlaceParser()
                ),
                new GeoNamesAliasZipReader(
                        new GeoNamesAliasParser()
                )
        );

        List<GeoNamesCatalogCity> cities = loader.load(
                geonamesDirectory.resolve("raw")
        );

        System.out.printf(
                "Catalogo caricato: %d città.%n",
                cities.size()
        );

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(
                             ImportConfiguration.class
                     )) {
            GeoNamesCatalogWriter writer =
                    context.getBean(GeoNamesCatalogWriter.class);

            System.out.println("Avvio salvataggio nel database...");

            long aliasCount = writer.write(cities);

            System.out.printf(
                    "Importazione completata: %d città elaborate, "
                            + "%d alias scritti.%n",
                    cities.size(),
                    aliasCount
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:application.properties")
    @EnableTransactionManagement
    public static class ImportConfiguration {

        @Bean
        public DataSource dataSource(Environment environment) {
            DriverManagerDataSource dataSource =
                    new DriverManagerDataSource();

            dataSource.setDriverClassName("org.postgresql.Driver");

            dataSource.setUrl(environment.getRequiredProperty(
                    "spring.datasource.url"
            ));

            dataSource.setUsername(environment.getRequiredProperty(
                    "spring.datasource.username"
            ));

            dataSource.setPassword(environment.getRequiredProperty(
                    "spring.datasource.password"
            ));

            return dataSource;
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        public PlatformTransactionManager transactionManager(
                DataSource dataSource
        ) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public GeoNamesCatalogWriter catalogWriter(
                JdbcTemplate jdbcTemplate
        ) {
            return new GeoNamesCatalogWriter(jdbcTemplate);
        }
    }
}