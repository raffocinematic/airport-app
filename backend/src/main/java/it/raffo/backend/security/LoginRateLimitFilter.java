package it.raffo.backend.security;

import it.raffo.backend.common.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;


public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LoginRateLimitFilter(
            LoginRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
        || !"/api/v1/auth/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        LoginRateLimiter.Decision decision =
                rateLimiter.tryAcquire(request.getRemoteAddr());

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                "Too many login attempts. Try again later.",
                Map.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(decision.retryAfterSeconds())
        );

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
