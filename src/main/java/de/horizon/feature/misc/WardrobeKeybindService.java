package de.horizon.feature.misc;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds keyboard navigation to the Hypixel Wardrobe screen.
 * Left/Right arrows flip wardrobe pages; number keys 1–9 equip the corresponding slot.
 */
public final class WardrobeKeybindService {
    private static final Pattern WARDROBE_TITLE = Pattern.compile("Wardrobe\\s*\\((\\d)/(\\d)\\)", Pattern.CASE_INSENSITIVE);

    // Hypixel wardrobe layout: 6-row chest
    // Row 5 (slots 36–44): the 9 equippable armor-set buttons
    // Row 6 (slots 45–53): navigation (prev=45, next=53)
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_NEXT_PAGE = 53;
    private static final int EQUIP_SLOT_BASE = 36;

    public boolean handleKeyPress(AbstractContainerScreen<?> screen, int keyCode, HorizonConfig config) {
        if (!config.isWardrobeKeybindsEnabled()) return false;
        String title = screen.getTitle().getString();
        Matcher m = WARDROBE_TITLE.matcher(title);
        if (!m.find()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;

        int currentPage = Integer.parseInt(m.group(1));
        int maxPage     = Integer.parseInt(m.group(2));

        if (keyCode == GLFW.GLFW_KEY_LEFT && currentPage > 1) {
            clickSlot(mc, screen, SLOT_PREV_PAGE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && currentPage < maxPage) {
            clickSlot(mc, screen, SLOT_NEXT_PAGE);
            return true;
        }
        // Number keys 1–9: equip wardrobe slot on current page
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int slotOffset = keyCode - GLFW.GLFW_KEY_1; // 0–8
            int slotIndex = EQUIP_SLOT_BASE + slotOffset;
            // Check if this slot has an item (prevent clicking empty wardrobe slots)
            if (slotIndex < screen.getMenu().slots.size()) {
                Slot slot = screen.getMenu().slots.get(slotIndex);
                if (!slot.getItem().isEmpty()) {
                    clickSlot(mc, screen, slotIndex);
                    return true;
                }
            }
        }
        // U = unequip current outfit (find equipped slot and click it)
        if (keyCode == GLFW.GLFW_KEY_U) {
            int equipped = findEquippedSlot(screen);
            if (equipped >= 0) {
                clickSlot(mc, screen, equipped);
                return true;
            }
        }
        return false;
    }

    private static int findEquippedSlot(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        for (int i = EQUIP_SLOT_BASE; i < EQUIP_SLOT_BASE + 9 && i < slots.size(); i++) {
            if (isEquipped(slots.get(i).getItem())) return i;
        }
        return -1;
    }

    private static boolean isEquipped(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Hypixel uses the hover name "Slot X: Equipped" on the active armor set
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains("equipped")) return true;
        // Also check lore as fallback
        var lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore == null) return false;
        return lore.lines().stream().anyMatch(l -> l.getString().toLowerCase(Locale.ROOT).contains("equipped"));
    }

    private static void clickSlot(Minecraft mc, AbstractContainerScreen<?> screen, int slotIndex) {
        int containerId = screen.getMenu().containerId;
        mc.gameMode.handleContainerInput(containerId, slotIndex, 0, ContainerInput.PICKUP, mc.player);
    }
}
