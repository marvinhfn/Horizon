package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.feature.chat.ChatHudAccess;
import de.horizon.feature.chat.ChatTabManager;
import de.horizon.render.PillarboxState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements ChatHudAccess {

    @Shadow
    MinecraftClient client;

    @Shadow
    private List<ChatHudLine.Visible> visibleMessages;

    @Shadow
    private int scrolledLines;

    @Shadow
    abstract double getChatScale();

    @Shadow
    abstract int getLineHeight();

    @Unique
    @Override
    public String horizon$getMessageTextAt(double mouseX, double mouseY, boolean fullEntry) {
        if (visibleMessages == null || visibleMessages.isEmpty()) {
            return null;
        }
        int scaledHeight = client.getWindow().getScaledHeight();
        double adjustedY = mouseY + ChatTabManager.TAB_BAR_LIFT;
        double d = (scaledHeight - adjustedY - 40.0) / ((double) getLineHeight() * getChatScale());
        if (d < 0.0) {
            return null;
        }
        int lineIndex = (int) d + scrolledLines;
        if (lineIndex < 0 || lineIndex >= visibleMessages.size()) {
            return null;
        }
        if (!fullEntry) {
            String single = horizon$lineToString(visibleMessages.get(lineIndex).content()).trim();
            return single.isEmpty() ? null : single;
        }
        // visibleMessages stores entries newest-first. Within each entry, the bottom-most
        // visible line is at the LOWEST index (endOfEntry=true), and higher indices hold
        // lines further up (reading order: highest index = top line of message).
        //
        // Find the bottom of this entry (walk toward lower indices until endOfEntry=true).
        int bottom = lineIndex;
        while (bottom > 0 && !visibleMessages.get(bottom).endOfEntry()) {
            bottom--;
        }
        // Find the top of this entry (walk toward higher indices until the next element
        // starts a new entry via endOfEntry=true, or we hit the end of the list).
        int top = lineIndex;
        while (top + 1 < visibleMessages.size() && !visibleMessages.get(top + 1).endOfEntry()) {
            top++;
        }
        // Collect from top down to bottom (high index → low index) for correct reading order.
        StringBuilder sb = new StringBuilder();
        for (int i = top; i >= bottom; i--) {
            if (i < top) {
                sb.append(' ');
            }
            sb.append(horizon$lineToString(visibleMessages.get(i).content()));
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    @Unique
    private String horizon$lineToString(OrderedText text) {
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
        int barOffset = PillarboxState.scaledBarWidth();
        if (focused || barOffset > 0) {
            context.getMatrices().pushMatrix();
            float translateY = focused ? -ChatTabManager.TAB_BAR_LIFT : 0.0f;
            context.getMatrices().translate(barOffset, translateY);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("TAIL"))
    private void horizon$popChatLift(DrawContext context, TextRenderer textRenderer, int ticks,
                                      int mouseX, int mouseY, boolean focused, boolean showingQueued,
                                      CallbackInfo ci) {
        int barOffset = PillarboxState.scaledBarWidth();
        if (focused || barOffset > 0) {
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
