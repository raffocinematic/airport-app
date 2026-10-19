package it.raffo.backend.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.login-rate-limit")
@Validated
public record LoginRateLimitProperties(
        @Min(1)
        int maxRequests,

        @NotNull
        Duration window,

        @Min(1)
        int maxTrackedIps
) {

    @AssertTrue(message = "Login rate limit windows must be positive")
    public boolean isWindowPositive() {
        return window == null
                || (!window.isZero() && !window.isNegative());
    }

}
