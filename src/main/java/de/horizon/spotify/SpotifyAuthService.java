package de.horizon.spotify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.horizon.HorizonMod;
import de.horizon.config.ConfigManager;
import de.horizon.config.HorizonConfig;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class SpotifyAuthService {
    private static final String CLIENT_ID = "404dc6ee7fc4474f8f1007d265c82959";
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SCOPE = "user-read-playback-state user-read-currently-playing user-modify-playback-state user-read-recently-played playlist-read-private playlist-read-collaborative";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private final ConfigManager configManager;
    private volatile HttpServer callbackServer;
    private volatile String pendingState;
    private volatile String pendingVerifier;
    private volatile boolean loginInProgress;
    private volatile String statusMessage = "Spotify nicht verbunden";

    public SpotifyAuthService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isLoggedIn() {
        return !config().getSpotifyRefreshToken().isBlank();
    }

    public boolean isLoginInProgress() {
        return loginInProgress;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void beginLogin() {
        if (loginInProgress) {
            return;
        }

        try {
            stopServer();
            pendingState = randomToken(24);
            pendingVerifier = randomToken(64);
            int port = config().getSpotifyRedirectPort();
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/spotify/callback", this::handleCallback);
            server.start();
            callbackServer = server;
            loginInProgress = true;
            statusMessage = "Spotify Login im Browser bestaetigen";
            Util.getOperatingSystem().open(URI.create(buildAuthorizationUrl(port, pendingState, pendingVerifier)));
        } catch (IOException exception) {
            HorizonMod.LOGGER.error("Failed to start Spotify callback server", exception);
            statusMessage = "Spotify Callback-Port ist blockiert";
            loginInProgress = false;
        }
    }

    public void disconnect() {
        stopServer();
        loginInProgress = false;
        pendingState = null;
        pendingVerifier = null;
        HorizonConfig config = config();
        config.setSpotifyAccessToken("");
        config.setSpotifyRefreshToken("");
        config.setSpotifyConnectedAccount("");
        config.setSpotifyTokenExpiresAt(0L);
        configManager.save();
        statusMessage = "Spotify getrennt";
    }

    public synchronized String requireAccessToken() {
        HorizonConfig config = config();
        if (config.getSpotifyRefreshToken().isBlank() && config.getSpotifyAccessToken().isBlank()) {
            throw new SpotifyApiException("Spotify nicht verbunden", 401);
        }

        long now = Instant.now().getEpochSecond();
        if (!config.getSpotifyAccessToken().isBlank() && now < config.getSpotifyTokenExpiresAt() - 30) {
            return config.getSpotifyAccessToken();
        }

        refreshToken();
        if (config.getSpotifyAccessToken().isBlank()) {
            throw new SpotifyApiException("Spotify Token konnte nicht erneuert werden", 401);
        }
        return config.getSpotifyAccessToken();
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        String responseHtml;
        int status = 200;
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                throw new IOException("Falsche Callback-Methode");
            }

            String query = exchange.getRequestURI().getRawQuery();
            String code = parameter(query, "code");
            String state = parameter(query, "state");
            String error = parameter(query, "error");

            if (error != null) {
                throw new IOException("Spotify Login abgebrochen: " + error);
            }

            if (!Objects.equals(state, pendingState) || code == null || pendingVerifier == null) {
                throw new IOException("Spotify Callback ungültig");
            }

            exchangeCode(code, pendingVerifier);
            responseHtml = successPage();
        } catch (Exception exception) {
            HorizonMod.LOGGER.error("Spotify login failed", exception);
            status = 500;
            statusMessage = exception.getMessage() == null ? "Spotify Login fehlgeschlagen" : exception.getMessage();
            responseHtml = errorPage(statusMessage);
        } finally {
            loginInProgress = false;
            pendingState = null;
            pendingVerifier = null;
            CompletableFuture.runAsync(this::stopServer);
        }

        byte[] body = responseHtml.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private void exchangeCode(String code, String verifier) throws IOException, InterruptedException {
        HorizonConfig config = config();
        String body = "client_id=" + encode(CLIENT_ID)
            + "&grant_type=authorization_code"
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(redirectUri(config.getSpotifyRedirectPort()))
            + "&code_verifier=" + encode(verifier);

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "HorizonMod/1.0")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Spotify Tokenaustausch fehlgeschlagen");
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        config.setSpotifyAccessToken(json.get("access_token").getAsString());
        if (json.has("refresh_token")) {
            config.setSpotifyRefreshToken(json.get("refresh_token").getAsString());
        }
        config.setSpotifyTokenExpiresAt(Instant.now().getEpochSecond() + json.get("expires_in").getAsLong());
        config.setSpotifyConnectedAccount("Verbunden");
        configManager.save();
        statusMessage = "Spotify verbunden";
    }

    private void refreshToken() {
        HorizonConfig config = config();
        if (config.getSpotifyRefreshToken().isBlank()) {
            return;
        }

        try {
            String body = "grant_type=refresh_token"
                + "&refresh_token=" + encode(config.getSpotifyRefreshToken())
                + "&client_id=" + encode(CLIENT_ID);
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "HorizonMod/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Spotify Token-Refresh fehlgeschlagen");
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            config.setSpotifyAccessToken(json.get("access_token").getAsString());
            config.setSpotifyTokenExpiresAt(Instant.now().getEpochSecond() + json.get("expires_in").getAsLong());
            if (json.has("refresh_token")) {
                config.setSpotifyRefreshToken(json.get("refresh_token").getAsString());
            }
            configManager.save();
            statusMessage = "Spotify verbunden";
        } catch (Exception exception) {
            HorizonMod.LOGGER.error("Failed to refresh Spotify token", exception);
            statusMessage = "Spotify Login erneuern";
            config.setSpotifyAccessToken("");
            config.setSpotifyTokenExpiresAt(0L);
            configManager.save();
        }
    }

    private String buildAuthorizationUrl(int port, String state, String verifier) {
        return AUTHORIZE_URL
            + "?client_id=" + encode(CLIENT_ID)
            + "&response_type=code"
            + "&redirect_uri=" + encode(redirectUri(port))
            + "&code_challenge_method=S256"
            + "&code_challenge=" + encode(codeChallenge(verifier))
            + "&scope=" + encode(SCOPE)
            + "&state=" + encode(state);
    }

    private String redirectUri(int port) {
        return "http://127.0.0.1:" + port + "/spotify/callback";
    }

    private String codeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create Spotify PKCE challenge", exception);
        }
    }

    private String randomToken(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String parameter(String query, String name) {
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String part : query.split("&")) {
            int separator = part.indexOf('=');
            String key = separator >= 0 ? part.substring(0, separator) : part;
            if (name.equals(key)) {
                String value = separator >= 0 ? part.substring(separator + 1) : "";
                return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String successPage() {
        return "<html><body style='background:#0b0f14;color:#d6dde3;font-family:Helvetica,Arial,sans-serif;padding:32px'>"
            + "<h2>Spotify verbunden</h2><p>Du kannst den Browser jetzt schliessen und zu Minecraft zurueckkehren.</p></body></html>";
    }

    private String errorPage(String message) {
        return "<html><body style='background:#0b0f14;color:#ff9f9f;font-family:Helvetica,Arial,sans-serif;padding:32px'>"
            + "<h2>Spotify Login fehlgeschlagen</h2><p>" + message + "</p></body></html>";
    }

    private void stopServer() {
        HttpServer server = callbackServer;
        callbackServer = null;
        if (server != null) {
            server.stop(0);
        }
    }

    private HorizonConfig config() {
        return configManager.getConfig();
    }
}
