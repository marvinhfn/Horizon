package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * F7/M7 Storm-phase timers:
 * <ul>
 *   <li><b>PY timer</b> (§5) — 75 ticks after Storm's "ENERGY HEED MY CALL!" / "THUNDER LET ME BE
 *       YOUR CATALYST!", once per Storm phase.</li>
 *   <li><b>Pad timer</b> (§b) — a repeating 20-tick cycle after "Pathetic Maxor, just like
 *       expected." until Storm is defeated.</li>
 * </ul>
 *
 * <p>Anchored to the server game tick ({@code level.getGameTime()}) so it stays aligned under lag.
 * PY takes display priority over Pad.
 */
public final class PurplePadTimerService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern PY_REGEX = Pattern.compile(
        "^\\[BOSS] Storm: (ENERGY HEED MY CALL!|THUNDER LET ME BE YOUR CATALYST!)$");
    private static final String PAD_TRIGGER = "[BOSS] Storm: Pathetic Maxor, just like expected.";
    private static final String STORM_DEFEAT = "[BOSS] Storm: I should have known that I stood no chance.";

    private static final int PY_TICKS = 75;
    private static final int PAD_PERIOD = 20;

    private boolean pyTriggered = false;
    private long pyAnchor = 0L;
    private boolean padActive = false;
    private long padAnchor = 0L;

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    public void handleChatMessage(String rawMessage, HorizonConfig config) {
        String plain = FORMATTING.matcher(rawMessage == null ? "" : rawMessage).replaceAll("").strip();

        // PY (§5) and Pad (§b) are separate timers, each with its own toggle.
        if (config.isPurplePadTimerEnabled() && PY_REGEX.matcher(plain).matches()) {
            if (!pyTriggered) {
                pyTriggered = true;
                pyAnchor = gameTime();
            }
        } else if (config.isPadTimerEnabled() && plain.equals(PAD_TRIGGER)) {
            padActive = true;
            padAnchor = gameTime();
        } else if (plain.equals(STORM_DEFEAT)) {
            reset();
        }
    }

    /** Kept for the tick wiring; the countdowns are computed lazily from game time. */
    public void tick() { }

    private boolean isPyActive() {
        return pyTriggered && (PY_TICKS - (gameTime() - pyAnchor)) > 0;
    }

    public boolean isActive() {
        return isPyActive() || padActive;
    }

    /** Fully coloured display string, e.g. {@code "§5PY: §a3.75s"} or {@code "§bPad: §a1.00s"}. */
    public String getDisplayText() {
        if (isPyActive()) {
            int ticks = (int) (PY_TICKS - (gameTime() - pyAnchor));
            return "§5PY: " + colorForRatio(ticks, PY_TICKS) + formatSeconds(ticks * 0.05f);
        }
        if (padActive) {
            int ticks = (int) (PAD_PERIOD - ((gameTime() - padAnchor) % PAD_PERIOD));
            return "§bPad: " + colorForRatio(ticks, PAD_PERIOD) + formatSeconds(ticks * 0.05f);
        }
        return "";
    }

    private static String colorForRatio(int value, int max) {
        float ratio = max <= 0 ? 0f : (float) value / max;
        if (ratio >= 0.66f) return "§a";
        if (ratio >= 0.33f) return "§6";
        return "§c";
    }

    public static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    public void reset() {
        pyTriggered = false;
        pyAnchor = 0L;
        padActive = false;
        padAnchor = 0L;
    }
}
