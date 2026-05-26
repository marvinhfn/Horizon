package de.horizon.feature.revive;

import de.horizon.config.HorizonConfig;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ReviveTracker {
    private static final int TICKS_PER_SECOND = 20;
    private final Map<ReviveSource, Integer> remainingTicks = new EnumMap<>(ReviveSource.class);

    private static final java.util.regex.Pattern FORMATTING = java.util.regex.Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    public void handleChatMessage(String rawMessage, HorizonConfig config) {
        String normalized = FORMATTING.matcher(rawMessage == null ? "" : rawMessage)
            .replaceAll("")
            .strip()
            .toLowerCase(Locale.ROOT);
        for (ReviveSource source : ReviveSource.values()) {
            for (String trigger : source.chatTriggers()) {
                if (normalized.contains(trigger)) {
                    trigger(source, config);
                    return;
                }
            }
        }
    }

    public void trigger(ReviveSource source, HorizonConfig config) {
        remainingTicks.put(source, source.cooldownSeconds(config) * TICKS_PER_SECOND);
    }

    public void tick() {
        for (ReviveSource source : ReviveSource.values()) {
            int remaining = remainingTicks.getOrDefault(source, 0);
            if (remaining > 0) {
                remainingTicks.put(source, remaining - 1);
            }
        }
    }

    public boolean isReady(ReviveSource source) {
        return getRemainingTicks(source) <= 0;
    }

    public int getRemainingTicks(ReviveSource source) {
        return remainingTicks.getOrDefault(source, 0);
    }

    public String getRemainingText(ReviveSource source) {
        int remaining = Math.max(0, getRemainingTicks(source));
        int totalSeconds = (remaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format(Locale.ROOT, "%02d", seconds);
    }
}
