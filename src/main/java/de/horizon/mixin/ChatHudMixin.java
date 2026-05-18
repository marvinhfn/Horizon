package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.feature.chat.ChatTabManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
