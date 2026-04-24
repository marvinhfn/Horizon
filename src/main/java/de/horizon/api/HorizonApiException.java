package de.horizon.api;

public final class HorizonApiException extends RuntimeException {
    public HorizonApiException(String message) {
        super(message);
    }

    public HorizonApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
