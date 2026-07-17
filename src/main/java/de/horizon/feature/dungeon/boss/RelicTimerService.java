package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Relic Timer for M7 P5.
 * Triggers on "[BOSS] Necron: All this, for nothing..." and counts down 45 ticks (2.25s)
 * until relics spawn.
 */
public final class RelicTimerService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");
    private static final String NECRON_TRIGGER = "[boss] necron: all this, for nothing...";
    private static final int SPAWN_TICKS = 45;

    private int ticksRemaining = -1;

    public void handleChatMessage(String rawMessage, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isRelicTimerEnabled()) return;
        if (rawMessage == null) return;
        if (!dungeonState.isInBoss() || !dungeonState.isF7()) return;

        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip().toLowerCase(Locale.ROOT);
        if (plain.contains(NECRON_TRIGGER)) {
            ticksRemaining = SPAWN_TICKS;
        }
    }

    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        } else if (ticksRemaining == 0) {
            ticksRemaining = -1;
        }
    }

    public boolean isActive() {
        return ticksRemaining > 0;
    }

    public float getSecondsRemaining() {
        return ticksRemaining > 0 ? ticksRemaining * 0.05f : 0f;
    }

    public void reset() {
        ticksRemaining = -1;
    }
}
