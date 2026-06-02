package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private boolean horizon$translated = false;
    @Unique
    private boolean horizon$scaled = false;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void horizon$pushTranslate(
            AbstractClientPlayerEntity player, float tickProgress, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
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
        matrices.push();
        matrices.translate((float) posX, (float) posY, (float) posZ);
    }

    @Inject(method = "renderFirstPersonItem",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
    private void horizon$pushScale(
            AbstractClientPlayerEntity player, float tickProgress, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
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
        matrices.push();
        float s = (float) scale;
        matrices.scale(s, s, s);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void horizon$popTransforms(
            AbstractClientPlayerEntity player, float tickProgress, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
            CallbackInfo ci) {
        if (horizon$scaled) {
            matrices.pop();
            horizon$scaled = false;
        }
        if (horizon$translated) {
            matrices.pop();
            horizon$translated = false;
        }
    }
}
