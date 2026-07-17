package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * Arrow Align solver for F7 Phase 3 (Goldor).
 * Item frames with arrows at x=-2, y=120-124, z=75-79.
 * Matches the best fitting solution from known patterns and shows clicks needed.
 */
public final class ArrowAlignService {
    // Known solutions — 37 entries each, index = (y-120)<<3 | (z-75), 9 = no frame, 0-7 = target rotation
    private static final int[][] DEV_SOLUTIONS = {
        {7,7,9,9,9, 9,9,9,7,9, 3,9,7,9,9, 9,7,9,3,9, 7,9,9,9,7, 9,3,9,7,9, 9,9,9,9,3, 1,1},
        {9,1,1,1,9, 9,9,9,9,9, 9,9,9,9,9, 9,9,1,1,1, 9,9,9,9,9, 9,9,9,9,9, 9,9,9,1,1, 1,9},
        {5,5,7,1,1, 9,9,9,3,9, 7,9,3,9,9, 9,3,9,9,9, 3,9,9,9,3, 9,9,9,3,9, 9,9,9,9,9, 9,9},
        {9,9,7,1,9, 9,9,9,9,1, 1,9,9,9,9, 9,9,9,7,1, 9,9,9,9,9, 1,1,9,9,9, 9,9,9,9,3, 1,9},
        {9,9,9,9,9, 9,9,9,9,7, 9,7,9,9,9, 9,7,1,9,5, 7,9,9,9,7, 9,9,9,7,9, 9,9,5,5,9, 1,1},
        {7,1,1,9,9, 9,9,9,7,9, 3,9,9,9,9, 9,9,9,3,9, 9,9,9,9,9, 9,3,9,7,9, 9,9,9,9,3, 1,1},
        {5,5,7,9,9, 9,9,9,3,9, 7,9,7,9,9, 9,3,9,9,9, 7,9,9,9,3, 9,9,9,7,9, 9,9,3,1,1, 1,1},
        {7,1,1,9,9, 9,9,9,7,9, 3,9,9,9,9, 9,9,9,9,9, 9,9,9,9,9, 9,7,9,3,9, 9,9,9,9,5, 5,3},
        {9,1,9,7,9, 9,9,9,9,3, 9,7,9,9,9, 9,9,3,9,7, 9,9,9,9,9, 3,9,7,9,9, 9,9,9,3,1, 1,9},
    };

    // Valid frame IDs: (dy<<3)|dz for dy=0..4, dz=0..4
    private static final boolean[] VALID_IDS = new boolean[37];
    static {
        for (int dy = 0; dy <= 4; dy++)
            for (int dz = 0; dz <= 4; dz++)
                VALID_IDS[(dy << 3) | dz] = true;
    }

    private int[] solution = null;
    private final int[] frameRotations = new int[37];
    private final int[] clicksNeeded = new int[37];
    private boolean atDevice = false;
    private int scanCooldown = 0;

    private static int getFrameId(int y, int z) {
        int dy = y - 120;
        int dz = z - 75;
        if (dy < 0 || dy > 4 || dz < 0 || dz > 4) return -1;
        return (dy << 3) | dz;
    }

