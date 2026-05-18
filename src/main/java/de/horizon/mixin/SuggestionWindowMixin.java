package de.horizon.mixin;

import de.horizon.feature.chat.ChatTabManager;
import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lifts the command-suggestion window so it appears above the chat-tab buttons
 * and accounts for the Hypixel sidebar raising the chat input.
 * Uses targets = "..." to avoid compile-time access to the private inner class.
 */
@Mixin(targets = "net.minecraft.client.gui.screen.ChatInputSuggestor$SuggestionWindow")
public abstract class SuggestionWindowMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void horizon$liftOnCreate(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Only adjust for the ChatScreen — command blocks use a fixed y=72 and need no lift.
        if (client == null || !(client.currentScreen instanceof ChatScreen)) {
            return;
        }
        // TAB_BAR_LIFT: clear the chat-filter tab buttons above the input.
        // lowerHudOffset: match the amount the chat input was raised for the Hypixel sidebar.
        int lift = ChatTabManager.TAB_BAR_LIFT + HypixelSidebarOverlay.lowerHudOffset(client);
        Rect2i area = ((SuggestionWindowAccessor) (Object) this).getArea();
        if (area != null) {
            area.setY(area.getY() - lift);
        }
    }
}
