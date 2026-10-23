package it.raffo.backend.routes;

import java.util.List;

public record RoutePageResponse(
        List<RouteResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
