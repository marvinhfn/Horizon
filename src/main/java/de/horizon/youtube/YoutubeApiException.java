package de.horizon.youtube;

public final class YoutubeApiException extends RuntimeException {
    private final int statusCode;

    public YoutubeApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
