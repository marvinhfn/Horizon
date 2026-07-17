package de.horizon.feature.dungeon.puzzle;

import com.google.gson.*;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Ice Fill solver. Loads path waypoints from iceFillFloors.json.
 * Identifies the current floor variant by checking block positions (air/not-air).
 * Renders a line path through the ice with per-floor coloring.
 * Retries identification on tick until all 3 floors are resolved.
 */
public final class IceFillSolver {
    private static final IceFillData DATA = loadData();

    // Per-floor colors: floor 1 = blue, floor 2 = cyan, floor 3 = purple
    private static final int COLOR_FLOOR_0 = 0xCC3366FF;
    private static final int COLOR_FLOOR_1 = 0xCC00CCCC;
    private static final int COLOR_FLOOR_2 = 0xCCCC44FF;

    @SuppressWarnings("unchecked")
    private final List<Vec3>[] floorPaths = new List[3];
    private DetectedDungeonRoom room;
    private DungeonRoomDetector detector;
    private boolean solved;

    public IceFillSolver() {
        for (int i = 0; i < 3; i++) floorPaths[i] = List.of();
    }

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector, Minecraft mc) {
        reset();
        this.room = room;
        this.detector = detector;
        this.solved = false;
        trySolve(mc);
    }

    /** Called every tick to retry identification if not yet solved. */
    public void tick(Minecraft mc) {
        if (solved || room == null || detector == null) return;
        trySolve(mc);
    }

    private void trySolve(Minecraft mc) {
        if (mc.level == null || DATA == null) return;

        boolean allFound = true;
        for (int floorIdx = 0; floorIdx < 3 && floorIdx < DATA.identifier.size(); floorIdx++) {
            if (!floorPaths[floorIdx].isEmpty()) continue; // already solved this floor

            List<List<BlockPos>> floorIdentifiers = DATA.identifier.get(floorIdx);
            boolean found = false;
            for (int patternIdx = 0; patternIdx < floorIdentifiers.size(); patternIdx++) {
                List<BlockPos> pair = floorIdentifiers.get(patternIdx);
                if (pair.size() < 2) continue;
                BlockPos checkAir   = detector.relativeToWorld(room, pair.get(0));
                BlockPos checkSolid = detector.relativeToWorld(room, pair.get(1));

                // Ensure chunk is loaded before checking
                if (!mc.level.getChunkSource().hasChunk(checkAir.getX() >> 4, checkAir.getZ() >> 4)) continue;

                boolean firstIsAir   = mc.level.getBlockState(checkAir).isAir();
                boolean secondIsAir  = mc.level.getBlockState(checkSolid).isAir();
                if (firstIsAir && !secondIsAir) {
                    List<List<List<BlockPos>>> patterns = DATA.easy;
                    if (floorIdx < patterns.size() && patternIdx < patterns.get(floorIdx).size()) {
                        floorPaths[floorIdx] = buildPath(patterns.get(floorIdx).get(patternIdx));
                    }
                    found = true;
                    break;
                }
            }
            if (!found) allFound = false;
        }
        if (allFound) solved = true;
    }

    private List<Vec3> buildPath(List<BlockPos> relativePositions) {
        List<Vec3> result = new ArrayList<>(relativePositions.size());
        for (BlockPos rel : relativePositions) {
            BlockPos world = detector.relativeToWorld(room, rel);
            result.add(new Vec3(world.getX() + 0.5, world.getY() + 0.1, world.getZ() + 0.5));
        }
        return result;
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        int[] colors = { COLOR_FLOOR_0, COLOR_FLOOR_1, COLOR_FLOOR_2 };
        for (int i = 0; i < 3; i++) {
            if (floorPaths[i].size() >= 2) {
                DungeonRenderUtil.drawLine(ctx, floorPaths[i], colors[i], false);
            }
        }
    }

    public void reset() {
        for (int i = 0; i < 3; i++) floorPaths[i] = List.of();
        room = null;
        detector = null;
        solved = false;
    }

    private static IceFillData loadData() {
        try (InputStream is = IceFillSolver.class.getResourceAsStream("/assets/horizon/puzzles/iceFillFloors.json")) {
            if (is == null) return null;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            List<List<List<BlockPos>>> identifier = parseFloors(root.getAsJsonArray("identifier"));
            List<List<List<BlockPos>>> easy       = parseFloors(root.getAsJsonArray("easy"));
            return new IceFillData(identifier, easy);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<List<List<BlockPos>>> parseFloors(JsonArray floorsArr) {
        List<List<List<BlockPos>>> floors = new ArrayList<>();
        for (JsonElement floorEl : floorsArr) {
            List<List<BlockPos>> patterns = new ArrayList<>();
            for (JsonElement patternEl : floorEl.getAsJsonArray()) {
                List<BlockPos> positions = new ArrayList<>();
                for (JsonElement posEl : patternEl.getAsJsonArray()) {
                    JsonObject obj = posEl.getAsJsonObject();
                    positions.add(new BlockPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt()));
                }
                patterns.add(positions);
            }
            floors.add(patterns);
        }
        return floors;
    }

    private record IceFillData(List<List<List<BlockPos>>> identifier, List<List<List<BlockPos>>> easy) {}
}
