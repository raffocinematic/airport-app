package it.raffo.backend.routes;

import it.raffo.backend.common.InvalidRouteException;
import org.springframework.stereotype.Service;
import it.raffo.backend.common.DuplicateResourceException;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import it.raffo.backend.common.ResourceNotFoundException;

import java.time.Instant;
import java.util.UUID;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import it.raffo.backend.destinations.DestinationCity;
import it.raffo.backend.destinations.DestinationCityRepository;

@Service
public class RouteService {

    // Non mi piacciono i nomi delle città di partenza e arrivo hard coded qui ... questa cosa la ripensiamo poi
    private static final String ORIGIN_CITY = "Napoli";

    private static final String ROUTE_UNIQUE_CONSTRAINT =
            "uk_routes_origin_destination_city";

    private static final Long NAPOLI_CITY_ID = 3172394L;

    private static final Set<Long> RETURN_REQUIRED_DESTINATIONS =
            Set.of(3173435L, 3169070L);

    private static final int MAX_PAGE_SIZE = 50;

    // serve a cercare duplicati, salvare e leggere tratte
    private final RouteRepository routeRepository;

    private final DestinationCityRepository destinationCityRepository;

    // serve per assegnare l'istante della creazione
    private final Clock clock;

    public RouteService(
            RouteRepository routeRepository,
            DestinationCityRepository destinationCityRepository,
            Clock clock
    ) {
        this.routeRepository = routeRepository;
        this.destinationCityRepository = destinationCityRepository;
        this.clock = clock;
    }

    @Transactional
    public RouteResponse create(CreateRouteRequest request) {
        DestinationCity destination =
                resolveDestination(request.destinationCityId());

        boolean returnEnabled = request.returnEnabled();

        validateBusinessRules(destination, returnEnabled);

        boolean alreadyExists =
                routeRepository.existsByOriginCityAndDestination_Id(
                        ORIGIN_CITY,
                        destination.getId()
                );

        if (alreadyExists) {
            throw new DuplicateResourceException("Route already exists");
        }

        Route route = new Route(
                UUID.randomUUID(),
                ORIGIN_CITY,
                destination,
                returnEnabled,
                Instant.now(clock)
        );

        Route savedRoute;

        try {
            savedRoute = routeRepository.saveAndFlush(route);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(
                    exception,
                    ROUTE_UNIQUE_CONSTRAINT
            )) {
                throw new DuplicateResourceException(
                        "Route already exists",
                        exception
                );
            }

            throw exception;
        }

        return toResponse(savedRoute);
    }

    /**
     *
     * Metodo per aggiornare una rotta
     *
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public RouteResponse update(UUID id, UpdateRouteRequest request) {
        Route route = routeRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Route not found")
                );

        DestinationCity destination =
                resolveDestination(request.destinationCityId());

        boolean returnEnabled = request.returnEnabled();

        validateBusinessRules(destination, returnEnabled);

        boolean alreadyExists =
                routeRepository.existsByOriginCityAndDestination_IdAndIdNot(
                        route.getOriginCity(),
                        destination.getId(),
                        route.getId()
                );

        if (alreadyExists) {
            throw new DuplicateResourceException("Route already exists");
        }

        route.updateDetails(destination, returnEnabled);

        try {
            routeRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(
                    exception,
                    ROUTE_UNIQUE_CONSTRAINT
            )) {
                throw new DuplicateResourceException(
                        "Route already exists",
                        exception
                );
            }

            throw exception;
        }

        return toResponse(route);
    }

    /**
     * Metodo per cambiare lo stato di una rotta
     *
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public RouteResponse changeStatus(UUID id, UpdateRouteStatusRequest request) {

        Route route = routeRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Route not found")
                );

        route.changeStatus(request.active());

        routeRepository.flush();

        return toResponse(route);
    }

    /**
     * Metodo per cancellare una rotta
     *
     * @param id
     */
    @Transactional
    public void delete(UUID id) {

        Route route = routeRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Route not found")
                );
        routeRepository.delete(route);
        routeRepository.flush();
    }



    /**
     *
     * @param page
     * @param size
     * @return
     */
    @Transactional(readOnly = true)
    public RoutePageResponse list(int page, int size) {

        Map<String, String> errors = new HashMap<>();

        if (page < 0) {
            errors.put("page", "Page must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            errors.put("size", "Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        if (!errors.isEmpty()) {
            throw new InvalidRouteException(errors);
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<RouteResponse> result = routeRepository
                .findAll(pageRequest)
                .map(this::toResponse);

        return new RoutePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ------------------------------------------------------------------------------------------------------------

    private DestinationCity resolveDestination(Long destinationCityId) {
        if (destinationCityId == null || destinationCityId <= 0) {
            throw new InvalidRouteException(Map.of(
                    "destinationCityId",
                    "Select a valid destination city"
            ));
        }

        return destinationCityRepository.findById(destinationCityId)
                .orElseThrow(() -> new InvalidRouteException(Map.of(
                        "destinationCityId",
                        "Destination city does not exist"
                )));
    }

    private void validateBusinessRules(
            DestinationCity destination,
            boolean returnEnabled
    ) {
        Map<String, String> errors = new HashMap<>();

        if (NAPOLI_CITY_ID.equals(destination.getId())) {
            errors.put(
                    "destinationCityId",
                    "Destination must be different from Napoli"
            );
        }

        if (RETURN_REQUIRED_DESTINATIONS.contains(destination.getId())
                && !returnEnabled) {
            errors.put(
                    "returnEnabled",
                    "Return connection is required for Milano and Roma"
            );
        }

        if (!errors.isEmpty()) {
            throw new InvalidRouteException(errors);
        }
    }

    private RouteResponse toResponse(Route route) {
        DestinationCity destination = route.getDestination();

        RouteCountry countryCode = destination.getCountryCode();

        RouteType routeType = countryCode == RouteCountry.IT
                ? RouteType.NAZIONALE
                : RouteType.INTERNAZIONALE;

        // I dati geografici provengono dalla città collegata nel catalogo.
        return new RouteResponse(
                route.getId(),
                route.getOriginCity(),
                destination.getId(),
                destination.getName(),
                countryCode,
                destination.getAdmin1Name(),
                destination.getAdmin2Name(),
                routeType,
                route.isReturnEnabled(),
                route.isActive(),
                route.getCreatedAt()
        );
    }

    private boolean isUniqueConstraintViolation(
            DataIntegrityViolationException exception,
            String expectedConstraintName
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && expectedConstraintName.equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
