package de.horizon.backend.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class DevTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 15L * 60L;

    private final String secret;

    public DevTokenService(String secret) {
        this.secret = secret == null ? "change-me" : secret;
    }

    public String issueToken(String uuid, String username, String audience, String proofValue) {
        long expiresAt = expiresAtEpochSeconds();
        String payload = String.join(":",
            sanitize(uuid),
            sanitize(username),
            sanitize(audience),
            Long.toString(expiresAt),
            sanitize(proofValue),
            sanitize(secret)
        );
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public long expiresAtEpochSeconds() {
        return Instant.now().getEpochSecond() + TOKEN_LIFETIME_SECONDS;
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(':', '_').trim();
    }
}
