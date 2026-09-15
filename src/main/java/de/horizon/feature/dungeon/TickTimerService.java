package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * F7/M7 boss server-tick timers: the per-phase "start" countdowns (Maxor / Storm / Goldor / Necron)
 * and the repeating Goldor damage-tick cycle.
 *
 * <p>All timing is anchored to the server <b>game tick</b> ({@code level.getGameTime()}), not the
 * client tick loop — the client keeps ticking at 20/s during server lag, which made a per-client-tick
 * counter drift out of sync with the real (server-side) damage ticks. Game time advances with server
 * ticks and is periodically corrected by the server, so the countdowns stay aligned under lag.
 */
public final class TickTimerService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");

    /** Repeating Goldor damage-tick period (every 60 ticks = 3s). */
    private static final int GOLDOR_PERIOD = 60;

    // Per-phase "start" countdown lengths (server ticks), keyed by the boss line that triggers them.
    private static final int MAXOR_START  = 167; // P1: Maxor greeting
    private static final int STORM_START  = 120; // P2: Maxor "I'M TOO YOUNG TO DIE AGAIN!"
    private static final int GOLDOR_START = 104; // P3: Storm defeated
    private static final int NECRON_START = 60;  // P4: Necron "your journey ends now"

    private String startLabel = null;
    private int startTicks = 0;
    private long startAnchor = 0L;

    private boolean goldorCycle = false;
    private long goldorAnchor = 0L;

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    /** Kept for the tick wiring; all values are computed lazily from game time. */
    public void tick(DungeonStateService dungeonState) { }

    public void handleChatMessage(String rawMessage, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isTickTimerEnabled()) return;
        String plain = FORMATTING.matcher(rawMessage == null ? "" : rawMessage).replaceAll("").strip();

        switch (plain) {
            case "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> { if (config.isTickTimerMaxor()) startPhase("Maxor", MAXOR_START); }
            case "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!"       -> { if (config.isTickTimerStorm()) startPhase("Storm", STORM_START); }
            case "[BOSS] Storm: I should have known that I stood no chance." -> { if (config.isTickTimerGoldor()) startPhase("Goldor", GOLDOR_START); }
            case "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                if (config.isTickTimerGoldor()) {
                    goldorCycle = true;
                    goldorAnchor = gameTime();
                }
            }
            case "[BOSS] Necron: I'm afraid, your journey ends now." -> { if (config.isTickTimerNecron()) startPhase("Necron", NECRON_START); }
            case "The Core entrance is opening!" -> goldorCycle = false;
            default -> { }
        }
    }

    private void startPhase(String label, int ticks) {
        startLabel = label;
        startTicks = ticks;
        startAnchor = gameTime();
    }

    private boolean isStartActive() {
        return startLabel != null && (gameTime() - startAnchor) < startTicks;
    }

    public boolean isActive() {
        return isStartActive() || goldorCycle;
    }

    /** Remaining ticks of the currently-shown timer (start countdown has priority over the Goldor cycle). */
    public int getTicksUntil() {
        if (isStartActive()) return (int) (startTicks - (gameTime() - startAnchor));
        if (goldorCycle) return (int) (GOLDOR_PERIOD - ((gameTime() - goldorAnchor) % GOLDOR_PERIOD));
        return 0;
    }

    public int getMaxTicks() {
        if (isStartActive()) return startTicks;
        return GOLDOR_PERIOD;
    }

    public float getSecondsRemaining() {
        return getTicksUntil() * 0.05f;
    }

    /** Fully coloured display string, e.g. {@code "§7Goldor: §a2.85s"}. */
    public String getDisplayText() {
        if (isStartActive()) {
            return "§b" + startLabel + ": " + colorForRatio(getTicksUntil(), startTicks)
                + formatSeconds(getSecondsRemaining());
        }
        if (goldorCycle) {
            int ticks = getTicksUntil();
            return "§7Goldor: " + colorForRatio(ticks, GOLDOR_PERIOD) + formatSeconds(ticks * 0.05f);
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
        startLabel = null;
        startTicks = 0;
        startAnchor = 0L;
        goldorCycle = false;
        goldorAnchor = 0L;
    }
}
