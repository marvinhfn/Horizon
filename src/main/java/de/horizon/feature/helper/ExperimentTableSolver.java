package de.horizon.feature.helper;

import de.horizon.config.HorizonConfig;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
 * <ul>
 *   <li><b>Chronomatron</b> is a faithful 1:1 port of Skyblocker's {@code ChronomatronSolver}: a
 *       REMEMBER→WAIT→SHOW→END state machine driven by a vanilla {@link ContainerListener}
 *       ({@link #slotChanged}). The flashing terracotta's enchant <i>foil</i> marks each step of the
 *       growing sequence; the terracotta is mapped to its glass equivalent so the SHOW-phase glass
 *       buttons can be matched + highlighted green.</li>
 *   <li><b>Ultrasequencer</b> reads the numbered board on every server inventory update
 *       ({@link #onInventoryUpdated}) and highlights the remaining order.</li>
 *   <li><b>Superpairs</b> learns every revealed reward and re-shows it on the face-down tile.</li>
 * </ul>
 */
public final class ExperimentTableSolver implements ContainerListener {

    private enum Game { NONE, SUPERPAIRS, ULTRASEQUENCER, CHRONOMATRON }
    private enum HelperPhase { READ, REPLICATE }

    private static final int PHASE_STATUS_SLOT = 49;

    private static final Pattern STRIP = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern REPLICATE_PHASE = Pattern.compile("Timer: \\d+s");
    private static final String READ_PHASE = "Remember the pattern!";

    private static final Set<String> PLACEHOLDER_NAMES = Set.of(
        "click any button!", "click a second button!", "?", "");

    private static final int GREEN = 0xFF44FF44;  // next click
    private static final int YELLOW = 0xFFFFFF44;  // the click after (ultrasequencer only)

    private static final boolean PREVENT_MISCLICKS = true;

    private Game game = Game.NONE;
    private String lastTitle = "";
    private boolean active = false;
    private AbstractContainerMenu activeMenu = null;

    // ── Superpairs ──
    private final Map<Integer, ItemStack> display = new HashMap<>();
    private final Map<Integer, String> pairNames = new HashMap<>();

    // ── Chronomatron (faithful Skyblocker ChronomatronSolver port) ──
    private enum ChronoState { REMEMBER, WAIT, SHOW, END }

    /** Maps the flashing terracotta (REMEMBER phase) to the glass button shown in the SHOW phase. */
    private static final Map<Item, Item> TERRACOTTA_TO_GLASS = Map.ofEntries(
        Map.entry(Items.RED_TERRACOTTA, Items.RED_STAINED_GLASS),
        Map.entry(Items.ORANGE_TERRACOTTA, Items.ORANGE_STAINED_GLASS),
        Map.entry(Items.YELLOW_TERRACOTTA, Items.YELLOW_STAINED_GLASS),
        Map.entry(Items.LIME_TERRACOTTA, Items.LIME_STAINED_GLASS),
        Map.entry(Items.GREEN_TERRACOTTA, Items.GREEN_STAINED_GLASS),
        Map.entry(Items.CYAN_TERRACOTTA, Items.CYAN_STAINED_GLASS),
        Map.entry(Items.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_BLUE_STAINED_GLASS),
        Map.entry(Items.BLUE_TERRACOTTA, Items.BLUE_STAINED_GLASS),
        Map.entry(Items.PURPLE_TERRACOTTA, Items.PURPLE_STAINED_GLASS),
        Map.entry(Items.PINK_TERRACOTTA, Items.PINK_STAINED_GLASS));

    private ChronoState chronoState = ChronoState.REMEMBER;
    private final List<Item> chronomatronSlots = new ArrayList<>();
    private int chronomatronChainLengthCount;
    private int chronomatronCurrentSlot;
    private int chronomatronCurrentOrdinal;
    private boolean isSingleRow;
    private boolean chronoListenerRegistered;

    // ── Ultrasequencer (server inventory-update reading) ──
    private HelperPhase currentAddonPhase = null;
    private final List<Integer> hypixelUltrasequencerData = new ArrayList<>();
    private final List<Integer> userUltrasequencerProgress = new ArrayList<>();
    private final Map<Integer, ItemStack> ultrasequencerDyeMap = new HashMap<>();
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
            if (g == Game.CHRONOMATRON) startChronomatron(screen);
        }
    }

    /** True if this screen is the one whose experiment we are currently solving. */
    public boolean isActiveMenu(AbstractContainerScreen<?> screen) {
        return active && screen != null && screen.getMenu() == activeMenu;
    }

    // ── Chronomatron (Skyblocker port) ───────────────────────────────────────────

    private void startChronomatron(AbstractContainerScreen<?> screen) {
        chronoState = ChronoState.REMEMBER;
        chronomatronSlots.clear();
        chronomatronChainLengthCount = 0;
        chronomatronCurrentSlot = 0;
        chronomatronCurrentOrdinal = 0;
        String title = STRIP.matcher(screen.getTitle().getString()).replaceAll("").strip();
        isSingleRow = title.endsWith("(High)") || title.endsWith("(Grand)") || title.endsWith("(Supreme)");
        if (!chronoListenerRegistered) {
            activeMenu.addSlotListener(this);
            chronoListenerRegistered = true;
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack) {
        if (!active || !inChronomatron() || handler != activeMenu) return;
        if (slotId < 17 || slotId > (isSingleRow ? 25 : 34) && slotId != PHASE_STATUS_SLOT) return;
        switch (chronoState) {
            case REMEMBER -> {
                if (slotId == PHASE_STATUS_SLOT) break;
                if (chronomatronCurrentSlot == 0) {
                    if (stack.hasFoil()) {
                        if (chronomatronSlots.size() <= chronomatronChainLengthCount) {
                            chronomatronSlots.add(TERRACOTTA_TO_GLASS.get(stack.getItem()));
                            chronoState = ChronoState.WAIT;
                        } else {
                            chronomatronChainLengthCount++;
                        }
                        chronomatronCurrentSlot = slotId;
                    }
                } else if (chronomatronCurrentSlot == slotId && !stack.hasFoil()) {
                    chronomatronCurrentSlot = 0;
                }
            }
            case WAIT -> {
                if (slotId == PHASE_STATUS_SLOT && slotNameClean(stack).startsWith("Timer: ")) {
                    chronoState = ChronoState.SHOW;
                }
            }
            case END -> {
                if (slotId == PHASE_STATUS_SLOT) {
                    String name = slotNameClean(stack);
                    if (!name.startsWith("Timer: ")) {
                        if (name.equals(READ_PHASE)) {
                            chronomatronChainLengthCount = 0;
                            chronomatronCurrentOrdinal = 0;
                            chronoState = ChronoState.REMEMBER;
                        } else {
                            resetChronomatron();
                        }
                    }
                }
            }
            default -> { }
        }
    }

    @Override
    public void dataChanged(AbstractContainerMenu handler, int id, int value) { }

    private void resetChronomatron() {
        chronomatronSlots.clear();
        chronomatronChainLengthCount = 0;
        chronomatronCurrentSlot = 0;
        chronomatronCurrentOrdinal = 0;
        chronoState = ChronoState.REMEMBER;
    }

    private void highlightChronomatron(Map<Integer, Slot> board, Map<Integer, Integer> highlights) {
        if (chronoState != ChronoState.SHOW || chronomatronSlots.size() <= chronomatronCurrentOrdinal) return;
        Item target = chronomatronSlots.get(chronomatronCurrentOrdinal);
        for (Map.Entry<Integer, Slot> e : board.entrySet()) {
            ItemStack st = e.getValue().getItem();
            if (st.is(target) || TERRACOTTA_TO_GLASS.get(st.getItem()) == target) {
                highlights.put(e.getKey(), GREEN);
            }
        }
    }

    private boolean handleChronomatronClick(ItemStack stack) {
        if (chronoState != ChronoState.SHOW || chronomatronSlots.size() <= chronomatronCurrentOrdinal) return false;
        Item target = chronomatronSlots.get(chronomatronCurrentOrdinal);
        if (stack.is(target) || TERRACOTTA_TO_GLASS.get(stack.getItem()) == target) {
            if (++chronomatronCurrentOrdinal >= chronomatronSlots.size()) chronoState = ChronoState.END;
            return false;
        }
        return PREVENT_MISCLICKS;
    }

    // ── Ultrasequencer (server inventory update reading) ─────────────────────────

    /** Called from the container set-content / set-slot packet hooks (after MC applies them). */
    public void onInventoryUpdated(AbstractContainerMenu menu) {
        if (!active || menu == null || menu != activeMenu || !inUltrasequencer()) return;
        Map<Integer, ItemStack> items = boardItems(menu);
        HelperPhase phase = readPhaseOrNull(items);
        if (phase == null) return;
        currentAddonPhase = phase;
        readUltrasequencer(items);
    }

    /** Retained for the sound-packet hook; Chronomatron no longer needs it (Skyblocker is event-driven). */
    public void onPlaySound(String name, float pitch, float volume) { }

    private HelperPhase readPhaseOrNull(Map<Integer, ItemStack> items) {
        String name = slotName(items, PHASE_STATUS_SLOT);
        if (REPLICATE_PHASE.matcher(name).find()) return HelperPhase.REPLICATE;
        if (name.equals(READ_PHASE)) return HelperPhase.READ;
        return null;
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
        boolean alreadyKnown = hypixelUltrasequencerData.size() == ordered.size() && !ordered.isEmpty()
            && containsSameSlots(ordered);
        if (isOld || alreadyKnown) return;

        hypixelUltrasequencerData.clear();
        userUltrasequencerProgress.clear();
        for (UltraSlot s : ordered) hypixelUltrasequencerData.add(s.slotIndex());
    }

    private boolean containsSameSlots(List<UltraSlot> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            if (i >= hypixelUltrasequencerData.size() || hypixelUltrasequencerData.get(i) != ordered.get(i).slotIndex()) {
                return false;
            }
        }
        return true;
    }

    private void highlightUltrasequencer(Map<Integer, Slot> board, Map<Integer, Integer> highlights) {
        if (currentAddonPhase != HelperPhase.REPLICATE || currentUltraSequencerRound < 1) return;
        List<Integer> remaining = new ArrayList<>();
        for (int i = userUltrasequencerProgress.size(); i < hypixelUltrasequencerData.size(); i++) {
            int slotIdx = hypixelUltrasequencerData.get(i);
            if (board.containsKey(slotIdx)) remaining.add(slotIdx);
        }
        for (int order = 0; order < remaining.size(); order++) {
            int slotIdx = remaining.get(order);
            int color = order == 0 ? GREEN : (Math.max(1, 255 / order) << 24) | (YELLOW & 0x00FFFFFF);
            highlights.put(slotIdx, color);
        }
    }

    // ── Slot clicks ──────────────────────────────────────────────────────────────

    /** @return true if the click should be cancelled (misclick prevention). */
    public boolean onSlotClick(AbstractContainerScreen<?> screen, int slotId, ItemStack stack, int button) {
        if (!isActiveMenu(screen)) return false;
        if (inChronomatron()) return handleChronomatronClick(stack);
        if (inUltrasequencer() && currentAddonPhase == HelperPhase.REPLICATE) return handleUltrasequencerClick(slotId);
        return false;
    }

    private boolean handleUltrasequencerClick(int slotId) {
        if (userUltrasequencerProgress.size() == hypixelUltrasequencerData.size()) return false;
        int expected = hypixelUltrasequencerData.get(userUltrasequencerProgress.size());
        if (slotId != expected) return PREVENT_MISCLICKS;
        userUltrasequencerProgress.add(slotId);
        return false;
    }

    // ── Stack replacement (superpairs re-show + ultrasequencer order reveal) ──────

    public ItemStack modifyDisplayStack(int slotIndex, ItemStack original) {
        if (!active) return original;
        if (game == Game.SUPERPAIRS) {
            ItemStack shown = display.get(slotIndex);
            return shown != null ? shown : original;
        }
        if (game == Game.ULTRASEQUENCER && currentAddonPhase == HelperPhase.REPLICATE) {
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

    // ── Superpairs (learn + re-show revealed rewards) ────────────────────────────

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

    private static String colorNameRaw(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return STRIP.matcher(stack.getHoverName().getString()).replaceAll("").strip();
    }

    private static String slotNameClean(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
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
        if (chronoListenerRegistered && activeMenu != null) {
            activeMenu.removeSlotListener(this);
        }
        chronoListenerRegistered = false;
        activeMenu = null;
        game = Game.NONE;
        active = false;
        lastTitle = "";
        display.clear();
        pairNames.clear();
        resetChronomatron();
        currentAddonPhase = null;
        hypixelUltrasequencerData.clear();
        userUltrasequencerProgress.clear();
        ultrasequencerDyeMap.clear();
        currentUltraSequencerRound = 0;
    }
}
