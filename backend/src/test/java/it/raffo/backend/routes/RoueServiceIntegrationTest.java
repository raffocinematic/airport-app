package it.raffo.backend.routes;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import({
        RouteService.class,
        RouteServiceIntegrationTest.TestClockConfiguration.class
})
@Testcontainers
class RouteServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void updatesRouteDetailsPreservingIdentityAndCreationDate() {
        // Arrange: creiamo la tratta da modificare.
        RouteResponse created = routeService.create(
                new CreateRouteRequest("Torino", "IT", true)
        );

        // Act: cambiamo destinazione, paese e collegamento di ritorno.
        RouteResponse updated = routeService.update(
                created.id(),
                new UpdateRouteRequest("  Londra  ", " gb ", false)
        );

        // Assert: controlliamo anche i dati restituiti dal servizio.
        assertEquals(created.id(), updated.id());
        assertEquals("Napoli", updated.originCity());
        assertEquals("Londra", updated.destinationCity());
        assertEquals(RouteCountry.GB, updated.destinationCountryCode());
        assertEquals(RouteType.INTERNAZIONALE, updated.routeType());
        assertFalse(updated.returnEnabled());
        assertTrue(updated.active());
        assertEquals(created.createdAt(), updated.createdAt());

        // Svuotiamo il contesto JPA per rileggere dal database.
        entityManager.clear();

        Route reloaded = routeRepository.findById(created.id())
                .orElseThrow();

        assertEquals("Napoli", reloaded.getOriginCity());
        assertEquals("Londra", reloaded.getDestinationCity());
        assertEquals("londra", reloaded.getNormalizedDestinationCity());
        assertEquals(RouteCountry.GB, reloaded.getDestinationCountryCode());
        assertFalse(reloaded.isReturnEnabled());
        assertTrue(reloaded.isActive());
        assertEquals(created.createdAt(), reloaded.getCreatedAt());
        assertEquals(1L, routeRepository.count());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-10-24T10:00:00Z"),
                    ZoneOffset.UTC
            );
        }
    }
}
