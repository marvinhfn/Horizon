package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected TextFieldWidget chatField;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void horizon$moveChatFieldUp(CallbackInfo ci) {
        if (chatField == null) {
            return;
        }
        chatField.setY(adjustedScreenHeight(height) - 12);
    }

    @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void horizon$handleLocalCommands(String chatText, boolean addToHistory, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client == null || chatText == null || !chatText.startsWith("/")) {
            return;
        }
        if (!client.executeLocalCommand(chatText, this)) {
            return;
        }
        if (chatField != null) {
            chatField.setText("");
        }
        if (this.client != null) {
            this.client.setScreen(null);
        }
        ci.cancel();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void horizon$raiseChatInputBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int offset = HypixelSidebarOverlay.lowerHudOffset(client);
        context.fill(x1, y1 - offset, x2, y2 - offset, color);
    }

    private int adjustedScreenHeight(int originalHeight) {
        int offset = HypixelSidebarOverlay.lowerHudOffset(client);
        return Math.max(originalHeight - offset, HypixelSidebarOverlay.BAR_HEIGHT);
    }
}
