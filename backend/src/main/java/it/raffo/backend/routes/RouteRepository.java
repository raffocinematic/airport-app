package it.raffo.backend.routes;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

// In ingresso: entità gestita + tipo della sua chiave primaria
public interface RouteRepository extends JpaRepository<Route, UUID>{

    // Controlla l'esistenza della tratta usando l'identità della città.
    boolean existsByOriginCityAndDestination_Id(
            String originCity,
            Long destinationCityId
    );

    // Durante una modifica esclude la tratta che stiamo aggiornando.
    boolean existsByOriginCityAndDestination_IdAndIdNot(
            String originCity,
            Long destinationCityId,
            UUID id
    );

    // Include la destinazione nel caricamento delle tratte della pagina.
    @Override
    @EntityGraph(attributePaths = "destination")
    Page<Route> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Route r where r.id = :id")
    Optional<Route> findByIdForUpdate(@Param("id") UUID id);
}
