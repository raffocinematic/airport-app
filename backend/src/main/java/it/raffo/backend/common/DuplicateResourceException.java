package it.raffo.backend.common;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    // con questo nuovo costruttore trasformiamo l'errore del db in un errore applicativo conservando anche
    // l'eccezione originale
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
