package it.raffo.backend.routes;

import it.raffo.backend.common.InvalidRouteException;
import org.springframework.stereotype.Service;
import it.raffo.backend.common.DuplicateResourceException;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class RouteService {

    // Non mi piacciono i nomi delle città di partenza e arrivo hard coded qui ... questa cosa la ripensiamo poi
    private static final String ORIGIN_CITY = "Napoli";

    private static final String ROUTE_UNIQUE_CONSTRAINT =
            "uk_routes_origin_destination_country";

    private static final Set<String> RETURN_REQUIRED_DESTINATIONS =
            Set.of("milano", "roma");

    private static final int MAX_PAGE_SIZE = 50;

    // serve a cercare duplicati, salvare e leggere tratte
    private final RouteRepository routeRepository;
    // serve per assegnare l'istante della creazione
    private final Clock clock;

    public RouteService(RouteRepository routeRepository, Clock clock) {
        this.routeRepository = routeRepository;
        this.clock = clock;
    }

    @Transactional
    public RouteResponse create(CreateRouteRequest request) {
        String destinationCity = request.destinationCity();

        String normalizedDestinationCity =
                destinationCity.toLowerCase(Locale.ROOT);

        RouteCountry destinationCountryCode =
                RouteCountry.valueOf(request.destinationCountryCode());

        boolean returnEnabled = request.returnEnabled();

        validateBusinessRules(
                normalizedDestinationCity,
                returnEnabled
        );

        boolean alreadyExists =
                routeRepository.existsByOriginCityAndNormalizedDestinationCityAndDestinationCountryCode(
                        ORIGIN_CITY,
                        normalizedDestinationCity,
                        destinationCountryCode
                );

        if (alreadyExists) {
            throw new DuplicateResourceException("Route already exists");
        }

        Route route = new Route(
                UUID.randomUUID(),
                ORIGIN_CITY,
                destinationCity,
                normalizedDestinationCity,
                destinationCountryCode,
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

    private void validateBusinessRules(
            String normalizedDestinationCity,
            boolean returnEnabled
    ) {
        Map<String, String> errors = new HashMap<>();

        if(ORIGIN_CITY.equalsIgnoreCase(normalizedDestinationCity)) {
            errors.put(
                    "destinationCity",
                    "Destination must be different from Napoli"
            );
        }

        if(RETURN_REQUIRED_DESTINATIONS.contains(normalizedDestinationCity)
        && !returnEnabled) {
            errors.put(
                    "returnEnabled",
                    "Return connection is required for Milano and Roma"
            );
        }

        if(!errors.isEmpty()) {
            throw new InvalidRouteException(errors);
        }
    }

    private RouteResponse toResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getOriginCity(),
                route.getDestinationCity(),
                route.getDestinationCountryCode(),
                route.getRouteType(),
                route.isReturnEnabled(),
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