    public void tick(Minecraft mc, DungeonStateService state, HorizonConfig config) {
        if (!config.isArrowAlignEnabled()) return;
        if (!state.isInBoss() || mc.level == null || mc.player == null) {
            if (atDevice) reset();
            return;
        }

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        atDevice = px >= -10 && px <= 25 && py >= 100 && py <= 145 && pz >= 50 && pz <= 130;

        if (!atDevice) {
            solution = null;
            return;
        }

        // Throttle scanning
        if (--scanCooldown > 0 && solution != null) {
            updateRotations(mc);
            return;
        }
        scanCooldown = 5;

        // Scan for item frames
        int[] frames = new int[37];
        Arrays.fill(frames, 9);
        int frameCount = 0;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemFrame frame)) continue;
            int fx = (int) Math.round(frame.getX());
            if (fx != -2) continue;
            int fy = (int) frame.getY();
            int fz = (int) frame.getZ();
            int id = getFrameId(fy, fz);
            if (id == -1 || id >= 37) continue;
            // Accept arrow items (regular, tipped, spectral)
            var item = frame.getItem().getItem();
            if (item != Items.ARROW && item != Items.TIPPED_ARROW && item != Items.SPECTRAL_ARROW) continue;
            frames[id] = frame.getRotation();
            frameCount++;
        }

        if (frameCount == 0) {
            solution = null;
            return;
        }

        // Match against known solutions — find the best match (most frame positions agree)
        int bestScore = -1;
        int[] bestSolution = null;
        for (int[] sol : DEV_SOLUTIONS) {
            int score = 0;
            boolean compatible = true;
            for (int i = 0; i < 37; i++) {
                if (!VALID_IDS[i]) continue;
                boolean solHasFrame = sol[i] != 9;
                boolean hasFrame = frames[i] != 9;
                if (solHasFrame && hasFrame) {
                    score += 2; // both agree a frame exists here
                } else if (!solHasFrame && !hasFrame) {
                    score += 1; // both agree no frame here
                } else if (solHasFrame && !hasFrame) {
                    // Solution expects a frame but we don't see one — might be out of render distance
                    // Don't penalize, just don't score
                } else {
                    // We see a frame but solution says none — bad match
                    compatible = false;
                    break;
                }
            }
            if (compatible && score > bestScore) {
                bestScore = score;
                bestSolution = sol;
            }
        }

        if (bestSolution != null && bestScore >= frameCount * 2) {
            solution = bestSolution;
            System.arraycopy(frames, 0, frameRotations, 0, 37);
            recalcClicks();
        } else if (solution == null) {
            // No match yet, keep scanning
        }
    }

    private void updateRotations(Minecraft mc) {
        if (mc.level == null) return;
        boolean changed = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemFrame frame)) continue;
            int fx = (int) Math.round(frame.getX());
            if (fx != -2) continue;
            int fy = (int) frame.getY();
            int fz = (int) frame.getZ();
            int id = getFrameId(fy, fz);
            if (id == -1 || id >= 37) continue;
            var item = frame.getItem().getItem();
            if (item != Items.ARROW && item != Items.TIPPED_ARROW && item != Items.SPECTRAL_ARROW) continue;
            int rot = frame.getRotation();
            if (frameRotations[id] != rot) {
                frameRotations[id] = rot;
                changed = true;
            }
        }
        if (changed) recalcClicks();
    }

    private void recalcClicks() {
        if (solution == null) return;
        for (int i = 0; i < 37; i++) {
            if (solution[i] == 9 || frameRotations[i] == 9) {
                clicksNeeded[i] = 0;
            } else {
                clicksNeeded[i] = (solution[i] - frameRotations[i] + 8) & 7;
            }
        }
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isArrowAlignEnabled() || !atDevice || solution == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        MultiBufferSource.BufferSource buffers = ctx.bufferSource();

        float scale = 0.04f;

        for (int y = 120; y <= 124; y++) {
            for (int z = 75; z <= 79; z++) {
                int id = getFrameId(y, z);
                if (id < 0 || id >= 37) continue;
                int clicks = clicksNeeded[id];
                if (clicks == 0) continue;

                String text = String.valueOf(clicks);
                float textOffset = -font.width(text) * 0.5f;

                // Render text facing the player (billboard)
                double dx = -1.5 - cam.x;
                double dy = y + 0.5 - cam.y;
                double dz = z + 0.5 - cam.z;

                var pose = ctx.poseStack();
                pose.pushPose();
                pose.translate((float) dx, (float) dy, (float) dz);
                // Billboard: face camera
                pose.last().rotate(ctx.levelState().cameraRenderState.orientation);
                pose.scale(-scale, -scale, -scale);

                font.drawInBatch(
                    text, textOffset, -4f,
                    0xFF55FF55, true,
                    pose.last().pose(),
                    buffers,
                    Font.DisplayMode.SEE_THROUGH,
                    0x40000000, 0xF000F0
                );

                pose.popPose();
            }
        }

        buffers.endBatch();
    }

    public void reset() {
        solution = null;
        Arrays.fill(frameRotations, 0);
        Arrays.fill(clicksNeeded, 0);
        atDevice = false;
        scanCooldown = 0;
    }
}
