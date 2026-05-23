package de.horizon.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScoreboardConfig {
    boolean customScoreboardEnabled = true;
    Map<String, Set<String>> scoreboardHiddenKeys = new HashMap<>();
    Map<String, Map<String, String>> scoreboardKnownLines = new HashMap<>();
    Set<String> scoreboardGlobalHiddenKeys = new HashSet<>();
    /** Per-island keys that explicitly override a global-hidden state (show on this island). */
    Map<String, Set<String>> scoreboardIslandShownKeys = new HashMap<>();

    Map<String, String> getKnownLines(String islandId) {
        Map<String, String> stored = scoreboardKnownLines != null
            ? scoreboardKnownLines.get(islandId) : null;
        if (stored == null) return new LinkedHashMap<>();
        // Re-normalize keys from their stored values to deduplicate stale entries
        // (e.g. "plot - 2" / "plot - 3" all normalize to "plot").
        Map<String, String> deduped = new LinkedHashMap<>();
        for (String value : stored.values()) {
            if (value == null || value.isBlank()) continue;
            String key = HorizonConfig.scoreboardLineKey(value);
            if (!key.isBlank()) {
                deduped.putIfAbsent(key, value);
            }
        }
        if (deduped.size() != stored.size() || !deduped.keySet().equals(stored.keySet())) {
            stored.clear();
            stored.putAll(deduped);
        }
        return stored;
    }

    void recordLines(String islandId, java.util.List<String> lines) {
        if (scoreboardKnownLines == null) scoreboardKnownLines = new HashMap<>();
        Map<String, String> known = scoreboardKnownLines.computeIfAbsent(islandId, k -> new LinkedHashMap<>());
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            String key = HorizonConfig.scoreboardLineKey(line);
            if (!key.isBlank()) {
                // Store the stable display label, not the raw dynamic line text.
                // putIfAbsent preserves the user's drag-and-drop order for new keys.
                known.putIfAbsent(key, HorizonConfig.formatScoreboardKeyLabel(key));
            }
        }
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
     * Ensures the dungeons island always has one stable entry per class.
     * Uses the key itself as stored value so that the deduplication in
     * {@link #getKnownLines(String)} never remaps the key to a different string.
     * Call this once after loading the config.
     */
    void ensureDungeonClassEntries() {
        if (scoreboardKnownLines == null) scoreboardKnownLines = new HashMap<>();
        Map<String, String> dungeonLines = scoreboardKnownLines.computeIfAbsent("dungeons", k -> new LinkedHashMap<>());
        dungeonLines.putIfAbsent("archer",  "Archer");
        dungeonLines.putIfAbsent("mage",    "Mage");
        dungeonLines.putIfAbsent("tank",    "Tank");
        // Store as "Berserk" so scoreboardLineKey("Berserk") → "berserk" (stable round-trip)
        dungeonLines.putIfAbsent("berserk", "Berserk");
        dungeonLines.putIfAbsent("healer",  "Healer");
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
