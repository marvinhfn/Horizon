package de.horizon.feature.misc;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds keyboard navigation to the Hypixel SkyBlock Loadout screen.
 * Left/Right arrows flip loadout pages; number keys 1–9, 0, -, = equip the 12 loadout slots.
 */
public final class LoadoutKeybindService {
    private static final Pattern LOADOUT_TITLE = Pattern.compile("\\((\\d)/(\\d)\\) Loadout");

    // 12 loadout buttons (3 columns × 4 rows) in a 6-row chest.
    private static final int[] LOADOUT_SLOTS = {
        14, 15, 16,
        23, 24, 25,
        32, 33, 34,
        41, 42, 43
    };
    private static final int SLOT_PREV_PAGE = 17;
    private static final int SLOT_NEXT_PAGE = 44;

    public boolean handleKeyPress(AbstractContainerScreen<?> screen, int keyCode, HorizonConfig config) {
        if (!config.isLoadoutKeybindsEnabled()) return false;
        Matcher m = LOADOUT_TITLE.matcher(screen.getTitle().getString());
        if (!m.find()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;

        int currentPage = Integer.parseInt(m.group(1));
        int maxPage = Integer.parseInt(m.group(2));

        if (keyCode == GLFW.GLFW_KEY_LEFT && currentPage > 1) {
            clickSlot(mc, screen, SLOT_PREV_PAGE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && currentPage < maxPage) {
            clickSlot(mc, screen, SLOT_NEXT_PAGE);
            return true;
        }

        int index = loadoutIndex(keyCode);
        if (index >= 0 && index < LOADOUT_SLOTS.length) {
            int slotIndex = LOADOUT_SLOTS[index];
            if (slotIndex < screen.getMenu().slots.size()) {
                Slot slot = screen.getMenu().slots.get(slotIndex);
                if (!slot.getItem().isEmpty()) {
                    clickSlot(mc, screen, slotIndex);
                    return true;
                }
            }
        }
        return false;
    }

    /** Maps a key to a loadout index 0–11: 1–9 → 0–8, 0 → 9, MINUS → 10, EQUAL → 11. */
    private static int loadoutIndex(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) return keyCode - GLFW.GLFW_KEY_1;
        if (keyCode == GLFW.GLFW_KEY_0) return 9;
        if (keyCode == GLFW.GLFW_KEY_MINUS) return 10;
        if (keyCode == GLFW.GLFW_KEY_EQUAL) return 11;
        return -1;
    }

    private static void clickSlot(Minecraft mc, AbstractContainerScreen<?> screen, int slotIndex) {
        int containerId = screen.getMenu().containerId;
        mc.gameMode.handleContainerInput(containerId, slotIndex, 0, ContainerInput.PICKUP, mc.player);
    }
}
