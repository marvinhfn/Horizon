package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides the Soulweaver Gloves cosmetic souls: small, invisible, floating, unnamed armor
 * stands that wear a textured player-head skull and orbit the local player. The filter is
 * deliberately tight so it only removes those souls and not other skull-bearing entities.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void horizon$hideSoulweaverSkulls(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(entity instanceof ArmorStand stand)) return;

        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isSoulweaverSkullsHidden()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        // Orbiting souls: invisible, unnamed cosmetic stands hovering next to the player.
        if (!stand.isInvisible() || stand.hasCustomName()) return;
        if (stand.distanceToSqr(mc.player) > 25.0) return; // within ~5 blocks (orbit radius)

        // Only a textured player-head skull (SkyBlock cosmetic), not decorative mob skulls.
        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() == Items.PLAYER_HEAD && head.has(DataComponents.PROFILE)) {
            cir.setReturnValue(false);
        }
    }
}
