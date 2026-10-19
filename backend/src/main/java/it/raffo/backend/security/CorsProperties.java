package it.raffo.backend.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

// questa annotazione indica a SpringBoot quale gruppo di proprietà leggere
@ConfigurationProperties(prefix = "app.security.cors")
@Validated
public record CorsProperties (
        @NotEmpty
        // Spring collega automaticamente il nome Java allowedOrigins al nome allowed-origins della configurazione
        List<@NotBlank String> allowedOrigins
) {
}
