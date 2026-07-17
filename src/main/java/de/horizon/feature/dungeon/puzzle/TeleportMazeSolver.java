package de.horizon.feature.dungeon.puzzle;

import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Teleport Maze puzzle solver.
 * Tracks teleport pads, uses player yaw to determine the forward direction
 * and eliminate wrong pads.
 * Color coding: GREEN (correct), RED (visited but wrong), ORANGE (possible).
 */
public final class TeleportMazeSolver {

    // Pad positions in component coords (x, z). Y is always 69.
    // Each pad has a center position and a "target" position for direction calculation.
    private static final int[][] COMP_PADS = {
        // cx, cz, tx, tz, special, isEnd
        { 4,  6,  5,  7, 0, 0},
        { 4, 12,  5, 11, 0, 0},
        { 4, 14,  5, 15, 0, 0},
        { 4, 20,  5, 19, 0, 0},
        { 4, 22,  5, 23, 0, 0},
        { 4, 28,  5, 27, 0, 0},
        {10,  6,  9,  7, 0, 0},
        {10, 12,  9, 11, 0, 0},
        {10, 14,  9, 15, 0, 0},
        {10, 20,  9, 19, 0, 0},
        {10, 22,  9, 23, 0, 0},
        {10, 28,  9, 27, 0, 0},
        {12, 22, 13, 23, 0, 0},
        {12, 28, 13, 27, 0, 0},
        {18, 22, 17, 23, 0, 0},
        {18, 28, 17, 27, 0, 0},
        {20,  6, 21,  7, 0, 0},
        {20, 12, 21, 11, 0, 0},
        {20, 14, 21, 15, 0, 0},
        {20, 20, 21, 19, 0, 0},
        {20, 22, 21, 23, 0, 0},
        {20, 28, 21, 27, 0, 0},
        {26,  6, 25,  7, 0, 0},
        {26, 12, 25, 11, 0, 0},
        {26, 14, 25, 15, 0, 0},
        {26, 20, 25, 19, 0, 0},
        {26, 22, 25, 23, 0, 0},
        {26, 28, 25, 27, 0, 0},
        // Special pads
        {15, 12, 14, 11, 1, 0},
        {15, 14, 16, 15, 1, 1}, // End pad
    };

    private final List<Pad> pads = new ArrayList<>();
    private boolean inMaze = false;

    private static class Pad {
        final int x, z;    // world position of center
        final int tx, tz;  // world position of target direction
        final boolean special, isEnd;
        boolean visited, correct, possible, incorrect;

        Pad(int x, int z, int tx, int tz, boolean special, boolean isEnd) {
            this.x = x; this.z = z; this.tx = tx; this.tz = tz;
            this.special = special; this.isEnd = isEnd;
        }
    }

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector) {
        reset();
        inMaze = true;

        for (int[] cp : COMP_PADS) {
            var center = detector.relativeToWorld(room, new net.minecraft.core.BlockPos(cp[0], 69, cp[1]));
            var target = detector.relativeToWorld(room, new net.minecraft.core.BlockPos(cp[2], 69, cp[3]));
            pads.add(new Pad(center.getX(), center.getZ(), target.getX(), target.getZ(),
                cp[4] == 1, cp[5] == 1));
        }
    }

    /**
     * Called when a teleport packet arrives.
     * @param newX new player X position
     * @param newZ new player Z position
     * @param oldX old player X position
     * @param oldZ old player Z position
     * @param yaw player yaw from the teleport packet
     */
    public void onTeleport(double newX, double newZ, double oldX, double oldZ, float yaw) {
        if (!inMaze || pads.isEmpty()) return;

        Pad oldPad = closestPad(oldX, oldZ);
        Pad newPad = closestPad(newX, newZ);
        if (oldPad == null || newPad == null) return;

        if (newPad.special) {
            // Reset all pad states when reaching a special pad
            for (Pad p : pads) {
                p.visited = false;
                p.correct = false;
                p.possible = false;
                p.incorrect = false;
            }
            if (newPad.isEnd) return;
        } else {
            oldPad.visited = true;
            newPad.visited = true;
        }

        // Calculate direction vector from yaw
        double rad = (yaw + 90.0) / 180.0 * Math.PI;
        double dirU = Math.cos(rad);
        double dirV = Math.sin(rad);

        // Update pad correctness based on direction
        for (Pad p : pads) {
            if (p == newPad || p.special) continue;

            double offsetU = p.tx - newPad.tx;
            double offsetV = p.tz - newPad.tz;
            boolean matches = isParallel(dirU, dirV, offsetU, offsetV);

            p.correct = matches && !p.incorrect;
            p.possible = p.possible || matches;
            p.incorrect = p.incorrect || !matches;
        }
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        if (!inMaze || pads.isEmpty()) return;

        for (Pad p : pads) {
            int color;
            if (p.correct) color = 0xAA00FF00;
            else if (p.visited) color = 0xAAFF0000;
            else if (p.possible) color = 0xAAFFA500;
            else continue;

            AABB box = new AABB(p.x, 69, p.z, p.x + 1, 70, p.z + 1);
            DungeonRenderUtil.drawBox(ctx, box, color, style, false);
        }
    }

    public boolean isInMaze() {
        return inMaze;
    }

    public void reset() {
        pads.clear();
        inMaze = false;
    }

    private Pad closestPad(double x, double z) {
        Pad best = null;
        double bestDist = Double.MAX_VALUE;
        for (Pad p : pads) {
            double dist = Math.abs(p.x - x) + Math.abs(p.z - z);
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private static boolean isParallel(double u1, double v1, double u2, double v2) {
        double eps = 0.01;
        if (Math.abs(u1) < eps) return Math.abs(u2) < eps && Math.signum(v1) == Math.signum(v2);
        if (Math.abs(v1) < eps) return Math.abs(v2) < eps && Math.signum(u1) == Math.signum(u2);
        return Math.abs(u1 * v2 - v1 * u2) < eps && Math.signum(u1) == Math.signum(u2);
    }
}
