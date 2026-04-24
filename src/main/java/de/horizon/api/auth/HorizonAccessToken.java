package de.horizon.api.auth;

public record HorizonAccessToken(String token, long expiresAtEpochSecond) {
    public boolean isExpired(long nowEpochSecond, long skewSeconds) {
        return token == null || token.isBlank() || expiresAtEpochSecond <= (nowEpochSecond + skewSeconds);
    }
}
