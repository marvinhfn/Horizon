package de.horizon.feature.helper;

import de.horizon.config.HorizonConfig;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Experimentation Table helper (Superpairs, Ultrasequencer, Chronomatron).
 *
 * <p>Rendering works by <b>replacing the displayed item stack</b> of a solved slot (via
 * {@link #modifyDisplayStack}, wired into the container-screen slot/tooltip rendering) rather than
 * drawing an icon overlay on top. This means remembered rewards render natively — the placeholder
 * glass is fully gone (no "glass behind the item"), the count/decorations draw correctly, and the
 * vanilla hover tooltip is built from the remembered stack automatically.
 *
 * <p>Chronomatron is driven by a per-frame poll of the live menu slots (in {@link #render}) plus the
 * slot click hook. The client menu never invokes container listeners for server slot updates, so the
 * flash capture must read the current slot contents each frame rather than react to update events.
 */
public final class ExperimentTableSolver {

    private enum Game { NONE, SUPERPAIRS, ULTRASEQUENCER, CHRONOMATRON }

    private enum State { REMEMBER, WAIT, SHOW, END }

    private static final Set<String> PLACEHOLDER_NAMES = Set.of(
        "click any button!", "click a second button!", "?", "");

    private static final int GREEN = 0xFF44FF44;
    private static final int YELLOW = 0xFFFFFF44;
    private static final int RED = 0xFFFF4444;

    private Game game = Game.NONE;
    private State state = State.REMEMBER;
    private String lastTitle = "";
    private boolean active = false;
    private AbstractContainerMenu activeMenu = null;

    /** Board slot index -> the stack to display there (Superpairs rewards / Ultrasequencer numbers). */
    private final Map<Integer, ItemStack> display = new HashMap<>();
    /** Board slot index -> highlight border colour (recomputed each frame). */
    private final Map<Integer, Integer> highlights = new HashMap<>();

    // ── Superpairs ──
    private final Map<Integer, String> pairNames = new HashMap<>();

    // ── Ultrasequencer ──
    private final Map<Integer, Integer> ultraNumbers = new HashMap<>();
    private int ultraNext = 1;

    // ── Chronomatron ──
    private enum ChronoPhase { NONE, READ, REPLICATE }
    private final List<String> chain = new java.util.ArrayList<>();
    private int chronoSeqIndex = 0;   // position while re-reading the replayed prefix
    private int chronoProgress = 0;   // how many the player has correctly clicked back
    private boolean chronHasBeenEmpty = true; // saw an empty (no lit block) frame since last capture
    private int chronoRound = 0;
    private boolean chronoClicked = false; // player started replaying this round → stop capturing flashes
    private ChronoPhase chronoPhase = ChronoPhase.NONE;

    // ── Detection ──────────────────────────────────────────────────────────────

    public boolean isExperimentScreen(String title) {
        return detectGame(title) != Game.NONE;
    }

    private static Game detectGame(String title) {
        if (title == null) return Game.NONE;
        String t = title.toLowerCase(Locale.ROOT);
        if (t.contains("superpairs") || t.contains("super pairs")) return Game.SUPERPAIRS;
        if (t.contains("ultrasequencer") || t.contains("ultra sequencer")) return Game.ULTRASEQUENCER;
        if (t.contains("chronomatron")) return Game.CHRONOMATRON;
        return Game.NONE;
    }

    public void onScreenOpen(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        Game g = detectGame(title);
        if (g == Game.NONE) {
            reset();
            return;
        }
        if (!title.equals(lastTitle) || screen.getMenu() != activeMenu) {
            reset();
            lastTitle = title;
            game = g;
            active = true;
            activeMenu = screen.getMenu();
            // Superpairs has no remember/show handshake — it is always in the "show" state.
            state = g == Game.SUPERPAIRS ? State.SHOW : State.REMEMBER;
        }
    }

    /** True if this screen is the one whose experiment we are currently solving. */
    public boolean isActiveMenu(AbstractContainerScreen<?> screen) {
        return active && screen != null && screen.getMenu() == activeMenu;
    }

    // ── Stack replacement (rendering + tooltips) ─────────────────────────────────

    /**
     * Returns the remembered stack to draw for {@code slotIndex}, or the original stack. Called from
     * the container-screen mixin for both item rendering and the hover tooltip, so a solved slot both
     * shows and tooltips its remembered reward.
     */
    public ItemStack modifyDisplayStack(int slotIndex, ItemStack original) {
        if (!active) return original;
        if (game == Game.SUPERPAIRS) {
            ItemStack shown = display.get(slotIndex);
            return shown != null ? shown : original;
        }
        if (game == Game.ULTRASEQUENCER && state == State.SHOW) {
            ItemStack shown = display.get(slotIndex);
            return shown != null ? shown : original;
        }
        return original;
    }

    // ── Slot clicks (advance Ultrasequencer / Chronomatron) ──────────────────────

    public void onSlotClick(AbstractContainerScreen<?> screen, int slotId, ItemStack stack, int button) {
        if (!isActiveMenu(screen)) return;
        switch (game) {
            case ULTRASEQUENCER -> {
                if (state == State.SHOW && ultraNumbers.getOrDefault(slotId, -1) == ultraNext) ultraNext++;
            }
            case CHRONOMATRON -> {
                if (chronoPhase == ChronoPhase.REPLICATE) {
                    chronoClicked = true; // the player is replaying → stop recording sequence flashes
                    if (chronoProgress < chain.size()) {
                        String key = colorName(stack);
                        if (key != null && key.equals(chain.get(chronoProgress))) chronoProgress++;
                    }
                }
            }
            default -> { }
        }
    }

    // ── Per-frame update + highlight rendering ───────────────────────────────────

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, HorizonConfig config) {
        if (!config.isExperimentSolverEnabled()) return;
        Game g = detectGame(screen.getTitle().getString());
        if (g == Game.NONE) {
            if (active) reset();
            return;
        }
        if (screen.getMenu() != activeMenu) onScreenOpen(screen);

        var menu = screen.getMenu();
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) (Object) screen;
        int left = accessor.getLeftPos();
        int top = accessor.getTopPos();

        Map<Integer, Slot> board = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            board.put(slot.index, slot);
        }

        highlights.clear();
        switch (g) {
            case SUPERPAIRS -> updateSuperpairs(board);
            case ULTRASEQUENCER -> updateUltrasequencer(board);
            case CHRONOMATRON -> updateChronomatron(board);
            default -> { }
        }

        // Draw the highlight borders (after the slot items were already drawn).
        for (Map.Entry<Integer, Integer> e : highlights.entrySet()) {
            Slot slot = board.get(e.getKey());
            if (slot == null) continue;
            drawOutline(ctx, left + slot.x, top + slot.y, e.getValue());
        }
    }

    // ── Superpairs ──────────────────────────────────────────────────────────────

    private void updateSuperpairs(Map<Integer, Slot> board) {
        // Learn every revealed reward (a real, non-placeholder item) and keep it drawn once hidden.
        for (Map.Entry<Integer, Slot> e : board.entrySet()) {
            ItemStack stack = e.getValue().getItem();
            if (stack.isEmpty() || isPlaceholderStack(stack)) continue;
            String name = stack.getHoverName().getString();
            if (PLACEHOLDER_NAMES.contains(name.toLowerCase(Locale.ROOT))) continue;
            pairNames.put(e.getKey(), name);
            display.put(e.getKey(), stack.copy());
        }
        // Outline tiles whose reward has a known partner, sharing a per-reward colour.
        Map<String, List<Integer>> byName = new HashMap<>();
        for (Map.Entry<Integer, String> e : pairNames.entrySet()) {
            byName.computeIfAbsent(e.getValue(), k -> new java.util.ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<Integer, String> e : pairNames.entrySet()) {
            List<Integer> group = byName.get(e.getValue());
            if (group != null && group.size() >= 2) highlights.put(e.getKey(), pairColor(e.getValue()));
        }
    }

    /** A face-down Superpairs tile / border: cyan glass, black pane, or empty. */
    private static boolean isPlaceholderStack(ItemStack stack) {
        if (stack.isEmpty()) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("cyan_stained_glass") || id.equals("black_stained_glass_pane");
    }

    // ── Ultrasequencer ──────────────────────────────────────────────────────────

    private void updateUltrasequencer(Map<Integer, Slot> board) {
        String instr = slot49Name(board);
        switch (state) {
            case REMEMBER -> {
                if (instr.equals("Remember the pattern!")) {
                    ultraNumbers.clear();
                    display.clear();
                    for (Map.Entry<Integer, Slot> e : board.entrySet()) {
                        ItemStack st = e.getValue().getItem();
                        int n = numberOf(st);
                        if (n > 0) {
                            ultraNumbers.put(e.getKey(), n);
                            display.put(e.getKey(), st.copy());
                        }
                    }
                    ultraNext = 1;
                    if (!ultraNumbers.isEmpty()) state = State.WAIT;
                }
            }
            case WAIT -> {
                if (instr.startsWith("Timer: ")) state = State.SHOW;
            }
            case SHOW -> {
                // Highlight the next number green, the following one yellow.
                for (Map.Entry<Integer, Integer> e : ultraNumbers.entrySet()) {
                    if (e.getValue() == ultraNext) highlights.put(e.getKey(), GREEN);
                    else if (e.getValue() == ultraNext + 1) highlights.put(e.getKey(), YELLOW);
                }
                if (!instr.startsWith("Timer: ") && !instr.equals("Remember the pattern!")) reset();
                else if (instr.equals("Remember the pattern!")) state = State.REMEMBER;
            }
            case END -> { }
        }
    }

    /** Ultrasequencer encodes the number as the item's display name ("1".."N"). */
    private static int numberOf(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        String name = stack.getHoverName().getString().trim();
        if (name.isEmpty()) return -1;
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    // ── Chronomatron ────────────────────────────────────────────────────────────

    /**
     * Chronomatron: each round replays the whole remembered sequence then adds one new flash. The
     * flashing button becomes a terracotta block of its colour (only one lit at a time). Slot 49
     * carries the phase ("Remember the pattern!" = read, "Timer: Xs" = replicate) and slot 4 the
     * round number.
     */
    private void updateChronomatron(Map<Integer, Slot> board) {
        int round = parseRound(board);
        if (round > 0) chronoRound = round;
        ChronoPhase phase = readChronoPhase(board);

        if (phase != chronoPhase) {
            if (phase == ChronoPhase.READ) {
                chronoSeqIndex = 0;
                chronHasBeenEmpty = true;
                chronoClicked = false;
                // A fresh round should need `round-1` remembered colours; if we already have at
                // least `round`, the game restarted (win/loss) → drop the stale sequence.
                if (round > 0 && chain.size() >= round) { chain.clear(); chronoProgress = 0; }
            } else if (phase == ChronoPhase.REPLICATE) {
                // Keep chronoSeqIndex + chronoLastLit so a new flash landing right at this transition
                // is still recognised as the round's new element (seqIndex must stay == chain.size()).
                chronoProgress = 0;
            }
            chronoPhase = phase;
        }

        // Capture the whole flashing sequence until the player STARTS replaying (chronoClicked). Don't
        // gate on the round number — that parse is unreliable and once it read low it froze the chain
        // at one element ("only the first is highlighted"). The sequence only auto-flashes during
        // READ (+ a straggler at the READ→REPLICATE flip), so pre-click capture can't over-grow.
        if (phase == ChronoPhase.READ) {
            if (!chronoClicked) captureChrono(board);
        } else if (phase == ChronoPhase.REPLICATE) {
            if (!chronoClicked) captureChrono(board);
            highlightChrono(board);
        }
    }

    /**
     * Records each lit terracotta flash exactly once, using SkyHanni's empty-frame edge detector: a new
     * flash is only counted once a fully-empty frame (no lit block) has been seen since the last one.
     * This correctly captures consecutive same-colour flashes (which pure colour-change detection missed,
     * leaving the chain one element long → "only the first click is highlighted").
     */
    private void captureChrono(Map<Integer, Slot> board) {
        String lit = null;
        for (Slot s : board.values()) {
            ItemStack st = s.getItem();
            if (st.isEmpty() || !isTerracotta(st)) continue;
            String c = colorName(st);
            if (c != null) { lit = c; break; }
        }
        if (lit == null) { chronHasBeenEmpty = true; return; } // between flashes → arm the next capture
        if (!chronHasBeenEmpty) return;   // still the same flash we already recorded this cycle
        chronHasBeenEmpty = false;

        String expected = chronoSeqIndex < chain.size() ? chain.get(chronoSeqIndex) : null;
        if (expected != null && !expected.equals(lit)) return; // diverges from the known prefix
        if (chronoSeqIndex == chain.size()) {
            chain.add(lit);           // genuinely new element (the round's last flash)
            chronoSeqIndex = 0;
            chronoProgress = 0;
        } else {
            chronoSeqIndex++;         // re-seeing an already-known flash
        }
    }

    private void highlightChrono(Map<Integer, Slot> board) {
        String next = chronoProgress < chain.size() ? chain.get(chronoProgress) : null;
        String nextNext = (chronoProgress + 1) < chain.size() ? chain.get(chronoProgress + 1) : null;
        if (next == null && nextNext == null) return;
        for (Map.Entry<Integer, Slot> e : board.entrySet()) {
            String c = colorName(e.getValue().getItem());
            if (c == null) continue;
            if (c.equals(next)) highlights.put(e.getKey(), GREEN);
            else if (c.equals(nextNext)) highlights.put(e.getKey(), YELLOW);
        }
    }

    private static ChronoPhase readChronoPhase(Map<Integer, Slot> board) {
        String s49 = slotName(board, 49);
        if (s49.contains("Remember the pattern")) return ChronoPhase.READ;
        if (s49.contains("Timer:")) return ChronoPhase.REPLICATE;
        return ChronoPhase.NONE;
    }

    private static int parseRound(Map<Integer, Slot> board) {
        String s = slotName(board, 4);
        int i = s.indexOf("Round:");
        if (i < 0) return 0;
        String digits = s.substring(i + 6).replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private static boolean isTerracotta(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.endsWith("_terracotta") || id.equals("stained_hardened_clay");
    }

    /** Colour word (e.g. "Purple") of a stained-glass / terracotta colour button, else null. */
    private static String colorName(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean colorBlock = id.endsWith("_stained_glass") || id.endsWith("_stained_glass_pane")
            || id.endsWith("_terracotta") || id.equals("stained_hardened_clay");
        if (!colorBlock) return null;
        String name = stack.getHoverName().getString().replaceAll("(?i)\\u00a7[0-9a-fk-or]", "").strip();
        return name.isEmpty() ? null : name;
    }

    private static String slotName(Map<Integer, Slot> board, int index) {
        Slot s = board.get(index);
        if (s == null || s.getItem().isEmpty()) return "";
        return s.getItem().getHoverName().getString();
    }

    private static String slot49Name(Map<Integer, Slot> board) {
        return slotName(board, 49);
    }

    // ── Shared helpers ──────────────────────────────────────────────────────────

    private static int pairColor(String name) {
        float hue = (Math.floorMod(name.hashCode(), 360)) / 360f;
        return 0xFF000000 | (Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0xFFFFFF);
    }

    private static void drawOutline(GuiGraphicsExtractor ctx, int x, int y, int color) {
        ctx.fill(x - 1, y - 1, x + 17, y + 1, color);   // top
        ctx.fill(x - 1, y + 15, x + 17, y + 17, color); // bottom
        ctx.fill(x - 1, y - 1, x + 1, y + 17, color);   // left
        ctx.fill(x + 15, y - 1, x + 17, y + 17, color); // right
    }

    public void reset() {
        activeMenu = null;
        game = Game.NONE;
        state = State.REMEMBER;
        active = false;
        lastTitle = "";
        display.clear();
        highlights.clear();
        pairNames.clear();
        ultraNumbers.clear();
        ultraNext = 1;
        chain.clear();
        chronoSeqIndex = 0;
        chronoProgress = 0;
        chronHasBeenEmpty = true;
        chronoClicked = false;
        chronoRound = 0;
        chronoPhase = ChronoPhase.NONE;
    }
}
