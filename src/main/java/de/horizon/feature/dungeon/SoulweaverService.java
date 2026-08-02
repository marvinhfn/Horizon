package de.horizon.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Identifies the Soulweaver Gloves cosmetic souls so only they get hidden — not the Wither/Blood
 * key display or the end-of-dungeon reward chest, which are also invisible, unnamed player-head
 * skull stands. The distinguishing feature: the souls continuously <b>orbit</b> the player (their
 * world position changes every tick), while the key and chest displays are static.
 */
public final class SoulweaverService {
    private static final double RANGE_SQ = 36.0;      // only track candidates within ~6 blocks
    private static final double MOVE_SQ = 0.0025;     // >0.05 block of movement per tick = orbiting
    private static final int SOUL_TTL = 40;           // stay flagged 2s after last movement

    private final Map<Integer, Vec3> lastPos = new HashMap<>();
    private final Map<Integer, Integer> soulUntil = new HashMap<>();
    private int tick = 0;

    public void tick(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) return;
        tick++;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand stand) || !isCandidate(stand, mc)) continue;
            int id = e.getId();
            Vec3 pos = e.position();
            Vec3 prev = lastPos.put(id, pos);
            if (prev != null && prev.distanceToSqr(pos) > MOVE_SQ) {
                soulUntil.put(id, tick + SOUL_TTL); // moved → it's an orbiting soul
            }
        }
        // Prune stale ids occasionally so the maps don't grow unbounded.
        if ((tick & 63) == 0) {
            soulUntil.entrySet().removeIf(en -> en.getValue() < tick);
            lastPos.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        }
    }

    /** True only for a currently-orbiting cosmetic skull (not the static key/chest displays). */
    public boolean isSoul(Entity entity) {
        Integer until = soulUntil.get(entity.getId());
        return until != null && until >= tick;
    }

    private boolean isCandidate(ArmorStand stand, Minecraft mc) {
        if (!stand.isInvisible() || stand.hasCustomName()) return false;
        if (stand.distanceToSqr(mc.player) > RANGE_SQ) return false;
        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() == Items.PLAYER_HEAD && head.has(DataComponents.PROFILE);
    }

    public void reset() {
        lastPos.clear();
        soulUntil.clear();
        tick = 0;
    }
}
