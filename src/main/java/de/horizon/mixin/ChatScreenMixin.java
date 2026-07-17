package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.feature.chat.ChatCopyMode;
import de.horizon.feature.chat.ChatHudAccess;
import de.horizon.feature.chat.ChatTab;
import de.horizon.feature.chat.ChatTabManager;
import de.horizon.hypixel.HypixelSidebarOverlay;
import de.horizon.render.PillarboxState;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected EditBox input;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void horizon$moveChatFieldUp(CallbackInfo ci) {
        if (input == null) {
            return;
        }
        input.setY(adjustedScreenHeight(height) - 12);
        int barOffset = PillarboxState.scaledBarWidth();
        if (barOffset > 0) {
            input.setX(input.getX() + barOffset);
            input.setWidth(input.getWidth() - 2 * barOffset);
        }
    }

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void horizon$handleLocalCommands(String chatText, boolean addToHistory, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client == null || chatText == null || !chatText.startsWith("/")) {
            return;
        }
        if (!client.executeLocalCommand(chatText, this)) {
            return;
        }
        if (this.minecraft != null) {
            this.minecraft.gui.getChat().addRecentChat(chatText);
        }
        if (input != null) {
            input.setValue("");
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
        ci.cancel();
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void horizon$raiseChatInputBackground(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color) {
        int offset = HypixelSidebarOverlay.lowerHudOffset(minecraft);
        int barOffset = PillarboxState.scaledBarWidth();
        context.fill(x1 + barOffset, y1 - offset, x2 - barOffset, y2 - offset, color);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void horizon$renderChatTabs(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (input == null) {
            return;
        }
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }
        ChatTabManager tabManager = horizonClient.getChatTabManager();
        HorizonConfig config = horizonClient.getConfigManager().getConfig();

        int tabY = input.getY() - 14;
        int x = 2 + PillarboxState.scaledBarWidth();

        for (ChatTab tab : ChatTab.values()) {
            boolean active = tab == tabManager.getActiveTab();
            int bgColor = active ? 0xCC75E7CA : 0x80333333;
            int textColor = active ? 0xFF1E2A37 : 0xFFCCCCCC;
            context.fill(x, tabY, x + 12, tabY + 10, bgColor);
            context.centeredText(font, Component.literal(tab.key()), x + 6, tabY + 1, textColor);
            x += 14;
        }

        // Bridge toggle — highlighted when bridge IS visible (not hidden)
        x += 4;
        boolean bridgeHidden = config.isChatBridgeHidden();
        int bridgeBg = bridgeHidden ? 0x80333333 : 0xCC75E7CA;
        int bridgeText = bridgeHidden ? 0xFFCCCCCC : 0xFF1E2A37;
        context.fill(x, tabY, x + 12, tabY + 10, bridgeBg);
        context.centeredText(font, Component.literal("B"), x + 6, tabY + 1, bridgeText);

        // Guild Chat toggle — highlighted when guild chat IS visible (not hidden)
        x += 14;
        boolean guildHidden = config.isGuildChatHidden();
        int guildBg = guildHidden ? 0x80333333 : 0xCC75E7CA;
        int guildTextColor = guildHidden ? 0xFFCCCCCC : 0xFF1E2A37;
        context.fill(x, tabY, x + 12, tabY + 10, guildBg);
        context.centeredText(font, Component.literal("G"), x + 6, tabY + 1, guildTextColor);
    }


    // ── 1.21.10: click detection calls ChatHud.mouseClicked(x, y) directly.
    @ModifyArg(method = "mouseClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;mouseClicked(DD)Z"),
            index = 1, require = 0)
    private double horizon$adjustChatHudClickY(double y) {
        return y + ChatTabManager.TAB_BAR_LIFT;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void horizon$handleChatCopy(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (this.minecraft == null || input == null) {
            return;
        }
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }
        ChatCopyMode mode = horizonClient.getConfigManager().getConfig().getChatCopyMode();
        if (mode == ChatCopyMode.OFF) {
            return;
        }
        boolean isLeft = click.button() == 0;
        boolean isRight = click.button() == 1;
        long handle = this.minecraft.getWindow().handle();
        boolean ctrlHeld = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean triggered = switch (mode) {
            case CTRL_LEFT -> isLeft && ctrlHeld;
            case RIGHT -> isRight;
            case BOTH -> (isLeft && ctrlHeld) || isRight;
            default -> false;
        };
        if (!triggered) {
            return;
        }
        boolean fullEntry = horizonClient.getConfigManager().getConfig().isChatCopyFullMessage();
        String text = ((ChatHudAccess) this.minecraft.gui.getChat()).horizon$getMessageTextAt(click.x(), click.y(), fullEntry);
        if (text == null || text.isBlank()) {
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(text);
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void horizon$handleTabClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || input == null) {
            return;
        }
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }

        int tabY = input.getY() - 14;
        double mx = click.x();
        double my = click.y();

        if (my < tabY || my > tabY + 10) {
            return;
        }

        int x = 2 + PillarboxState.scaledBarWidth();
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
            return;
        }

        // Guild Chat button
        x += 14;
        if (mx >= x && mx <= x + 12) {
            HorizonConfig config = horizonClient.getConfigManager().getConfig();
            config.setGuildChatHidden(!config.isGuildChatHidden());
            horizonClient.getConfigManager().save();
            horizonClient.getChatTabManager().repopulateAfterGuildToggle(config);
            cir.setReturnValue(true);
        }
    }

    private int adjustedScreenHeight(int originalHeight) {
        int offset = HypixelSidebarOverlay.lowerHudOffset(minecraft);
        return Math.max(originalHeight - offset, HypixelSidebarOverlay.BAR_HEIGHT);
    }
}
