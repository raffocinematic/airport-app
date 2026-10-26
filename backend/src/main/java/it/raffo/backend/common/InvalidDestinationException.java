package it.raffo.backend.common;

import java.util.Map;

/**
 * Segnala dati o parametri non validi relativi alle destinazioni.
 */
public class InvalidDestinationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidDestinationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}