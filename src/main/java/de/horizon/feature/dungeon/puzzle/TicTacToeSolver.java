package de.horizon.feature.dungeon.puzzle;

import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Tic Tac Toe puzzle solver.
 * Reads board state from ItemFrame map items.
 * Map color index 114 at position 2700 = "X", otherwise "O".
 * Board positions at component coords: x=8, y=70-72, z=15-17.
 */
public final class TicTacToeSolver {
    // Board positions: (x, y, z) in room-component coords
    // Row-major: top-left to bottom-right
    private static final int[][] BOARD_POS = {
        {8, 72, 17}, {8, 72, 16}, {8, 72, 15},
        {8, 71, 17}, {8, 71, 16}, {8, 71, 15},
        {8, 70, 17}, {8, 70, 16}, {8, 70, 15}
    };

    // Preferred move order (center first, then corners, then edges)
    private static final int[] BOARD_ORDER = {4, 0, 2, 6, 8, 1, 3, 5, 7};

    private static final int[][] WIN_LINES = {
        {0,1,2},{3,4,5},{6,7,8}, // rows
        {0,3,6},{1,4,7},{2,5,8}, // cols
        {0,4,8},{2,4,6}          // diagonals
    };

    private final String[] board = new String[9]; // null, "X", "O"
    private final List<FrameEntry> frameEntries = new ArrayList<>();
    private int bestMoveIdx = -1;
    private DetectedDungeonRoom currentRoom;
    private DungeonRoomDetector currentDetector;
    private boolean inTTT = false;

    private record FrameEntry(int boardIdx, int x, int y, int z, String status) {}

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector) {
        reset();
        currentRoom = room;
        currentDetector = detector;
        inTTT = true;
    }

    /** Called every tick to scan ItemFrame entities and solve. */
    public void tick(Minecraft mc) {
        if (!inTTT || mc == null || mc.level == null || currentRoom == null || currentDetector == null) return;

        // Scan ItemFrame entities for map items
        boolean changed = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemFrame frame)) continue;
            if (!frame.hasFramedMap()) continue;

            var mapId = frame.getFramedMapId(frame.getItem());
            if (mapId == null) continue;
            var mapData = MapItem.getSavedData(mapId, mc.level);
            if (mapData == null) continue;
            byte[] colors = mapData.colors;

            int idx114 = indexOf(colors, (byte) 114);
            if (idx114 == -1) continue;

            String status = (idx114 == 2700) ? "X" : "O";
            int fx = (int) Math.floor(e.getX());
            int fy = e.blockPosition().getY();
            int fz = (int) Math.floor(e.getZ());

            // Check if already tracked
            boolean found = false;
            for (FrameEntry fe : frameEntries) {
                if (fe.x == fx && fe.y == fy && fe.z == fz) { found = true; break; }
            }
            if (found) continue;

            // Convert world position to component position
            BlockPos compPos = currentDetector.worldToRelative(currentRoom, new BlockPos(fx, fy, fz));
            int ci = compPos.getX(), cy = fy, cz = compPos.getZ();

            // Find matching board position
            int boardIdx = -1;
            for (int i = 0; i < BOARD_POS.length; i++) {
                if (BOARD_POS[i][0] == ci && BOARD_POS[i][1] == cy && BOARD_POS[i][2] == cz) {
                    boardIdx = i;
                    break;
                }
            }
            if (boardIdx == -1) continue;

            frameEntries.add(new FrameEntry(boardIdx, fx, fy, fz, status));
            changed = true;
        }

        if (!changed && bestMoveIdx != -1) return;

        // Rebuild board from frame entries
        java.util.Arrays.fill(board, null);
        for (FrameEntry fe : frameEntries) {
            board[fe.boardIdx] = fe.status;
        }

        // Determine who just moved — if the last placed mark was X, we play O (and vice versa)
        String lastStatus = null;
        if (!frameEntries.isEmpty()) {
            lastStatus = frameEntries.getLast().status;
        }

        // Player is always O in Hypixel TTT
        if ("X".equals(lastStatus)) {
            bestMoveIdx = bestMove(board, "O");
        }
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        if (bestMoveIdx < 0 || currentRoom == null || currentDetector == null) return;

        int[] pos = BOARD_POS[bestMoveIdx];
        // fromComp uses (x-1, z) offset for correct alignment
        BlockPos worldPos = currentDetector.relativeToWorld(currentRoom, new BlockPos(pos[0] - 1, pos[1], pos[2]));
        DungeonRenderUtil.drawBox(ctx, new AABB(worldPos), 0xAA00FF44, style, false);
    }

    public void reset() {
        java.util.Arrays.fill(board, null);
        frameEntries.clear();
        bestMoveIdx = -1;
        currentRoom = null;
        currentDetector = null;
        inTTT = false;
    }

    // ── Minimax with alpha-beta pruning ─────────────────────────────────────

    private int bestMove(String[] b, String player) {
        boolean maximizing = "X".equals(player);
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int best = -1;

        // Early optimization: only 1 mark on board → center or corner
        int count = 0;
        for (String s : b) if (s != null) count++;
        if (count == 1) {
            return b[4] == null ? 4 : 0;
        }

        for (int idx : BOARD_ORDER) {
            if (b[idx] != null) continue;
            String[] temp = b.clone();
            temp[idx] = player;
            int score = minimax(temp, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, !maximizing);
            if (maximizing) {
                if (score > bestScore) { bestScore = score; best = idx; }
            } else {
                if (score < bestScore) { bestScore = score; best = idx; }
            }
        }
        return best;
    }

    private int minimax(String[] b, int depth, int alpha, int beta, boolean isMax) {
        if (isWinner(b, "X")) return 10 - depth;
        if (isWinner(b, "O")) return depth - 10;
        boolean hasEmpty = false;
        for (String s : b) if (s == null) { hasEmpty = true; break; }
        if (!hasEmpty) return 0;

        if (isMax) {
            int best = Integer.MIN_VALUE;
            for (int idx : BOARD_ORDER) {
                if (b[idx] != null) continue;
                String[] temp = b.clone();
                temp[idx] = "X";
                int score = minimax(temp, depth + 1, alpha, beta, false);
                best = Math.max(best, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int idx : BOARD_ORDER) {
                if (b[idx] != null) continue;
                String[] temp = b.clone();
                temp[idx] = "O";
                int score = minimax(temp, depth + 1, alpha, beta, true);
                best = Math.min(best, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) break;
            }
            return best;
        }
    }

    private boolean isWinner(String[] b, String player) {
        for (int[] line : WIN_LINES) {
            if (player.equals(b[line[0]]) && player.equals(b[line[1]]) && player.equals(b[line[2]]))
                return true;
        }
        return false;
    }

    private static int indexOf(byte[] arr, byte val) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == val) return i;
        }
        return -1;
    }
}
