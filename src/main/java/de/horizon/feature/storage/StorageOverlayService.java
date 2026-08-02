package de.horizon.feature.storage;

import de.horizon.mixin.AbstractContainerScreenAccessor;
import de.horizon.mixin.SlotAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Combined storage view for the SkyBlock Ender Chest / Backpacks.
 *
 * <ul>
 *   <li><b>Storage hub</b> ("Storage" title): a read-only combined overview of every cached page,
 *       togglable back to the interactive vanilla menu.</li>
 *   <li><b>A storage page</b> ("Ender Chest (N/M)" / "…Backpack (Slot #N)"): a fully interactive
 *       overlay — the open page's real slots are live (clicks/shift-clicks are forwarded to the
 *       server so items move in and out), the player inventory is shown and interactive, and the
 *       other cached pages sit alongside; clicking an Ender Chest page navigates to it.</li>
 * </ul>
 *
 * <p>Pages are laid out {@value #PAGE_COLS} across. The cache is a per-session snapshot; each opened
 * page overwrites its snapshot (full grid, empty slots included).
 */
public final class StorageOverlayService {

    public record Page(String key, String label, int order, List<ItemStack> items) {}

    private static final Pattern ENDER = Pattern.compile("Ender Chest (?:✦ )?\\((\\d+)/(\\d+)\\)");
    private static final Pattern BACKPACK_SLOT = Pattern.compile("Backpack (?:✦ )?\\(Slot #(\\d+)\\)");

    // Layout
    private static final int CELL = 18;
    private static final int COLS = 9;
    private static final int HEADER = 44;
    private static final int GRID_W = COLS * CELL;
    private static final int PAGE_COLS = 3;
    private static final int PAGE_GAP = 16;

    // Colours
    private static final int BACKDROP = 0xC0101018; // translucent, like the config menu (not the opaque scoreboard)
    private static final int CELL_BG = 0x60F0F1F3;
    private static final int CELL_HOVER = 0xA0A0C0FF;
    private static final int DIM = 0xC0101820;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB8B8B8;
    private static final int ACCENT = 0xFF55FFFF;
    private static final int CURRENT_BORDER = 0xFF55FF55;

    private final Map<String, Page> pages = new ConcurrentHashMap<>();
    private int maxEnderPages = 0; // learned from the "/M" in an "Ender Chest (N/M)" title
    private double savedCursorX = -1, savedCursorY = -1; // preserve mouse pos across page navigation
    // Backpack heads seen in the "Storage" hub, keyed by their "(Slot #N)" number:
    // number -> real hub GUI slot index (to open it) and number -> display name (for placeholders).
    private final Map<Integer, Integer> hubBackpackByNum = new java.util.TreeMap<>();
    private final Map<Integer, String> hubBackpackName = new java.util.TreeMap<>();
    private int pendingBackpackNum = -1; // backpack to open once we've navigated back to the hub
    // Matches both the page title "(Slot #13)" and the hub head name "Backpack Slot 13".
    private static final Pattern SLOT_HASH = Pattern.compile("Slot #?(\\d+)");
    private final List<int[]> hubNav = new ArrayList<>(); // {x, y, w, h, enderPageNumber}
    private final List<int[]> pageNav = new ArrayList<>(); // page-overlay nav rects {x, y, w, h}
    private final List<Page> pageNavPages = new ArrayList<>(); // parallel to pageNav: the page each rect opens

    // Shared state (one scroll value so the hub overview and page overlay stay at the same position)
    private String search = "";
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean overviewOpen = true;
    private static final int BTN_X = 4, BTN_Y = 4, BTN_W = 78, BTN_H = 14;

    // Interactive page state
    private int pageContentHeight = 0;
    private int pageViewportH = 200; // visible page-grid height (set in render, used by scroll clamp)
    private static final int HIGHLIGHT = 0x9000C853;

    // ── Detection ────────────────────────────────────────────────────────────────

    public boolean isStorageMenu(AbstractContainerScreen<?> screen) {
        return strip(screen.getTitle().getString()).strip().equalsIgnoreCase("Storage");
    }

    public boolean isStoragePage(AbstractContainerScreen<?> screen) {
        return pageKeyOf(screen) != null;
    }

    public boolean isOverviewOpen() {
        return overviewOpen;
    }

    public void onStorageOpen() {
        overviewOpen = true;
        restoreCursor(); // keep the mouse where it was if we came back here via navigation
    }

    /**
     * If a backpack open is queued (from a page click that navigated back to the hub), try it once the
     * hub's slots have loaded. Called each render frame (after scanHub) so it retries until ready.
     */
    private void tryPendingBackpack(AbstractContainerScreen<?> screen) {
        if (pendingBackpackNum < 0) return;
        Integer slot = hubBackpackByNum.get(pendingBackpackNum);
        if (slot == null) return; // hub not fully loaded yet — retry next frame
        pendingBackpackNum = -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && mc.gameMode != null) {
            mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot, 0, ContainerInput.PICKUP, mc.player);
        }
    }

    private static int findGoBackSlot(AbstractContainerScreen<?> screen) {
        for (Slot s : screen.getMenu().slots) {
            if (s.container instanceof Inventory || s.getItem().isEmpty()) continue;
            String n = strip(s.getItem().getHoverName().getString()).strip().toLowerCase(Locale.ROOT);
            if (n.equals("go back") || n.startsWith("go back")) return s.index;
        }
        return -1;
    }

    private static String pageKeyOf(AbstractContainerScreen<?> screen) {
        String title = strip(screen.getTitle().getString()).strip();
        Matcher ec = ENDER.matcher(title);
        if (ec.find()) return "ender_" + parseInt(ec.group(1));
        Matcher bp = BACKPACK_SLOT.matcher(title);
        if (bp.find()) return "backpack_" + parseInt(bp.group(1));
        return null;
    }

    private static int enderPageOf(String key) {
        return key.startsWith("ender_") ? parseInt(key.substring(6)) : 0;
    }

    /** Placeholder grid size so a page keeps the same on-screen height once opened (no position jump). */
    private static int placeholderCount(Page page) {
        return page.key().startsWith("ender_") ? 45 : 9; // ender = 45 slots (5 rows); backpack default 9
    }

    // ── Capture ──────────────────────────────────────────────────────────────────

    public void capture(AbstractContainerScreen<?> screen) {
        loadIfNeeded();
        String title = strip(screen.getTitle().getString()).strip();

        String key;
        String label;
        int order;
        Matcher ec = ENDER.matcher(title);
        if (ec.find()) {
            int n = parseInt(ec.group(1));
            key = "ender_" + n;
            label = "Ender Chest " + n;
            order = n;
            maxEnderPages = Math.max(maxEnderPages, parseInt(ec.group(2))); // the /M total
        } else if (title.toLowerCase(Locale.ROOT).contains("backpack")) {
            Matcher m = BACKPACK_SLOT.matcher(title);
            int n = m.find() ? parseInt(m.group(1)) : 0;
            key = "backpack_" + (n == 0 ? title.hashCode() : n);
            label = title;
            order = 1000 + n;
        } else {
            return;
        }

        // Store the whole storage grid (container minus the first/navigation row), empties included,
        // so pages render as real inventories and empty slots stay placeable.
        List<ItemStack> items = new ArrayList<>();
        boolean navLoaded = false; // row 0 (nav row) has items once the container is actually loaded
        for (Slot s : screen.getMenu().slots) {
            if (s.container instanceof Inventory) continue;
            if (s.getContainerSlot() < 9) {
                if (!s.getItem().isEmpty()) navLoaded = true;
                continue;
            }
            items.add(s.getItem().copy());
        }
        // Cache once the container is loaded — even a genuinely EMPTY backpack (nav row present, storage
        // rows empty) is now stored; only the not-yet-loaded frame (everything empty) is skipped.
        if (!navLoaded) return;
        Page prev = pages.get(key);
        if (prev != null && sameItems(prev.items(), items)) return; // unchanged → no re-save
        pages.put(key, new Page(key, label, order, items));
        save();
    }

    private static boolean sameItems(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            ItemStack x = a.get(i), y = b.get(i);
            if (x.getCount() != y.getCount() || !ItemStack.isSameItemSameComponents(x, y)) return false;
        }
        return true;
    }

    /** Learns the backpack heads shown in the "Storage" hub ("Backpack Slot N" → GUI slot + name). */
    public void scanHub(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();
        for (Slot s : menu.slots) {
            if (s.container instanceof Inventory || s.getItem().isEmpty()) continue;
            // Real backpacks are player-head skulls; the "Backpacks" header + empty slots aren't heads.
            if (s.getItem().getItem() != net.minecraft.world.item.Items.PLAYER_HEAD) continue;
            String name = strip(s.getItem().getHoverName().getString()).strip();
            if (!name.toLowerCase(Locale.ROOT).contains("backpack")) continue;
            int n = extractSlotNum(name); // "Backpack Slot 13" → 13
            if (n < 0) for (String l : loreLines(s.getItem())) { n = extractSlotNum(l); if (n >= 0) break; }
            if (n >= 0) {
                hubBackpackByNum.put(n, s.index);
                hubBackpackName.put(n, name);
            }
        }
    }

    private static int extractSlotNum(String s) {
        Matcher m = SLOT_HASH.matcher(strip(s));
        return m.find() ? parseInt(m.group(1)) : -1;
    }

    private static List<String> loreLines(ItemStack stack) {
        net.minecraft.world.item.component.ItemLore lore =
            stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore == null) return List.of();
        List<String> out = new ArrayList<>();
        for (net.minecraft.network.chat.Component c : lore.lines()) out.add(c.getString());
        return out;
    }

    public List<Page> ordered() {
        List<Page> out = new ArrayList<>(pages.values());
        out.sort((a, b) -> Integer.compare(a.order, b.order));
        return out;
    }

    /** Cached pages + a placeholder (items == null) for every not-yet-opened Ender Chest / Backpack. */
    public List<Page> orderedWithPlaceholders() {
        List<Page> out = ordered();
        java.util.Set<String> have = new java.util.HashSet<>();
        for (Page p : out) have.add(p.key());
        // Ender Chest placeholders (count known from the "(N/M)" title).
        for (int n = 1; n <= maxEnderPages; n++) {
            String k = "ender_" + n;
            if (!have.contains(k)) out.add(new Page(k, "Ender Chest " + n, n, null));
        }
        // Backpack placeholders (from the hub scan) for any backpack whose page isn't cached yet,
        // matched by the "(Slot #N)" number so a cached backpack never gets a duplicate placeholder.
        for (Map.Entry<Integer, String> e : hubBackpackName.entrySet()) {
            if (have.contains("backpack_" + e.getKey())) continue;
            out.add(new Page("backpack_" + e.getKey(), e.getValue(), 2000 + e.getKey(), null));
        }
        out.sort((a, b) -> Integer.compare(a.order, b.order));
        return out;
    }

    /** Opens the page for a clicked hub block: ender via /ec, backpack by clicking its real hub slot. */
    private void openPage(AbstractContainerScreen<?> screen, Page page) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        saveCursor();
        int enderNum = page.key().startsWith("ender_") ? parseInt(page.key().substring(6)) : 0;
        if (enderNum > 0 && mc.getConnection() != null) {
            mc.getConnection().sendCommand("ec " + enderNum);
            return;
        }
        // Backpacks open via "/bp N" (like ender chests via "/ec N") — works from anywhere, no hub
        // round-trip. N is the "(Slot #N)" number from the page key.
        int num = page.key().startsWith("backpack_") ? parseInt(page.key().substring(9)) : -1;
        if (num >= 0 && mc.getConnection() != null) {
            mc.getConnection().sendCommand("bp " + num);
        }
    }

    public boolean isEmpty() {
        return pages.isEmpty();
    }

    public void clear() {
        pages.clear();
        save();
    }

    // ── Disk persistence (survives game restarts) ────────────────────────────────

    private boolean loaded = false;

    private static java.nio.file.Path cacheFile() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
            .resolve("horizon").resolve("storage_cache.nbt");
    }

    /** Loads the cached pages from disk once a world (with registries) is available. */
    public void loadIfNeeded() {
        if (loaded) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return; // need the registry access to decode items
        loaded = true;
        try {
            java.nio.file.Path f = cacheFile();
            if (!java.nio.file.Files.exists(f)) return;
            net.minecraft.nbt.CompoundTag root = net.minecraft.nbt.NbtIo.read(f);
            if (root == null) return;
            var ops = mc.level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            net.minecraft.nbt.ListTag pageList = root.getListOrEmpty("pages");
            for (int i = 0; i < pageList.size(); i++) {
                net.minecraft.nbt.CompoundTag pt = pageList.getCompoundOrEmpty(i);
                String key = pt.getStringOr("key", "");
                if (key.isEmpty()) continue;
                String label = pt.getStringOr("label", key);
                int order = pt.getIntOr("order", 0);
                List<ItemStack> items = new ArrayList<>();
                net.minecraft.nbt.ListTag itemList = pt.getListOrEmpty("items");
                for (int j = 0; j < itemList.size(); j++) {
                    items.add(ItemStack.CODEC.parse(ops, itemList.getCompoundOrEmpty(j)).result().orElse(ItemStack.EMPTY));
                }
                pages.put(key, new Page(key, label, order, items));
            }
        } catch (Exception ignored) {
            // Corrupt/old cache → just start empty.
        }
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        try {
            var ops = mc.level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            net.minecraft.nbt.ListTag pageList = new net.minecraft.nbt.ListTag();
            for (Page page : pages.values()) {
                net.minecraft.nbt.CompoundTag pt = new net.minecraft.nbt.CompoundTag();
                pt.putString("key", page.key());
                pt.putString("label", page.label());
                pt.putInt("order", page.order());
                net.minecraft.nbt.ListTag itemList = new net.minecraft.nbt.ListTag();
                for (ItemStack st : page.items()) {
                    if (st.isEmpty()) itemList.add(new net.minecraft.nbt.CompoundTag());
                    else itemList.add(ItemStack.CODEC.encodeStart(ops, st).result().orElse(new net.minecraft.nbt.CompoundTag()));
                }
                pt.put("items", itemList);
                pageList.add(pt);
            }
            net.minecraft.nbt.CompoundTag root = new net.minecraft.nbt.CompoundTag();
            root.put("pages", pageList);
            java.nio.file.Path f = cacheFile();
            java.nio.file.Files.createDirectories(f.getParent());
            net.minecraft.nbt.NbtIo.write(root, f);
        } catch (Exception ignored) {
        }
    }

    // ── Hub overview (read-only, togglable) ──────────────────────────────────────

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        loadIfNeeded();
        scanHub(screen); // learn backpack heads for placeholders + opening
        tryPendingBackpack(screen); // run a queued backpack open once the hub has loaded
        if (!overviewOpen) {
            renderReopenButton(screen, ctx, mouseX, mouseY);
            return;
        }
        int width = screen.width;
        int height = screen.height;
        Font font = Minecraft.getInstance().font;

        ctx.fill(0, 0, width, height, BACKDROP);
        ctx.text(font, Component.literal("§bStorage"), 16, 10, ACCENT);
        ctx.text(font, Component.literal("§8[X] / ESC → Menü"), 60, 12, MUTED);

        String cursor = ((System.currentTimeMillis() / 400L) % 2L == 0L) ? "_" : "";
        String shown = search.isEmpty() ? "Suche... (tippen)" : search + cursor;
        ctx.text(font, Component.literal("§7Suche: §f" + shown), 16, 26, search.isEmpty() ? MUTED : TEXT);
        ctx.fill(16, 38, width - 16, 39, 0xFF4A5568);
        ctx.text(font, Component.literal("§c[X]"), width - 20, 8, 0xFFFF7777);

        List<Page> pageList = orderedWithPlaceholders();
        if (pageList.isEmpty()) {
            ctx.centeredText(font, Component.literal(
                    "§7Keine gecachten Container. Oeffne Ender Chest / Backpacks zum Fuellen."),
                width / 2, height / 2, MUTED);
            return;
        }

        String query = search.trim().toLowerCase(Locale.ROOT);
        int pageBlockW = GRID_W + PAGE_GAP;
        int totalW = PAGE_COLS * pageBlockW - PAGE_GAP;
        int startX = Math.max(8, (width - totalW) / 2);

        ctx.enableScissor(0, HEADER, width, height);
        int rowTop = HEADER - scrollOffset;
        int rowMaxH = 0;
        ItemStack hovered = ItemStack.EMPTY;
        hubNav.clear();

        for (int p = 0; p < pageList.size(); p++) {
            int col = p % PAGE_COLS;
            if (col == 0 && p > 0) { rowTop += rowMaxH + PAGE_GAP; rowMaxH = 0; }
            int px = startX + col * pageBlockW;

            Page page = pageList.get(p);
            boolean placeholder = page.items() == null;
            List<ItemStack> items = placeholder ? null : page.items();
            int count = placeholder ? placeholderCount(page) : items.size();
            int rows = Math.max(1, (count + COLS - 1) / COLS);
            int blockH = 14 + rows * CELL;
            rowMaxH = Math.max(rowMaxH, blockH);
            int enderNum = page.key().startsWith("ender_") ? parseInt(page.key().substring(6)) : 0;
            hubNav.add(new int[]{px, rowTop, GRID_W, blockH, enderNum});

            if (rowTop + blockH >= HEADER && rowTop <= height) {
                String head = placeholder ? "§7" + page.label() + " §8(Klick: laden)"
                    : "§e" + page.label() + (enderNum > 0 ? " §8(Klick: öffnen)" : "");
                ctx.text(font, Component.literal(head), px, rowTop + 2, TEXT);
                for (int i = 0; i < count; i++) {
                    int cx = px + (i % COLS) * CELL;
                    int cy = rowTop + 14 + (i / COLS) * CELL;
                    if (cy + CELL < HEADER || cy > height) continue;
                    if (placeholder) { ctx.fill(cx, cy, cx + CELL, cy + CELL, 0x40FFFFFF); continue; }
                    ItemStack stack = items.get(i);
                    boolean hov = mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL;
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, hov ? CELL_HOVER : CELL_BG);
                    if (stack.isEmpty()) continue;
                    ctx.item(stack, cx + 1, cy + 1);
                    ctx.itemDecorations(font, stack, cx + 1, cy + 1);
                    if (!(query.isEmpty()
                        || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))) {
                        ctx.fill(cx, cy, cx + CELL, cy + CELL, DIM);
                    }
                    if (hov) hovered = stack;
                }
            }
        }
        ctx.disableScissor();

        contentHeight = (rowTop + rowMaxH + scrollOffset) - HEADER;
        if (!hovered.isEmpty()) ctx.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
    }

    private void renderReopenButton(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        boolean hov = mx >= BTN_X && mx < BTN_X + BTN_W && my >= BTN_Y && my < BTN_Y + BTN_H;
        ctx.fill(BTN_X, BTN_Y, BTN_X + BTN_W, BTN_Y + BTN_H, hov ? 0xFF2A3550 : 0xE0101820);
        ctx.fill(BTN_X, BTN_Y, BTN_X + BTN_W, BTN_Y + 1, 0xFF4A5568);
        ctx.text(font, Component.literal("§b☰ Übersicht"), BTN_X + 6, BTN_Y + 3, ACCENT);
    }

    public boolean onScroll(AbstractContainerScreen<?> screen, double vertical) {
        if (!overviewOpen) return false;
        int viewport = screen.height - HEADER;
        int max = Math.max(0, contentHeight - viewport);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.round(vertical * CELL * 1.5)));
        return true;
    }

    public boolean onClick(AbstractContainerScreen<?> screen, double mx, double my) {
        if (!overviewOpen) {
            if (mx >= BTN_X && mx < BTN_X + BTN_W && my >= BTN_Y && my < BTN_Y + BTN_H) {
                overviewOpen = true;
                return true;
            }
            return false;
        }
        if (mx >= screen.width - 22 && my >= 4 && my < 22) { overviewOpen = false; return true; }
        // Click a page block → open that page. Ender via /ec, Backpacks by clicking their real hub slot.
        List<Page> pl = orderedWithPlaceholders();
        for (int i = 0; i < hubNav.size() && i < pl.size(); i++) {
            int[] r = hubNav.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                openPage(screen, pl.get(i));
                return true;
            }
        }
        return true;
    }

    public boolean onKey(KeyEvent input) {
        if (!overviewOpen) return false;
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) return false; // let vanilla close the whole storage
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            scrollOffset = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) return true;
        Character c = charFor(key);
        if (c != null && search.length() < 48) { search += c; scrollOffset = 0; return true; }
        return true;
    }

    // ── Interactive page overlay (real slots relocated → native handling) ─────────

    public void onPageOpen() {
        // Keep search + scroll across page navigation (opening another ender/backpack shouldn't jump
        // back to the top or clear the search). Restore the cursor so it doesn't recenter on nav.
        restoreCursor();
    }

    /** Records the current cursor position so it can be restored after a navigation reopens the GUI. */
    private void saveCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) { savedCursorX = mc.mouseHandler.xpos(); savedCursorY = mc.mouseHandler.ypos(); }
    }

    private void restoreCursor() {
        if (savedCursorX < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), savedCursorX, savedCursorY);
        }
        savedCursorX = savedCursorY = -1;
    }

    /**
     * Runs from the extractBackground mixin (before the real slots render): sets the container bounds
     * to the whole screen (so vanilla never treats a grid click as "outside" → no accidental close),
     * relocates the open page's real slots + player inventory into the combined grid, and draws the
     * backdrop, cached other pages and search box. Vanilla then draws the relocated slots natively.
     */
    public void relocateAndRenderBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        AbstractContainerMenu menu = screen.getMenu();
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) (Object) screen;
        int width = screen.width;
        int height = screen.height;
        // Container spans the whole screen at (0,0): slot.x/y are absolute AND no click is "outside".
        acc.setLeftPos(0);
        acc.setTopPos(0);
        acc.setImageWidth(width);
        acc.setImageHeight(height);

        String curKey = pageKeyOf(screen);
        String query = search.trim().toLowerCase(Locale.ROOT);
        pageNav.clear();
        pageNavPages.clear();
        for (Slot s : menu.slots) setPos(s, -9999, -9999); // park all; visible ones re-placed below

        ctx.fill(0, 0, width, height, BACKDROP);
        ctx.text(font, Component.literal("§bStorage"), 16, 10, ACCENT);
        String cursor = ((System.currentTimeMillis() / 400L) % 2L == 0L) ? "_" : "";
        ctx.text(font, Component.literal("§7Suche: §f" + (search.isEmpty() ? "..." : search + cursor)),
            16, 26, search.isEmpty() ? MUTED : TEXT);
        ctx.fill(16, 38, width - 16, 39, 0xFF4A5568);
        ctx.text(font, Component.literal("§8ESC schließt"), width - 90, 12, MUTED);

        // Player inventory (fixed bottom) — relocate the real inventory slots.
        int invOx = (width - GRID_W) / 2;
        int invTop = height - (16 + 4 * CELL) - 40; // higher so it clears the custom scoreboard
        ctx.text(font, Component.literal("§7Inventar"), invOx, invTop, MUTED);
        for (Slot s : menu.slots) {
            if (!(s.container instanceof Inventory)) continue;
            int cs = s.getContainerSlot();
            int col = cs < 9 ? cs : (cs - 9) % 9;
            int row = cs < 9 ? 3 : (cs - 9) / 9;
            int cx = invOx + col * CELL;
            int cy = invTop + 12 + row * CELL + (cs < 9 ? 4 : 0);
            drawCellBg(ctx, cx, cy, matches(s.getItem(), query));
            setPos(s, cx + 1, cy + 1);
        }

        // Storage pages (scroll area, 3 across).
        int areaTop = HEADER;
        int areaBottom = invTop - 6;
        pageViewportH = areaBottom - areaTop; // remember for the scroll clamp
        List<Slot> storageSlots = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (!(s.container instanceof Inventory) && s.getContainerSlot() >= 9) storageSlots.add(s);
        }
        int pageBlockW = GRID_W + PAGE_GAP;
        int totalW = PAGE_COLS * pageBlockW - PAGE_GAP;
        int startX = Math.max(8, (width - totalW) / 2);
        List<Page> pageList = orderedWithPlaceholders();
        // Ensure the just-opened page shows (from its live slots) even before it's cached (backpacks
        // have no placeholder, and the current ender page renders live via isCurrent below).
        if (curKey != null && pageList.stream().noneMatch(p -> p.key().equals(curKey))) {
            int ord = curKey.startsWith("ender_") ? parseInt(curKey.substring(6)) : 1000;
            String lbl = curKey.startsWith("ender_") ? "Ender Chest " + curKey.substring(6)
                : strip(screen.getTitle().getString()).strip();
            pageList = new ArrayList<>(pageList);
            pageList.add(new Page(curKey, lbl, ord, List.of()));
            pageList.sort((a, b) -> Integer.compare(a.order(), b.order()));
        }

        ctx.enableScissor(0, areaTop, width, areaBottom);
        int rowTop = areaTop - scrollOffset;
        int rowMaxH = 0;
        for (int p = 0; p < pageList.size(); p++) {
            int col = p % PAGE_COLS;
            if (col == 0 && p > 0) { rowTop += rowMaxH + PAGE_GAP; rowMaxH = 0; }
            int px = startX + col * pageBlockW;
            Page page = pageList.get(p);
            boolean isCurrent = page.key().equals(curKey);
            boolean placeholder = !isCurrent && page.items() == null;
            int count = isCurrent ? storageSlots.size() : (placeholder ? placeholderCount(page) : page.items().size());
            int rows = Math.max(1, (count + COLS - 1) / COLS);
            int blockH = 14 + rows * CELL;
            rowMaxH = Math.max(rowMaxH, blockH);
            int enderNum = page.key().startsWith("ender_") ? parseInt(page.key().substring(6)) : 0;

            if (rowTop + blockH >= areaTop && rowTop <= areaBottom) {
                String head = isCurrent ? "§a▶ " + page.label()
                    : placeholder ? "§7" + page.label() + " §8(Klick: laden)" : "§e" + page.label();
                ctx.text(font, Component.literal(head), px, rowTop + 2, TEXT);
                if (isCurrent) ctx.fill(px - 2, rowTop + 12, px - 1, rowTop + blockH, CURRENT_BORDER);
                for (int i = 0; i < count; i++) {
                    int cx = px + (i % COLS) * CELL;
                    int cy = rowTop + 14 + (i / COLS) * CELL;
                    boolean cellVisible = cy >= areaTop && cy + CELL <= areaBottom;
                    if (!cellVisible) continue;
                    if (isCurrent) {
                        Slot s = storageSlots.get(i);
                        drawCellBg(ctx, cx, cy, matches(s.getItem(), query));
                        setPos(s, cx + 1, cy + 1); // real slot → vanilla renders + handles it
                    } else if (placeholder) {
                        ctx.fill(cx, cy, cx + CELL, cy + CELL, 0x40FFFFFF);
                    } else {
                        ItemStack st = page.items().get(i);
                        drawCellBg(ctx, cx, cy, matches(st, query));
                        if (!st.isEmpty()) { ctx.item(st, cx + 1, cy + 1); ctx.itemDecorations(font, st, cx + 1, cy + 1); }
                    }
                }
            }
            if (!isCurrent) { pageNav.add(new int[]{px, rowTop, GRID_W, blockH}); pageNavPages.add(page); }
        }
        ctx.disableScissor();
        pageContentHeight = (rowTop + rowMaxH + scrollOffset) - areaTop;
    }

    private static void setPos(Slot s, int x, int y) {
        SlotAccessor a = (SlotAccessor) (Object) s;
        a.setX(x);
        a.setY(y);
    }

    private void drawCellBg(GuiGraphicsExtractor ctx, int cx, int cy, boolean highlight) {
        ctx.fill(cx, cy, cx + CELL, cy + CELL, highlight ? HIGHLIGHT : CELL_BG);
    }

    private static boolean matches(ItemStack stack, String query) {
        return !query.isEmpty() && !stack.isEmpty()
            && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** @return true if consumed. Real slots are left to vanilla; cached pages navigate; empty swallows. */
    public boolean onPageClick(AbstractContainerScreen<?> screen, double mx, double my) {
        AbstractContainerMenu menu = screen.getMenu();
        for (Slot s : menu.slots) {
            if (s.x > -1000 && mx >= s.x && mx < s.x + 16 && my >= s.y && my < s.y + 16) {
                return false; // a real (relocated) slot → vanilla handles it natively
            }
        }
        for (int i = 0; i < pageNav.size() && i < pageNavPages.size(); i++) {
            int[] n = pageNav.get(i);
            if (mx >= n[0] && mx < n[0] + n[2] && my >= n[1] && my < n[1] + n[3]) {
                openPage(screen, pageNavPages.get(i)); // ender via /ec, backpack via its hub slot
                return true;
            }
        }
        return true; // empty overlay space → swallow (prevents a vanilla drop/close)
    }

    public boolean onPageScroll(AbstractContainerScreen<?> screen, double vertical) {
        int max = Math.max(0, pageContentHeight - pageViewportH + CELL); // +CELL so the last row fully clears
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.round(vertical * CELL * 1.5)));
        return true;
    }

    public boolean onPageKey(KeyEvent input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options.keyInventory.matches(input)) return false;
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            scrollOffset = 0;
            return true;
        }
        Character c = charFor(key);
        if (c != null && search.length() < 48) { search += c; scrollOffset = 0; return true; }
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static Character charFor(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return (char) ('a' + (key - GLFW.GLFW_KEY_A));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return (char) ('0' + (key - GLFW.GLFW_KEY_0));
        if (key == GLFW.GLFW_KEY_SPACE) return ' ';
        return null;
    }

    private static boolean isFiller(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.endsWith("stained_glass_pane") || id.equals("black_stained_glass");
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
    }
}
