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

    Map<String, String> getKnownLines(String islandId) {
        return scoreboardKnownLines.getOrDefault(islandId, new LinkedHashMap<>());
    }

    void recordLines(String islandId, java.util.List<String> lines) {
        Map<String, String> known = scoreboardKnownLines.computeIfAbsent(islandId, k -> new LinkedHashMap<>());
        for (String line : lines) {
            String key = HorizonConfig.scoreboardLineKey(line);
            if (!key.isBlank()) {
                known.put(key, line);
            }
        }
    }

    boolean isLineHidden(String islandId, String lineKey) {
        Set<String> hidden = scoreboardHiddenKeys.get(islandId);
        return hidden != null && hidden.contains(lineKey);
    }

    void toggleLine(String islandId, String lineKey) {
        Set<String> hidden = scoreboardHiddenKeys.computeIfAbsent(islandId, k -> new HashSet<>());
        if (!hidden.remove(lineKey)) {
            hidden.add(lineKey);
        }
    }

    boolean isGlobalLineHidden(String lineKey) {
        return scoreboardGlobalHiddenKeys.contains(lineKey);
    }

    void toggleGlobalLine(String lineKey) {
        if (!scoreboardGlobalHiddenKeys.remove(lineKey)) {
            scoreboardGlobalHiddenKeys.add(lineKey);
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
