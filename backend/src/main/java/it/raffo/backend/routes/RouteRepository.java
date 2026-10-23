package it.raffo.backend.routes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// In ingresso: entità gestita + tipo della sua chiave primaria
public interface RouteRepository extends JpaRepository<Route, UUID>{

    boolean existsByOriginCityAndNormalizedDestinationCityAndDestinationCountryCode(
            String originCity,
            String normalizeDestinationCity,
            RouteCountry destinationCountryCode
    );
}
