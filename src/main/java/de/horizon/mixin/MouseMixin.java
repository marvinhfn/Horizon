package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void horizon$lockMouse(double timeDelta, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null && client.getInventoryButtonService().isMouseLocked()) {
            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
        }
    }
}
