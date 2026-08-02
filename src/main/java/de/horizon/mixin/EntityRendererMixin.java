package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the Soulweaver Gloves cosmetic souls — invisible, unnamed player-head skull stands that
 * <b>orbit</b> the player. The orbit (continuous movement) is what distinguishes them from the
 * static Wither/Blood key and reward-chest displays; that check lives in SoulweaverService.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void horizon$hideSoulweaverSkulls(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !(entity instanceof ArmorStand)) return;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon != null && horizon.shouldHideSoulweaverSkull(entity)) {
            cir.setReturnValue(false);
        }
    }
}
