package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.CopyOnWriteArraySet;

import static de.horizon.feature.dungeon.puzzle.DungeonRenderUtil.drawBox;

/**
 * SharpShooter (I4/Arrow Device) solver for F7 Phase 3.
 *
 * <p>While solving, the emerald grid cells are boxed (green = target, red = already hit). Completion
 * is driven by the golden pressure plate's <b>"Device Active"</b> hologram — NOT by counting hits, which used to make "Done" appear prematurely at P3 start.
 * Once the device is active the boxes are hidden and a static "Done" label (plus an optional title)
 * is shown.
 */
public final class SharpShooterService {
    private static final BlockPos[] EMERALD_POSITIONS = {
        new BlockPos(68, 130, 50),
        new BlockPos(66, 130, 50),
        new BlockPos(64, 130, 50),
        new BlockPos(68, 128, 50),
        new BlockPos(66, 128, 50),
        new BlockPos(64, 128, 50),
        new BlockPos(68, 126, 50),
        new BlockPos(66, 126, 50),
        new BlockPos(64, 126, 50),
    };
    private static final BlockPos BASE_POSITION = new BlockPos(63, 127, 35);
    private static final double ACTIVE_RANGE_SQ = 144; // 12 blocks around I4 base — isolates it from other devices
    private static final int COLOR_UNHIT = 0x5000FF00; // green
    private static final int COLOR_HIT   = 0x50FF0000; // red

    private final CopyOnWriteArraySet<BlockPos> tracked = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<BlockPos> hitBlocks = new CopyOnWriteArraySet<>();
    private volatile boolean deviceActive = false; // true once the plate shows "Device Active" = done

    public void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState) {
        // Emerald appears → track it as a target (green). Only when the local player is at the base,
        // so a teammate's device doesn't light up. A fresh emerald after completion = a NEW run.
        if (newState.is(Blocks.EMERALD_BLOCK) && isDevicePos(pos)) {
            if (deviceActive) { tracked.clear(); hitBlocks.clear(); deviceActive = false; }
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                int dist = Math.abs(BASE_POSITION.getX() - (int) mc.player.getX())
                         + Math.abs(BASE_POSITION.getY() - (int) mc.player.getY())
                         + Math.abs(BASE_POSITION.getZ() - (int) mc.player.getZ());
                if (dist <= 2) tracked.add(new BlockPos(pos));
            }
        }
        // Emerald → blue terracotta = a hit (red box while solving).
        if (newState.is(Blocks.BLUE_TERRACOTTA) && isDevicePos(pos)) {
            hitBlocks.add(new BlockPos(pos));
        }
    }

    private static boolean isDevicePos(BlockPos pos) {
        for (BlockPos ep : EMERALD_POSITIONS) if (ep.equals(pos)) return true;
        return false;
    }

    /** Detect the "Device Active" hologram at the I4 plate → completion; fire the title on the edge. */
    public void tick(Minecraft mc, HorizonConfig config) {
        if (!config.isSharpShooterEnabled() || mc == null || mc.level == null) return;

        boolean active = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!e.hasCustomName()) continue;
            if (e.distanceToSqr(BASE_POSITION.getX() + 0.5, BASE_POSITION.getY() + 0.5, BASE_POSITION.getZ() + 0.5) > ACTIVE_RANGE_SQ) continue;
            String name = e.getCustomName().getString().toLowerCase();
            // Phrase match: "device inactive" must NOT trigger (it contains "active" as a substring).
            if (name.contains("device active")) { active = true; break; }
        }

        // Only title when *I* did the device: the local player must be standing on the golden
        // pressure plate (at the base) as it completes — not when a teammate finishes it remotely.
        boolean onPlate = mc.player != null
            && mc.player.distanceToSqr(BASE_POSITION.getX() + 0.5, mc.player.getY(), BASE_POSITION.getZ() + 0.5) <= 2.25;
        if (active && !deviceActive && onPlate && config.isSharpShooterDoneTitleEnabled()) {
            int rgb = config.getSharpShooterDoneColor() & 0xFFFFFF;
            mc.gui.setTitle(Component.literal("I4 Done!").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)));
            mc.gui.setSubtitle(Component.empty());
            mc.gui.setTimes(2, 40, 8);
        }
        deviceActive = active;
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isSharpShooterEnabled()) return;

        // Solving state: box the grid (green target / red hit). Hidden once the device is active.
        if (!deviceActive) {
            for (BlockPos pos : tracked) {
                int color = hitBlocks.contains(pos) ? COLOR_HIT : COLOR_UNHIT;
                AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                drawBox(ctx, box, color, 2, false);
            }
            return;
        }

        // Completed: static "Done" centred on the grid, on the player's (−z) side of the wall.
        if (config.isSharpShooterDoneEnabled()) {
            DungeonRenderUtil.drawString(ctx, "Done", 66.5, 128.5, 49.3,
                config.getSharpShooterDoneColor(), config.getSharpShooterDoneScale(), DungeonRenderUtil.FACE_SOUTH);
        }
    }

    public void reset() {
        tracked.clear();
        hitBlocks.clear();
        deviceActive = false;
    }
}
