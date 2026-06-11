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

    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void horizon$lockMouse(double timeDelta, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null && client.getInventoryButtonService().isMouseLocked()) {
            this.cursorDeltaX = 0;
            this.cursorDeltaY = 0;
        }
    }
}
