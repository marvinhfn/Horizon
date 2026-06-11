package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void horizon$blockDropAfterTerminal(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null && client.getTerminalDropService().shouldBlockDrop()) {
            cir.setReturnValue(false);
        }
    }
}
