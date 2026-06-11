package de.horizon.mixin;
import de.horizon.HorizonClient;
import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_CONTAINER_HARDCORE = Identifier.withDefaultNamespace("hud/heart/container_hardcore");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier ABSORBING_HEART_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_full");
    private static final Identifier ABSORBING_HEART_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_half");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private static void extractArmor(GuiGraphicsExtractor context, Player player, int x, int y, int height, int blinkingHeartIndex) {
        throw new AssertionError();
    }

    @Shadow
    private void extractHearts(GuiGraphicsExtractor context, Player player, int x, int y, int height, int blinkingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        throw new AssertionError();
    }

    @Inject(method = "extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void horizon$hideHypixelSidebar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (HypixelSidebarOverlay.shouldReplaceVanillaSidebar(minecraft)) {
            ci.cancel();
        }
    }

    @Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"))
    private int horizon$raiseHotbar(GuiGraphicsExtractor context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "extractSelectedItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"))
    private int horizon$raiseHeldItemTooltip(GuiGraphicsExtractor context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "extractVehicleHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"))
    private int horizon$raiseMountHud(GuiGraphicsExtractor context) {
        return adjustedHeight(context);
    }

    @Redirect(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"))
    private int horizon$raiseStatusBars(GuiGraphicsExtractor context) {
        return adjustedHeight(context);
    }

    /**
     * Raise the Hypixel action-bar health/defense/mana overlay when the compact-hearts
     * feature is off, so it sits above the vanilla heart rows instead of overlapping them.
     */
    @Redirect(method = "extractOverlayMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"))
    private int horizon$raiseHypixelBar(GuiGraphicsExtractor context) {
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(minecraft) || isCompactHypixelHealthEnabled()) {
            return context.guiHeight();
        }
        return context.guiHeight() - HypixelSidebarOverlay.HOTBAR_OFFSET;
    }

    @Redirect(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractArmor(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIII)V"))
    private void horizon$conditionallyHideDefenseBar(GuiGraphicsExtractor context, Player player, int x, int y, int height, int blinkingHeartIndex) {
        if (shouldHideDefenseBar()) {
            return;
        }
        extractArmor(context, player, x, y, height, blinkingHeartIndex);
    }

    @Redirect(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractHearts(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"))
    private void horizon$compressHypixelHealthBar(Gui hud, GuiGraphicsExtractor context, Player player, int x, int y, int height, int blinkingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        if (!shouldCompressHypixelHealth(maxHealth, absorption)) {
            extractHearts(context, player, x, y, height, blinkingHeartIndex, maxHealth, lastHealth, health, absorption, blinking);
            return;
        }

        renderCompactHypixelHealth(context, player, x, y, health, absorption);
    }

    @Redirect(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void horizon$raiseExperienceLevelNumber(GuiGraphicsExtractor context, Font textRenderer, int level) {
        if (level <= 0) {
            return;
        }

        Component text = Component.translatable("gui.experience.level", level);
        int x = (context.guiWidth() - textRenderer.width(text)) / 2;
        int y = adjustedHeight(context) - 35;
        context.text(textRenderer, text, x + 1, y, 0xFF000000, false);
        context.text(textRenderer, text, x - 1, y, 0xFF000000, false);
        context.text(textRenderer, text, x, y + 1, 0xFF000000, false);
        context.text(textRenderer, text, x, y - 1, 0xFF000000, false);
        context.text(textRenderer, text, x, y, 0xFF80FF20, false);
    }

    private int adjustedHeight(GuiGraphicsExtractor context) {
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(minecraft)) {
            return context.guiHeight();
        }
        return context.guiHeight() - HypixelSidebarOverlay.HOTBAR_OFFSET;
    }

    private boolean shouldCompressHypixelHealth(float maxHealth, int absorption) {
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(minecraft)) {
            return false;
        }
        if (!isCompactHypixelHealthEnabled()) {
            return false;
        }
        return maxHealth + absorption > 20.0F;
    }

    private boolean isCompactHypixelHealthEnabled() {
        HorizonClient horizon = HorizonClient.getInstance();
        return horizon == null || horizon.getConfigManager().getConfig().isCompactHypixelHealthEnabled();
    }

    private void renderCompactHypixelHealth(GuiGraphicsExtractor context, Player player, int x, int y, int health, int absorption) {
        boolean hardcore = player.level().getLevelData().isHardcore();
        // Show up to 10 red hearts for current health, then golden absorption hearts on top.
        int baseUnits = Mth.clamp(health, 0, 20);
        int absUnits  = Mth.clamp(absorption, 0, 20);

        for (int slot = 9; slot >= 0; slot--) {
            int heartX = x + slot * 8;
            int unitStart = slot * 2;

            drawVanillaHeart(context, hardcore ? HEART_CONTAINER_HARDCORE : HEART_CONTAINER, heartX, y);

            int baseRemainder = baseUnits - unitStart;
            if (baseRemainder > 0) {
                drawVanillaHeart(context, baseRemainder == 1 ? HEART_HALF : HEART_FULL, heartX, y);
            }

            int absRemainder = absUnits - unitStart;
            if (absRemainder > 0) {
                drawVanillaHeart(context, absRemainder == 1 ? ABSORBING_HEART_HALF : ABSORBING_HEART_FULL, heartX, y);
            }
        }
    }

    private void drawVanillaHeart(GuiGraphicsExtractor context, Identifier texture, int x, int y) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x, y, 9, 9);
    }

    private boolean shouldHideDefenseBar() {
        HorizonClient horizon = HorizonClient.getInstance();
        return horizon != null && horizon.getConfigManager().getConfig().isHideDefenseBar();
    }
}
