package it.raffo.backend.common;

import java.util.Map;

public class InvalidRouteException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidRouteException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    // La mappa collega nome del campo -> msg di errore
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
