package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.feature.chat.ChatTabManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow
    MinecraftClient client;

    /**
     * Intercept every game message before it is added to the visible chat.
     * We store all messages in our own history buffer and only let through
     * those that match the active tab (and bridge toggle).
     */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void horizon$interceptMessage(Text message, CallbackInfo ci) {
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }
        ChatTabManager tabManager = horizonClient.getChatTabManager();
        if (tabManager.isRepopulating()) {
            // Repopulate pass — let the message through without storing it again
            return;
        }
        boolean show = tabManager.onMessageAdded(message, horizonClient.getConfigManager().getConfig());
        if (!show) {
            ci.cancel();
        }
    }

    /**
     * Lift the entire chat render upward by TAB_BAR_LIFT pixels when the chat
     * screen is focused, so the tab buttons below have clear space.
     */
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
    private void horizon$pushChatLift(DrawContext context, TextRenderer textRenderer, int ticks,
                                       int mouseX, int mouseY, boolean focused, boolean showingQueued,
                                       CallbackInfo ci) {
        if (focused) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, -ChatTabManager.TAB_BAR_LIFT);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("TAIL"))
    private void horizon$popChatLift(DrawContext context, TextRenderer textRenderer, int ticks,
                                      int mouseX, int mouseY, boolean focused, boolean showingQueued,
                                      CallbackInfo ci) {
        if (focused) {
            context.getMatrices().popMatrix();
        }
    }

    // ── 1.21.10: fix click detection for chat links.
    // getTextStyleAt calls toChatLineY(y) to map screen Y to a chat line index.
    // The visual render applies translate(0, -TAB_BAR_LIFT), so messages appear
    // TAB_BAR_LIFT pixels higher than their logical positions. We compensate by
    // adding TAB_BAR_LIFT to y before toChatLineY converts it, so the line index
    // matches the visual position. require=0: toChatLineY does not exist in 1.21.11.
    @ModifyArg(method = "getTextStyleAt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;toChatLineY(D)D"),
            require = 0)
    private double horizon$liftChatLinkClickY(double y) {
        return (client.currentScreen instanceof ChatScreen) ? y + ChatTabManager.TAB_BAR_LIFT : y;
    }

    // ── 1.21.11: fix click detection for chat links.
    // ChatScreen.mouseClicked passes Window.getScaledHeight() as scaledHeight to
    // ChatHud.render(DrawnTextConsumer, scaledHeight, ...) for hit-testing. Messages
    // are positioned at (scaledHeight - 40 - lineIdx*lineHeight). The visual render
    // applies translate(0, -TAB_BAR_LIFT), shifting messages up by TAB_BAR_LIFT pixels.
    // Reducing scaledHeight by TAB_BAR_LIFT shifts hit-test positions by the same amount,
    // aligning click areas with the visual chat. require=0: this overload does not exist
    // in 1.21.10.
    @ModifyVariable(method = "render(Lnet/minecraft/client/font/DrawnTextConsumer;IIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int horizon$liftClickRenderScaledHeight(int scaledHeight) {
        return (client.currentScreen instanceof ChatScreen)
                ? scaledHeight - ChatTabManager.TAB_BAR_LIFT
                : scaledHeight;
    }
}
