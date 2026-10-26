package it.raffo.backend.destinations;

import it.raffo.backend.common.InvalidDestinationException;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class DestinationCityService {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 20;

    private final DestinationCityRepository destinationCityRepository;

    public DestinationCityService(DestinationCityRepository destinationCityRepository) {
        this.destinationCityRepository = destinationCityRepository;
    }

    @Transactional(readOnly = true)
    public DestinationCityPageResponse search(
            String query,
            int page,
            int size
    ) {
        String normalizedQuery = DestinationNameNormalizer.normalize(query);

        Map<String, String> errors = createErrors(page, size, normalizedQuery);

        if (!errors.isEmpty()) {
            throw new InvalidDestinationException(errors);
        }

        // Escape dei caratteri LIKE, poi ricerca per inizio del nome.
        String namePattern = normalizedQuery
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                + "%";

        PageRequest pageRequest = PageRequest.of(page, size);

        Page<DestinationCityResponse> result =
                destinationCityRepository
                        .searchByAlias(namePattern, pageRequest)
                        .map(this::toResponse);

        return new DestinationCityPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private static @NonNull Map<String, String> createErrors(int page, int size, String normalizedQuery) {
        Map<String, String> errors = new HashMap<>();

        if (normalizedQuery.length() < MIN_QUERY_LENGTH
                || normalizedQuery.length() > MAX_QUERY_LENGTH) {
            errors.put(
                    "q",
                    "Search text must contain between 2 and 100 characters"
            );
        }

        if (page < 0) {
            errors.put(
                    "page",
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            errors.put(
                    "size",
                    "Size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
        return errors;
    }

    private DestinationCityResponse toResponse(DestinationCity city) {
        return new DestinationCityResponse(
                city.getId(),
                city.getName(),
                city.getCountryCode(),
                city.getAdmin1Name(),
                city.getAdmin2Name()
        );
    }
}
