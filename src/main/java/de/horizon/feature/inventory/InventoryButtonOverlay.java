package de.horizon.feature.inventory;

import de.horizon.config.ConfigManager;
import de.horizon.hypixel.HypixelSidebarOverlay;
import de.horizon.hypixel.SkyBlockIsland;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Renders inventory buttons around any {@link AbstractContainerScreen}.
 *
 * Positions are computed relative to the GUI background centre so that they
 * align regardless of screen resolution.  The default inventory size 176 × 166
 * is used as the reference; buttons sit 4 px away from each edge.
 */
public final class InventoryButtonOverlay {

    // visual constants
    private static final int BTN_SIZE  = 18;  // each button slot: 18 × 18
    private static final int GAP       = 4;   // gap between inventory edge and buttons
    private static final int SLOT_FILL = 0x88333333;
    private static final int SLOT_BORDER = 0xFFAAAAAA;
    private static final int ACTIVE_FILL = 0x8822AA44;
    private static final int INACTIVE_FILL = 0x88AA2222;

    private final ConfigManager configManager;
    private final InventoryButtonService service;

    // Hit-test data rebuilt each render frame
    private final SlotHit[] hits = new SlotHit[26];
    private int hitCount = 0;

    public InventoryButtonOverlay(ConfigManager configManager, InventoryButtonService service) {
        this.configManager = configManager;
        this.service = service;
    }

    // ── Render ───────────────────────────────────────────────────────────────

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (!configManager.getConfig().isInventoryButtonsEnabled()) return;

        hitCount = 0;
        int cx = screen.width  / 2;
        int cy = screen.height / 2;

        List<InventoryButton> buttons = configManager.getConfig().getInventoryButtons();

        for (int i = 0; i < 9; i++) {
            renderSlot(context, mc, buttons, "top_"    + i,
                    cx - 80 + i * BTN_SIZE, cy - 83 - GAP - BTN_SIZE, mouseX, mouseY);
            renderSlot(context, mc, buttons, "bottom_" + i,
                    cx - 80 + i * BTN_SIZE, cy + 83 + GAP,             mouseX, mouseY);
        }
        for (int i = 0; i < 4; i++) {
            renderSlot(context, mc, buttons, "left_"   + i,
                    cx - 88 - GAP - BTN_SIZE, cy - 50 + i * BTN_SIZE, mouseX, mouseY);
            renderSlot(context, mc, buttons, "right_"  + i,
                    cx + 88 + GAP,             cy - 50 + i * BTN_SIZE, mouseX, mouseY);
        }
    }

    private void renderSlot(GuiGraphicsExtractor context, Minecraft mc,
                             List<InventoryButton> buttons, String slotId,
                             int x, int y, int mouseX, int mouseY) {
        InventoryButton button = findButton(buttons, slotId);
        if (button == null) return; // nothing configured → invisible in overlay mode
        if (button.islandFilterEnabled && !button.allowedIslands.isEmpty()) {
            SkyBlockIsland current = HypixelSidebarOverlay.liveIsland(mc);
            if (!button.allowedIslands.contains(current.id())) return;
        }

        boolean hovered = mouseX >= x && mouseX < x + BTN_SIZE
                       && mouseY >= y && mouseY < y + BTN_SIZE;

        int fill = SLOT_FILL;
        if (button.toggle) {
            fill = button.toggleActive ? ACTIVE_FILL : INACTIVE_FILL;
        }
        context.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, fill);
        if (hovered) {
            context.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0x33FFFFFF);
        }
        drawBorder(context, x, y, BTN_SIZE, BTN_SIZE, SLOT_BORDER);

        // Item icon
        String itemId = (button.toggle && !button.toggleActive)
                ? button.itemIdInactive : button.itemIdActive;
        ItemStack stack = InventoryButtonItems.resolve(itemId);
        context.item(stack, x + 1, y + 1);

        // Tooltip on hover
        if (hovered && !button.label.isBlank()) {
            context.setTooltipForNextFrame(mc.font,
                    net.minecraft.network.chat.Component.literal(button.label),
                    mouseX, mouseY);
        }

        // Register hit area
        if (hitCount < hits.length) {
            if (hits[hitCount] == null) hits[hitCount] = new SlotHit();
            hits[hitCount].button = button;
            hits[hitCount].x = x; hits[hitCount].y = y;
            hitCount++;
        }
    }

    // ── Mouse click ──────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int btn) {
        if (btn != 0) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        for (int i = 0; i < hitCount; i++) {
            SlotHit h = hits[i];
            if (mouseX >= h.x && mouseX < h.x + BTN_SIZE
             && mouseY >= h.y && mouseY < h.y + BTN_SIZE) {
                service.activateButton(h.button, mc);
                return true;
            }
        }
        return false;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static InventoryButton findButton(List<InventoryButton> buttons, String slotId) {
        for (InventoryButton b : buttons) {
            if (slotId.equals(b.slotId)) return b;
        }
        return null;
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private static final class SlotHit {
        InventoryButton button;
        int x, y;
    }
}
