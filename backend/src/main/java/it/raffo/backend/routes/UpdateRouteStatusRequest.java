package it.raffo.backend.routes;

import jakarta.validation.constraints.NotNull;

/**
 * DTO che servirà al futuro endpoint per disattivare una tratta
 * @param active
 */
public record UpdateRouteStatusRequest(
        @NotNull
        Boolean active
) {
}
