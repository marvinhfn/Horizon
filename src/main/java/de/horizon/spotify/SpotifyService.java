package de.horizon.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.HorizonMod;
import de.horizon.config.ConfigManager;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class SpotifyService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private final ConfigManager configManager;
    private final SpotifyAuthService authService;

    private volatile SpotifyPlaybackState playbackState = SpotifyPlaybackState.disconnected("Spotify nicht verbunden");
    private volatile List<SpotifyPlaylist> recentPlaylists = List.of();
    private volatile List<SpotifyDevice> devices = List.of();
    private volatile long lastStateFetch;
    private volatile long lastPlaylistFetch;
    private volatile long lastDeviceFetch;
    private volatile long rateLimitedUntilMillis;
    private volatile long playlistRateLimitedUntilMillis;
    private volatile long deviceRateLimitedUntilMillis;
    private volatile boolean stateFetchInFlight;
    private volatile boolean playlistFetchInFlight;
    private volatile boolean deviceFetchInFlight;

    public SpotifyService(ConfigManager configManager) {
        this.configManager = configManager;
        this.authService = new SpotifyAuthService(configManager);
    }

    public SpotifyAuthService auth() {
        return authService;
    }

    public SpotifyPlaybackState getPlaybackState() {
        return playbackState;
    }

    public List<SpotifyPlaylist> getRecentPlaylists() {
        return recentPlaylists;
    }

    public List<SpotifyDevice> getDevices() {
        return devices;
    }

    public void requestStateRefresh(boolean force) {
        long now = Instant.now().toEpochMilli();
        if (now < rateLimitedUntilMillis) {
            return;
        }
        if (!force && (now - lastStateFetch) < 10000L) {
            return;
        }
        if (stateFetchInFlight) {
            return;
        }

        stateFetchInFlight = true;
        CompletableFuture.runAsync(() -> {
            try {
                playbackState = fetchPlaybackState();
                lastStateFetch = Instant.now().toEpochMilli();
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("Spotify state refresh failed", exception);
                if (exception instanceof SpotifyApiException spotifyException && spotifyException.getStatusCode() == 429) {
                    playbackState = SpotifyPlaybackState.unavailable("Spotify Rate Limit - warte kurz", authService.isLoggedIn());
                } else {
                    playbackState = SpotifyPlaybackState.unavailable(exception.getMessage() == null ? "Spotify Status unbekannt" : exception.getMessage(), authService.isLoggedIn());
                }
            } finally {
                stateFetchInFlight = false;
            }
        });
    }

    public void skipNext() {
        commandThenRefresh("POST", "/me/player/next", null);
    }

    public void skipPrevious() {
        commandThenRefresh("POST", "/me/player/previous", null);
    }

    public void playPause() {
        requestStateRefresh(true);
        if (playbackState.playing()) {
            command("PUT", "/me/player/pause", null, true);
        } else {
            command("PUT", "/me/player/play", "", true);
        }
    }

    public void changeVolume(int delta) {
        requestStateRefresh(true);
        if (!playbackState.supportsVolume()) {
            return;
        }

        int target = Math.max(0, Math.min(100, playbackState.volumePercent() + delta));
        setVolume(target);
    }

    public void setVolume(int volumePercent) {
        int target = Math.max(0, Math.min(100, volumePercent));
        command("PUT", "/me/player/volume?volume_percent=" + target + deviceQuery(), null, false);
    }

    public void playPlaylist(SpotifyPlaylist playlist) {
        if (playlist == null || playlist.uri().isBlank()) {
            return;
        }
        command("PUT", "/me/player/play" + playDeviceQuery(), "{\"context_uri\":\"" + playlist.uri() + "\"}", true);
    }

    public void requestDevicesRefresh(boolean force) {
        long now = Instant.now().toEpochMilli();
        if (now < deviceRateLimitedUntilMillis || now < rateLimitedUntilMillis) {
            return;
        }
        if (!force && (now - lastDeviceFetch) < 15000L) {
            return;
        }
        if (deviceFetchInFlight) {
            return;
        }

        deviceFetchInFlight = true;
        CompletableFuture.runAsync(() -> {
            try {
                devices = fetchDevices();
                lastDeviceFetch = Instant.now().toEpochMilli();
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("Spotify device refresh failed", exception);
                if (exception instanceof SpotifyApiException spotifyException && spotifyException.getStatusCode() == 429) {
                    deviceRateLimitedUntilMillis = Math.max(deviceRateLimitedUntilMillis, Instant.now().toEpochMilli() + 60000L);
                }
            } finally {
                deviceFetchInFlight = false;
            }
        });
    }

    public void selectDevice(SpotifyDevice device) {
        if (device == null || device.id().isBlank()) {
            return;
        }
        command("PUT", "/me/player", "{\"device_ids\":[\"" + device.id() + "\"],\"play\":" + playbackState.playing() + "}", true);
    }

    public void requestRecentPlaylistsRefresh(boolean force) {
        long now = Instant.now().toEpochMilli();
        if (now < playlistRateLimitedUntilMillis || now < rateLimitedUntilMillis) {
            return;
        }
        if (!force && (now - lastPlaylistFetch) < 300000L) {
            return;
        }
        if (playlistFetchInFlight) {
            return;
        }

        playlistFetchInFlight = true;
        CompletableFuture.runAsync(() -> {
            try {
                recentPlaylists = fetchDisplayPlaylists();
                lastPlaylistFetch = Instant.now().toEpochMilli();
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("Spotify playlist refresh failed", exception);
                if (exception instanceof SpotifyApiException spotifyException && spotifyException.getStatusCode() == 429) {
                    playlistRateLimitedUntilMillis = Math.max(playlistRateLimitedUntilMillis, Instant.now().toEpochMilli() + 60000L);
                }
            } finally {
                playlistFetchInFlight = false;
            }
        });
    }

    private SpotifyPlaybackState fetchPlaybackState() throws IOException, InterruptedException {
        if (!authService.isLoggedIn()) {
            return SpotifyPlaybackState.disconnected(authService.getStatusMessage());
        }

        JsonObject playerState = request("GET", "/me/player", null);
        if (playerState == null || !playerState.has("device") || playerState.get("device").isJsonNull()) {
            return SpotifyPlaybackState.unavailable("Kein aktives Spotify-Geraet", true);
        }

        JsonObject activeDevice = playerState.getAsJsonObject("device");
        boolean playing = playerState != null && playerState.has("is_playing") && playerState.get("is_playing").getAsBoolean();
        String track = "";
        String artist = "";
        if (playerState != null && playerState.has("item") && playerState.get("item").isJsonObject()) {
            JsonObject item = playerState.getAsJsonObject("item");
            track = item.has("name") ? item.get("name").getAsString() : "";
            artist = artists(item.getAsJsonArray("artists"));
        }

        return new SpotifyPlaybackState(
            true,
            true,
            playing,
            true,
            activeDevice.has("supports_volume") && activeDevice.get("supports_volume").getAsBoolean(),
            track,
            artist,
            activeDevice.has("name") ? activeDevice.get("name").getAsString() : "",
            activeDevice.has("id") && !activeDevice.get("id").isJsonNull() ? activeDevice.get("id").getAsString() : "",
            activeDevice.has("volume_percent") && !activeDevice.get("volume_percent").isJsonNull() ? activeDevice.get("volume_percent").getAsInt() : 0,
            "Spotify verbunden"
        );
    }

    private void command(String method, String path, String body, boolean refreshAfter) {
        CompletableFuture.runAsync(() -> {
            try {
                request(method, path, body);
                if (refreshAfter) {
                    requestStateRefresh(true);
                }
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("Spotify command failed", exception);
            }
        });
    }

    /** Like command(), but waits 800 ms before refreshing so Spotify finishes the transition. */
    private void commandThenRefresh(String method, String path, String body) {
        CompletableFuture.runAsync(() -> {
            try {
                request(method, path, body);
                Thread.sleep(800L);
                requestStateRefresh(true);
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("Spotify command failed", exception);
            }
        });
    }

    private JsonObject request(String method, String path, String body) throws IOException, InterruptedException {
        String token = authService.requireAccessToken();
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1" + path))
            .timeout(Duration.ofSeconds(8))
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "HorizonMod/1.0");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(publisher);
            case "PUT" -> builder.PUT(publisher);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 429) {
            long retryMillis = retryAfterMillis(response);
            rateLimitedUntilMillis = Math.max(rateLimitedUntilMillis, Instant.now().toEpochMilli() + retryMillis);
            throw new SpotifyApiException("Spotify Rate Limit", 429);
        }
        if (response.statusCode() == 204 || response.body().isBlank()) {
            return null;
        }
        if (response.statusCode() >= 400) {
            throw new SpotifyApiException("Spotify API Fehler " + response.statusCode(), response.statusCode());
        }

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private long retryAfterMillis(HttpResponse<String> response) {
        return response.headers()
            .firstValue("Retry-After")
            .map(value -> {
                try {
                    return Math.max(1L, Long.parseLong(value)) * 1000L;
                } catch (NumberFormatException ignored) {
                    return 60000L;
                }
            })
            .orElse(60000L);
    }

    private List<SpotifyPlaylist> fetchDisplayPlaylists() throws IOException, InterruptedException {
        if (!authService.isLoggedIn()) {
            return List.of();
        }

        Map<String, SpotifyPlaylist> playlists = new LinkedHashMap<>();
        JsonObject library = request("GET", "/me/playlists?limit=6", null);
        if (library != null && library.has("items")) {
            JsonArray items = library.getAsJsonArray("items");
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject playlist = element.getAsJsonObject();
                if (!playlist.has("uri")) {
                    continue;
                }
                String uri = playlist.get("uri").getAsString();
                String name = playlist.has("name") && !playlist.get("name").isJsonNull() ? playlist.get("name").getAsString() : "Playlist";
                playlists.put(uri, new SpotifyPlaylist(name, uri));
                if (playlists.size() >= 6) {
                    return new ArrayList<>(playlists.values());
                }
            }
        }

        JsonObject recent = request("GET", "/me/player/recently-played?limit=50", null);
        if (recent == null || !recent.has("items")) {
            return new ArrayList<>(playlists.values());
        }

        JsonArray items = recent.getAsJsonArray("items");
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject play = element.getAsJsonObject();
            if (!play.has("context") || play.get("context").isJsonNull()) {
                continue;
            }
            JsonObject context = play.getAsJsonObject("context");
            if (!context.has("type") || !"playlist".equals(context.get("type").getAsString()) || !context.has("uri")) {
                continue;
            }

            String uri = context.get("uri").getAsString();
            if (playlists.containsKey(uri)) {
                continue;
            }
            playlists.put(uri, new SpotifyPlaylist(fetchPlaylistName(uri), uri));
            if (playlists.size() >= 6) {
                break;
            }
        }
        return new ArrayList<>(playlists.values());
    }

    private List<SpotifyDevice> fetchDevices() throws IOException, InterruptedException {
        if (!authService.isLoggedIn()) {
            return List.of();
        }
        JsonObject response = request("GET", "/me/player/devices", null);
        if (response == null || !response.has("devices")) {
            return List.of();
        }
        List<SpotifyDevice> result = new ArrayList<>();
        for (JsonElement element : response.getAsJsonArray("devices")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject device = element.getAsJsonObject();
            result.add(new SpotifyDevice(
                device.has("id") && !device.get("id").isJsonNull() ? device.get("id").getAsString() : "",
                device.has("name") && !device.get("name").isJsonNull() ? device.get("name").getAsString() : "Geraet",
                device.has("type") && !device.get("type").isJsonNull() ? device.get("type").getAsString() : "",
                device.has("is_active") && device.get("is_active").getAsBoolean(),
                device.has("is_restricted") && device.get("is_restricted").getAsBoolean(),
                device.has("supports_volume") && device.get("supports_volume").getAsBoolean()
            ));
        }
        return result;
    }

    private String fetchPlaylistName(String uri) {
        String prefix = "spotify:playlist:";
        if (!uri.startsWith(prefix)) {
            return "Playlist";
        }
        try {
            String id = uri.substring(prefix.length());
            JsonObject playlist = request("GET", "/playlists/" + URLEncoder.encode(id, StandardCharsets.UTF_8), null);
            if (playlist != null && playlist.has("name")) {
                return playlist.get("name").getAsString();
            }
        } catch (Exception exception) {
            HorizonMod.LOGGER.debug("Could not load Spotify playlist name", exception);
        }
        return "Playlist " + uri.substring(Math.max(0, uri.length() - 6));
    }

    private String artists(JsonArray artists) {
        if (artists == null || artists.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < artists.size(); index++) {
            JsonObject artist = artists.get(index).getAsJsonObject();
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(artist.get("name").getAsString());
        }
        return builder.toString();
    }

    private String deviceQuery() {
        SpotifyPlaybackState state = playbackState;
        if (state.deviceId().isBlank()) {
            return "";
        }
        return "&device_id=" + URLEncoder.encode(state.deviceId(), StandardCharsets.UTF_8);
    }

    private String playDeviceQuery() {
        SpotifyPlaybackState state = playbackState;
        if (state.deviceId().isBlank()) {
            return "";
        }
        return "?device_id=" + URLEncoder.encode(state.deviceId(), StandardCharsets.UTF_8);
    }
}
