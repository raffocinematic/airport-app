package it.raffo.backend.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminPingController {

    @GetMapping("/api/v1/admin/ping")
    public Map<String, String> ping() {
        return Map.of("message", "ADMIN access granted");
    }
}
