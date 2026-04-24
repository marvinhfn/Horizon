package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void horizon$trackPuzzleBlockInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<?> cir) {
        HorizonClient client = HorizonClient.getInstance();
        if (client == null || hitResult == null) {
            return;
        }
        BlockPos pos = hitResult.getBlockPos();
        client.getDungeonSolverOverlay().handleBlockInteract(pos, client.getDungeonStateService(), client.getDungeonRoomDetector());
    }
}
