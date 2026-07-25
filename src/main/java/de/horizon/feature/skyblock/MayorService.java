package de.horizon.feature.skyblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks the current SkyBlock mayor via the public Hypixel election resource
 * (no API key required). Refreshed lazily on a background thread because the
 * mayor only changes every few days.
 *
 * <p>Used by the dungeon score to award Paul's {@code EZPZ} perk bonus (+10)
 * automatically.
 */
public final class MayorService {
    private static final String ELECTION_URL = "https://api.hypixel.net/v2/resources/skyblock/election";
    private static final long REFRESH_INTERVAL_MS = 15 * 60 * 1000L; // mayor changes rarely

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final AtomicBoolean fetching = new AtomicBoolean(false);

    private volatile String mayorName = "";
    private volatile boolean ezpzPerkActive = false;
    private volatile long lastFetch = 0L;

    /** Call periodically; triggers a background refresh when the cache is stale. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastFetch < REFRESH_INTERVAL_MS) return;
        if (!fetching.compareAndSet(false, true)) return;
        lastFetch = now;
        Thread worker = new Thread(this::refresh, "horizon-mayor-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private void refresh() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ELECTION_URL))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            parse(response.body());
        } catch (Exception ignored) {
            // Network hiccups just leave the last known value in place.
        } finally {
            fetching.set(false);
        }
    }

    private void parse(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) return;
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("mayor") || !obj.get("mayor").isJsonObject()) return;
        JsonObject mayor = obj.getAsJsonObject("mayor");
        String name = mayor.has("name") ? mayor.get("name").getAsString() : "";
        boolean ezpz = false;
        if (mayor.has("perks") && mayor.get("perks").isJsonArray()) {
            for (JsonElement perk : mayor.getAsJsonArray("perks")) {
                if (!perk.isJsonObject()) continue;
                JsonObject p = perk.getAsJsonObject();
                if (p.has("name") && p.get("name").getAsString().equalsIgnoreCase("EZPZ")) {
                    ezpz = true;
                    break;
                }
            }
        }
        this.mayorName = name;
        this.ezpzPerkActive = ezpz;
    }

    public String getMayorName() {
        return mayorName;
    }

    public boolean isPaul() {
        return "paul".equalsIgnoreCase(mayorName.trim());
    }

    /** True when Paul is mayor and his dungeon-score perk (EZPZ, +10) is active. */
    public boolean hasDungeonScoreBonus() {
        return isPaul() && ezpzPerkActive;
    }
}
