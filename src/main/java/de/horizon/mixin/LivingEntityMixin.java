package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void horizon$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        double speed = horizon.getConfigManager().getConfig().getSwingSpeed();
        if (speed == 1.0) return;
        int original = cir.getReturnValue();
        int modified = Math.max(1, (int) Math.round(original / speed));
        cir.setReturnValue(modified);
    }
}
