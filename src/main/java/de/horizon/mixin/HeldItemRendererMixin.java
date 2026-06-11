package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private boolean horizon$translated = false;
    @Unique
    private boolean horizon$scaled = false;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void horizon$pushTranslate(
            AbstractClientPlayer player, float tickProgress, float pitch,
            InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
            PoseStack matrices, SubmitNodeCollector queue, int light,
            CallbackInfo ci) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) {
            horizon$translated = false;
            return;
        }
        HorizonConfig config = horizon.getConfigManager().getConfig();
        double posX = config.getItemPositionX();
        double posY = config.getItemPositionY();
        double posZ = config.getItemPositionZ();
        if (posX == 0.0 && posY == 0.0 && posZ == 0.0) {
            horizon$translated = false;
            return;
        }
        horizon$translated = true;
        matrices.pushPose();
        matrices.translate((float) posX, (float) posY, (float) posZ);
    }

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void horizon$pushScale(
            AbstractClientPlayer player, float tickProgress, float pitch,
            InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
            PoseStack matrices, SubmitNodeCollector queue, int light,
            CallbackInfo ci) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) {
            horizon$scaled = false;
            return;
        }
        double scale = horizon.getConfigManager().getConfig().getItemScale();
        if (scale == 1.0) {
            horizon$scaled = false;
            return;
        }
        horizon$scaled = true;
        matrices.pushPose();
        float s = (float) scale;
        matrices.scale(s, s, s);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void horizon$popTransforms(
            AbstractClientPlayer player, float tickProgress, float pitch,
            InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
            PoseStack matrices, SubmitNodeCollector queue, int light,
            CallbackInfo ci) {
        if (horizon$scaled) {
            matrices.popPose();
            horizon$scaled = false;
        }
        if (horizon$translated) {
            matrices.popPose();
            horizon$translated = false;
        }
    }
}
