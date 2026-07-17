package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Purple Pad Timer for F7 Phase 2 (Storm).
 * Counts down 96 ticks (4.8s) after Storm's energy call — signals when to step on purple pad.
 */
public final class PurplePadTimerService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");
    private static final Pattern STORM_REGEX = Pattern.compile(
        "^\\[BOSS] Storm: (ENERGY HEED MY CALL!|THUNDER LET ME BE YOUR CATALYST!)$"
    );

    private boolean triggered = false;
    private int ticksRemaining = 0;

    public void handleChatMessage(String rawMessage, HorizonConfig config) {
        if (!config.isPurplePadTimerEnabled()) return;
        String plain = FORMATTING.matcher(rawMessage == null ? "" : rawMessage).replaceAll("").strip();
        if (!triggered && STORM_REGEX.matcher(plain).matches()) {
            ticksRemaining = 96;
            triggered = true;
        }
    }

    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public boolean isActive() {
        return ticksRemaining > 0;
    }

    public float getSecondsRemaining() {
        return ticksRemaining * 0.05f;
    }

    public static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    public void reset() {
        triggered = false;
        ticksRemaining = 0;
    }
}
