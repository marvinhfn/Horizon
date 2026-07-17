package de.horizon.feature.dungeon.puzzle;

import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Ice Path puzzle solver.
 * Draws line segments showing where the silverfish needs to go.
 * Draws guide lines at y=67.5 with automatic progression.
 */
public final class IcePathSolver {
    // Waypoints in room-component coords (x1, z1 → x2, z2), y is always 66
    private static final int[][] SOLUTIONS = {
        {8, 9, 12, 9},
        {12, 9, 12, 8},
        {12, 8, 20, 8},
        {20, 8, 20, 24},
        {20, 24, 19, 24},
        {19, 24, 19, 23},
        {19, 23, 21, 23},
        {21, 23, 21, 14},
        {21, 14, 14, 14},
        {14, 14, 14, 25}
    };

    private final ConcurrentLinkedQueue<LineSegment> currentSolution = new ConcurrentLinkedQueue<>();
    private boolean inPath = false;
    private Silverfish silverfishEntity = null;

    private record LineSegment(double x1, double z1, double x2, double z2) {}

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector) {
        currentSolution.clear();
        inPath = true;

        for (int[] sol : SOLUTIONS) {
            BlockPos p1 = detector.relativeToWorld(room, new BlockPos(sol[0], 66, sol[1]));
            BlockPos p2 = detector.relativeToWorld(room, new BlockPos(sol[2], 66, sol[3]));
            currentSolution.add(new LineSegment(p1.getX(), p1.getZ(), p2.getX(), p2.getZ()));
        }
    }

    public void tick(Minecraft mc) {
        if (!inPath || mc == null || mc.level == null) return;

        // Find silverfish entity
        if (silverfishEntity == null || silverfishEntity.isDeadOrDying()) {
            silverfishEntity = null;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof Silverfish sf && !sf.isDeadOrDying()) {
                    silverfishEntity = sf;
                    break;
                }
            }
        }

        // Progress tracking: remove completed segments
        if (silverfishEntity != null && !silverfishEntity.isDeadOrDying()) {
            LineSegment first = currentSolution.peek();
            if (first != null) {
                double dist = Math.abs(silverfishEntity.getX() - first.x2 - 0.5)
                            + Math.abs(silverfishEntity.getZ() - first.z2 - 0.5);
                if (dist < 0.8) {
                    currentSolution.poll();
                }
            }
        } else {
            // Silverfish died — if only 1 segment left, clear it
            if (currentSolution.size() == 1) currentSolution.clear();
        }
    }

    public void renderWorld(LevelRenderContext ctx, int style, Minecraft mc) {
        if (currentSolution.isEmpty()) return;

        List<LineSegment> segments = new ArrayList<>(currentSolution);
        for (int i = 0; i < segments.size(); i++) {
            LineSegment seg = segments.get(i);
            int color = i == 0 ? 0xAA00FF00 : 0xAAFF0000;
            DungeonRenderUtil.drawLine(ctx, List.of(
                new Vec3(seg.x1 + 0.5, 67.5, seg.z1 + 0.5),
                new Vec3(seg.x2 + 0.5, 67.5, seg.z2 + 0.5)
            ), color, true);
        }
    }

    public void reset() {
        currentSolution.clear();
        inPath = false;
        silverfishEntity = null;
    }
}
