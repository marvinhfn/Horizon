package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void horizon$blockDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null && !client.getConfigManager().getConfig().isBreakParticlesEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void horizon$blockBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null && !client.getConfigManager().getConfig().isBreakParticlesEnabled()) {
            ci.cancel();
        }
    }
}
