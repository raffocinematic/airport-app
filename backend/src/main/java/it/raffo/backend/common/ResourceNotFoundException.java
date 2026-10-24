package it.raffo.backend.common;

// Eccezione applicativa che può essere lanciata senza aggiungere throws alla firma dei metodi
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
