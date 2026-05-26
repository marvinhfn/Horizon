package de.horizon.feature.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.HorizonMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazily fetches Hypixel SkyBlock item skull-texture values from the public
 * Hypixel resources API and caches them in memory.
 *
 * Key   : Hypixel SkyBlock item ID, e.g. "CONDENSED_FERMENTO"
 * Value : base64 texture string for use in a ProfileComponent skull item.
 */
public final class SkyBlockHeadCache {

    private static final String API_URL =
            "https://api.hypixel.net/v2/resources/skyblock/items";

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean loading  = false;
    private static volatile boolean loaded   = false;
    private static volatile boolean failed   = false;

    private SkyBlockHeadCache() {}

    /** Triggers an async fetch if not already done or if a previous attempt failed. */
    public static void ensureLoaded() {
        if (loaded || loading) return;
        loading = true;
        failed  = false;
        Thread.ofVirtual().name("horizon-skyblock-heads").start(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(20))
                        .header("Accept", "application/json")
                        .header("User-Agent", "HorizonMod/" + HorizonMod.VERSION)
                        .GET().build();
                HttpResponse<String> resp =
                        client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null) {
                    parse(resp.body());
                    loaded = true;
                } else {
                    HorizonMod.LOGGER.warn("SkyBlockHeadCache: HTTP {}", resp.statusCode());
                    failed = true;
                }
            } catch (Exception e) {
                HorizonMod.LOGGER.warn("SkyBlockHeadCache fetch failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                failed = true;
            } finally {
                loading = false;
            }
        });
    }

    private static void parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("items")) {
                HorizonMod.LOGGER.warn("SkyBlockHeadCache: response missing 'items'. Keys: {}", root.keySet());
                return;
            }
            JsonArray items = root.getAsJsonArray("items");
            HorizonMod.LOGGER.info("SkyBlockHeadCache: {} items in API response, parsing skins...", items.size());
            int skinCount = 0;
            for (JsonElement el : items) {
                if (!el.isJsonObject()) continue;
                JsonObject item = el.getAsJsonObject();
                if (!item.has("id")) continue;
                String id = item.get("id").getAsString();
                if (id.isBlank()) continue;

                // skin can be a plain base64 string or {"value":"..."}
                String skin = null;
                if (item.has("skin")) {
                    JsonElement skinEl = item.get("skin");
                    if (skinEl.isJsonPrimitive()) {
                        skin = skinEl.getAsString();
                    } else if (skinEl.isJsonObject()) {
                        JsonObject skinObj = skinEl.getAsJsonObject();
                        if (skinObj.has("value")) skin = skinObj.get("value").getAsString();
                    }
                }
                if (skin != null && !skin.isBlank()) {
                    CACHE.put(id.toUpperCase(), skin);
                    skinCount++;
                }
            }
            HorizonMod.LOGGER.info("SkyBlockHeadCache: loaded {} skull textures", skinCount);
        } catch (Exception e) {
            HorizonMod.LOGGER.warn("SkyBlockHeadCache parse error: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** Returns the base64 texture value for the given item ID, or {@code null}. */
    public static String getTexture(String skyBlockItemId) {
        if (skyBlockItemId == null) return null;
        return CACHE.get(skyBlockItemId.toUpperCase());
    }

    /**
     * Returns all cache entries whose key contains the given upper-case sub-string.
     * Used by the item picker screen to enumerate matching SkyBlock heads.
     */
    public static Iterable<java.util.Map.Entry<String, String>> getMatchingEntries(String upperQuery) {
        java.util.List<java.util.Map.Entry<String, String>> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, String> e : CACHE.entrySet()) {
            if (upperQuery.isBlank() || e.getKey().contains(upperQuery)) {
                result.add(e);
            }
        }
        result.sort(java.util.Map.Entry.comparingByKey());
        return result;
    }

    public static boolean isLoading() { return loading; }
    public static boolean isLoaded()  { return loaded;  }
    public static boolean hasFailed() { return failed;  }
}
