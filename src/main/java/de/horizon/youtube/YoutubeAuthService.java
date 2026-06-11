package de.horizon.youtube;

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

public final class YoutubeAuthService {
    private static final String CLIENT_ID = System.getProperty("horizon.youtube.clientId", "");
    private static final String CLIENT_SECRET = System.getProperty("horizon.youtube.clientSecret", "");
    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.readonly";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private final ConfigManager configManager;
    private volatile HttpServer callbackServer;
    private volatile String pendingState;
    private volatile String pendingVerifier;
    private volatile boolean loginInProgress;
    private volatile String statusMessage = "YouTube Music nicht verbunden";

    public YoutubeAuthService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isLoggedIn() {
        return !config().getYoutubeRefreshToken().isBlank();
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
            int port = config().getYoutubeRedirectPort();
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/youtube/callback", this::handleCallback);
            server.start();
            callbackServer = server;
            loginInProgress = true;
            statusMessage = "YouTube Login im Browser bestaetigen";
            Util.getPlatform().openUri(URI.create(buildAuthorizationUrl(port, pendingState, pendingVerifier)));
        } catch (IOException exception) {
            HorizonMod.LOGGER.error("Failed to start YouTube callback server", exception);
            statusMessage = "YouTube Callback-Port ist blockiert";
            loginInProgress = false;
        }
    }

    public void disconnect() {
        stopServer();
        loginInProgress = false;
        pendingState = null;
        pendingVerifier = null;
        HorizonConfig config = config();
        config.setYoutubeAccessToken("");
        config.setYoutubeRefreshToken("");
        config.setYoutubeConnectedAccount("");
        config.setYoutubeTokenExpiresAt(0L);
        configManager.save();
        statusMessage = "YouTube Music getrennt";
    }

    public synchronized String requireAccessToken() {
        HorizonConfig config = config();
        if (config.getYoutubeRefreshToken().isBlank() && config.getYoutubeAccessToken().isBlank()) {
            throw new YoutubeApiException("YouTube Music nicht verbunden", 401);
        }

        long now = Instant.now().getEpochSecond();
        if (!config.getYoutubeAccessToken().isBlank() && now < config.getYoutubeTokenExpiresAt() - 30) {
            return config.getYoutubeAccessToken();
        }

        refreshToken();
        if (config.getYoutubeAccessToken().isBlank()) {
            throw new YoutubeApiException("YouTube Token konnte nicht erneuert werden", 401);
        }
        return config.getYoutubeAccessToken();
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
                throw new IOException("YouTube Login abgebrochen: " + error);
            }

            if (!Objects.equals(state, pendingState) || code == null || pendingVerifier == null) {
                throw new IOException("YouTube Callback ungueltig");
            }

            exchangeCode(code, pendingVerifier);
            responseHtml = successPage();
        } catch (Exception exception) {
            HorizonMod.LOGGER.error("YouTube login failed", exception);
            status = 500;
            String errorMsg = exception.getMessage() == null ? "YouTube Login fehlgeschlagen" : exception.getMessage();
            if (!isLoggedIn()) {
                statusMessage = errorMsg;
            }
            responseHtml = errorPage(errorMsg);
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
        String body = "code=" + encode(code)
            + "&client_id=" + encode(CLIENT_ID)
            + "&client_secret=" + encode(CLIENT_SECRET)
            + "&code_verifier=" + encode(verifier)
            + "&grant_type=authorization_code"
            + "&redirect_uri=" + encode(redirectUri(config.getYoutubeRedirectPort()));

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "HorizonMod/1.0")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("YouTube Tokenaustausch fehlgeschlagen: " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        config.setYoutubeAccessToken(json.get("access_token").getAsString());
        if (json.has("refresh_token")) {
            config.setYoutubeRefreshToken(json.get("refresh_token").getAsString());
        }
        config.setYoutubeTokenExpiresAt(Instant.now().getEpochSecond() + json.get("expires_in").getAsLong());
        config.setYoutubeConnectedAccount("Verbunden");
        configManager.save();
        statusMessage = "YouTube Music verbunden";
    }

    private void refreshToken() {
        HorizonConfig config = config();
        if (config.getYoutubeRefreshToken().isBlank()) {
            return;
        }

        try {
            String body = "grant_type=refresh_token"
                + "&refresh_token=" + encode(config.getYoutubeRefreshToken())
                + "&client_id=" + encode(CLIENT_ID)
                + "&client_secret=" + encode(CLIENT_SECRET);
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "HorizonMod/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("YouTube Token-Refresh fehlgeschlagen");
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            config.setYoutubeAccessToken(json.get("access_token").getAsString());
            config.setYoutubeTokenExpiresAt(Instant.now().getEpochSecond() + json.get("expires_in").getAsLong());
            if (json.has("refresh_token")) {
                config.setYoutubeRefreshToken(json.get("refresh_token").getAsString());
            }
            configManager.save();
            statusMessage = "YouTube Music verbunden";
        } catch (Exception exception) {
            HorizonMod.LOGGER.error("Failed to refresh YouTube token", exception);
            statusMessage = "YouTube Music Login erneuern";
            config.setYoutubeAccessToken("");
            config.setYoutubeTokenExpiresAt(0L);
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
            + "&state=" + encode(state)
            + "&access_type=offline"
            + "&prompt=consent";
    }

    private String redirectUri(int port) {
        return "http://127.0.0.1:" + port + "/youtube/callback";
    }

    private String codeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create YouTube PKCE challenge", exception);
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
            + "<h2>YouTube Music verbunden</h2><p>Du kannst den Browser jetzt schliessen und zu Minecraft zurueckkehren.</p></body></html>";
    }

    private String errorPage(String message) {
        return "<html><body style='background:#0b0f14;color:#ff9f9f;font-family:Helvetica,Arial,sans-serif;padding:32px'>"
            + "<h2>YouTube Login fehlgeschlagen</h2><p>" + message + "</p></body></html>";
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
