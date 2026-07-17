package de.horizon.feature.dungeon.puzzle;

import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Creeper Beams puzzle solver.
 * Highlights sea lantern pairs that need to be connected.
 * Uses hardcoded sea lantern pair solutions.
 * Pairs are blacklisted when their blocks change to prismarine (completed).
 */
public final class CreeperBeamsSolver {
    // Hardcoded solutions: [x1, y1, z1, x2, y2, z2] in room-component coords
    private static final int[][] RAW_PAIRS = {
        {15, 74, 15, 15, 84, 13},
        {15, 78, 3, 15, 76, 27},
        {5, 76, 24, 24, 77, 7},
        {2, 75, 16, 27, 78, 14},
        {4, 72, 8, 25, 79, 21},
        {4, 75, 9, 25, 76, 23},
        {22, 80, 22, 4, 72, 8},
        {3, 76, 18, 26, 78, 12},
        {9, 81, 20, 26, 70, 7},
        {18, 81, 21, 9, 69, 3},
        {18, 82, 8, 10, 69, 27},
        {25, 76, 23, 6, 74, 5},
        {6, 74, 5, 25, 76, 23},
        {26, 70, 7, 9, 81, 20},
    };

    // Distinct colors per pair (ARGB)
    private static final int[] PAIR_COLORS = {
        0xAA00FFFF, 0xAA00FF00, 0xAAFF0000, 0xAAFF8800
    };

    private List<PairEntry> activePairs = new ArrayList<>();
    private DetectedDungeonRoom currentRoom = null;
    private DungeonRoomDetector currentDetector = null;

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector, Minecraft mc) {
        currentRoom = room;
        currentDetector = detector;
        scanPairs(mc);
    }

    private void scanPairs(Minecraft mc) {
        if (mc.level == null || currentRoom == null || currentDetector == null) {
            activePairs = new ArrayList<>();
            return;
        }
        List<PairEntry> pairs = new ArrayList<>();
        for (int[] raw : RAW_PAIRS) {
            BlockPos p1 = currentDetector.relativeToWorld(currentRoom, new BlockPos(raw[0], raw[1], raw[2]));
            BlockPos p2 = currentDetector.relativeToWorld(currentRoom, new BlockPos(raw[3], raw[4], raw[5]));

            boolean l1 = mc.level.getBlockState(p1).is(Blocks.SEA_LANTERN);
            boolean l2 = mc.level.getBlockState(p2).is(Blocks.SEA_LANTERN);
            if (!l1 || !l2) continue;

            // Skip if one of the positions already appears in another pair
            boolean dup = false;
            for (PairEntry existing : pairs) {
                if (existing.a.equals(p1) || existing.a.equals(p2) || existing.b.equals(p1) || existing.b.equals(p2)) {
                    dup = true;
                    break;
                }
            }
            if (dup) continue;

            int color = PAIR_COLORS[pairs.size() % PAIR_COLORS.length];
            pairs.add(new PairEntry(p1, p2, color, false));
        }
        activePairs = pairs;
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        for (int i = 0; i < activePairs.size() && i < 4; i++) {
            PairEntry pair = activePairs.get(i);
            if (pair.blacklisted) continue;
            DungeonRenderUtil.drawBox(ctx, new net.minecraft.world.phys.AABB(pair.a), pair.color, style, true);
            DungeonRenderUtil.drawBox(ctx, new net.minecraft.world.phys.AABB(pair.b), pair.color, style, true);
            DungeonRenderUtil.drawLine(ctx, List.of(
                Vec3.atCenterOf(pair.a), Vec3.atCenterOf(pair.b)
            ), pair.color, true);
        }
    }

    /**
     * Called on block changes. If a tracked pair's block becomes prismarine, blacklist it.
     */
    public void onBlockChange(BlockPos pos, Minecraft mc) {
        if (currentRoom == null || activePairs.isEmpty()) return;
        if (mc.level == null) return;

        boolean changed = false;
        for (int i = 0; i < activePairs.size(); i++) {
            PairEntry p = activePairs.get(i);
            if (p.blacklisted) continue;
            if (!p.a.equals(pos) && !p.b.equals(pos)) continue;
            if (mc.level.getBlockState(pos).is(Blocks.PRISMARINE)) {
                activePairs.set(i, new PairEntry(p.a, p.b, p.color, true));
                changed = true;
            }
        }
    }

    public void reset() {
        activePairs = new ArrayList<>();
        currentRoom = null;
        currentDetector = null;
    }

    private record PairEntry(BlockPos a, BlockPos b, int color, boolean blacklisted) {}
}
