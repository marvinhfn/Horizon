package de.horizon.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.HorizonMod;
import de.horizon.api.auth.HorizonApiAuthService;
import de.horizon.config.ConfigManager;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class HorizonApiClient {
    private final ConfigManager configManager;
    private final HorizonApiAuthService authService;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public HorizonApiClient(ConfigManager configManager, HorizonApiAuthService authService) {
        this.configManager = configManager;
        this.authService = authService;
    }

    public JsonObject getJson(String path, Map<String, String> query) throws IOException, InterruptedException {
        ensureBackendEnabled();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint(path, query))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .header("User-Agent", "HorizonMod/" + HorizonMod.VERSION)
            .GET();

        String authorization = authService.authorizationHeader();
        if (!authorization.isBlank()) {
            requestBuilder.header("Authorization", authorization);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            authService.invalidateToken();
            throw new HorizonApiException("Horizon-Backend verweigert die Anmeldung.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new HorizonApiException("Horizon-Backend antwortete mit Status " + response.statusCode() + ".");
        }
        if (response.body() == null || response.body().isBlank()) {
            throw new HorizonApiException("Horizon-Backend lieferte keine Daten.");
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void ensureBackendEnabled() {
        if (!configManager.getConfig().isHorizonBackendEnabled()) {
            throw new HorizonApiException("Eigenes Horizon-Backend ist noch nicht aktiviert.");
        }
    }

    private URI endpoint(String path, Map<String, String> query) {
        String baseUrl = configManager.getConfig().getHorizonBackendBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        StringBuilder builder = new StringBuilder(normalizedBase).append(normalizedPath);
        if (!query.isEmpty()) {
            builder.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    builder.append('&');
                }
                first = false;
                builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            }
        }
        return URI.create(builder.toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
