package de.horizon.api.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.HorizonMod;
import de.horizon.config.ConfigManager;
import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HorizonApiAuthService {
    private static final long REFRESH_SKEW_SECONDS = 120L;
    private static final long FAILURE_RETRY_SECONDS = 60L;
    private static final String USER_AGENT = "HorizonMod/Auth";

    private final ConfigManager configManager;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean refreshInFlight;
    private volatile long lastFailureAtEpochSecond;

    public HorizonApiAuthService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void tick() {
        if (!isBackendEnabled()) {
            return;
        }
        HorizonAccessToken token = currentToken();
        long now = Instant.now().getEpochSecond();
        if (!refreshInFlight && token.isExpired(now, REFRESH_SKEW_SECONDS) && now >= lastFailureAtEpochSecond + FAILURE_RETRY_SECONDS) {
            refreshAsync();
        }
    }

    public boolean isBackendEnabled() {
        return configManager.getConfig().isHorizonBackendEnabled();
    }

    public String authorizationHeader() {
        HorizonAccessToken token = currentToken();
        return token.token().isBlank() ? "" : "Bearer " + token.token();
    }

    public CompletableFuture<Boolean> refreshAsync() {
        if (refreshInFlight) {
            return CompletableFuture.completedFuture(false);
        }
        refreshInFlight = true;
        return CompletableFuture.supplyAsync(this::refreshBlocking, executor)
            .whenComplete((ignored, throwable) -> refreshInFlight = false);
    }

    public void invalidateToken() {
        HorizonConfig config = configManager.getConfig();
        config.setHorizonBackendAccessToken("");
        config.setHorizonBackendTokenExpiresAt(0L);
        configManager.save();
    }

    private boolean refreshBlocking() {
        try {
            JsonObject payload = buildTokenRequestPayload();
            HttpRequest request = HttpRequest.newBuilder(tokenEndpoint())
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().isBlank()) {
                lastFailureAtEpochSecond = Instant.now().getEpochSecond();
                HorizonMod.LOGGER.warn("Horizon backend auth failed with status {}", response.statusCode());
                return false;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String accessToken = stringValue(root, "accessToken");
            long expiresAt = longValue(root, "expiresAt");
            if (accessToken.isBlank() || expiresAt <= 0L) {
                lastFailureAtEpochSecond = Instant.now().getEpochSecond();
                HorizonMod.LOGGER.warn("Horizon backend auth returned incomplete token payload");
                return false;
            }

            HorizonConfig config = configManager.getConfig();
            config.setHorizonBackendAccessToken(accessToken);
            config.setHorizonBackendTokenExpiresAt(expiresAt);
            configManager.save();
            lastFailureAtEpochSecond = 0L;
            HorizonMod.LOGGER.info("Refreshed Horizon backend token");
            return true;
        } catch (Exception exception) {
            lastFailureAtEpochSecond = Instant.now().getEpochSecond();
            HorizonMod.LOGGER.warn("Failed to refresh Horizon backend token", exception);
            return false;
        }
    }

    private JsonObject buildTokenRequestPayload() throws IOException {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getUser() == null) {
            throw new IOException("Minecraft session unavailable");
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("minecraftUuid", client.getUser().getProfileId() == null ? "" : client.getUser().getProfileId().toString());
        payload.addProperty("minecraftUsername", client.getUser().getName());
        payload.addProperty("audience", configManager.getConfig().getHorizonBackendAudience());
        payload.addProperty("clientVersion", HorizonMod.VERSION);
        payload.addProperty("minecraftVersion", FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown"));

        // TODO: replace with signed profile-key challenge once the backend endpoint exists.
        payload.addProperty("proofType", "session-placeholder");
        payload.addProperty("proofValue", client.getUser().getName() + ":" + payload.get("minecraftUuid").getAsString());
        return payload;
    }

    private URI tokenEndpoint() {
        String baseUrl = configManager.getConfig().getHorizonBackendBaseUrl();
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalized + "/v1/auth/token");
    }

    private HorizonAccessToken currentToken() {
        HorizonConfig config = configManager.getConfig();
        return new HorizonAccessToken(config.getHorizonBackendAccessToken(), config.getHorizonBackendTokenExpiresAt());
    }

    private String stringValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private long longValue(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsLong() : 0L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
