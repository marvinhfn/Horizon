package de.horizon.feature.dungeon.terminal;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Floor-7 terminal solver (custom overlay). Detects the terminal by its screen title,
 * computes the required clicks per type ({@link #solve()}), and draws a self-contained "custom"
 * overlay (title + background + border + coloured slots) in place of the vanilla chest. Clicks on
 * the overlay are translated to server container clicks; every other click on the screen is
 * swallowed so misclicks can never reach the server.
 */
public final class TerminalSolverService {

    public enum TerminalType {
        NONE("", 0),
        PANES("Panes", 45),         // "Correct all the panes!"  (red -> green)
        ORDER("Order", 36),         // "Click in order!"          (1..14 numbers)
        SAME_COLOR("Same Color", 45),// "Change all to same color!" (rubix)
        ITEM_NAME("Item Name", 45), // "What starts with: 'x'?"
        COLOURED_ITEMS("Coloured Items", 54), // "Select all the <colour> items!"
        MELODY("Melody", 54);       // "Click the button on time!"

        private final String label;
        private final int slotCount;
        TerminalType(String label, int slotCount) { this.label = label; this.slotCount = slotCount; }
        public String label() { return label; }
        public int slotCount() { return slotCount; }
    }

    /** A single click the solver wants performed: {@code btn} is the rubix direction (0 = fwd, 1 = back). */
    private record TerminalClick(int slotId, int btn) {
        TerminalClick(int slotId) { this(slotId, 0); }
    }

    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("^What starts with: '(.*?)'\\?$");
    private static final Pattern COLOURED_ITEMS_PATTERN = Pattern.compile("^Select all the (.*?) items!$");

    // Rubix colour cycle order (rubix order).
    private static final Item[] RUBIX_ORDER = {
        Items.RED_STAINED_GLASS_PANE,
        Items.ORANGE_STAINED_GLASS_PANE,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.GREEN_STAINED_GLASS_PANE,
        Items.BLUE_STAINED_GLASS_PANE,
    };
    private static final int[] RUBIX_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};

    // Items that carry a permanent glint (their glint cannot signal "already clicked").
    private static Set<Item> specialItems;

    private static Set<Item> specialItems() {
        if (specialItems == null) {
            Set<Item> set = new HashSet<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item.components().has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) set.add(item);
            }
            specialItems = set;
        }
        return specialItems;
    }

    // ── Live terminal state ────────────────────────────────────────────────────
    private TerminalType currentType = TerminalType.NONE;
    private String currentTitle = "";
    private final Map<Integer, ItemStack> currentItems = new HashMap<>();
    private String lastSignature = null;

    private final List<TerminalClick> solution = new ArrayList<>();
    private final Map<Integer, Integer> numbersSlotCounts = new HashMap<>();

    // Startwith bookkeeping for permanently-glinted items.
    private final Set<Integer> clickedSlots = new HashSet<>();
    private int pendingSpecialClick = -1;

    // Melody state.
    private Integer melodyCorrect, melodyButton, melodyCurrent;

    // Custom-overlay geometry cache for click mapping (screen-space, before user scale).
    private float scaleCache = 1f, offsetXCache, offsetYCache;
    private int windowSizeCache;

    // ── Title detection ────────────────────────────────────────────────────────

    /** Classifies a terminal by its screen title (also used by the waypoint service). */
    public static TerminalType detectType(String title) {
        if (title == null) return TerminalType.NONE;
        return switch (title) {
            case "Correct all the panes!" -> TerminalType.PANES;
            case "Click in order!" -> TerminalType.ORDER;
            case "Change all to same color!" -> TerminalType.SAME_COLOR;
            case "Click the button on time!" -> TerminalType.MELODY;
            default -> {
                if (ITEM_NAME_PATTERN.matcher(title).matches()) yield TerminalType.ITEM_NAME;
                if (COLOURED_ITEMS_PATTERN.matcher(title).matches()) yield TerminalType.COLOURED_ITEMS;
                yield TerminalType.NONE;
            }
        };
    }

    public void onScreenOpen(AbstractContainerScreen<?> screen) {
        reset();
        currentTitle = screen.getTitle().getString();
        currentType = detectType(currentTitle);
    }

    public void onScreenTick(AbstractContainerScreen<?> screen) {
        if (currentType == TerminalType.NONE) return;
        int slotCount = currentType.slotCount();
        var menu = screen.getMenu();
        List<ItemStack> items = menu.getItems();

        currentItems.clear();
        StringBuilder sig = new StringBuilder();
        for (int i = 0; i < slotCount && i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            currentItems.put(i, stack);
            sig.append(i).append(':')
               .append(BuiltInRegistries.ITEM.getKey(stack.getItem())).append(':')
               .append(stack.getCount()).append(TerminalNames.hasGlint(stack) ? "g" : "").append(';');
        }

        // Only re-solve when the container contents actually changed (anti-flicker model):
        // a click via CLONE does not alter client items, so between click and the server's update
        // the signature is unchanged and the predicted solution stays put.
        String signature = sig.toString();
        if (!signature.equals(lastSignature)) {
            lastSignature = signature;
            solve();
        }
    }

    // ── Solving ────────────────────────────────────────────────────────────────

    private void solve() {
        solution.clear();
        switch (currentType) {
            case PANES -> { // red panes -> click all
                for (var e : currentItems.entrySet()) {
                    if (e.getValue().getItem() == Items.RED_STAINED_GLASS_PANE) solution.add(new TerminalClick(e.getKey()));
                }
            }
            case ORDER -> { // red panes numbered by stack size, click ascending
                numbersSlotCounts.clear();
                List<Map.Entry<Integer, ItemStack>> panes = new ArrayList<>();
                for (var e : currentItems.entrySet()) {
                    if (e.getValue().getItem() == Items.RED_STAINED_GLASS_PANE) panes.add(e);
                }
                panes.sort((a, b) -> Integer.compare(a.getValue().getCount(), b.getValue().getCount()));
                for (var e : panes) {
                    numbersSlotCounts.put(e.getKey(), e.getValue().getCount());
                    solution.add(new TerminalClick(e.getKey()));
                }
            }
            case ITEM_NAME -> {
                Matcher m = ITEM_NAME_PATTERN.matcher(currentTitle);
                if (!m.matches()) break;
                String letter = m.group(1).toLowerCase();
                // A special item that was clicked keeps its glint, so remember it once the window updates.
                if (pendingSpecialClick >= 0) {
                    ItemStack s = currentItems.get(pendingSpecialClick);
                    if (s != null && specialItems().contains(s.getItem())) clickedSlots.add(pendingSpecialClick);
                    pendingSpecialClick = -1;
                }
                for (var e : currentItems.entrySet()) {
                    int idx = e.getKey();
                    ItemStack stack = e.getValue();
                    String name = TerminalNames.legacyName(TerminalNames.displayName(stack)).toLowerCase();
                    if (!name.startsWith(letter)) continue;
                    if (clickedSlots.contains(idx)) continue;
                    if (!TerminalNames.hasGlint(stack) || specialItems().contains(stack.getItem())) {
                        solution.add(new TerminalClick(idx));
                    }
                }
            }
            case COLOURED_ITEMS -> {
                Matcher m = COLOURED_ITEMS_PATTERN.matcher(currentTitle);
                if (!m.matches()) break;
                String colour = m.group(1).toLowerCase();
                for (var e : currentItems.entrySet()) {
                    ItemStack stack = e.getValue();
                    if (stack.getItem() == Items.BLACK_STAINED_GLASS_PANE) continue;
                    if (TerminalNames.hasGlint(stack)) continue;
                    String name = TerminalNames.fixColorName(TerminalNames.displayName(stack)).toLowerCase();
                    if (name.startsWith(colour)) solution.add(new TerminalClick(e.getKey()));
                }
            }
            case SAME_COLOR -> {
                List<int[]> panes = new ArrayList<>(); // {slot, colourIndex}
                for (int slot : RUBIX_SLOTS) {
                    ItemStack stack = currentItems.get(slot);
                    if (stack == null) continue;
                    int ci = indexOf(RUBIX_ORDER, stack.getItem());
                    if (ci >= 0) panes.add(new int[]{slot, ci});
                }
                int[] costs = new int[5];
                for (int target = 0; target < 5; target++) {
                    for (int[] p : panes) {
                        int dist = Math.abs(target - p[1]);
                        costs[target] += dist > 2 ? 5 - dist : dist;
                    }
                }
                int origin = 0;
                for (int i = 1; i < 5; i++) if (costs[i] < costs[origin]) origin = i;
                for (int[] p : panes) {
                    if (p[1] == origin) continue;
                    int diff = origin - p[1];
                    if (diff > 2) diff -= 5;
                    if (diff < -2) diff += 5;
                    solution.add(new TerminalClick(p[0], diff));
                }
            }
            case MELODY -> solveMelody();
            default -> {}
        }
    }

    private void solveMelody() {
        melodyCorrect = melodyButton = melodyCurrent = null;
        Integer limeSlot = null, targetSlot = null;
        for (var e : currentItems.entrySet()) {
            Item it = e.getValue().getItem();
            if (it == Items.LIME_STAINED_GLASS_PANE) limeSlot = e.getKey();
            // The "where to click" marker is a purple/magenta pane depending on the version.
            else if (it == Items.MAGENTA_STAINED_GLASS_PANE || it == Items.PURPLE_STAINED_GLASS_PANE) targetSlot = e.getKey();
        }
        if (targetSlot != null) melodyCorrect = targetSlot % 9; // target column (0..8)
        if (limeSlot == null) return;
        melodyButton = (int) Math.floor(limeSlot / 9.0) - 1;
        melodyCurrent = limeSlot % 9 - 1;
    }

    private static int indexOf(Item[] arr, Item item) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == item) return i;
        return -1;
    }

    // ── Rendering (custom overlay) ────────────────────────────────────────

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, HorizonConfig config) {
        if (currentType == TerminalType.NONE || !config.isTerminalSolverEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;
        Font font = mc.font;

        float s = config.getTerminalGuiScale();
        if (s <= 0) s = 1f;
        int windowSize = currentType.slotCount();
        int width = 9 * 18;
        int height = windowSize / 9 * 18;
        float sw = screen.width / s;
        float sh = screen.height / s;
        float offsetX = sw / 2f - width / 2f;
        float offsetY = sh / 2f - height / 2f;

        scaleCache = s;
        offsetXCache = offsetX;
        offsetYCache = offsetY;
        windowSizeCache = windowSize;

        ctx.pose().pushMatrix();
        ctx.pose().scale(s, s);

        drawCenteredString(ctx, font, currentType.label(), offsetX + width / 2f, offsetY - 15f, config.getTermColorTitle(), 1.2f);
        drawRect(ctx, offsetX, offsetY, width, height, config.getTermColorBackground());
        drawBorder(ctx, offsetX, offsetY, width, height, config.getTermColorBorder(), 1);

        int baseColor = solutionColor(config);
        for (int index = 0; index < solution.size(); index++) {
            TerminalClick click = solution.get(index);
            int slot = click.slotId();
            float sx = slot % 9 * 18 + offsetX;
            float sy = slot / 9 * 18 + offsetY;

            switch (currentType) {
                case ORDER -> {
                    if (index > 2) break;
                    int color = numbersColor(config, index);
                    drawSlot(ctx, sx, sy, color, config.getTerminalSlotStyle());
                    if (config.isTerminalShowNumbers()) {
                        int count = numbersSlotCounts.getOrDefault(slot, 0);
                        drawCenteredString(ctx, font, String.valueOf(count), sx + 8, sy + 8 - font.lineHeight / 2f, config.getTermColorOverlayText(), 1f);
                    }
                }
                case SAME_COLOR -> {
                    int color = rubixColor(config, click.btn() > 0);
                    drawSlot(ctx, sx, sy, color, config.getTerminalSlotStyle());
                    drawCenteredString(ctx, font, String.valueOf(click.btn()), sx + 8, sy + 8 - font.lineHeight / 2f, config.getTermColorOverlayText(), 1f);
                }
                default -> drawSlot(ctx, sx, sy, baseColor, config.getTerminalSlotStyle());
            }
        }

        if (currentType == TerminalType.MELODY) {
            int style = config.getTerminalSlotStyle();
            // WHERE to click: the target column marker (purple/magenta), highlighted across the rows.
            if (melodyCorrect != null) {
                drawSlotSized(ctx, offsetX + melodyCorrect * 18, offsetY + 18, 16, 70, melodyColor(config, 0), style);
            }
            // WHEN / which row: the button to press (green) + the moving indicator + wrong buttons.
            if (melodyButton != null && melodyCurrent != null) {
                int buttonSlot = melodyButton * 9 + 16;      // the button to press = which ROW to click
                int currentSlot = melodyButton * 9 + 10 + melodyCurrent;
                for (int i = 0; i < windowSize; i++) {
                    float x = i % 9 * 18 + offsetX;
                    float y = i / 9 * 18 + offsetY;
                    if (i == buttonSlot) drawSlot(ctx, x, y, baseColor, style);
                    else if (i == 16 || i == 25 || i == 34 || i == 43) drawSlot(ctx, x, y, melodyColor(config, 2), style);
                    else if (i == currentSlot) drawSlot(ctx, x, y, melodyColor(config, 1), style);
                }
            }
        }

        ctx.pose().popMatrix();
    }

    private void drawSlot(GuiGraphicsExtractor ctx, float x, float y, int color, int style) {
        drawSlotSized(ctx, x, y, 16, 16, color, style);
    }

    private void drawSlotSized(GuiGraphicsExtractor ctx, float x, float y, int w, int h, int color, int style) {
        switch (style) {
            case 1 -> { // Bordered-Rect
                drawBorder(ctx, x, y, w, h, color, 1);
                drawRect(ctx, x, y, w, h, (color & 0x00FFFFFF) | (40 << 24));
            }
            case 2 -> drawFloatingRect(ctx, (int) x, (int) y, w, h, darker(color)); // Button
            default -> drawRect(ctx, x, y, w, h, color); // Rect
        }
    }

    private static void drawRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int color) {
        ctx.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, float x, float y, float w, float h, int color, int t) {
        drawRect(ctx, x, y, w, t, color);
        drawRect(ctx, x, y + h - t, w, t, color);
        drawRect(ctx, x, y + t, t, h - t * 2, color);
        drawRect(ctx, x + w - t, y + t, t, h - t * 2, color);
    }

    private static void drawFloatingRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int base) {
        int light = brighter(base), dark = darker(base);
        ctx.fill(x, y, x + 1, y + h, light);
        ctx.fill(x + 1, y, x + w, y + 1, light);
        ctx.fill(x + w - 1, y + 1, x + w, y + h, dark);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, dark);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);
    }

    private void drawCenteredString(GuiGraphicsExtractor ctx, Font font, String text, float centerX, float y, int color, float scale) {
        float w = font.width(text) * scale;
        float x = centerX - w / 2f;
        if (scale == 1f) {
            ctx.text(font, text, (int) x, (int) y, color);
            return;
        }
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(scale, scale);
        ctx.text(font, text, 0, 0, color);
        ctx.pose().popMatrix();
    }

    // ── Solver colours (HUD-derived with automatic offsets, or per-role config) ──

    private static final int SLOT_ALPHA = 0xC8; // opacity used for HUD-derived slot highlights

    /** Parses the HUD accent hex (#RRGGBB) into an opaque ARGB, with a safe fallback. */
    private static int hudBase(HorizonConfig config) {
        String hex = config.getHudAccentColor();
        if (hex != null) {
            try {
                String h = hex.startsWith("#") ? hex.substring(1) : hex;
                return 0xFF000000 | (Integer.parseInt(h, 16) & 0xFFFFFF);
            } catch (NumberFormatException ignored) {}
        }
        return 0xFF55FF55;
    }

    private int solutionColor(HorizonConfig c) {
        return c.isTerminalUseHudColor() ? withAlpha(hudBase(c), SLOT_ALPHA) : c.getTermColorSolution();
    }

    /** Order terminal: 3 shades. HUD mode = base + two darker steps; else the three configured colours. */
    private int numbersColor(HorizonConfig c, int idx) {
        if (c.isTerminalUseHudColor()) {
            int b = withAlpha(hudBase(c), SLOT_ALPHA);
            return idx <= 0 ? b : idx == 1 ? scaleBrightness(b, 0.72f) : scaleBrightness(b, 0.5f);
        }
        return idx <= 0 ? c.getTermColorNumbers1() : idx == 1 ? c.getTermColorNumbers2() : c.getTermColorNumbers3();
    }

    /** Rubix: 2 distinct colours. HUD mode = base (+) and a hue-shifted contrast (-). */
    private int rubixColor(HorizonConfig c, boolean positive) {
        if (c.isTerminalUseHudColor()) {
            int b = withAlpha(hudBase(c), SLOT_ALPHA);
            return positive ? b : hueShift(b, 160f);
        }
        return positive ? c.getTermColorRubixPos() : c.getTermColorRubixNeg();
    }

    /** Melody: which = 0 column, 1 indicator, 2 wrong. HUD mode uses hue offsets from the base. */
    private int melodyColor(HorizonConfig c, int which) {
        if (c.isTerminalUseHudColor()) {
            int b = withAlpha(hudBase(c), SLOT_ALPHA);
            return which == 0 ? b : which == 1 ? hueShift(b, 120f) : hueShift(b, 210f);
        }
        return which == 0 ? c.getTermColorMelodyColumn() : which == 1 ? c.getTermColorMelodyIndicator() : c.getTermColorMelodyWrong();
    }

    private static int withAlpha(int argb, int a) {
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int scaleBrightness(int argb, float f) {
        int a = argb >>> 24;
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int hueShift(int argb, float degrees) {
        int a = argb >>> 24;
        float[] hsb = java.awt.Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
        int rgb = java.awt.Color.HSBtoRGB((hsb[0] + degrees / 360f) % 1f, hsb[1], hsb[2]) & 0xFFFFFF;
        return (a << 24) | rgb;
    }

    private static int brighter(int argb) {
        return (argb & 0xFF000000) | scaleChannel(argb, 1.25f);
    }
    private static int darker(int argb) {
        return (argb & 0xFF000000) | scaleChannel(argb, 0.7f);
    }
    private static int scaleChannel(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }

    // ── Click handling ──────────────────────────────────────────────────────────

    /**
     * Handles a mouse click on the overlay. Always returns {@code true} when a terminal is active so
     * the caller cancels the vanilla click (the chest is fully replaced by the overlay). Only a click
     * on a valid solution slot is forwarded to the server.
     */
    public boolean onOverlayMouseClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY, boolean leftClick, HorizonConfig config) {
        if (currentType == TerminalType.NONE || !config.isTerminalSolverEnabled()) return false;

        int slot = slotAt(mouseX, mouseY);
        if (slot < 0 || slot >= windowSizeCache) return true;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null || mc.player == null) return true;
        int windowId = screen.getMenu().containerId;

        if (currentType == TerminalType.MELODY) {
            if (slot == 16 || slot == 25 || slot == 34 || slot == 43) sendClickPacket(windowId, slot, 0);
            return true;
        }

        TerminalClick click = null;
        switch (currentType) {
            case ORDER -> {
                if (!solution.isEmpty() && solution.get(0).slotId() == slot) click = solution.get(0);
            }
            case SAME_COLOR -> {
                for (TerminalClick c : solution) {
                    if (c.slotId() == slot) { click = new TerminalClick(slot, c.btn() > 0 ? 0 : 1); break; }
                }
            }
            default -> { // PANES, ITEM_NAME, COLOURED_ITEMS
                for (TerminalClick c : solution) {
                    if (c.slotId() == slot) { click = c; break; }
                }
            }
        }
        if (click == null) return true;

        predict(click);
        sendClickPacket(windowId, click.slotId(), click.btn());
        if (currentType == TerminalType.ITEM_NAME) pendingSpecialClick = click.slotId();
        return true;
    }

    /** Locally advances the solution so the highlight clears instantly (before the server responds). */
    private void predict(TerminalClick click) {
        if (currentType == TerminalType.SAME_COLOR) {
            for (int i = 0; i < solution.size(); i++) {
                TerminalClick c = solution.get(i);
                if (c.slotId() != click.slotId()) continue;
                int change = click.btn() == 0 ? -1 : 1;
                int newDiff = c.btn() + change;
                if (newDiff == 0) solution.remove(i);
                else solution.set(i, new TerminalClick(c.slotId(), newDiff));
                return;
            }
        } else {
            for (int i = 0; i < solution.size(); i++) {
                if (solution.get(i).slotId() == click.slotId()) { solution.remove(i); return; }
            }
        }
    }

    private void sendClickPacket(int windowId, int slot, int btn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null || mc.player == null) return;
        // btn 0 -> middle-click CLONE (does not move the item client-side, avoids flicker); else PICKUP.
        mc.gameMode.handleContainerInput(
            windowId, slot,
            btn == 0 ? 2 : btn,
            btn == 0 ? ContainerInput.CLONE : ContainerInput.PICKUP,
            mc.player);
    }

    /** Maps a mouse position to a chest slot index in the overlay, or -1 if outside the grid. */
    private int slotAt(double mouseX, double mouseY) {
        if (scaleCache <= 0) return -1;
        double mx = mouseX / scaleCache;
        double my = mouseY / scaleCache;
        int slotX = (int) Math.floor((mx - offsetXCache) / 18);
        int slotY = (int) Math.floor((my - offsetYCache) / 18);
        if (slotX < 0 || slotX > 8 || slotY < 0) return -1;
        return slotX + slotY * 9;
    }

    // ── Lifecycle / queries ──────────────────────────────────────────────────────

    public TerminalType getCurrentType() { return currentType; }

    /** True when a solvable terminal is open (same condition that draws the overlay). */
    public boolean isActiveTerminal() { return currentType != TerminalType.NONE; }

    public void reset() {
        currentType = TerminalType.NONE;
        currentTitle = "";
        currentItems.clear();
        lastSignature = null;
        solution.clear();
        numbersSlotCounts.clear();
        clickedSlots.clear();
        pendingSpecialClick = -1;
        melodyCorrect = melodyButton = melodyCurrent = null;
        scaleCache = 1f;
    }
}
