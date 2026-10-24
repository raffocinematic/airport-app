package it.raffo.backend.routes;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// In ingresso: entità gestita + tipo della sua chiave primaria
public interface RouteRepository extends JpaRepository<Route, UUID>{

    boolean existsByOriginCityAndNormalizedDestinationCityAndDestinationCountryCode(
            String originCity,
            String normalizeDestinationCity,
            RouteCountry destinationCountryCode
    );

    boolean existsByOriginCityAndNormalizedDestinationCityAndDestinationCountryCodeAndIdNot(
            String originCity,
            String normalizedDestinationCity,
            RouteCountry destinationCountryCode,
            UUID id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Route r where r.id = :id")
    Optional<Route> findByIdForUpdate(@Param("id") UUID id);
}
