package de.horizon.feature.inventory;

import de.horizon.config.HorizonConfig;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Locale;

/**
 * Highlights the currently-summoned pet in the Pets menu — the one whose lore says
 * "Click to despawn!". Drawn before the tooltip so it sits behind hover tooltips.
 */
public final class PetHighlightService {
    private static final int HIGHLIGHT = 0x8000C853;

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, HorizonConfig config) {
        if (!config.isPetHighlightEnabled()) return;
        String title = strip(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
        if (!title.contains("pets")) return;

        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) (Object) screen;
        int left = acc.getLeftPos();
        int top = acc.getTopPos();
        for (Slot s : screen.getMenu().slots) {
            if (s.container instanceof Inventory) continue;
            ItemStack stack = s.getItem();
            if (stack.isEmpty()) continue;
            if (hasDespawnLore(stack)) {
                int x = left + s.x, y = top + s.y;
                ctx.fill(x, y, x + 16, y + 16, HIGHLIGHT);
            }
        }
    }

    private static boolean hasDespawnLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component c : lore.lines()) {
            if (strip(c.getString()).toLowerCase(Locale.ROOT).contains("click to despawn")) return true;
        }
        return false;
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
    }
}
