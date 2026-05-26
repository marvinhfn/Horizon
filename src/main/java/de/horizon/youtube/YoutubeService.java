package de.horizon.youtube;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.HorizonMod;
import de.horizon.config.ConfigManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class YoutubeService {
    private static final String API_BASE = "https://www.googleapis.com/youtube/v3";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private final ConfigManager configManager;
    private final YoutubeAuthService authService;

    private volatile List<YoutubePlaylist> playlists = List.of();
    private volatile long lastPlaylistFetch;
    private volatile long rateLimitedUntilMillis;
    private volatile boolean playlistFetchInFlight;

    public YoutubeService(ConfigManager configManager) {
        this.configManager = configManager;
        this.authService = new YoutubeAuthService(configManager);
    }

    public YoutubeAuthService auth() {
        return authService;
    }

    public List<YoutubePlaylist> getPlaylists() {
        return playlists;
    }

    public void requestPlaylistsRefresh(boolean force) {
        long now = Instant.now().toEpochMilli();
        if (now < rateLimitedUntilMillis) {
            return;
        }
        if (!force && (now - lastPlaylistFetch) < 300000L) {
            return;
        }
        if (playlistFetchInFlight) {
            return;
        }
        if (!authService.isLoggedIn()) {
            return;
        }

        playlistFetchInFlight = true;
        CompletableFuture.runAsync(() -> {
            try {
                playlists = fetchPlaylists();
                lastPlaylistFetch = Instant.now().toEpochMilli();
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("YouTube playlist refresh failed", exception);
                if (exception instanceof YoutubeApiException ytException && ytException.getStatusCode() == 429) {
                    rateLimitedUntilMillis = Math.max(rateLimitedUntilMillis, Instant.now().toEpochMilli() + 60000L);
                }
            } finally {
                playlistFetchInFlight = false;
            }
        });
    }

    private List<YoutubePlaylist> fetchPlaylists() throws IOException, InterruptedException {
        JsonObject response = request("GET", "/playlists?part=snippet&mine=true&maxResults=8");
        List<YoutubePlaylist> result = new ArrayList<>();
        if (response == null || !response.has("items")) {
            return result;
        }
        JsonArray items = response.getAsJsonArray("items");
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String id = item.has("id") && !item.get("id").isJsonNull() ? item.get("id").getAsString() : "";
            if (id.isBlank()) {
                continue;
            }
            String title = "Playlist";
            if (item.has("snippet") && item.get("snippet").isJsonObject()) {
                JsonObject snippet = item.getAsJsonObject("snippet");
                if (snippet.has("title") && !snippet.get("title").isJsonNull()) {
                    title = snippet.get("title").getAsString();
                }
            }
            result.add(new YoutubePlaylist(title, id));
        }

        // Also add liked songs playlist (special YouTube Music list)
        result.add(0, new YoutubePlaylist("Liked Songs", "LM"));
        return result;
    }

    private JsonObject request(String method, String path) throws IOException, InterruptedException {
        String token = authService.requireAccessToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + path))
            .timeout(Duration.ofSeconds(8))
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "HorizonMod/1.0")
            .GET()
            .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 429) {
            rateLimitedUntilMillis = Math.max(rateLimitedUntilMillis, Instant.now().toEpochMilli() + 60000L);
            throw new YoutubeApiException("YouTube Rate Limit", 429);
        }
        if (response.statusCode() == 204 || response.body().isBlank()) {
            return null;
        }
        if (response.statusCode() >= 400) {
            throw new YoutubeApiException("YouTube API Fehler " + response.statusCode(), response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
