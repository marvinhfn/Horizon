package de.horizon.feature.dungeon.terminal;

import de.horizon.config.HorizonConfig;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terminal Solver for F7 boss terminals.
 * Supports: Panes (Red/Green), Order/Numbers, Rubix, Starts With, Select All, Melody (recognized only).
 */
public final class TerminalSolverService {

    public enum TerminalType { PANES, ORDER, RUBIX, STARTS_WITH, SELECT_ALL, MELODY, NONE }

    // Rubix pane color order: left click = next, right click = prev
    private static final List<DyeColor> RUBIX_ORDER = List.of(
        DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BLUE, DyeColor.RED
    );
    private static final int[] RUBIX_INDICES = {12, 13, 14, 21, 22, 23, 30, 31, 32};

    // ARGB colors for highlights
    private static final int COLOR_CORRECT_1  = 0xFF4F8F2F;
    private static final int COLOR_CORRECT_2  = 0xFFB5A12E;
    private static final int COLOR_CORRECT_3  = 0xFFB86A2E;
    private static final int COLOR_OTHER      = 0xFF8F2E2E;
    private static final int COLOR_BG_SLOT    = 0xFF191919;
    private static final long PENDING_TIMEOUT_MS = 3000; // 3s until pending click expires
    // Rubix: left click = green, right click = blue
    private static final int COLOR_RUBIX_LEFT  = 0xFF4F8F2F;
    private static final int COLOR_RUBIX_RIGHT = 0xFF2F4F8F;

    // Title patterns for terminal detection
    private static final Pattern STARTS_WITH_PATTERN = Pattern.compile("^What starts with: '(.*?)'\\?$");
    private static final Pattern SELECT_ALL_PATTERN = Pattern.compile("^Select all the (.*?) items!$");

