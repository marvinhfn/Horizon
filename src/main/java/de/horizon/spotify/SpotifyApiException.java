package de.horizon.spotify;

public final class SpotifyApiException extends RuntimeException {
    private final int statusCode;

    public SpotifyApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
