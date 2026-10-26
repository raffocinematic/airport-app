package it.raffo.backend.destinations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinationCityRepository
        extends JpaRepository<DestinationCity, Long> {

    /**
     * Cerca le città attraverso i loro alias normalizzati.
     * Il servizio prepara il pattern e i parametri di paginazione.
     */
    @Query(
            value = """
                    select c
                    from DestinationCity c
                    where exists (
                        select a.id
                        from DestinationCityAlias a
                        where a.city = c
                          and a.normalizedName like :namePattern escape '!'
                    )
                    order by c.name asc, c.countryCode asc, c.id asc
                    """,
            countQuery = """
                    select count(c)
                    from DestinationCity c
                    where exists (
                        select a.id
                        from DestinationCityAlias a
                        where a.city = c
                          and a.normalizedName like :namePattern escape '!'
                    )
                    """
    )
    Page<DestinationCity> searchByAlias(
            @Param("namePattern") String namePattern,
            Pageable pageable
    );
}