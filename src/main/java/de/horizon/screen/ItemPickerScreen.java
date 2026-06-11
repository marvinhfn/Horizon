package de.horizon.screen;

import de.horizon.feature.inventory.InventoryButtonItems;
import de.horizon.feature.inventory.SkyBlockHeadCache;
import de.horizon.hud.HudStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A searchable item-picker screen.
 *
 * Supports plain Minecraft item IDs ("minecraft:diamond") and
 * Hypixel SkyBlock skull items ("HEAD:CONDENSED_FERMENTO").
 *
 * The caller receives the selected item-ID string via the {@code callback}.
 */
public final class ItemPickerScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int COLS      = 9;
    private static final int CELL_SIZE = 20;
    private static final int PAD       = 16;
    private static final int HEADER    = 52;   // space for title + search

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int BG          = 0xD0101820;
    private static final int CELL_BG     = 0x60F0F1F3;
    private static final int CELL_HOVER  = 0xA0A0C0FF;
    private static final int TEXT_COLOR  = 0xFFFFFFFF;
    private static final int MUTED       = 0xFFB8B8B8;
    private static final int BORDER      = 0xFF4A5568;

    private final Screen parent;
    private final Consumer<String> callback;

    private String searchInput = "";
    private int    scrollOffset = 0;
    private List<Entry> entries = new ArrayList<>();

    // cached item list (all Minecraft items)
    private static List<String> allItemIds = null;

    public ItemPickerScreen(Screen parent, Consumer<String> callback) {
        super(Component.literal("Item waehlen"));
        this.parent   = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        refreshEntries();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!Character.isISOControl(input.codepoint()) && searchInput.length() < 64) {
            searchInput += Character.toString(input.codepoint());
            scrollOffset = 0;
            refreshEntries();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchInput.isEmpty()) {
                searchInput = searchInput.substring(0, searchInput.length() - 1);
                scrollOffset = 0;
                refreshEntries();
            }
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        // Paste
        if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && input.key() == GLFW.GLFW_KEY_V) {
            if (minecraft != null) {
                String clip = minecraft.keyboardHandler.getClipboard();
                for (int i = 0; i < clip.length() && searchInput.length() < 64; i++) {
                    char c = clip.charAt(i);
                    if (!Character.isISOControl(c)) searchInput += c;
                }
                scrollOffset = 0;
                refreshEntries();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        int maxScroll = maxScroll();
        scrollOffset = Math.max(0, Math.min(maxScroll,
                scrollOffset - (int) Math.round(vAmount * CELL_SIZE)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        // Check grid cells
        int cols  = gridCols();
        int ox    = gridOriginX(cols);
        int oy    = HEADER;
        int visibleRows = (height - HEADER) / CELL_SIZE;
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx  = ox + col * CELL_SIZE;
            int cy  = oy + row * CELL_SIZE - scrollOffset;
            if (cy < HEADER - CELL_SIZE || cy > height) continue;
            if (click.x() >= cx && click.x() < cx + CELL_SIZE
             && click.y() >= cy && click.y() < cy + CELL_SIZE) {
                select(entries.get(i).id);
                return true;
            }
        }
        // Close button area
        if (click.x() >= width - 20 && click.x() < width
         && click.y() >= 0          && click.y() < 20) {
            onClose();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    // Whether the cache was loaded during the last render cycle
    private boolean wasCacheLoaded = false;

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Auto-refresh grid once the SkyBlock head cache finishes loading
        if (searchInput.toLowerCase(Locale.ROOT).startsWith("head:")) {
            boolean nowLoaded = SkyBlockHeadCache.isLoaded();
            if (nowLoaded && !wasCacheLoaded) {
                wasCacheLoaded = true;
                refreshEntries();
            }
        }

        context.fill(0, 0, width, height, BG);

        // Title
        int accent = HudStyle.accent();
        context.text(font,
                Component.literal("Item waehlen"), PAD, 10, accent);

        // Search field
        String cursor = ((System.currentTimeMillis() / 400L) % 2L == 0L) ? "_" : "";
        String display = searchInput.isEmpty()
                ? "Suche... (HEAD:ITEM_ID fuer SkyBlock Skulls)"
                : searchInput + cursor;
        int fieldColor = searchInput.isEmpty() ? MUTED : TEXT_COLOR;
        context.text(font,
                Component.literal("Suche: " + display), PAD, 28, fieldColor);
        // underline
        context.fill(PAD, 40, width - PAD, 41, BORDER);

        // Status for HEAD: searches
        if (searchInput.toUpperCase().startsWith("HEAD:")) {
            String status = SkyBlockHeadCache.isLoading() ? "Lade Hypixel SkyBlock Items..."
                    : SkyBlockHeadCache.hasFailed()  ? "Fehler beim Laden der SkyBlock Items."
                    : "";
            if (!status.isEmpty()) {
                context.text(font, Component.literal(status), PAD, 44, MUTED);
            }
        }

        // Grid
        context.enableScissor(0, HEADER, width, height);
        int cols = gridCols();
        int ox   = gridOriginX(cols);
        for (int i = 0; i < entries.size(); i++) {
            Entry e   = entries.get(i);
            int col   = i % cols;
            int row   = i / cols;
            int cx    = ox + col * CELL_SIZE;
            int cy    = HEADER + row * CELL_SIZE - scrollOffset;
            if (cy + CELL_SIZE < HEADER || cy > height) continue;

            boolean hov = mouseX >= cx && mouseX < cx + CELL_SIZE
                       && mouseY >= cy && mouseY < cy + CELL_SIZE;
            context.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE,
                    hov ? CELL_HOVER : CELL_BG);
            context.item(e.stack, cx + 2, cy + 2);
        }
        context.disableScissor();

        // Tooltip
        for (int i = 0; i < entries.size(); i++) {
            Entry e   = entries.get(i);
            int col   = i % cols;
            int row   = i / cols;
            int cx    = gridOriginX(cols) + col * CELL_SIZE;
            int cy    = HEADER + row * CELL_SIZE - scrollOffset;
            if (mouseX >= cx && mouseX < cx + CELL_SIZE
             && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                context.setTooltipForNextFrame(font, Component.literal(e.id), mouseX, mouseY);
                break;
            }
        }

        // Close hint
        context.text(font, Component.literal("[X]"),
                width - 20, 6, 0xFFFF7777);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void refreshEntries() {
        entries = new ArrayList<>();
        String query = searchInput.toLowerCase(Locale.ROOT);

        if (query.startsWith("head:")) {
            // Hypixel SkyBlock skull search
            SkyBlockHeadCache.ensureLoaded();
            String skinId = query.substring(5);
            // Enumerate matching cache entries (empty skinId = show all)
            for (java.util.Map.Entry<String, String> kv :
                    getSkyBlockEntries(skinId.toUpperCase())) {
                String headId = "HEAD:" + kv.getKey();
                ItemStack stack = InventoryButtonItems.createSkullFromTexture(kv.getValue());
                entries.add(new Entry(headId, stack));
                if (entries.size() >= 200) break;
            }
            // If cache is empty / still loading, show a placeholder
            if (entries.isEmpty()) {
                entries.add(new Entry("HEAD:", new ItemStack(Items.PLAYER_HEAD)));
            }
            return;
        }

        // Plain Minecraft items
        for (String id : getAllItemIds()) {
            if (id.contains(query)) {
                Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse(id))
                        .map(ref -> ref.value()).orElse(Items.AIR);
                if (item != Items.AIR) {
                    entries.add(new Entry(id, new ItemStack(item)));
                }
            }
            if (entries.size() >= 500) break;
        }
    }

    private static List<String> getAllItemIds() {
        if (allItemIds == null) {
            allItemIds = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                allItemIds.add(id.toString());
            }
            allItemIds.sort(String::compareTo);
        }
        return allItemIds;
    }

    /** Returns cache entries whose key contains the given sub-string. */
    private static Iterable<java.util.Map.Entry<String, String>> getSkyBlockEntries(String query) {
        // Access the cache via reflection-free approach: iterate known IDs.
        // We expose a filtered view by calling SkyBlockHeadCache.getTexture() per match.
        // Since we don't have a public keySet(), we maintain a parallel list here.
        // Workaround: Re-expose needed subset via a helper on SkyBlockHeadCache.
        return SkyBlockHeadCache.getMatchingEntries(query);
    }

    private int gridCols() {
        return Math.max(1, (width - 2 * PAD) / CELL_SIZE);
    }

    private int gridOriginX(int cols) {
        return (width - cols * CELL_SIZE) / 2;
    }

    private int maxScroll() {
        int cols  = gridCols();
        int rows  = (int) Math.ceil((double) entries.size() / cols);
        int total = rows * CELL_SIZE;
        int visible = height - HEADER;
        return Math.max(0, total - visible);
    }

    private void select(String id) {
        callback.accept(id);
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private record Entry(String id, ItemStack stack) {}
}
