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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aggregated SkyBlock item prices, refreshed in the background every ~10 minutes.
 *
 * <p>Sources (all public, no API key): Bazaar instant-sell from the Hypixel Bazaar endpoint, lowest
 * auction BINs from a third-party aggregate, and item names/NPC prices from the Hypixel items
 * resource. {@link #getPrice(String)} returns the Bazaar sell price if the item is a Bazaar product,
 * otherwise the lowest BIN.
 */
public final class SkyblockPriceService {
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String LOWESTBINS_URL = "https://lb.tricked.dev/lowestbins";
    private static final String AVGBIN_URL = "https://lb.odtheking.com/averages/3day";
    private static final String ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items";
    private static final long REFRESH_INTERVAL_MS = 10 * 60 * 1000L;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final AtomicBoolean fetching = new AtomicBoolean(false);

    private final Map<String, Long> bazaar = new ConcurrentHashMap<>();
    private final Map<String, Long> bazaarBuy = new ConcurrentHashMap<>();
    private final Map<String, Long> lowestBin = new ConcurrentHashMap<>();
    private final Map<String, Long> avgBin = new ConcurrentHashMap<>();
    private final Map<String, Long> npcSell = new ConcurrentHashMap<>();
    private final Map<String, String> nameToId = new ConcurrentHashMap<>();

    private volatile long lastFetch = 0L;
    private volatile boolean loaded = false;

    public boolean isLoaded() { return loaded; }

    /** Call periodically; triggers a background refresh when the cache is stale. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastFetch < REFRESH_INTERVAL_MS) return;
        if (!fetching.compareAndSet(false, true)) return;
        lastFetch = now;
        Thread worker = new Thread(this::refresh, "horizon-price-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private void refresh() {
        // Each source is independent — one failing endpoint must not block the others or `loaded`.
        try { updateBazaar(); } catch (Exception ignored) { }
        try { updateLowestBins(); } catch (Exception ignored) { }
        try { updateAvgBins(); } catch (Exception ignored) { }
        try { updateItems(); } catch (Exception ignored) { }
        if (!bazaar.isEmpty() || !lowestBin.isEmpty()) loaded = true;
        fetching.set(false);
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 ? response.body() : null;
    }

    private void updateBazaar() throws Exception {
        String body = get(BAZAAR_URL);
        if (body == null) return;
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (!root.has("products")) return;
        JsonObject products = root.getAsJsonObject("products");
        for (Map.Entry<String, JsonElement> e : products.entrySet()) {
            JsonObject product = e.getValue().getAsJsonObject();
            JsonObject qs = product.has("quick_status") ? product.getAsJsonObject("quick_status") : null;
            // Hypixel's summary names are inverted vs. intuition (verified against quick_status):
            //   buy_summary  top ≈ quick_status.buyPrice  = INSTABUY  (what you pay to buy now, higher)
            //   sell_summary top ≈ quick_status.sellPrice = INSTASELL (what you get selling now, lower)
            //                                              ≈ the price of a competitive BUY ORDER.
            JsonArray buySummary = product.has("buy_summary") ? product.getAsJsonArray("buy_summary") : null;
            long instabuy = buySummary != null && !buySummary.isEmpty()
                ? (long) buySummary.get(0).getAsJsonObject().get("pricePerUnit").getAsDouble()
                : (qs != null && qs.has("buyPrice") ? (long) qs.get("buyPrice").getAsDouble() : 0L);
            if (instabuy > 0) bazaarBuy.put(e.getKey(), instabuy);   // getBazaarBuy() = instabuy

            JsonArray sellSummary = product.has("sell_summary") ? product.getAsJsonArray("sell_summary") : null;
            long instasell = sellSummary != null && !sellSummary.isEmpty()
                ? (long) sellSummary.get(0).getAsJsonObject().get("pricePerUnit").getAsDouble()
                : (qs != null && qs.has("sellPrice") ? (long) qs.get("sellPrice").getAsDouble() : 0L);
            if (instasell > 0) bazaar.put(e.getKey(), instasell);   // getBazaarSell() = instasell / buy-order
        }
    }

    private void updateLowestBins() throws Exception {
        String body = get(LOWESTBINS_URL);
        if (body == null) return;
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            try {
                lowestBin.put(e.getKey(), (long) e.getValue().getAsDouble());
            } catch (Exception ignored) { }
        }
    }

    private void updateAvgBins() throws Exception {
        String body = get(AVGBIN_URL);
        if (body == null) return;
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            try {
                avgBin.put(e.getKey(), (long) e.getValue().getAsDouble());
            } catch (Exception ignored) { }
        }
    }

    // Individual price sources for the tooltip features.
    public boolean isBazaarItem(String id) { return id != null && bazaar.containsKey(id); }
    /** Instasell price (what you get selling now) ≈ the competitive buy-order price — the LOWER side. */
    public Long getBazaarSell(String id) { return id == null ? null : bazaar.get(id); }
    /** Instabuy price (what you pay to buy now) — the HIGHER side. */
    public Long getBazaarBuy(String id) { return id == null ? null : bazaarBuy.get(id); }
    public Long getLowestBin(String id) { return id == null ? null : lowestBin.get(id); }
    public Long getAvgBin(String id) { return id == null ? null : avgBin.get(id); }

    private void updateItems() throws Exception {
        String body = get(ITEMS_URL);
        if (body == null) return;
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (!root.has("items")) return;
        for (JsonElement el : root.getAsJsonArray("items")) {
            JsonObject item = el.getAsJsonObject();
            if (!item.has("id")) continue;
            String id = item.get("id").getAsString().replace(':', '-');
            if (item.has("name")) nameToId.put(stripFormatting(item.get("name").getAsString()), id);
            if (item.has("npc_sell_price")) {
                try { npcSell.put(id, item.get("npc_sell_price").getAsLong()); } catch (Exception ignored) { }
            }
        }
    }

    /**
     * Price of one unit of {@code id} for craft-value summing. Bazaar items use Buy Order
     * ({@link #getBazaarSell}) when {@code buyOrder}, else Instabuy ({@link #getBazaarBuy}); Buy Order
     * does not exist for BIN-only items, so those always fall back to lowest BIN / {@link #getPrice}.
     */
    public long componentPrice(String id, boolean buyOrder) {
        if (id == null || id.isEmpty()) return 0L;
        if (isBazaarItem(id)) {
            Long v = buyOrder ? getBazaarSell(id) : getBazaarBuy(id);
            if (v != null) return v;
        }
        Long lb = getLowestBin(id);
        if (lb != null) return lb;
        return getPrice(id);
    }

    /** Bazaar sell price, falling back to lowest BIN, then NPC sell, else 0. */
    public long getPrice(String id) {
        if (id == null || id.isEmpty()) return 0L;
        Long b = bazaar.get(id);
        if (b != null) return b;
        Long lb = lowestBin.get(id);
        if (lb != null) return lb;
        Long npc = npcSell.get(id);
        return npc != null ? npc : 0L;
    }

    /** Resolves a plain (unformatted) item display name to its SkyBlock id, or null. */
    public String idFromName(String name) {
        return name == null ? null : nameToId.get(stripFormatting(name).strip());
    }

    private static String stripFormatting(String s) {
        return s == null ? "" : s.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
    }
}
