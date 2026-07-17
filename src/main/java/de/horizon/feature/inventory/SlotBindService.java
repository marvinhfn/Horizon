package de.horizon.feature.inventory;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.HashMap;
import java.util.Map;

import java.util.Map;

/**
 * Slot Binds: bind an inventory slot to a hotbar slot.
 * Shift+clicking the inventory slot swaps it with the bound hotbar slot instead of the default shift-click behaviour.
 *
 * Slot indices follow the player inventory convention (Minecraft creative/survival):
 *  - Hotbar:           slots 36–44  (hotbar 0–8)
 *  - Main inventory:   slots 9–35
 *  - Armour / offhand: slots 45+ in container, but we use the menu slot index directly.
 *
 * We intercept shift-clicks on the player inventory screen only.
 */
public final class SlotBindService {
    private Integer pendingBindSlot = null;

    /**
     * Called when a shift-click occurs inside an InventoryScreen.
     * @param menu     the container menu
     * @param slotIndex the clicked slot index
     * @return {@code true} if the click was intercepted (should cancel vanilla behaviour)
     */
    public boolean handleShiftClick(AbstractContainerMenu menu, int slotIndex, HorizonConfig config, Minecraft mc) {
        if (!config.isSlotBindsEnabled()) return false;
        Map<Integer, Integer> binds = config.getSlotBinds();
        Integer boundHotbar = binds.get(slotIndex);
        if (boundHotbar == null) return false;

        // boundHotbar is a hotbar index 0–8; convert to container slot index 36–44
        int hotbarContainerSlot = 36 + boundHotbar;
        if (hotbarContainerSlot >= menu.slots.size()) return false;

        // Determine swap direction: from inventory slot to hotbar slot
        int from, to;
        if (slotIndex >= 36 && slotIndex <= 44) {
            // Source is hotbar, target is bound inventory slot
            from = boundHotbar; // hotbar number for SWAP click type
            to   = slotIndex;   // the inventory slot clicked
        } else {
            // Source is inventory, target is hotbar
            from = boundHotbar;
            to   = slotIndex;
        }

        mc.gameMode.handleContainerInput(
            menu.containerId,
            to,
            from,   // button = hotbar number for SWAP
            ContainerInput.SWAP,
            mc.player
        );
        return true;
    }

    /**
     * Starts or completes a bind-creation gesture.
     * First key press selects the first slot; second selects the second.
     * At least one slot must be hotbar (36–44).
     * @return a message to show the user, or null if nothing happened yet
     */
    public String handleBindKeyPress(int slotIndex, HorizonConfig config, Minecraft mc) {
        if (!config.isSlotBindsEnabled()) return null;

        // Range check: only slots 9–44 (inventory + hotbar)
        if (slotIndex < 9 || slotIndex > 44) return null;

        if (pendingBindSlot == null) {
            // Check if already bound – remove the binding
            Map<Integer, Integer> binds = config.getSlotBinds();
            if (binds.containsKey(slotIndex)) {
                Integer removed = binds.remove(slotIndex);
                return "§cRemoved bind on slot " + slotIndex;
            }
            pendingBindSlot = slotIndex;
            return "§eSelect second slot to bind to slot " + slotIndex + "…";
        } else {
            int first  = pendingBindSlot;
            int second = slotIndex;
            pendingBindSlot = null;

            if (first == second) {
                return "§cCan't bind a slot to itself.";
            }
            boolean firstIsHotbar  = (first  >= 36 && first  <= 44);
            boolean secondIsHotbar = (second >= 36 && second <= 44);
            if (!firstIsHotbar && !secondIsHotbar) {
                return "§cOne slot must be a hotbar slot (36–44).";
            }

            // Store binding from inventory slot → hotbar index
            int invSlot, hotbarSlot;
            if (firstIsHotbar) { hotbarSlot = first - 36; invSlot = second; }
            else               { invSlot = first; hotbarSlot = second - 36; }

            config.getSlotBinds().put(invSlot, hotbarSlot);
            return "§aBound slot " + invSlot + " ↔ hotbar " + (hotbarSlot + 1);
        }
    }

    public boolean hasPendingBind() {
        return pendingBindSlot != null;
    }

    public int getPendingBindSlot() {
        return pendingBindSlot != null ? pendingBindSlot : -1;
    }

    public void cancelPendingBind() {
        pendingBindSlot = null;
    }

    /**
     * Renders slot bind visualizations on an inventory screen:
     * - Outlines the pending (first-selected) slot and draws a tracer to the mouse cursor.
     * - When {@code showActive} is true, outlines and connects all active bind pairs.
     */
    public void renderOverlay(GuiGraphicsExtractor context, AbstractContainerScreen<?> screen,
                              int leftPos, int topPos, double mouseX, double mouseY,
                              HorizonConfig config, boolean showActive) {
        if (!config.isSlotBindsEnabled()) return;

        // Build slot index → top-left screen coordinates map
        Map<Integer, int[]> positions = new HashMap<>();
        for (Slot slot : screen.getMenu().slots) {
            positions.put(slot.index, new int[]{leftPos + slot.x, topPos + slot.y});
        }

        // Highlight first-selected slot and draw tracer to mouse
        if (pendingBindSlot != null) {
            int[] pos = positions.get(pendingBindSlot);
            if (pos != null) {
                context.outline(pos[0] - 2, pos[1] - 2, 20, 20, 0xFFFFAA00); // gold
                drawLine2D(context, pos[0] + 8, pos[1] + 8, (int) mouseX, (int) mouseY, 0xCCFFAA00);
            }
        }

        // Show all active binds with connection lines when key held
        if (showActive) {
            Map<Integer, Integer> binds = config.getSlotBinds();
            int[] palette = {0xAA55FF55, 0xAA5555FF, 0xAAFF55FF, 0xAA55FFFF, 0xAAFFFF55};
            int ci = 0;
            for (Map.Entry<Integer, Integer> e : binds.entrySet()) {
                int invIdx    = e.getKey();
                int hotbarIdx = 36 + e.getValue();
                int[] inv = positions.get(invIdx);
                int[] hot = positions.get(hotbarIdx);
                int color = palette[ci++ % palette.length];
                if (inv != null) context.outline(inv[0] - 2, inv[1] - 2, 20, 20, color);
                if (hot != null) context.outline(hot[0] - 2, hot[1] - 2, 20, 20, color);
                if (inv != null && hot != null) {
                    drawLine2D(context, inv[0] + 8, inv[1] + 8, hot[0] + 8, hot[1] + 8, color);
                }
            }
        }
    }

    private static void drawLine2D(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            context.fill(x1, y1, x1 + 2, y1 + 2, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            context.fill(x, y, x + 2, y + 2, color);
        }
    }
}
