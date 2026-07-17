package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void horizon$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof LocalPlayer)) return;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        double speed = horizon.getConfigManager().getConfig().getSwingSpeed();
        if (speed == 1.0) return;
        int original = cir.getReturnValue();
        int modified = Math.max(1, (int) Math.round(original / speed));
        cir.setReturnValue(modified);
    }
}
