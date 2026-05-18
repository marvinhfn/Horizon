package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.feature.chat.ChatTab;
import de.horizon.feature.chat.ChatTabManager;
import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.gui.Click;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "render", at = @At("TAIL"))
    private void horizon$renderChatTabs(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (chatField == null) {
            return;
        }
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }
        ChatTabManager tabManager = horizonClient.getChatTabManager();
        HorizonConfig config = horizonClient.getConfigManager().getConfig();

        int tabY = chatField.getY() - 14;
        int x = 2;

        for (ChatTab tab : ChatTab.values()) {
            boolean active = tab == tabManager.getActiveTab();
            int bgColor = active ? 0xCC75E7CA : 0x80333333;
            int textColor = active ? 0xFF1E2A37 : 0xFFCCCCCC;
            context.fill(x, tabY, x + 12, tabY + 10, bgColor);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(tab.key()), x + 6, tabY + 1, textColor);
            x += 14;
        }

        // Bridge toggle — highlighted when bridge IS visible (not hidden)
        x += 4;
        boolean bridgeHidden = config.isChatBridgeHidden();
        int bridgeBg = bridgeHidden ? 0x80333333 : 0xCC75E7CA;
        int bridgeText = bridgeHidden ? 0xFFCCCCCC : 0xFF1E2A37;
        context.fill(x, tabY, x + 12, tabY + 10, bridgeBg);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("B"), x + 6, tabY + 1, bridgeText);
    }

    /**
     * The chat HUD is rendered 14 px higher via a matrix translate in {@link ChatHudMixin}.
     * When MC detects which message was clicked it re-renders the chat with a hit-test consumer
     * using the raw mouse Y. We add TAB_BAR_LIFT to that Y so the re-render operates at the
     * logical (un-shifted) positions, matching the visual rendering.
     */
    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Click;y:()D"))
    private double horizon$adjustClickYForChatLift(Click click) {
        return click.y() + ChatTabManager.TAB_BAR_LIFT;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void horizon$handleTabClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || chatField == null) {
            return;
        }
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }

        int tabY = chatField.getY() - 14;
        double mx = click.x();
        double my = click.y();

        if (my < tabY || my > tabY + 10) {
            return;
        }

        int x = 2;
        for (ChatTab tab : ChatTab.values()) {
            if (mx >= x && mx <= x + 12) {
                horizonClient.getChatTabManager().setActiveTabAndRepopulate(tab, horizonClient.getConfigManager().getConfig());
                cir.setReturnValue(true);
                return;
            }
            x += 14;
        }

        // Bridge button
        x += 4;
        if (mx >= x && mx <= x + 12) {
            HorizonConfig config = horizonClient.getConfigManager().getConfig();
            config.setChatBridgeHidden(!config.isChatBridgeHidden());
            horizonClient.getConfigManager().save();
            horizonClient.getChatTabManager().repopulateAfterBridgeToggle(config);
            cir.setReturnValue(true);
        }
    }

    private int adjustedScreenHeight(int originalHeight) {
        int offset = HypixelSidebarOverlay.lowerHudOffset(client);
        return Math.max(originalHeight - offset, HypixelSidebarOverlay.BAR_HEIGHT);
    }
}
