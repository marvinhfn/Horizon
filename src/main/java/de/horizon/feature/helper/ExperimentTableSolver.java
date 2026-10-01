package de.horizon.feature.helper;

import de.horizon.config.HorizonConfig;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Experimentation Table helper — Superpairs, Ultrasequencer and Chronomatron.
 *
 * <p>The Chronomatron and Ultrasequencer logic is a faithful port of SkyHanni's
 * {@code ExperimentsAddonsHelper}. Those two games ("addon experiments") are driven by SERVER
 * inventory updates and the round-complete level-up sound, NOT by per-frame polling — so
 * {@link #onInventoryUpdated} (wired to the container set-content/set-slot packets) and
 * {@link #onPlaySound} (wired to the sound packet) do the reading, exactly like SkyHanni's
 * {@code InventoryUpdatedEvent} / {@code PlaySoundEvent}. {@link #render} only draws the next-click
 * highlights from the recovered sequence.
 *
 * <p>Superpairs keeps Horizon's approach: it learns every revealed reward and re-displays it on the
 * face-down tile via {@link #modifyDisplayStack}, so known pairs stay visible with a shared colour.
 */
public final class ExperimentTableSolver {

    private enum Game { NONE, SUPERPAIRS, ULTRASEQUENCER, CHRONOMATRON }
    private enum HelperPhase { READ, REPLICATE }

    private static final int ROUND_STATUS_SLOT = 4;
    private static final int PHASE_STATUS_SLOT = 49;

    private static final Pattern STRIP = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern ROUND_ITEM = Pattern.compile("Round: (\\d+)");
    private static final Pattern REPLICATE_PHASE = Pattern.compile("Timer: \\d+s");
    private static final String READ_PHASE = "Remember the pattern!";

    private static final Set<String> PLACEHOLDER_NAMES = Set.of(
        "click any button!", "click a second button!", "?", "");

    private static final Set<String> LORENZ_COLOR_NAMES = Set.of(
        "BLACK", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA", "DARK_RED", "DARK_PURPLE", "GOLD", "GRAY",
        "DARK_GRAY", "BLUE", "GREEN", "AQUA", "RED", "LIGHT_PURPLE", "YELLOW", "WHITE");

    private static final int GREEN = 0xFF44FF44;  // next click
    private static final int YELLOW = 0xFFFFFF44;  // the click after

    private static final boolean HIGHLIGHT_NEXT_CLICK = true;
    private static final boolean PREVENT_MISCLICKS = true;

    private static final long FAR_PAST = Long.MIN_VALUE / 2;

    private Game game = Game.NONE;
    private String lastTitle = "";
    private boolean active = false;
    private AbstractContainerMenu activeMenu = null;

    // ── Superpairs ──
    private final Map<Integer, ItemStack> display = new HashMap<>();
    private final Map<Integer, String> pairNames = new HashMap<>();

    // ── Addon experiments (Chronomatron / Ultrasequencer) — mirrors SkyHanni state ──
    private HelperPhase currentAddonPhase = null;
    private final List<String> hypixelChronomatronData = new ArrayList<>();
    private final List<String> userChronomatronProgress = new ArrayList<>();
    private final List<Integer> hypixelUltrasequencerData = new ArrayList<>();
    private final List<Integer> userUltrasequencerProgress = new ArrayList<>();
    private final Map<Integer, ItemStack> ultrasequencerDyeMap = new HashMap<>();
    private boolean chronHasBeenEmpty = true;
    private long lastChronomatronSound = FAR_PAST;
    private int chronomatronSequenceIndex = 0;
    private int currentChronomatronRound = 0;
    private int currentUltraSequencerRound = 0;

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

    private boolean inChronomatron() { return game == Game.CHRONOMATRON; }
    private boolean inUltrasequencer() { return game == Game.ULTRASEQUENCER; }
    private boolean inAddon() { return inChronomatron() || inUltrasequencer(); }

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
        }
    }

    /** True if this screen is the one whose experiment we are currently solving. */
    public boolean isActiveMenu(AbstractContainerScreen<?> screen) {
        return active && screen != null && screen.getMenu() == activeMenu;
    }

    // ── Server inventory update (SkyHanni InventoryUpdatedEvent) ─────────────────

    /** Called from the container set-content / set-slot packet hooks (after MC applies them). */
    public void onInventoryUpdated(AbstractContainerMenu menu) {
        if (!active || menu == null || menu != activeMenu || !inAddon()) return;
        Map<Integer, ItemStack> items = boardItems(menu);

        HelperPhase phase = readPhaseOrNull(items);
        if (phase == null) return; // slot 49 not a known phase → keep old phase, don't read
        HelperPhase oldPhase = currentAddonPhase;
        currentAddonPhase = phase;

        if (inChronomatron()) readNextChronomatron(items, oldPhase);
        if (inUltrasequencer()) readUltrasequencer(items);
    }

    /** Called from the sound packet hook — the level-up marks a finished Chronomatron round. */
    public void onPlaySound(String name, float pitch, float volume) {
        if (!inChronomatron()) return;
        if (!"entity.player.levelup".equals(name) || pitch != 1.7619047f || volume != 0.7f) return;
        lastChronomatronSound = System.currentTimeMillis();
    }

    private HelperPhase readPhaseOrNull(Map<Integer, ItemStack> items) {
        String name = slotName(items, PHASE_STATUS_SLOT);
        if (REPLICATE_PHASE.matcher(name).find()) return HelperPhase.REPLICATE;
        if (name.equals(READ_PHASE)) return HelperPhase.READ;
        return null;
    }

    private int readChronomatronRoundOrNull(Map<Integer, ItemStack> items) {
        String name = slotName(items, ROUND_STATUS_SLOT);
        var m = ROUND_ITEM.matcher(name);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private void readNextChronomatron(Map<Integer, ItemStack> items, HelperPhase oldPhase) {
        int round = readChronomatronRoundOrNull(items);
        if (round < 0) return;
        currentChronomatronRound = round;
        int hypixelSizeNow = hypixelChronomatronData.size();
        int userSizeNow = userChronomatronProgress.size();

        List<String> activeColors = new ArrayList<>();
        for (ItemStack st : items.values()) {
            if (!isTerracotta(st)) continue;
            String c = lorenzColorKeyOrNull(st);
            if (c != null && !activeColors.contains(c)) activeColors.add(c);
        }

        if (activeColors.isEmpty()) {
            chronHasBeenEmpty = true;
            return;
        } else if (!chronHasBeenEmpty) {
            return; // still the same flash we already recorded this cycle
        } else {
            chronHasBeenEmpty = false;
        }

        String clickedColor = null;
        for (String itemColor : activeColors) {
            String expected = chronomatronSequenceIndex < hypixelChronomatronData.size()
                    ? hypixelChronomatronData.get(chronomatronSequenceIndex) : null;
            if (expected == null || itemColor.equals(expected)) { clickedColor = itemColor; break; }
        }
        if (clickedColor == null) return;

        boolean shouldReadLastReplicate = oldPhase == HelperPhase.READ || hypixelSizeNow < currentChronomatronRound;
        boolean isReadingReady = oldPhase == null || oldPhase == HelperPhase.READ;
        boolean shouldNotReadYet;
        switch (currentAddonPhase) {
            case REPLICATE -> shouldNotReadYet = !shouldReadLastReplicate;
            case READ -> shouldNotReadYet = !isReadingReady;
            default -> shouldNotReadYet =
                userSizeNow < hypixelSizeNow || (lastChronomatronSound == FAR_PAST && chronomatronSequenceIndex != 0);
        }
        if (shouldNotReadYet) return;

        if (chronomatronSequenceIndex == hypixelSizeNow) {
            hypixelChronomatronData.add(clickedColor);
            lastChronomatronSound = FAR_PAST;
            chronomatronSequenceIndex = 0;
            userChronomatronProgress.clear();
        } else {
            chronomatronSequenceIndex++;
        }
    }

    private record UltraSlot(int sequenceNumber, int slotIndex, ItemStack stack) {}

    private void readUltrasequencer(Map<Integer, ItemStack> items) {
        List<UltraSlot> ordered = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
            ItemStack st = e.getValue();
            String name = colorNameRaw(st);
            if (name.isEmpty()) continue;
            int seq;
            try {
                seq = Integer.parseInt(name);
            } catch (NumberFormatException ignored) {
                continue;
            }
            currentUltraSequencerRound = Math.max(currentUltraSequencerRound, seq);
            ultrasequencerDyeMap.putIfAbsent(seq, st.copy());
            ordered.add(new UltraSlot(seq, e.getKey(), st));
        }
        ordered.sort((a, b) -> Integer.compare(a.sequenceNumber, b.sequenceNumber));

        boolean isOld = currentUltraSequencerRound != ordered.size();
        // SkyHanni uses hypixelChronomatronData.size() here on purpose (≈0 during Ultrasequencer) so the
        // board is RE-READ every inventory update and the order is always current. Do NOT "fix" this to
        // the ultrasequencer list — that locks in the first (often partial) read and highlights the wrong order.
        boolean alreadyKnown = hypixelChronomatronData.size() == ordered.size();
        if (isOld || alreadyKnown) return;

        hypixelUltrasequencerData.clear();
        userUltrasequencerProgress.clear();
        for (UltraSlot s : ordered) hypixelUltrasequencerData.add(s.slotIndex());
    }

    // ── Slot clicks (advance the replicate phase) ───────────────────────────────

    /** @return true if the click should be cancelled (misclick prevention). */
    public boolean onSlotClick(AbstractContainerScreen<?> screen, int slotId, ItemStack stack, int button) {
        if (!isActiveMenu(screen) || !inAddon() || currentAddonPhase != HelperPhase.REPLICATE) return false;
        if (inChronomatron()) return handleChronomatronClick(stack);
        if (inUltrasequencer()) return handleUltrasequencerClick(slotId);
        return false;
    }

    private boolean handleChronomatronClick(ItemStack stack) {
        if (userChronomatronProgress.size() == hypixelChronomatronData.size()) return false;
        String expected = hypixelChronomatronData.get(userChronomatronProgress.size());
        String clicked = lorenzColorKeyOrNull(stack);
        if (clicked == null || !clicked.equals(expected)) return PREVENT_MISCLICKS;
        userChronomatronProgress.add(clicked);
        return false;
    }

    private boolean handleUltrasequencerClick(int slotId) {
        if (userUltrasequencerProgress.size() == hypixelUltrasequencerData.size()) return false;
        int expected = hypixelUltrasequencerData.get(userUltrasequencerProgress.size());
        if (slotId != expected) return PREVENT_MISCLICKS;
        userUltrasequencerProgress.add(slotId);
        return false;
    }

    // ── Stack replacement (glint next chronomatron click, reveal ultrasequencer order) ──

    public ItemStack modifyDisplayStack(int slotIndex, ItemStack original) {
        if (!active) return original;
        if (game == Game.SUPERPAIRS) {
            ItemStack shown = display.get(slotIndex);
            return shown != null ? shown : original;
        }
        if (!HIGHLIGHT_NEXT_CLICK || currentAddonPhase != HelperPhase.REPLICATE) return original;

        if (game == Game.CHRONOMATRON) {
            String next = hypixelChronomatronData.size() > userChronomatronProgress.size()
                    ? hypixelChronomatronData.get(userChronomatronProgress.size()) : null;
            String c = lorenzColorKeyOrNull(original);
            if (next != null && next.equals(c)) {
                ItemStack copy = original.copy();
                copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                return copy;
            }
            return original;
        }
        if (game == Game.ULTRASEQUENCER) {
            int idx = hypixelUltrasequencerData.indexOf(slotIndex);
            if (idx < 0) return original;
            ItemStack dye = ultrasequencerDyeMap.get(idx + 1);
            return dye != null ? dye : original;
        }
        return original;
    }

    // ── Per-frame highlight rendering ───────────────────────────────────────────

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, HorizonConfig config) {
        if (!config.isExperimentSolverEnabled()) return;
        Game g = detectGame(screen.getTitle().getString());
        if (g == Game.NONE) {
            if (active) reset();
            return;
        }
        if (screen.getMenu() != activeMenu) onScreenOpen(screen);

        AbstractContainerMenu menu = screen.getMenu();
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) (Object) screen;
        int left = accessor.getLeftPos();
        int top = accessor.getTopPos();

        Map<Integer, Slot> board = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            board.put(slot.index, slot);
        }

        Map<Integer, Integer> highlights = new HashMap<>();
        switch (g) {
            case SUPERPAIRS -> updateSuperpairs(board, highlights);
            case CHRONOMATRON -> highlightChronomatron(board, highlights);
            case ULTRASEQUENCER -> highlightUltrasequencer(board, highlights);
            default -> { }
        }

        for (Map.Entry<Integer, Integer> e : highlights.entrySet()) {
            Slot slot = board.get(e.getKey());
            if (slot == null) continue;
            drawOutline(ctx, left + slot.x, top + slot.y, e.getValue());
        }
    }

    private void highlightChronomatron(Map<Integer, Slot> board, Map<Integer, Integer> highlights) {
        if (!HIGHLIGHT_NEXT_CLICK || currentAddonPhase != HelperPhase.REPLICATE || currentChronomatronRound < 1) return;
        String next = hypixelChronomatronData.size() > userChronomatronProgress.size()
                ? hypixelChronomatronData.get(userChronomatronProgress.size()) : null;
        String nextNext = hypixelChronomatronData.size() > userChronomatronProgress.size() + 1
                ? hypixelChronomatronData.get(userChronomatronProgress.size() + 1) : null;
        if (next == null && nextNext == null) return;
        for (Map.Entry<Integer, Slot> e : board.entrySet()) {
            String c = lorenzColorKeyOrNull(e.getValue().getItem());
            if (c == null) continue;
            if (c.equals(next)) highlights.put(e.getKey(), GREEN);
            else if (c.equals(nextNext)) highlights.put(e.getKey(), YELLOW);
        }
    }

    private void highlightUltrasequencer(Map<Integer, Slot> board, Map<Integer, Integer> highlights) {
        if (!HIGHLIGHT_NEXT_CLICK || currentAddonPhase != HelperPhase.REPLICATE || currentUltraSequencerRound < 1) return;
        // Remaining slots in sequence order, starting at the next one the user must click.
        List<Integer> remaining = new ArrayList<>();
        for (int i = userUltrasequencerProgress.size(); i < hypixelUltrasequencerData.size(); i++) {
            int slotIdx = hypixelUltrasequencerData.get(i);
            if (board.containsKey(slotIdx)) remaining.add(slotIdx);
        }
        for (int order = 0; order < remaining.size(); order++) {
            int slotIdx = remaining.get(order);
            int color;
            if (order == 0) {
                color = GREEN; // next click, full colour
            } else {
                int alpha = 255 / order; // SkyHanni: addAlpha(255 / slotIndex) falloff
                color = (alpha << 24) | (YELLOW & 0x00FFFFFF);
            }
            highlights.put(slotIdx, color);
        }
    }

    // ── Superpairs (unchanged behaviour: learn + re-show revealed rewards) ───────

    private void updateSuperpairs(Map<Integer, Slot> board, Map<Integer, Integer> highlights) {
        for (Map.Entry<Integer, Slot> e : board.entrySet()) {
            ItemStack stack = e.getValue().getItem();
            if (stack.isEmpty() || isPlaceholderStack(stack)) continue;
            String name = stack.getHoverName().getString();
            if (PLACEHOLDER_NAMES.contains(name.toLowerCase(Locale.ROOT))) continue;
            pairNames.put(e.getKey(), name);
            display.put(e.getKey(), stack.copy());
        }
        Map<String, List<Integer>> byName = new HashMap<>();
        for (Map.Entry<Integer, String> e : pairNames.entrySet()) {
            byName.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (Map.Entry<Integer, String> e : pairNames.entrySet()) {
            List<Integer> group = byName.get(e.getValue());
            if (group != null && group.size() >= 2) highlights.put(e.getKey(), pairColor(e.getValue()));
        }
    }

    private static boolean isPlaceholderStack(ItemStack stack) {
        if (stack.isEmpty()) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("cyan_stained_glass") || id.equals("black_stained_glass_pane");
    }

    // ── Item helpers ─────────────────────────────────────────────────────────────

    private static boolean isTerracotta(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.endsWith("_terracotta") || id.equals("stained_hardened_clay");
    }

    /** SkyHanni's SafeItemStack.getLorenzColorOrNull(): canonical colour key, or null. */
    private static String lorenzColorKeyOrNull(ItemStack stack) {
        String clean = colorNameRaw(stack);
        if (clean.isEmpty()) return null;
        switch (clean) {
            case "Green":  return "DARK_GREEN";
            case "Lime":   return "GREEN";
            case "Pink":   return "LIGHT_PURPLE";
            case "Cyan":   return "DARK_AQUA";
            case "Orange": return "GOLD";
            case "Purple": return "DARK_PURPLE";
            default:
                String up = clean.toUpperCase(Locale.ROOT);
                return LORENZ_COLOR_NAMES.contains(up) ? up : null; // runCatching { valueOf } getOrNull
        }
    }

    private static String colorNameRaw(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return STRIP.matcher(stack.getHoverName().getString()).replaceAll("").strip();
    }

    private static String slotName(Map<Integer, ItemStack> items, int index) {
        ItemStack s = items.get(index);
        if (s == null || s.isEmpty()) return "";
        return STRIP.matcher(s.getHoverName().getString()).replaceAll("").strip();
    }

    private static Map<Integer, ItemStack> boardItems(AbstractContainerMenu menu) {
        Map<Integer, ItemStack> out = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            out.put(slot.index, slot.getItem());
        }
        return out;
    }

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
        active = false;
        lastTitle = "";
        display.clear();
        pairNames.clear();
        currentAddonPhase = null;
        hypixelChronomatronData.clear();
        userChronomatronProgress.clear();
        hypixelUltrasequencerData.clear();
        userUltrasequencerProgress.clear();
        ultrasequencerDyeMap.clear();
        chronHasBeenEmpty = true;
        lastChronomatronSound = FAR_PAST;
        chronomatronSequenceIndex = 0;
        currentChronomatronRound = 0;
        currentUltraSequencerRound = 0;
    }
}
