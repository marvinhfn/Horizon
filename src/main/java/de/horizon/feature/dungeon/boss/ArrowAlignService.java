package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arrow Align solver (F7 Goldor P3) — arrow-align device solver.
 *
 * <p>The device is a fixed 5x5 wall of item frames at corner (-2, 120, 75); the corners hold
 * red/green wool markers, the rest are arrows. Only the ARROW frames are read. Their rotations are
 * matched against one of nine known target patterns and the remaining click count (clicks =
 * {@code (8 - current + target) % 8}) is drawn on each arrow.
 *
 * <p>Rendering uses the level-context buffer with {@link Font.DisplayMode#NORMAL} (the only text
 * path confirmed to render in the AFTER_SOLID_FEATURES pass), positioned on the camera's side of
 * the wall so the depth-tested text sits in front of the framed arrows.
 */
public final class ArrowAlignService {

    private static final BlockPos GRID_CORNER = new BlockPos(-2, 120, 75);
    private static final double RANGE_SQ = 200;      // only run near the fixed device corner
    private static final long CLICK_LOCK_MS = 1000;  // ignore world updates briefly after a local click

    private final int[] frameRotations = new int[25];
    private final net.minecraft.world.phys.Vec3[] framePos = new net.minecraft.world.phys.Vec3[25];
    private final Map<Integer, Long> clickTimestamps = new HashMap<>();
    private final Map<Integer, Integer> clicksRemaining = new HashMap<>();
    private int[] solution = null;

    public ArrowAlignService() { java.util.Arrays.fill(frameRotations, -1); }

    // ── Detection / solving ─────────────────────────────────────────────────────

    /** Grid index (0..24) of a frame's block position, or -1 if it's not a device cell. */
    private static int frameIndex(BlockPos pos) {
        // Wall at x=-2; a frame's blockPosition may round to either side of the 1-block slab.
        if (pos.getX() < GRID_CORNER.getX() || pos.getX() > GRID_CORNER.getX() + 1) return -1;
        int index = (pos.getY() - GRID_CORNER.getY()) + (pos.getZ() - GRID_CORNER.getZ()) * 5;
        return (index < 0 || index > 24) ? -1 : index;
    }

    /** True if this is an arrow frame on the device grid (used to hide its vanilla name label). */
    public boolean isDeviceFrame(ItemFrame frame) {
        return frame != null && frame.getItem().getItem() == Items.ARROW && frameIndex(frame.blockPosition()) >= 0;
    }

