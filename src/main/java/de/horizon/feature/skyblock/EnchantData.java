package de.horizon.feature.skyblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Static table of Hypixel SkyBlock enchant max levels + lore display names, loaded once from
 * {@code /assets/horizon/enchants.json}. Used by the maxed-enchant gradient to decide which enchants
 * are at (or above) their cap and to build the exact lore token ({@code "<name> <roman>"}) to match.
 */
public final class EnchantData {
    private EnchantData() {}

    private record Entry(String name, int max) {}

    private static final Map<String, Entry> ENCHANTS = new HashMap<>();
    private static volatile boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream is = EnchantData.class.getResourceAsStream("/assets/horizon/enchants.json")) {
            if (is == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject enchants = root.getAsJsonObject("enchants");
            for (String key : enchants.keySet()) {
                JsonObject e = enchants.getAsJsonObject(key);
                ENCHANTS.put(key.toLowerCase(), new Entry(e.get("name").getAsString(), e.get("max").getAsInt()));
            }
        } catch (Exception ignored) { }
    }

    /** True when {@code level} meets or exceeds the known max for {@code id} (unknown ids never max). */
    public static boolean isMaxed(String id, int level) {
        if (id == null) return false;
        Entry e = ENCHANTS.get(id.toLowerCase());
        return e != null && level >= e.max;
    }

    /** Lore display name for an enchant id, or null when unknown. */
    public static String displayName(String id) {
        if (id == null) return null;
        Entry e = ENCHANTS.get(id.toLowerCase());
        return e == null ? null : e.name;
    }

    /** The exact lore token Hypixel renders for a maxed enchant, e.g. {@code "Sharpness VII"}, or null. */
    public static String loreToken(String id, int level) {
        String name = displayName(id);
        return name == null ? null : name + " " + toRoman(level);
    }

    public static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> Integer.toString(n);
        };
    }
}
