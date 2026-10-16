package it.raffo.backend.common;

public class InvalidPasswordException extends RuntimeException
{
    public InvalidPasswordException(String message) {

        super(message);
    }
}
