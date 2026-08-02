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

    public void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, HorizonConfig config) {
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
            if (hitBlocks.add(new BlockPos(pos)) && config != null) {
                de.horizon.feature.misc.CustomSoundPlayer.play(config.getSharpShooterSound());
            }
        }
    }

    private static boolean isDevicePos(BlockPos pos) {
        for (BlockPos ep : EMERALD_POSITIONS) if (ep.equals(pos)) return true;
        return false;
    }

    /** Detect the I4 plate state hologram → completion; fire the title on the edge. */
    public void tick(Minecraft mc, HorizonConfig config) {
        if (!config.isSharpShooterEnabled() || mc == null || mc.level == null) return;

        // The state hologram sits at the plate (~63,125-126,34). Hypixel splits it into TWO armor
        // stands — one named "Inactive"/"Active" (the state), one named "Device" (the label) — so the
        // old single-entity "device active" match never fired here. We now look for a stand named
        // "Active" (but not "Inactive"/"Not Activated") in a tight box around the plate. Uses an AABB
        // query (not entitiesForRendering, which is frustum-culled).
        boolean active = false;
        AABB area = new AABB(58, 120, 30, 68, 133, 40);
        for (Entity e : mc.level.getEntitiesOfClass(Entity.class, area, e -> e.hasCustomName())) {
            String name = e.getCustomName().getString().toLowerCase(java.util.Locale.ROOT);
            // "active" matches "Active" and "Device Active"; excludes "inactive". "activated" (in
            // "Not Activated") does NOT contain "active" as a substring, so it's safely ignored.
            if (name.contains("active") && !name.contains("inactive")) { active = true; break; }
        }

        // Only title when *I* did the device: the local player must be standing on the golden
        // pressure plate (at the base) as it completes — not when a teammate finishes it remotely.
        boolean onPlate = mc.player != null
            && mc.player.distanceToSqr(BASE_POSITION.getX() + 0.5, mc.player.getY(), BASE_POSITION.getZ() + 0.5) <= 2.25;
        // LATCH: once active (via hologram scan here OR the chat trigger) stay active until a fresh
        // emerald spawns (new run, onBlockUpdate resets it) — don't let a missed hologram scan flip it
        // back off, which was re-showing the red hit boxes after "Done".
        if (active && onPlate) {
            if (!deviceActive) fireDoneTitle(mc, config);
            deviceActive = true;
        }
    }

    private void fireDoneTitle(Minecraft mc, HorizonConfig config) {
        if (mc == null || mc.gui == null || !config.isSharpShooterDoneTitleEnabled()) return;
        int rgb = config.getSharpShooterDoneColor() & 0xFFFFFF;
        mc.gui.setTitle(Component.literal("I4 Done!").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)));
        mc.gui.setSubtitle(Component.empty());
        mc.gui.setTimes(2, 40, 8);
    }

    /**
     * Fires the I4 Done title/text as soon as the "activated a device" chat line arrives while the
     * player stands on the golden plate — faster than waiting for the hologram scan. Also marks the
     * device active so the boxes hide and the "Done" world text shows immediately.
     */
    public void handleChatMessage(String rawMessage, HorizonConfig config) {
        if (rawMessage == null || !config.isSharpShooterEnabled()) return;
        String lower = rawMessage.toLowerCase(java.util.Locale.ROOT).replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
        if (!lower.contains("activated a device")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        boolean onPlate = mc.player.distanceToSqr(
            BASE_POSITION.getX() + 0.5, mc.player.getY(), BASE_POSITION.getZ() + 0.5) <= 2.25;
        if (!onPlate) return;
        if (!deviceActive) fireDoneTitle(mc, config);
        deviceActive = true; // hide boxes + show "Done" world text right away
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
