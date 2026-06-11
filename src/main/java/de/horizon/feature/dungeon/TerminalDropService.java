package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import de.horizon.mixin.KeyBindingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Blocks item dropping for a short time after the player leaves a dungeon
 * terminal to prevent accidental drops.  Also provides instant continuous
 * dropping when the drop key is held (no OS key-repeat delay).
 */
public final class TerminalDropService {

    private static final long DROP_BLOCK_MILLIS = 2000L;
    /** Ticks to wait before continuous dropping starts (1 tick = 50 ms). */
    private static final int HOLD_DROP_DELAY_TICKS = 3;

    private boolean wasInTerminal = false;
    private long terminalClosedAt = 0L;
    private boolean dropBlockActive = false;
    private int dropKeyHeldTicks = 0;

    public void tick(Minecraft mc, HorizonConfig config) {
        if (mc.player == null || !config.isTerminalDropSwapEnabled()) {
            wasInTerminal = false;
            return;
        }

        boolean inTerminal = isInTerminal(mc);

        if (wasInTerminal && !inTerminal) {
            terminalClosedAt = System.currentTimeMillis();
            dropBlockActive = true;
        }

        // Consume any drop key presses during the block window
        if (dropBlockActive) {
            if (System.currentTimeMillis() - terminalClosedAt >= DROP_BLOCK_MILLIS) {
                dropBlockActive = false;
            } else {
                while (mc.options.keyDrop.consumeClick()) {
                    // eat the input
                }
            }
        }

        // Fast continuous dropping: bypass OS key-repeat delay.
        // When the drop key is physically held, drop every tick after a
        // short initial delay.
        if (mc.screen == null && !dropBlockActive && isDropKeyHeld(mc)) {
            dropKeyHeldTicks++;
            if (dropKeyHeldTicks > HOLD_DROP_DELAY_TICKS) {
                while (mc.options.keyDrop.consumeClick()) { /* consume */ }
                mc.player.drop(false);
            }
        } else {
            dropKeyHeldTicks = 0;
        }

        wasInTerminal = inTerminal;
    }

    /** Returns true if item dropping should be blocked (2s after leaving a terminal). */
    public boolean shouldBlockDrop() {
        return dropBlockActive;
    }

    public void onDisconnect() {
        wasInTerminal = false;
        terminalClosedAt = 0L;
        dropBlockActive = false;
        dropKeyHeldTicks = 0;
    }

    private static boolean isDropKeyHeld(Minecraft mc) {
        InputConstants.Key key = ((KeyBindingAccessor) mc.options.keyDrop).getBoundKey();
        long handle = mc.getWindow().handle();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, key.getValue()) == GLFW.GLFW_PRESS;
    }

    private static boolean isInTerminal(Minecraft mc) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return false;
        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        return title.contains("correct all the panes")
            || title.contains("change all to same color")
            || title.contains("click in order")
            || title.contains("what starts with")
            || title.contains("select all the")
            || title.contains("click the button on time")
            || title.contains("navigate the maze");
    }
}
