package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.feature.chat.ChatHudAccess;
import de.horizon.feature.chat.ChatTabManager;
import de.horizon.render.PillarboxState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin implements ChatHudAccess {

    @Shadow
    Minecraft minecraft;

    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    abstract double getScale();

    @Shadow
    abstract int getLineHeight();

    @Unique
    @Override
    public String horizon$getMessageTextAt(double mouseX, double mouseY, boolean fullEntry) {
        if (trimmedMessages == null || trimmedMessages.isEmpty()) {
            return null;
        }
        int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
        double adjustedY = mouseY + ChatTabManager.TAB_BAR_LIFT;
        double d = (scaledHeight - adjustedY - 40.0) / ((double) getLineHeight() * getScale());
        if (d < 0.0) {
            return null;
        }
        int lineIndex = (int) d + chatScrollbarPos;
        if (lineIndex < 0 || lineIndex >= trimmedMessages.size()) {
            return null;
        }
        if (!fullEntry) {
            String single = horizon$lineToString(trimmedMessages.get(lineIndex).content()).trim();
            return single.isEmpty() ? null : single;
        }
        int bottom = lineIndex;
        while (bottom > 0 && !trimmedMessages.get(bottom).endOfEntry()) {
            bottom--;
        }
        int top = lineIndex;
        while (top + 1 < trimmedMessages.size() && !trimmedMessages.get(top + 1).endOfEntry()) {
            top++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = top; i >= bottom; i--) {
            if (i < top) {
                sb.append(' ');
            }
            sb.append(horizon$lineToString(trimmedMessages.get(i).content()));
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    @Unique
    private String horizon$lineToString(FormattedCharSequence text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codepoint) -> {
            sb.appendCodePoint(codepoint);
            return true;
        });
        return sb.toString();
    }

    /**
     * Intercept every message before it is added to the visible chat.
     * We store all messages in our own history buffer and only let through
     * those that match the active tab (and bridge toggle).
     */
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    private void horizon$interceptMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        HorizonClient horizonClient = HorizonClient.getInstance();
        if (horizonClient == null) {
            return;
        }
        ChatTabManager tabManager = horizonClient.getChatTabManager();
        if (tabManager.isRepopulating()) {
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
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"))
    private void horizon$pushChatLift(GuiGraphicsExtractor context, Font textRenderer, int ticks,
                                       int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean showingQueued,
                                       CallbackInfo ci) {
        boolean focused = displayMode == ChatComponent.DisplayMode.FOREGROUND;
        int barOffset = PillarboxState.scaledBarWidth();
        if (focused || barOffset > 0) {
            context.pose().pushMatrix();
            float translateY = focused ? -ChatTabManager.TAB_BAR_LIFT : 0.0f;
            context.pose().translate(barOffset, translateY);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("TAIL"))
    private void horizon$popChatLift(GuiGraphicsExtractor context, Font textRenderer, int ticks,
                                      int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean showingQueued,
                                      CallbackInfo ci) {
        boolean focused = displayMode == ChatComponent.DisplayMode.FOREGROUND;
        int barOffset = PillarboxState.scaledBarWidth();
        if (focused || barOffset > 0) {
            context.pose().popMatrix();
        }
    }

    @ModifyVariable(method = "captureClickableText(Lnet/minecraft/client/gui/ActiveTextCollector;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int horizon$liftClickRenderScaledHeight(int scaledHeight) {
        return (minecraft.screen instanceof ChatScreen)
                ? scaledHeight - ChatTabManager.TAB_BAR_LIFT
                : scaledHeight;
    }
}
