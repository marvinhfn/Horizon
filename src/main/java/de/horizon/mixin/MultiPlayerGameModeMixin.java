package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void horizon$onBlockInteract(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || hitResult == null) return;
        if (player != null) horizon.onRightClickItem(player.getItemInHand(hand)); // mining ability (right-click on block)
        if (horizon.onBlockInteract(hitResult.getBlockPos())) {
            cir.setReturnValue(InteractionResult.FAIL); // Simon Says: block the wrong-button click
        }
    }

    // Right-click with an item in the air (mining abilities like Mining Speed Boost / Pickobulus).
    @Inject(method = "useItem", at = @At("HEAD"))
    private void horizon$onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon != null && player != null) horizon.onRightClickItem(player.getItemInHand(hand));
    }

    // Waypoint edit mode: left-click (attack) a waypoint block opens its config instead of breaking it.
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void horizon$onLeftClickBlock(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon != null && pos != null && horizon.onWaypointLeftClick(pos)) {
            cir.setReturnValue(false); // consume the attack so the block isn't hit
        }
    }

    // Block right-clicks on already-aligned Arrow Align frames (block-wrong-clicks).
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void horizon$onEntityInteract(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        if (horizon.shouldBlockArrowInteract(target)) cir.setReturnValue(InteractionResult.FAIL);
    }
}