    // Legacy name mapping for "Starts With" terminal (MC modern → Hypixel expected names)
    private static final Map<String, String> LEGACY_NAMES = Map.ofEntries(
        Map.entry("Grass", "Grass Block"),
        Map.entry("Redstone Dust", "Redstone"),
        Map.entry("Empty Map", "Map"),
        Map.entry("Oak Planks", "Oak Wood Planks"),
        Map.entry("Spruce Planks", "Spruce Wood Planks"),
        Map.entry("Birch Planks", "Birch Wood Planks"),
        Map.entry("Jungle Planks", "Jungle Wood Planks"),
        Map.entry("Acacia Planks", "Acacia Wood Planks"),
        Map.entry("Dark Oak Planks", "Dark Oak Wood Planks"),
        Map.entry("Tall Grass", "Double Tallgrass"),
        Map.entry("Brown Mushroom", "Mushroom"),
        Map.entry("Red Mushroom", "Mushroom"),
        Map.entry("Brick Slab", "Bricks Slab"),
        Map.entry("Stone Brick Slab", "Stone Bricks Slab"),
        Map.entry("Oak Slab", "Oak Wood Slab"),
        Map.entry("Spruce Slab", "Spruce Wood Slab"),
        Map.entry("Birch Slab", "Birch Wood Slab"),
        Map.entry("Jungle Slab", "Jungle Wood Slab"),
        Map.entry("Acacia Slab", "Acacia Wood Slab"),
        Map.entry("Dark Oak Slab", "Dark Oak Wood Slab"),
        Map.entry("Mossy Cobblestone", "Mossy Stone"),
        Map.entry("Oak Stairs", "Oak Wood Stairs"),
        Map.entry("Spruce Stairs", "Spruce Wood Stairs"),
        Map.entry("Birch Stairs", "Birch Wood Stairs"),
        Map.entry("Jungle Stairs", "Jungle Wood Stairs"),
        Map.entry("Acacia Stairs", "Acacia Wood Stairs"),
        Map.entry("Dark Oak Stairs", "Dark Oak Wood Stairs"),
        Map.entry("Oak Pressure Plate", "Wooden Pressure Plate"),
        Map.entry("Light Weighted Pressure Plate", "Weighted Pressure Plate (Light)"),
        Map.entry("Heavy Weighted Pressure Plate", "Weighted Pressure Plate (Heavy)"),
        Map.entry("Oak Button", "Button"),
        Map.entry("Stone Button", "Button"),
        Map.entry("White Carpet", "Carpet"),
        Map.entry("Black Terracotta", "Black Stained Clay"),
        Map.entry("Red Terracotta", "Red Stained Clay"),
        Map.entry("Green Terracotta", "Green Stained Clay"),
        Map.entry("Brown Terracotta", "Brown Stained Clay"),
        Map.entry("Blue Terracotta", "Blue Stained Clay"),
        Map.entry("Purple Terracotta", "Purple Stained Clay"),
        Map.entry("Cyan Terracotta", "Cyan Stained Clay"),
        Map.entry("Light Gray Terracotta", "Light Gray Stained Clay"),
        Map.entry("Gray Terracotta", "Gray Stained Clay"),
        Map.entry("Pink Terracotta", "Pink Stained Clay"),
        Map.entry("Lime Terracotta", "Lime Stained Clay"),
        Map.entry("Yellow Terracotta", "Yellow Stained Clay"),
        Map.entry("Light Blue Terracotta", "Light Blue Stained Clay"),
        Map.entry("Magenta Terracotta", "Magenta Stained Clay"),
        Map.entry("Orange Terracotta", "Orange Stained Clay"),
        Map.entry("White Terracotta", "White Stained Clay"),
        Map.entry("Terracotta", "Hardened Clay"),
        Map.entry("Nether Portal", "Portal"),
        Map.entry("White Wool", "Wool"),
        Map.entry("Block of Lapis Lazuli", "Lapis Lazuli Block"),
        Map.entry("Red Bed", "Bed"),
        Map.entry("White Bed", "Bed"),
        Map.entry("Oak Trapdoor", "Wooden Trapdoor"),
        Map.entry("Infested Stone", "Stone Monster Egg"),
        Map.entry("Infested Cobblestone", "Cobblestone Monster Egg"),
        Map.entry("Infested Stone Bricks", "Stone Brick Monster Egg"),
        Map.entry("Infested Mossy Stone Bricks", "Mossy Stone Brick Monster Egg"),
        Map.entry("Infested Cracked Stone Bricks", "Cracked Stone Brick Monster Egg"),
        Map.entry("Infested Chiseled Stone Bricks", "Chiseled Stone Brick Monster Egg"),
        Map.entry("Enchanting Table", "Enchantment Table"),
        Map.entry("Chipped Anvil", "Slightly Damaged Anvil"),
        Map.entry("Damaged Anvil", "Very Damaged Anvil"),
        Map.entry("Daylight Detector", "Daylight Sensor"),
        Map.entry("Quartz Pillar", "Pillar Quartz Block"),
        Map.entry("Wheat Seeds", "Seeds"),
        Map.entry("Chainmail Helmet", "Chain Helmet"),
        Map.entry("Chainmail Chestplate", "Chain Chestplate"),
        Map.entry("Chainmail Leggings", "Chain Leggings"),
        Map.entry("Chainmail Boots", "Chain Boots"),
        Map.entry("Oak Boat", "Boat"),
        Map.entry("Milk Bucket", "Milk"),
        Map.entry("Sugar Cane", "Sugar Canes"),
        Map.entry("Raw Cod", "Raw Fish"),
        Map.entry("Tropical Fish", "Clownfish"),
        Map.entry("Cooked Cod", "Cooked Fish"),
        Map.entry("Red Dye", "Rose Red"),
        Map.entry("Green Dye", "Cactus Green"),
        Map.entry("Yellow Dye", "Dandelion Yellow"),
        Map.entry("Glistering Melon Slice", "Glistering Melon"),
        Map.entry("Player Head", "Head"),
        Map.entry("Golden Horse Armor", "Gold Horse Armor")
    );

