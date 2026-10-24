package it.raffo.backend.routes;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.UUID;

/**
 * Esponiamo qui le operazioni HTTP
 */
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse create(
            @Valid @RequestBody CreateRouteRequest request
    ) {
        return routeService.create(request);
    }

    @GetMapping
    public RoutePageResponse list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return routeService.list(page, size);
    }

    @PutMapping("/{id}")
    public RouteResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRouteRequest request
    ) {
        return routeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public RouteResponse changeStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRouteStatusRequest request
    ) {
        return routeService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        routeService.delete(id);
    }
}
