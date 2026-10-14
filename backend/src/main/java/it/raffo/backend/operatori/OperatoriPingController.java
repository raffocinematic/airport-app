package it.raffo.backend.operatori;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OperatoriPingController {

    @GetMapping("/api/v1/operatori/ping")
    public Map<String, String> ping() {
        return Map.of("message", "OPERATORI access granted");
    }
}