    // Color name normalization for "Select All" terminal
    private static final Map<String, String> COLOR_FIXES = Map.ofEntries(
        Map.entry("light gray", "silver"),
        Map.entry("wool", "white"),
        Map.entry("bone", "white"),
        Map.entry("ink", "black"),
        Map.entry("lapis", "blue"),
        Map.entry("cocoa", "brown"),
        Map.entry("dandelion", "yellow"),
        Map.entry("rose", "red"),
        Map.entry("cactus", "green")
    );

    // Static flag for mixin to check if terminal custom mode rendering should suppress container
    private static volatile boolean customModeRendering = false;
    public static boolean isCustomModeRendering() { return customModeRendering; }

    private TerminalType currentType = TerminalType.NONE;
    private String searchParam = ""; // letter for StartsWith, color for SelectAll

    // Per-slot solution arrays (indexed by container slot)
    private boolean[] solutionSlots = new boolean[0]; // true = should be clicked (StartsWith, SelectAll, Panes)
    private int[] orderCounts = new int[0]; // count value for ORDER terminal (0 = non-target)
    private int[] rubixClicks = new int[0]; // clicks needed for RUBIX (0 = already correct)

    // ORDER: cache initial slot states to handle animation flickering
    private int[] orderInitSlots = null;
    private int orderMinCount = 14;

    // RUBIX: track last click for held-item compensation
    private int rubixLastClicked = -1;
    private boolean rubixLastClickWasLeft = false;

    // Pending click tracking: slot stays hidden until server acknowledges or timeout
    private long[] pendingClickTimes = new long[0];

    public void onScreenOpen(AbstractContainerScreen<?> screen) {
        reset();
        String title = screen.getTitle().getString();

        if (title.equals("Correct all the panes!")) {
            currentType = TerminalType.PANES;
        } else if (title.equals("Click in order!")) {
            currentType = TerminalType.ORDER;
        } else if (title.equals("Change all to same color!")) {
            currentType = TerminalType.RUBIX;
        } else if (title.equals("Click the button on time!")) {
            currentType = TerminalType.MELODY;
        } else {
            Matcher swMatcher = STARTS_WITH_PATTERN.matcher(title);
            if (swMatcher.matches()) {
                currentType = TerminalType.STARTS_WITH;
                searchParam = swMatcher.group(1);
            } else {
                Matcher saMatcher = SELECT_ALL_PATTERN.matcher(title);
                if (saMatcher.matches()) {
                    currentType = TerminalType.SELECT_ALL;
                    searchParam = saMatcher.group(1);
                }
            }
        }
    }

    public void onScreenTick(AbstractContainerScreen<?> screen) {
        if (currentType == TerminalType.NONE || currentType == TerminalType.MELODY) return;
        computeSolution(screen);
    }

    private void computeSolution(AbstractContainerScreen<?> screen) {
        List<ItemStack> items = screen.getMenu().getItems();
        int size = items.size();
        ensurePendingArrays(size);

        switch (currentType) {
            case PANES -> solvePanes(items, size);
            case ORDER -> solveOrder(items, size, screen);
            case RUBIX -> solveRubix(items, size, screen);
            case STARTS_WITH -> solveStartsWith(items, size);
            case SELECT_ALL -> solveSelectAll(items, size);
            default -> {}
        }

        // Apply pending click tracking for applicable terminal types
        if (currentType == TerminalType.PANES || currentType == TerminalType.STARTS_WITH
                || currentType == TerminalType.SELECT_ALL) {
            applyPendingTracking(size);
        }
    }

    private void ensurePendingArrays(int size) {
        if (pendingClickTimes.length != size) {
            pendingClickTimes = new long[size];
        }
    }

