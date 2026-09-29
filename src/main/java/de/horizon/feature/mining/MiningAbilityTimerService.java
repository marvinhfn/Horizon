package de.horizon.feature.mining;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mining ability cooldown timer.
 *
 * <p>When the player right-clicks a mining tool (drill / pickaxe / gauntlet — anything that shows a
 * "Breaking Power" line and a "Cooldown: Ns" ability) the tool's ability goes on cooldown. This
 * service reads the cooldown length straight from the item lore and drives a small charge bar drawn
 * under the crosshair: grey while charging, the configured "ready" colour (rose by default) once the
 * ability is available again. The bar is outlined so the remaining charge time is easy to read.
 */
public final class MiningAbilityTimerService {
    private static final Pattern STRIP = Pattern.compile("(?i)§[0-9a-fk-or]");
    // "Cooldown: 30s", "Cooldown: 120 Seconds", "Cooldown: 5 s"
    private static final Pattern COOLDOWN = Pattern.compile("(?i)cooldown:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:s\\b|second)");

    private static final int GREY = 0xFFB0B0B0;
    private static final int BAR_WIDTH = 90;
    private static final int BAR_HEIGHT = 5;

    private String activeToolName = "";
    private long cooldownStartMs = 0L;
    private long cooldownTotalMs = 0L;

    /** Right-click with a held item. Starts the timer if it is a mining tool with an ability cooldown. */
    public void onRightClick(ItemStack held) {
        if (held == null || held.isEmpty()) return;
        if (!isMiningTool(held)) return;
        double seconds = parseCooldownSeconds(held);
        if (seconds <= 0) return;

        String name = held.getHoverName().getString();
        long now = System.currentTimeMillis();
        // Ability is still on cooldown → the click can't have re-triggered it, keep the running timer.
        if (name.equals(activeToolName) && now < cooldownStartMs + cooldownTotalMs) return;

        activeToolName = name;
        cooldownStartMs = now;
        cooldownTotalMs = (long) (seconds * 1000.0);
    }

    public void render(GuiGraphicsExtractor ctx, Minecraft mc, HorizonConfig config) {
        if (!config.isMiningAbilityTimerEnabled()) return;
        if (mc == null || mc.player == null || cooldownTotalMs <= 0) return;

        // Only while the timed tool is actually held — switching away hides the bar.
        ItemStack main = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main == null || main.isEmpty() || !main.getHoverName().getString().equals(activeToolName)) return;

        long now = System.currentTimeMillis();
        float progress = Math.min(1f, (now - cooldownStartMs) / (float) cooldownTotalMs);
        boolean ready = progress >= 1f;
        int fillColor = ready ? (config.getMiningAbilityReadyColor() | 0xFF000000) : GREY;

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        int x = (guiW - BAR_WIDTH) / 2;
        int y = guiH / 2 + 12; // just below the crosshair

        // Outline of the full bar region (shows how much is left to charge).
        int outline = 0xC0000000;
        ctx.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y, outline);
        ctx.fill(x - 1, y + BAR_HEIGHT, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, outline);
        ctx.fill(x - 1, y, x, y + BAR_HEIGHT, outline);
        ctx.fill(x + BAR_WIDTH, y, x + BAR_WIDTH + 1, y + BAR_HEIGHT, outline);

        // Dark track behind the fill.
        ctx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x80202020);
        int fillW = ready ? BAR_WIDTH : Math.max(0, Math.round(progress * BAR_WIDTH));
        if (fillW > 0) ctx.fill(x, y, x + fillW, y + BAR_HEIGHT, fillColor);
    }

    private static boolean isMiningTool(ItemStack stack) {
        for (String line : loreLines(stack)) {
            if (line.toLowerCase(Locale.ROOT).contains("breaking power")) return true;
        }
        return false;
    }

    private static double parseCooldownSeconds(ItemStack stack) {
        for (String line : loreLines(stack)) {
            Matcher m = COOLDOWN.matcher(line);
            if (m.find()) {
                try {
                    return Double.parseDouble(m.group(1));
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static List<String> loreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return List.of();
        return lore.lines().stream()
                .map(Component::getString)
                .map(s -> STRIP.matcher(s).replaceAll(""))
                .toList();
    }
}
