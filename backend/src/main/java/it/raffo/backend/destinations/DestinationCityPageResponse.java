package it.raffo.backend.destinations;

import java.util.List;

/**
 * Pagina dei risultati della ricerca delle destinazioni.
 */
public record DestinationCityPageResponse(
        List<DestinationCityResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}