package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CopyOnWriteArraySet;

import static de.horizon.feature.dungeon.puzzle.DungeonRenderUtil.drawBox;

/**
 * SharpShooter (I4/Arrow Device) solver for F7 Phase 3.
 * Tracks which emerald blocks have been hit and highlights them.
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
    private static final int COLOR_UNHIT = 0x5000FF00; // green
    private static final int COLOR_HIT   = 0x50FF0000; // red

    private record TrackedBlock(BlockPos pos, boolean hit) {}

    private final CopyOnWriteArraySet<BlockPos> tracked = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<BlockPos> hitBlocks = new CopyOnWriteArraySet<>();

    public void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState) {
        // Emerald block appears → track it (if player is near base)
        if (newState.is(Blocks.EMERALD_BLOCK)) {
            for (BlockPos ep : EMERALD_POSITIONS) {
                if (ep.equals(pos)) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc == null || mc.player == null) break;
                    int dist = Math.abs(BASE_POSITION.getX() - (int) mc.player.getX())
                             + Math.abs(BASE_POSITION.getY() - (int) mc.player.getY())
                             + Math.abs(BASE_POSITION.getZ() - (int) mc.player.getZ());
                    if (dist <= 2) {
                        tracked.add(new BlockPos(pos));
                    }
                    break;
                }
            }
        }
        // Blue terracotta at tracked position = hit
        if (newState.is(Blocks.BLUE_TERRACOTTA)) {
            for (BlockPos tp : tracked) {
                if (tp.equals(pos)) {
                    hitBlocks.add(tp);
                    break;
                }
            }
        }
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isSharpShooterEnabled()) return;
        if (tracked.isEmpty()) return;

        boolean allHit = !hitBlocks.isEmpty() && hitBlocks.size() >= tracked.size();

        for (BlockPos pos : tracked) {
            boolean hit = hitBlocks.contains(pos);
            int color = hit ? COLOR_HIT : COLOR_UNHIT;
            AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            drawBox(ctx, box, color, 2, false);
        }

        // Render "Done" in the world when all tracked blocks are hit
        if (allHit) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Font font = mc.font;
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            MultiBufferSource.BufferSource buffers = ctx.bufferSource();
            // Render at center of the emerald grid
            double dx = 66.5 - cam.x;
            double dy = 131.5 - cam.y;
            double dz = 50.5 - cam.z;
            var pose = ctx.poseStack();
            pose.pushPose();
            pose.translate((float) dx, (float) dy, (float) dz);
            pose.last().rotate(ctx.levelState().cameraRenderState.orientation);
            float scale = 0.06f;
            pose.scale(-scale, -scale, -scale);
            String text = "Done";
            float xOff = -font.width(text) * 0.5f;
            font.drawInBatch(text, xOff, 0f, 0xFF55FF55, true,
                pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            pose.popPose();
        }
    }

    public void reset() {
        tracked.clear();
        hitBlocks.clear();
    }

    public void handleChatMessage(String raw) {
        if (tracked.size() >= 9) {
            tracked.clear();
            hitBlocks.clear();
        }
    }
}