    public void tick(Minecraft mc, DungeonStateService state, HorizonConfig config) {
        // Gate ONLY on proximity to the fixed corner. Do not gate on isInDungeon()/phase: those
        // flicker in the F7 boss and any reset() mid-fight wipes the detected rotations, so the
        // presence-mask stops matching and the overlay blinks out. The device only exists here.
        if (!config.isArrowAlignEnabled() || mc.level == null || mc.player == null
                || mc.player.distanceToSqr(GRID_CORNER.getX(), GRID_CORNER.getY(), GRID_CORNER.getZ()) > RANGE_SQ) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        // Read arrow rotations from every rendered item frame on the grid (a tight AABB missed the
        // thin item-frame hitboxes). Only ARROW frames — the corner wool markers must be ignored or
        // the presence-mask never matches a known solution.
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemFrame frame) || frame.getItem().getItem() != Items.ARROW) continue;
            int index = frameIndex(frame.blockPosition());
            if (index < 0) continue;
            framePos[index] = frame.position(); // exact wall position of the arrow (for label depth)
            if (now - clickTimestamps.getOrDefault(index, 0L) > CLICK_LOCK_MS) {
                frameRotations[index] = frame.getRotation();
            }
        }

        solution = null;
        clicksRemaining.clear();

        for (int[] arr : POSSIBLE_SOLUTIONS) {
            boolean matches = true;
            for (int i = 0; i < 25; i++) {
                boolean solHas = arr[i] != -1;
                boolean gotHas = frameRotations[i] != -1;
                if (solHas != gotHas) { matches = false; break; } // presence-mask must match exactly
            }
            if (!matches) continue;

            solution = arr;
            for (int i = 0; i < 25; i++) {
                if (frameRotations[i] == -1) continue;
                int needed = clicks(frameRotations[i], arr[i]);
                if (needed != 0) clicksRemaining.put(i, needed);
            }
            break;
        }
    }

    /**
     * Entity-interact hook: advances a frame's local rotation for instant feedback and returns true
     * when a right-click on an already-aligned frame should be cancelled (block-wrong-clicks).
     */
    public boolean shouldBlockInteract(Entity entity, HorizonConfig config) {
        if (!config.isArrowAlignEnabled()) return false;
        if (!(entity instanceof ItemFrame frame) || frame.getItem().getItem() != Items.ARROW) return false;
        int index = frameIndex(frame.blockPosition());
        if (index < 0 || frameRotations[index] == -1) return false;

        Minecraft mc = Minecraft.getInstance();
        boolean crouching = mc != null && mc.player != null && mc.player.isCrouching();
        boolean block = config.isArrowAlignBlockWrongClicks() && (crouching == config.isArrowAlignInvertSneak());
        // Only block when a solution is actually matched — otherwise (no numbers known) never block,
        // so the player can still rotate arrows freely instead of being locked out of the device.
        if (solution != null && !clicksRemaining.containsKey(index) && block) return true;

        clickTimestamps.put(index, System.currentTimeMillis());
        frameRotations[index] = (frameRotations[index] + 1) % 8;
        if (solution != null && clicks(frameRotations[index], solution[index]) == 0) clicksRemaining.remove(index);
        return false;
    }

    private static int clicks(int current, int target) {
        return target == -1 ? 0 : (8 - current + target) % 8;
    }

    // ── Rendering ───────────────────────────────────────────────────────────────

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isArrowAlignEnabled() || clicksRemaining.isEmpty()) return;

        // Render via DungeonRenderUtil (global buffer + SEE_THROUGH + endBatch) — the same world-text
        // path the secret waypoints use, which actually renders. (ctx.bufferSource() text does not.)
        // Depth (x) comes from the frame's ACTUAL entity position, not a guessed block face — the
        // earlier grid-derived x was off by ~a block. Nudge a hair toward the camera so the label
        // sits just in front of the arrow. y/z stay grid-derived (those were already correct).
        Minecraft mc = Minecraft.getInstance();
        double camX = mc != null ? mc.gameRenderer.getMainCamera().position().x : GRID_CORNER.getX();

        List<DungeonRenderUtil.ColoredStringSpec> labels = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : clicksRemaining.entrySet()) {
            int index = e.getKey();
            int count = e.getValue();
            double frameX = framePos[index] != null ? framePos[index].x : GRID_CORNER.getX() + 0.5;
            double x = frameX + (camX > frameX ? 0.06 : -0.06);
            double y = GRID_CORNER.getY() + (index % 5) + 0.5;
            double z = GRID_CORNER.getZ() + (index / 5) + 0.5;
            int color = config.getArrowAlignColorStyle() == 1
                ? config.getArrowAlignTextColor()
                : (count < 3 ? 0xFF55FF55 : count < 5 ? 0xFFFFAA00 : 0xFFFF5555);
            labels.add(new DungeonRenderUtil.ColoredStringSpec(String.valueOf(count), x, y, z, color));
        }
        // FACE_WEST → permanently static: straight for a camera looking west (how you view the arrow
        // wall at x=-2 from the +x side) and it never re-orients as you move.
        DungeonRenderUtil.drawColoredStringsBatched(ctx, labels, DungeonRenderUtil.FACE_WEST);
    }

    public void reset() {
        java.util.Arrays.fill(frameRotations, -1);
        java.util.Arrays.fill(framePos, null);
        solution = null;
        clicksRemaining.clear();
        clickTimestamps.clear();
    }

    // Nine known target patterns (25 frames each; -1 = no frame in that cell).
    private static final int[][] POSSIBLE_SOLUTIONS = {
        {7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, -1, -1, 7, 1},
        {-1, -1, 7, 7, 5, -1, 7, 1, -1, 5, -1, -1, -1, -1, -1, -1, 7, 5, -1, 1, -1, -1, 7, 7, 1},
        {7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, -1, 7, 5, -1, -1, -1, -1, 5, -1, -1, -1, 3, 3},
        {5, 3, 3, 3, -1, 5, -1, -1, -1, -1, 7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, -1},
        {5, 3, 3, 3, 3, 5, -1, -1, -1, 1, 7, 7, -1, -1, 1, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1},
        {7, 7, 7, 7, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1},
        {-1, -1, -1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, 7, 7, 7, 7, 1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1, -1, 1, -1, 1, -1, 7, 1, 7, 1, 3, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1},
    };
}
