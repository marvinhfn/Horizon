package de.horizon.feature.dungeon.puzzle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Boulder puzzle solver.
 * Reads the boulder pattern in room-relative coords and looks up the click sequence.
 */
public final class BoulderSolver {
    // Each step: [render_x, render_z, click_x, click_z] in room-relative coords (y=65)
    private static final Map<String, List<int[]>> SOLUTIONS = loadSolutions();
    private static final int RENDER_Y = 65;
    private static final int SCAN_Y = 66;

    private List<StepEntry> steps = List.of();

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector, Minecraft mc) {
        steps = List.of();
        if (mc.level == null) return;

        StringBuilder key = new StringBuilder();
        for (int z = 24; z >= 9; z -= 3) {
            for (int x = 24; x >= 6; x -= 3) {
                BlockPos world = detector.relativeToWorld(room, new BlockPos(x, SCAN_Y, z));
                boolean air = mc.level.getBlockState(world).isAir();
                key.append(air ? '0' : '1');
            }
        }

        List<int[]> solution = SOLUTIONS.get(key.toString());
        if (solution == null) return;

        List<StepEntry> result = new ArrayList<>();
        for (int[] step : solution) {
            BlockPos render = detector.relativeToWorld(room, new BlockPos(step[0], RENDER_Y, step[1]));
            BlockPos click  = detector.relativeToWorld(room, new BlockPos(step[2], RENDER_Y, step[3]));
            result.add(new StepEntry(render, click));
        }
        steps = result;
    }

    // Colors: step 0 = bright green, step 1 = orange
    private static final int COLOR_STEP_0 = 0xAA00FF44;
    private static final int COLOR_STEP_1 = 0xAAFF8800;

    public void renderWorld(LevelRenderContext ctx, int style) {
        if (steps.isEmpty()) return;
        // Only render the next two steps; third and beyond stay hidden
        DungeonRenderUtil.drawBox(ctx, new AABB(steps.get(0).render), COLOR_STEP_0, style, false);
        if (steps.size() >= 2) {
            DungeonRenderUtil.drawBox(ctx, new AABB(steps.get(1).render), COLOR_STEP_1, style, false);
        }
    }

    /** Called when any block changes state; advances if the expected click position changed. */
    public void onBlockChange(BlockPos pos) {
        if (steps.isEmpty()) return;
        if (pos.equals(steps.get(0).click)) {
            steps = new ArrayList<>(steps.subList(1, steps.size()));
        }
    }

    public void reset() { steps = List.of(); }

    private static Map<String, List<int[]>> loadSolutions() {
        Map<String, List<int[]>> map = new HashMap<>();
        try (InputStream is = BoulderSolver.class.getResourceAsStream("/assets/horizon/puzzles/boulderSolutions.json")) {
            if (is == null) return map;
            com.google.gson.JsonObject obj = JsonParser.parseReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                List<int[]> steps = new ArrayList<>();
                for (JsonElement step : entry.getValue().getAsJsonArray()) {
                    JsonArray arr = step.getAsJsonArray();
                    steps.add(new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt(),
                                        arr.get(2).getAsInt(), arr.get(3).getAsInt()});
                }
                map.put(entry.getKey(), steps);
            }
        } catch (Exception ignored) {}
        return map;
    }

    private record StepEntry(BlockPos render, BlockPos click) {}
}
