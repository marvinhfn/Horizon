package de.horizon.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
}
