package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Goldor Damage Tick Timer.
 * Counts down to the next damage tick during Goldor (F7 P3).
 * Also shows a pre-Goldor countdown after Storm's defeat.
 */
public final class TickTimerService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    private boolean preGoldor = false;
    private boolean inGoldor = false;
    private int ticksUntil = 0;
    private int preGoldorTicks = 0;

    /** Called every server tick. */
    public void tick(DungeonStateService dungeonState) {
        if (preGoldor) {
            if (preGoldorTicks > 0) preGoldorTicks--;
            return;
        }
        if (!inGoldor) return;
        ticksUntil = ticksUntil > 1 ? ticksUntil - 1 : 60;
    }

    /**
     * Called for every received chat message.
     */
    public void handleChatMessage(String rawMessage, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isTickTimerEnabled()) return;

        String plain = FORMATTING.matcher(rawMessage == null ? "" : rawMessage).replaceAll("").strip();

        if (plain.equals("[BOSS] Goldor: Who dares trespass into my domain?")) {
            inGoldor = true;
            preGoldor = false;
            ticksUntil = 60;
            preGoldorTicks = 0;
        } else if (plain.equals("[BOSS] Storm: I should have known that I stood no chance.")) {
            preGoldor = true;
            preGoldorTicks = 100;
        } else if (plain.equals("The Core entrance is opening!")) {
            inGoldor = false;
        }
    }

    public boolean isActive() {
        return (preGoldor && preGoldorTicks > 0) || inGoldor;
    }

    public boolean isPreGoldor() {
        return preGoldor && preGoldorTicks > 0;
    }

    /** Current countdown in seconds (2 decimal places). */
    public float getSecondsRemaining() {
        if (preGoldor && preGoldorTicks > 0) return preGoldorTicks * 0.05f;
        if (inGoldor) return ticksUntil * 0.05f;
        return 0;
    }

    public int getTicksUntil() {
        if (preGoldor) return preGoldorTicks;
        return ticksUntil;
    }

    public int getMaxTicks() {
        if (preGoldor) return 100;
        return 60;
    }

    public static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    public void reset() {
        preGoldor = false;
        inGoldor = false;
        ticksUntil = 0;
        preGoldorTicks = 0;
    }
}
