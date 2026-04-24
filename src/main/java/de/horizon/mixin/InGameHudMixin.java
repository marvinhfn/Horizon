package de.horizon.mixin;
import de.horizon.HorizonClient;
import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.math.MathHelper;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_CONTAINER_HARDCORE = Identifier.ofVanilla("hud/heart/container_hardcore");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier PURPLE_HEART_FULL = Identifier.of("horizon", "textures/gui/heart/purple_full.png");
    private static final Identifier PURPLE_HEART_HALF = Identifier.of("horizon", "textures/gui/heart/purple_half.png");

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private static void renderArmor(DrawContext context, PlayerEntity player, int x, int y, int height, int blinkingHeartIndex) {
        throw new AssertionError();
    }

    @Shadow
    private void renderHealthBar(DrawContext context, PlayerEntity player, int x, int y, int height, int blinkingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        throw new AssertionError();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void horizon$hideHypixelSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HypixelSidebarOverlay.shouldReplaceVanillaSidebar(client)) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"))
    private int horizon$raiseHotbar(DrawContext context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"))
    private int horizon$raiseHeldItemTooltip(DrawContext context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "renderMountHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"))
    private int horizon$raiseMountHud(DrawContext context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"))
    private int horizon$raiseStatusBars(DrawContext context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderArmor(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIII)V"))
    private void horizon$conditionallyHideDefenseBar(DrawContext context, PlayerEntity player, int x, int y, int height, int blinkingHeartIndex) {
        if (shouldHideDefenseBar()) {
            return;
        }
        renderArmor(context, player, x, y, height, blinkingHeartIndex);
    }

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"))
    private void horizon$compressHypixelHealthBar(InGameHud hud, DrawContext context, PlayerEntity player, int x, int y, int height, int blinkingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        if (!shouldCompressHypixelHealth(maxHealth, absorption)) {
            renderHealthBar(context, player, x, y, height, blinkingHeartIndex, maxHealth, lastHealth, health, absorption, blinking);
            return;
        }

        renderCompactHypixelHealth(context, player, x, y, health, absorption);
    }

    @Redirect(method = "renderMainHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"))
    private void horizon$raiseExperienceLevelNumber(DrawContext context, TextRenderer textRenderer, int level) {
        if (level <= 0) {
            return;
        }

        Text text = Text.translatable("gui.experience.level", level);
        int x = (context.getScaledWindowWidth() - textRenderer.getWidth(text)) / 2;
        int y = adjustedHeight(context) - 35;
        context.drawText(textRenderer, text, x + 1, y, 0xFF000000, false);
        context.drawText(textRenderer, text, x - 1, y, 0xFF000000, false);
        context.drawText(textRenderer, text, x, y + 1, 0xFF000000, false);
        context.drawText(textRenderer, text, x, y - 1, 0xFF000000, false);
        context.drawText(textRenderer, text, x, y, 0xFF80FF20, false);
    }

    private int adjustedHeight(DrawContext context) {
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(client)) {
            return context.getScaledWindowHeight();
        }
        return context.getScaledWindowHeight() - HypixelSidebarOverlay.HOTBAR_OFFSET;
    }

    private boolean shouldCompressHypixelHealth(float maxHealth, int absorption) {
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(client)) {
            return false;
        }
        return maxHealth + absorption > 20.0F;
    }

    private void renderCompactHypixelHealth(DrawContext context, PlayerEntity player, int x, int y, int health, int absorption) {
        boolean hardcore = player.getEntityWorld().getLevelProperties().isHardcore();
        int combinedHealth = Math.max(health + absorption, 0);
        int overflowUnits = MathHelper.clamp(combinedHealth - 20, 0, 20);
        int baseUnits = MathHelper.clamp(combinedHealth, 0, 20);

        for (int slot = 9; slot >= 0; slot--) {
            int heartX = x + slot * 8;
            int heartY = y;
            int unitStart = slot * 2;

            drawVanillaHeart(context, hardcore ? HEART_CONTAINER_HARDCORE : HEART_CONTAINER, heartX, heartY);

            int baseRemainder = baseUnits - unitStart;
            if (baseRemainder > 0) {
                drawVanillaHeart(context, baseRemainder == 1 ? HEART_HALF : HEART_FULL, heartX, heartY);
            }

            int overflowRemainder = overflowUnits - unitStart;
            if (overflowRemainder > 0) {
                drawPurpleHeart(context, overflowRemainder == 1 ? PURPLE_HEART_HALF : PURPLE_HEART_FULL, heartX, heartY);
            }
        }
    }

    private void drawVanillaHeart(DrawContext context, Identifier texture, int x, int y) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 9, 9);
    }

    private void drawPurpleHeart(DrawContext context, Identifier texture, int x, int y) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 9, 9, 9, 9, 0xFFFFFFFF);
    }

    private boolean shouldHideDefenseBar() {
        HorizonClient horizon = HorizonClient.getInstance();
        return horizon != null && horizon.getConfigManager().getConfig().isHideDefenseBar();
    }
}
