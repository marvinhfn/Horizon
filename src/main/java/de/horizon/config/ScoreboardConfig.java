package de.horizon.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScoreboardConfig {
    private static final Map<String, LinkedHashMap<String, String>> ISLAND_DEFAULTS;
    static {
        Map<String, LinkedHashMap<String, String>> m = new LinkedHashMap<>();
        m.put("hub",             defaults(
            "location","Location (⏣)", "purse","Purse", "bits","Bits",
            "profile","Profile", "skills","Skills",
            "time","Time", "season","Season", "server_code","Date",
            "slayer quest","Slayer Quest", "combat exp","Slayer Quest Combat EXP", "next tier","Slayer Quest Next Tier",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("dungeons",        defaults(
            "location","Location (⏣)",
            "time elapsed","Time Elapsed", "score","Score", "cleared","Cleared",
            "secrets found","Secrets Found", "crypts","Crypts", "deaths","Deaths", "kills","Kills",
            "archer","Archer", "mage","Mage", "tank","Tank", "berserk","Berserk", "healer","Healer",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("garden",          defaults(
            "location","Location (⏣)", "purse","Purse", "bits","Bits",
            "plot","Plot", "time","Time", "season","Season",
            "profile","Profile", "server_code","Date", "www.hypixel.net","www.hypixel.net"
        ));
        m.put("dwarven_mines",   defaults(
            "location","Location (⏣)", "purse","Purse",
            "commission","Commission", "mithril powder","Mithril Powder", "gemstone powder","Gemstone Powder",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("crystal_hollows", defaults(
            "location","Location (⏣)", "purse","Purse",
            "mithril powder","Mithril Powder", "gemstone powder","Gemstone Powder", "kills","Kills",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("crimson_isle",    defaults(
            "location","Location (⏣)", "purse","Purse", "motes","Motes",
            "slayer quest","Slayer Quest", "combat exp","Slayer Quest Combat EXP", "next tier","Slayer Quest Next Tier",
            "kills","Kills",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("farming_islands", defaults(
            "location","Location (⏣)", "purse","Purse", "bits","Bits",
            "time","Time", "season","Season",
            "profile","Profile", "server_code","Date", "www.hypixel.net","www.hypixel.net"
        ));
        m.put("rift",            defaults(
            "location","Location (⏣)", "motes","Motes", "rift time","Rift Time", "lifetime","Lifetime",
            "slayer quest","Slayer Quest", "combat exp","Slayer Quest Combat EXP", "next tier","Slayer Quest Next Tier",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("spiders_den",     defaults(
            "location","Location (⏣)", "purse","Purse",
            "slayer quest","Slayer Quest", "combat exp","Slayer Quest Combat EXP", "next tier","Slayer Quest Next Tier",
            "kills","Kills",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        m.put("end",             defaults(
            "location","Location (⏣)", "purse","Purse",
            "slayer quest","Slayer Quest", "combat exp","Slayer Quest Combat EXP", "next tier","Slayer Quest Next Tier",
            "kills","Kills",
            "profile","Profile", "time","Time", "season","Season", "server_code","Date",
            "www.hypixel.net","www.hypixel.net"
        ));
        ISLAND_DEFAULTS = Collections.unmodifiableMap(m);
    }

    private static LinkedHashMap<String, String> defaults(String... pairs) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    boolean customScoreboardEnabled = true;
    Map<String, Set<String>> scoreboardHiddenKeys = new HashMap<>();
    Map<String, Map<String, String>> scoreboardKnownLines = new HashMap<>();
    Set<String> scoreboardGlobalHiddenKeys = new HashSet<>();
    /** Per-island keys that explicitly override a global-hidden state (show on this island). */
    Map<String, Set<String>> scoreboardIslandShownKeys = new HashMap<>();

    Map<String, String> getKnownLines(String islandId) {
        if (scoreboardKnownLines == null) return new LinkedHashMap<>();
        Map<String, String> stored = scoreboardKnownLines.get(islandId);
        return stored != null ? stored : new LinkedHashMap<>();
    }

    /**
     * Returns true when the line should be hidden for the given island,
     * considering global hidden state and per-island overrides.
     * An island-specific "show" entry overrides a global hidden setting.
     */
    boolean isLineEffectivelyHidden(String islandId, String lineKey) {
        if (scoreboardIslandShownKeys != null) {
            Set<String> shown = scoreboardIslandShownKeys.get(islandId);
            if (shown != null && shown.contains(lineKey)) return false;
        }
        if (scoreboardGlobalHiddenKeys != null && scoreboardGlobalHiddenKeys.contains(lineKey)) return true;
        if (scoreboardHiddenKeys != null) {
            Set<String> hidden = scoreboardHiddenKeys.get(islandId);
            if (hidden != null && hidden.contains(lineKey)) return true;
        }
        return false;
    }

    void toggleLine(String islandId, String lineKey) {
        boolean globallyHidden = scoreboardGlobalHiddenKeys != null
            && scoreboardGlobalHiddenKeys.contains(lineKey);
        if (globallyHidden) {
            // Toggle per-island show-override (un-hide / re-hide from global hidden)
            if (scoreboardIslandShownKeys == null) scoreboardIslandShownKeys = new HashMap<>();
            Set<String> shown = scoreboardIslandShownKeys.computeIfAbsent(islandId, k -> new HashSet<>());
            if (!shown.remove(lineKey)) {
                shown.add(lineKey);
            }
            // Ensure not also in island-hidden
            if (scoreboardHiddenKeys != null) {
                Set<String> hidden = scoreboardHiddenKeys.get(islandId);
                if (hidden != null) hidden.remove(lineKey);
            }
        } else {
            // Toggle island-specific hidden
            if (scoreboardHiddenKeys == null) scoreboardHiddenKeys = new HashMap<>();
            Set<String> hidden = scoreboardHiddenKeys.computeIfAbsent(islandId, k -> new HashSet<>());
            if (!hidden.remove(lineKey)) {
                hidden.add(lineKey);
            }
            // Ensure not also in island-shown
            if (scoreboardIslandShownKeys != null) {
                Set<String> shown = scoreboardIslandShownKeys.get(islandId);
                if (shown != null) shown.remove(lineKey);
            }
        }
    }

    boolean isGlobalLineHidden(String lineKey) {
        return scoreboardGlobalHiddenKeys != null && scoreboardGlobalHiddenKeys.contains(lineKey);
    }

    void toggleGlobalLine(String lineKey) {
        if (scoreboardGlobalHiddenKeys == null) scoreboardGlobalHiddenKeys = new HashSet<>();
        if (!scoreboardGlobalHiddenKeys.remove(lineKey)) {
            scoreboardGlobalHiddenKeys.add(lineKey);
        }
    }

    /**
     * Initialises all islands with their static predefined line keys.
     * Removes any keys not in the predefined set (cleanup from old dynamic recording).
     * Preserves the user's drag-and-drop order for keys that already exist.
     * Call this once after loading the config.
     */
    void ensureIslandDefaults() {
        if (scoreboardKnownLines == null) scoreboardKnownLines = new HashMap<>();
        // Remove entries for islands that no longer have a tab (e.g. old glacite_tunnels, kuudra)
        scoreboardKnownLines.keySet().retainAll(ISLAND_DEFAULTS.keySet());
        for (Map.Entry<String, LinkedHashMap<String, String>> e : ISLAND_DEFAULTS.entrySet()) {
            String islandId = e.getKey();
            Map<String, String> defaults = e.getValue();
            // GSON may deserialize inner maps as LinkedTreeMap (alphabetically sorted).
            // Always work with a LinkedHashMap to preserve insertion/drag-drop order.
            Map<String, String> raw = scoreboardKnownLines.get(islandId);
            LinkedHashMap<String, String> known;
            if (raw instanceof LinkedHashMap) {
                known = (LinkedHashMap<String, String>) raw;
            } else if (raw != null) {
                known = new LinkedHashMap<>(raw);
                scoreboardKnownLines.put(islandId, known);
            } else {
                known = new LinkedHashMap<>();
                scoreboardKnownLines.put(islandId, known);
            }
            // Remove non-predefined keys (pollution from old dynamic line recording)
            known.keySet().retainAll(defaults.keySet());
            // Add missing predefined keys at the end, preserving user order for existing ones
            for (Map.Entry<String, String> d : defaults.entrySet()) {
                known.putIfAbsent(d.getKey(), d.getValue());
            }
        }
    }

    void reorderLine(String islandId, String key, int newIndex) {
        Map<String, String> known = scoreboardKnownLines.get(islandId);
        if (known == null) {
            return;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(known.entrySet());
        int oldIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(key)) {
                oldIndex = i;
                break;
            }
        }
        if (oldIndex < 0) {
            return;
        }
        Map.Entry<String, String> moved = entries.remove(oldIndex);
        int target = Math.max(0, Math.min(newIndex, entries.size()));
        entries.add(target, moved);
        known.clear();
        for (Map.Entry<String, String> e : entries) {
            known.put(e.getKey(), e.getValue());
        }
    }
}
