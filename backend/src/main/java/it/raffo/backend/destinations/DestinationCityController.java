package it.raffo.backend.destinations;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Espone la ricerca paginata delle destinazioni del catalogo.
 * Validazione e ricerca sono delegate al servizio.
 */
@RestController
@RequestMapping("/api/v1/destination-cities")
public class DestinationCityController {

    private final DestinationCityService destinationCityService;

    public DestinationCityController(
            DestinationCityService destinationCityService
    ) {
        this.destinationCityService = destinationCityService;
    }

    @GetMapping
    public DestinationCityPageResponse search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return destinationCityService.search(query, page, size);
    }
}