    /**
     * Post-processing: pending slots are hidden from the solution until the server
     * changes the item (acknowledged) or the timeout expires (flicker back).
     */
    private void applyPendingTracking(int size) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            if (i >= pendingClickTimes.length || pendingClickTimes[i] == 0) continue;
            boolean stillTarget = i < solutionSlots.length && solutionSlots[i];
            if (!stillTarget) {
                // Server acknowledged — item changed
                pendingClickTimes[i] = 0;
            } else if (now - pendingClickTimes[i] > PENDING_TIMEOUT_MS) {
                // Timeout — flicker back to clickable
                pendingClickTimes[i] = 0;
            } else {
                // Still pending — hide from solution
                solutionSlots[i] = false;
            }
        }
    }

    // ── Solvers ──────────────────────────────────────────────────────────────

    private void solvePanes(List<ItemStack> items, int size) {
        if (solutionSlots.length != size) {
            solutionSlots = new boolean[size];
        }
        for (int i = 0; i < size; i++) {
            solutionSlots[i] = items.get(i).getItem() == Items.RED_STAINED_GLASS_PANE;
        }
    }

    private void solveOrder(List<ItemStack> items, int size, AbstractContainerScreen<?> screen) {
        // Initialize cached slot counts on first tick (prevents flickering during animation)
        if (orderInitSlots == null && size > 45 && !items.get(size - 45).isEmpty()) {
            orderInitSlots = new int[size];
            for (int i = 0; i < size; i++) {
                ItemStack stack = items.get(i);
                orderInitSlots[i] = (stack.getItem() == Items.RED_STAINED_GLASS_PANE) ? stack.getCount() : 0;
            }
        }

        orderCounts = new int[size];
        orderMinCount = 14;
        for (int i = 0; i < size; i++) {
            ItemStack stack = items.get(i);
            int count = (stack.getItem() == Items.RED_STAINED_GLASS_PANE) ? stack.getCount() : 0;

            // Use cached value if item disappeared or changed (animation glitch compensation)
            int cached = (orderInitSlots != null && i < orderInitSlots.length) ? orderInitSlots[i] : 0;
            int fixedCount;
            if (cached > 0 && (stack.isEmpty() || (count != 0 && count != cached))) {
                fixedCount = cached;
            } else {
                fixedCount = count;
            }

            orderCounts[i] = fixedCount;
            if (fixedCount > 0) orderMinCount = Math.min(orderMinCount, fixedCount);
        }
    }

    private void solveRubix(List<ItemStack> items, int size, AbstractContainerScreen<?> screen) {
        rubixClicks = new int[size];

        // Collect current colors of the 9 rubix panes
        record RubixSlot(int idx, int colorIndex) {}
        List<RubixSlot> panes = new ArrayList<>(9);

        ItemStack held = screen.getMenu().getCarried();

        for (int idx : RUBIX_INDICES) {
            if (idx >= size) continue;
            ItemStack item = items.get(idx);

            // If this slot was just clicked, the item might be in the carried slot
            if (item.isEmpty() && idx == rubixLastClicked && !held.isEmpty()) {
                item = held;
            }

            DyeColor color = getPaneColor(item);
            if (color == null) continue;
            int colorIdx = RUBIX_ORDER.indexOf(color);
            if (colorIdx < 0) continue;

            // Compensate for held item (item in transit after click)
            if (item == held && rubixLastClicked == idx) {
                if (rubixLastClickWasLeft) colorIdx++;
                else colorIdx--;
                if (colorIdx < 0) colorIdx += RUBIX_ORDER.size();
                if (colorIdx >= RUBIX_ORDER.size()) colorIdx -= RUBIX_ORDER.size();
            }

            panes.add(new RubixSlot(idx, colorIdx));
        }

        // Find optimal target color (fewest total clicks)
        int bestCost = 19;
        int[] bestClicks = null;
        for (int target = 0; target < RUBIX_ORDER.size(); target++) {
            int totalClicks = 0;
            int[] clicks = new int[size];
            for (RubixSlot pane : panes) {
                int dist = Math.abs(target - pane.colorIndex);
                if (dist >= 3) dist = 5 - dist;
                totalClicks += dist;

                // Left clicks needed: positive = how many left clicks
                int lc = target - pane.colorIndex;
                if (lc < 0) lc += 5;
                clicks[pane.idx] = lc;
            }
            if (totalClicks < bestCost) {
                bestCost = totalClicks;
                bestClicks = clicks;
            }
        }

        if (bestClicks != null) {
            rubixClicks = bestClicks;
        }
    }

    private void solveStartsWith(List<ItemStack> items, int size) {
        solutionSlots = new boolean[size];
        if (searchParam.isEmpty()) return;

        for (int i = 0; i < size; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            // Check for enchantment glint override (= already clicked/completed)
            Boolean glint = stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            if (glint != null && glint) continue;

            String name = getItemDisplayName(stack);
            // Apply legacy name mapping
            String legacyName = LEGACY_NAMES.get(name);
            if (legacyName != null) name = legacyName;

            solutionSlots[i] = name.regionMatches(true, 0, searchParam, 0, searchParam.length());
        }
    }

    private void solveSelectAll(List<ItemStack> items, int size) {
        solutionSlots = new boolean[size];
        if (searchParam.isEmpty()) return;

        for (int i = 0; i < size; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            Boolean glint = stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            if (glint != null && glint) continue;

            String name = getItemDisplayName(stack);
            // Apply color name fixes
            for (Map.Entry<String, String> fix : COLOR_FIXES.entrySet()) {
                if (name.regionMatches(true, 0, fix.getKey(), 0, fix.getKey().length())) {
                    name = fix.getValue();
                    break;
                }
            }

            solutionSlots[i] = name.regionMatches(true, 0, searchParam, 0, searchParam.length());
        }
    }

    // ── Custom mode grid layout per terminal type ──────────────────────────
    // rows, cols, startRow (in 9-wide container), startCol
    private static final int[][] GRID_PARAMS = new int[6][];
    static {
        GRID_PARAMS[TerminalType.PANES.ordinal()]      = new int[]{3, 5, 1, 2};
        GRID_PARAMS[TerminalType.ORDER.ordinal()]       = new int[]{2, 7, 1, 1};
        GRID_PARAMS[TerminalType.RUBIX.ordinal()]       = new int[]{3, 3, 1, 3};
        GRID_PARAMS[TerminalType.STARTS_WITH.ordinal()] = new int[]{3, 7, 1, 1};
        GRID_PARAMS[TerminalType.SELECT_ALL.ordinal()]  = new int[]{4, 7, 1, 1};
    }
    private static final int CUSTOM_SLOT_SIZE = 24;
    private static final int CUSTOM_GAP = 2;
    private static final int CUSTOM_BG_COLOR = 0xCC1A1A1A;

    // Cached custom grid origin for click mapping
    private float customOriginX, customOriginY;
    private float customScale;
    private int customRows, customCols, customStartRow, customStartCol;

    // ── Rendering ────────────────────────────────────────────────────────────

    /**
     * Called BEFORE rendering to set the customModeRendering flag.
     * The HandledScreenMixin checks this flag to cancel container rendering.
     */
    public void updateCustomModeFlag(HorizonConfig config) {
        customModeRendering = config.isTerminalSolverCustomMode()
            && config.isTerminalSolverEnabled()
            && currentType != TerminalType.NONE
            && currentType != TerminalType.MELODY;
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, HorizonConfig config) {
        if (!config.isTerminalSolverEnabled() || currentType == TerminalType.NONE || currentType == TerminalType.MELODY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        boolean customMode = config.isTerminalSolverCustomMode();
        if (customMode) {
            customScale = config.getTerminalGuiScale();
            renderCustomMode(screen, ctx, mc);
        } else {
            renderNormalMode(screen, ctx, mc);
        }
    }

    private void renderCustomMode(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, Minecraft mc) {
        int[] params = GRID_PARAMS[currentType.ordinal()];
        if (params == null) return;

        int rows = params[0], cols = params[1], startRow = params[2], startCol = params[3];
        float scale = customScale > 0 ? customScale : 2.0f;

        int gap = CUSTOM_GAP;
        int slotSize = CUSTOM_SLOT_SIZE;
        int gridW = cols * slotSize + (cols - 1) * gap;
        int gridH = rows * slotSize + (rows - 1) * gap;
        float originX = (screen.width - gridW * scale) / 2f;
        float originY = (screen.height - gridH * scale) / 2f;

        // Cache for click mapping
        customOriginX = originX;
        customOriginY = originY;
        customScale = scale;
        customRows = rows;
        customCols = cols;
        customStartRow = startRow;
        customStartCol = startCol;

        boolean completed = isCompleted();

        ctx.pose().pushMatrix();
        ctx.pose().translate(originX, originY);
        ctx.pose().scale(scale, scale);

        // Background
        int pad = 4;
        ctx.fill(-pad, -pad, gridW + pad, gridH + pad, CUSTOM_BG_COLOR);

        // Render slots
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slotIndex = (startRow + row) * 9 + (startCol + col);
                int bx = col * (slotSize + gap);
                int by = row * (slotSize + gap);

                if (completed) {
                    ctx.fill(bx, by, bx + slotSize, by + slotSize, COLOR_CORRECT_1);
                } else {
                    boolean isSolution = isSlotSolution(slotIndex);
                    if (isSolution) {
                        ctx.fill(bx, by, bx + slotSize, by + slotSize, getSolutionColor(slotIndex));
                    } else {
                        ctx.fill(bx, by, bx + slotSize, by + slotSize, COLOR_BG_SLOT);
                    }
                }

                // Text overlays (numbers for ORDER, click counts for RUBIX)
                if (!completed && mc.font != null) {
                    String text = getSlotText(slotIndex);
                    if (text != null) {
                        int textColor = getSlotTextColor(slotIndex);
                        int tw = mc.font.width(text);
                        int tx = bx + (slotSize - tw) / 2;
                        int ty = by + (slotSize - mc.font.lineHeight) / 2 + 1;
                        ctx.text(mc.font, text, tx, ty, textColor);
                    }
                }
            }
        }

        // "Done" text
        if (completed && mc.font != null) {
            String done = "Done";
            int tw = mc.font.width(done);
            ctx.text(mc.font, done, (gridW - tw) / 2, (gridH - mc.font.lineHeight) / 2, 0xFF55FF55);
        }

        ctx.pose().popMatrix();
    }

    private void renderNormalMode(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, Minecraft mc) {
        var accessor = (AbstractContainerScreenAccessor)(Object) screen;
        int leftPos = accessor.getLeftPos();
        int topPos = accessor.getTopPos();
        int imgW = accessor.getImageWidth();
        int imgH = accessor.getImageHeight();

        boolean completed = isCompleted();
        var menu = screen.getMenu();

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container == mc.player.getInventory()) continue;

            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            boolean isSolution = isSlotSolution(i);

            if (completed) {
                ctx.fill(sx, sy, sx + 16, sy + 16, COLOR_CORRECT_1);
            } else if (!isSolution) {
                ctx.fill(sx, sy, sx + 16, sy + 16, 0xCC000000);
            } else {
                int color = getSolutionColor(i);
                ctx.fill(sx, sy, sx + 16, sy + 16, color);
            }
        }

        // "Done" overlay when completed
        if (completed && mc.font != null) {
            ctx.centeredText(mc.font, "Done", leftPos + imgW / 2, topPos + imgH / 2 - 4, 0xFF55FF55);
        }

        // Render numbers for ORDER terminal
        if (!completed && currentType == TerminalType.ORDER && mc.font != null) {
            for (int i = 0; i < menu.slots.size(); i++) {
                if (i >= orderCounts.length) break;
                int count = orderCounts[i];
                if (count <= 0) continue;
                Slot slot = menu.slots.get(i);
                if (slot.container == mc.player.getInventory()) continue;
                ctx.centeredText(mc.font, String.valueOf(count), leftPos + slot.x + 8, topPos + slot.y + 4, 0xFFFFFFFF);
            }
        }

        // Render click counts for RUBIX terminal
        if (!completed && currentType == TerminalType.RUBIX && mc.font != null) {
            for (int i = 0; i < menu.slots.size(); i++) {
                if (i >= rubixClicks.length) break;
                int clicks = rubixClicks[i];
                if (clicks == 0) continue;
                Slot slot = menu.slots.get(i);
                if (slot.container == mc.player.getInventory()) continue;
                int display = clicks >= 3 ? clicks - 5 : clicks;
                int textColor = display > 0 ? 0xFF55FF55 : 0xFFFF8800;
                ctx.centeredText(mc.font, String.valueOf(display), leftPos + slot.x + 8, topPos + slot.y + 4, textColor);
            }
        }
    }

    /** Returns text to display on a slot in custom mode (ORDER numbers, RUBIX click counts). */
    private String getSlotText(int slotIndex) {
        if (currentType == TerminalType.ORDER) {
            if (slotIndex < orderCounts.length && orderCounts[slotIndex] > 0) {
                return String.valueOf(orderCounts[slotIndex]);
            }
        } else if (currentType == TerminalType.RUBIX) {
            if (slotIndex < rubixClicks.length && rubixClicks[slotIndex] != 0) {
                int display = rubixClicks[slotIndex] >= 3 ? rubixClicks[slotIndex] - 5 : rubixClicks[slotIndex];
                return String.valueOf(display);
            }
        }
        return null;
    }

    /** Returns text color for slot overlay text. */
    private int getSlotTextColor(int slotIndex) {
        if (currentType == TerminalType.ORDER) return 0xFFFFFFFF;
        if (currentType == TerminalType.RUBIX && slotIndex < rubixClicks.length) {
            int display = rubixClicks[slotIndex] >= 3 ? rubixClicks[slotIndex] - 5 : rubixClicks[slotIndex];
            return display > 0 ? 0xFF55FF55 : 0xFFFF8800;
        }
        return 0xFFFFFFFF;
    }

    /**
     * Maps a mouse click position to a custom mode grid slot index.
     * Returns -1 if the click is outside the grid.
     */
    public int getCustomModeSlotIndex(double mouseX, double mouseY) {
        if (!customModeRendering || customScale <= 0) return -1;
        float bx = (float) ((mouseX - customOriginX) / customScale);
        float by = (float) ((mouseY - customOriginY) / customScale);
        int gap = CUSTOM_GAP;
        int slotSize = CUSTOM_SLOT_SIZE;
        for (int row = 0; row < customRows; row++) {
            for (int col = 0; col < customCols; col++) {
                int sx = col * (slotSize + gap);
                int sy = row * (slotSize + gap);
                if (bx >= sx && bx < sx + slotSize && by >= sy && by < sy + slotSize) {
                    return (customStartRow + row) * 9 + (customStartCol + col);
                }
            }
        }
        return -1;
    }

    /** Returns true when the terminal puzzle is fully solved. */
    private boolean isCompleted() {
        return switch (currentType) {
            case PANES -> {
                for (boolean b : solutionSlots) if (b) yield false;
                yield solutionSlots.length > 0;
            }
            case STARTS_WITH, SELECT_ALL -> {
                for (boolean b : solutionSlots) if (b) yield false;
                yield solutionSlots.length > 0;
            }
            case ORDER -> {
                for (int c : orderCounts) if (c > 0) yield false;
                yield orderCounts.length > 0;
            }
            case RUBIX -> {
                for (int c : rubixClicks) if (c != 0) yield false;
                yield rubixClicks.length > 0;
            }
            default -> false;
        };
    }

    private boolean isSlotPending(int slotIdx) {
        return slotIdx >= 0 && slotIdx < pendingClickTimes.length && pendingClickTimes[slotIdx] > 0;
    }

    private boolean isSlotSolution(int slotIdx) {
        return switch (currentType) {
            case PANES, STARTS_WITH, SELECT_ALL ->
                slotIdx < solutionSlots.length && solutionSlots[slotIdx];
            case ORDER ->
                slotIdx < orderCounts.length && orderCounts[slotIdx] > 0;
            case RUBIX ->
                slotIdx < rubixClicks.length && rubixClicks[slotIdx] > 0;
            default -> false;
        };
    }

    private int getSolutionColor(int slotIdx) {
        return switch (currentType) {
            case PANES, STARTS_WITH, SELECT_ALL -> COLOR_CORRECT_1;
            case ORDER -> {
                if (slotIdx >= orderCounts.length) yield COLOR_CORRECT_1;
                int rank = orderCounts[slotIdx] - orderMinCount;
                yield switch (rank) {
                    case 0 -> COLOR_CORRECT_1;
                    case 1 -> COLOR_CORRECT_2;
                    case 2 -> COLOR_CORRECT_3;
                    default -> COLOR_OTHER;
                };
            }
            case RUBIX -> {
                if (slotIdx >= rubixClicks.length) yield COLOR_CORRECT_1;
                int clicks = rubixClicks[slotIdx];
                int display = clicks >= 3 ? clicks - 5 : clicks;
                // Left click (positive) = green, Right click (negative) = blue
                yield display > 0 ? COLOR_RUBIX_LEFT : COLOR_RUBIX_RIGHT;
            }
            default -> COLOR_CORRECT_1;
        };
    }

    /** Returns true if the click on the given slot should be blocked. */
    public boolean shouldBlockClick(int slotIndex, boolean isLeftClick) {
        return switch (currentType) {
            case PANES, STARTS_WITH, SELECT_ALL ->
                slotIndex >= solutionSlots.length || !solutionSlots[slotIndex] || isSlotPending(slotIndex);
            case ORDER -> slotIndex >= orderCounts.length || orderCounts[slotIndex] == 0;
            case RUBIX -> {
                if (slotIndex >= rubixClicks.length) yield true;
                int clicks = rubixClicks[slotIndex];
                if (clicks == 0) yield true;
                // Track click for held-item compensation
                rubixLastClicked = slotIndex;
                rubixLastClickWasLeft = isLeftClick;
                yield false;
            }
            default -> false;
        };
    }

    /** Overload for backward compatibility. */
    public boolean shouldBlockClick(int slotIndex) {
        return shouldBlockClick(slotIndex, true);
    }

    /** Called after a slot click to mark it as pending server acknowledgment. */
    public void onSlotClicked(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < pendingClickTimes.length) {
            pendingClickTimes[slotIndex] = System.currentTimeMillis();
        }
    }

    public TerminalType getCurrentType() { return currentType; }

    public void reset() {
        currentType = TerminalType.NONE;
        searchParam = "";
        solutionSlots = new boolean[0];
        orderCounts = new int[0];
        rubixClicks = new int[0];
        orderInitSlots = null;
        orderMinCount = 14;
        rubixLastClicked = -1;
        rubixLastClickWasLeft = false;
        pendingClickTimes = new long[0];
        customModeRendering = false;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String getItemDisplayName(ItemStack stack) {
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) return customName.getString();
        return stack.getItemName().getString();
    }

    private static DyeColor getPaneColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof StainedGlassPaneBlock pane) {
            return pane.getColor();
        }
        return null;
    }
}
