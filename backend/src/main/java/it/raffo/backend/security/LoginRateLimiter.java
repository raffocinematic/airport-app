package it.raffo.backend.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import java.time.Duration;

@Service
public class LoginRateLimiter {

    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final Map<String, AttemptWindow> windows = new HashMap<>();

    public LoginRateLimiter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Controlla se l'IP può effettuare una richiesta di login.
     * Rimuove le finestre scadute e, se la richiesta è consentita,
     * incrementa il contatore o crea una nuova finestra.
     *
     * Rifiuta la richiesta se l'IP ha raggiunto la soglia oppure
     * se la mappa è piena e non può accogliere nuovi IP.
     * Le richieste successive non prolungano la finestra.
     *
     * Synchronized rende atomici controllo e aggiornamento sulla stessa
     * istanza. I contatori sono in memoria e si azzerano al riavvio.
     *
     * @param ip indirizzo IP da controllare
     * @return decisione con esito e secondi di attesa, zero se consentita
     */
    public synchronized Decision tryAcquire(String ip) {
        Instant now = Instant.now(clock);

        // Elimina le finestre già scadute.
        windows.entrySet().removeIf(entry ->
                !entry.getValue().expiresAt().isAfter(now)
        );

        AttemptWindow current = windows.get(ip);

        // L'IP possiede già una finestra attiva.
        if (current != null) {
            if (current.requestCount() >= properties.maxRequests()) {
                return rejectUntil(now, current.expiresAt());
            }

            windows.put(
                    ip,
                    new AttemptWindow(
                            current.expiresAt(),
                            current.requestCount() + 1
                    )
            );

            return new Decision(true, 0);
        }

        // L'IP è nuovo, ma la mappa potrebbe essere piena.
        if (windows.size() >= properties.maxTrackedIps()) {
            Instant earliestExpiry = windows.values().stream()
                    .map(AttemptWindow::expiresAt)
                    .min(Instant::compareTo)
                    .orElseThrow();

            return rejectUntil(now, earliestExpiry);
        }

        // Crea una finestra e conta la prima richiesta.
        windows.put(
                ip,
                new AttemptWindow(
                        now.plus(properties.window()),
                        1
                )
        );

        return new Decision(true, 0);
    }

    private Decision rejectUntil(Instant now, Instant expiresAt) {
        Duration remaining = Duration.between(now, expiresAt);

        long retryAfterSeconds = remaining.getSeconds();

        if (remaining.getNano() > 0) {
            retryAfterSeconds++;
        }

        return new Decision(false, Math.max(1, retryAfterSeconds));
    }

    private record AttemptWindow(
            Instant expiresAt,
            int requestCount
    ) {

    }

    public record Decision(
            boolean allowed,
            long retryAfterSeconds
    ) {

    }
}
