package de.horizon.feature.dungeon.puzzle;

import com.google.gson.*;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Water Board puzzle solver.
 * Detects variant from block at specific positions,
 * subvariant from wool colors (AIR = extended piston),
 * then loads timed solution from JSON.
 */
public final class WaterSolver {

    // Wool colors with relative positions
    private enum WoolColor {
        PURPLE(15, 19),
        ORANGE(15, 18),
        BLUE(15, 17),
        GREEN(15, 16),
        RED(15, 15);

        final int cx, cz;
        WoolColor(int cx, int cz) { this.cx = cx; this.cz = cz; }
    }

    // Lever types with relative positions
    private enum LeverBlock {
        COAL("coal_block", 20, 61, 10),
        GOLD("gold_block", 20, 61, 15),
        QUARTZ("quartz_block", 20, 61, 20),
        DIAMOND("diamond_block", 10, 61, 20),
        EMERALD("emerald_block", 10, 61, 15),
        CLAY("hardened_clay", 10, 61, 10),
        WATER("water", 15, 60, 5),
        NONE("none", 0, 0, 0);

        final String key;
        final int cx, cy, cz;
        int clickCount = 0;

        LeverBlock(String key, int cx, int cy, int cz) {
            this.key = key; this.cx = cx; this.cy = cy; this.cz = cz;
        }

        static LeverBlock fromKey(String key) {
            for (LeverBlock lb : values()) if (lb.key.equals(key)) return lb;
            return NONE;
        }
    }

    private static final Map<String, Map<String, Map<String, Map<String, List<Double>>>>> SOLUTIONS = loadSolutions();

    private final Map<LeverBlock, double[]> solutions = new LinkedHashMap<>();
    private int patternIdentifier = -1;
    private int openedWaterTicks = -1;
    private int tickCounter = 0;
    private DetectedDungeonRoom currentRoom;
    private DungeonRoomDetector currentDetector;

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector, Minecraft mc) {
        reset();
        currentRoom = room;
        currentDetector = detector;
        scan(mc);
    }

    /**
     * Re-seed the room/detector without wiping an in-progress solution. The room detector flickers to
     * null while standing in the room, which was clearing {@code currentRoom} and stopping the scan;
     * this restores it from the cached (stable) room so {@link #tick} can keep scanning.
     */
    public void ensureRoom(DetectedDungeonRoom room, DungeonRoomDetector detector) {
        if (currentRoom == null || currentDetector == null) {
            currentRoom = room;
            currentDetector = detector;
        }
    }

    public boolean tryAutoDetect(DetectedDungeonRoom room, DungeonRoomDetector detector, Minecraft mc) {
        if (!solutions.isEmpty() || patternIdentifier != -1) return true;
        currentRoom = room;
        currentDetector = detector;
        scan(mc);
        if (patternIdentifier != -1) return true;
        currentRoom = null;
        currentDetector = null;
        return false;
    }

    private void scan(Minecraft mc) {
        if (mc.level == null || currentRoom == null || currentDetector == null) return;
        if (patternIdentifier != -1) return;

        // Variant comes from STATIC blocks (present from room-load) → detectable immediately, unlike
        // the animated wool row. Try primary (Z=27), then fallback (Z=26).
        Integer variant = detectVariantPrimary(mc);
        if (variant == null) variant = detectVariantFallback(mc);
        if (variant == null) return;

        var optMap = SOLUTIONS == null ? null : SOLUTIONS.get("false");
        var varMap = optMap == null ? null : optMap.get(String.valueOf(variant));
        if (varMap == null) return;

        // Gate/subvariant: instead of a rigid "exactly 3 air" heuristic (only true once the pistons
        // animate as the water flows — far too late), read the STABLE gate state and try every
        // plausible 3-gate key against the JSON, latching the instant one matches. Candidates: gates
        // WITH wool at Y56/Y57 and their complements — whichever set is the real 3-gate key hits.
        for (String key : gateKeyCandidates(mc)) {
            var leverMap = varMap.get(key);
            if (leverMap == null) continue;
            patternIdentifier = variant;
            solutions.clear();
            for (var entry : leverMap.entrySet()) {
                LeverBlock lever = LeverBlock.fromKey(entry.getKey());
                if (lever == LeverBlock.NONE) continue;
                double[] times = entry.getValue().stream().mapToDouble(Double::doubleValue).toArray();
                solutions.put(lever, times);
            }
            return;
        }
    }

    /** All plausible 3-gate JSON keys from the current wool state (gates with wool + complements). */
    private java.util.List<String> gateKeyCandidates(Minecraft mc) {
        java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
        for (int y : new int[]{56, 57}) {
            java.util.TreeSet<Integer> wool = new java.util.TreeSet<>();
            for (WoolColor wc : WoolColor.values()) {
                BlockPos p = currentDetector.relativeToWorld(currentRoom, new BlockPos(wc.cx, y, wc.cz));
                if (isWool(mc.level.getBlockState(p))) wool.add(wc.ordinal());
            }
            java.util.TreeSet<Integer> air = new java.util.TreeSet<>(java.util.Set.of(0, 1, 2, 3, 4));
            air.removeAll(wool);
            if (wool.size() == 3) keys.add(joinOrdinals(wool));
            if (air.size() == 3) keys.add(joinOrdinals(air));
        }
        return new java.util.ArrayList<>(keys);
    }

    private static boolean isWool(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.WOOL);
    }

    private static String joinOrdinals(java.util.Collection<Integer> ords) {
        StringBuilder sb = new StringBuilder();
        for (int o : ords) sb.append(o);
        return sb.toString();
    }

    /** Primary variant detection: single blocks at Z=27 */
    private Integer detectVariantPrimary(Minecraft mc) {
        BlockPos p14_77_27 = currentDetector.relativeToWorld(currentRoom, new BlockPos(14, 77, 27));
        BlockPos p16_78_27 = currentDetector.relativeToWorld(currentRoom, new BlockPos(16, 78, 27));
        BlockPos p14_78_27 = currentDetector.relativeToWorld(currentRoom, new BlockPos(14, 78, 27));

        if (mc.level.getBlockState(p14_77_27).is(Blocks.TERRACOTTA)) return 0;
        if (mc.level.getBlockState(p16_78_27).is(Blocks.EMERALD_BLOCK)) return 1;
        if (mc.level.getBlockState(p14_78_27).is(Blocks.DIAMOND_BLOCK)) return 2;
        if (mc.level.getBlockState(p14_78_27).is(Blocks.QUARTZ_BLOCK)) return 3;
        return null;
    }

    /** Fallback variant detection: block pairs at Z=26 */
    private Integer detectVariantFallback(Minecraft mc) {
        // Try Y=77 and Y=78
        for (int y : new int[]{77, 78}) {
            BlockPos leftPos = currentDetector.relativeToWorld(currentRoom, new BlockPos(16, y, 26));
            BlockPos rightPos = currentDetector.relativeToWorld(currentRoom, new BlockPos(14, y, 26));
            var left = mc.level.getBlockState(leftPos);
            var right = mc.level.getBlockState(rightPos);

            if (left.is(Blocks.GOLD_BLOCK) && right.is(Blocks.TERRACOTTA)) return 0;
            if (left.is(Blocks.EMERALD_BLOCK) && right.is(Blocks.QUARTZ_BLOCK)) return 1;
            if (left.is(Blocks.QUARTZ_BLOCK) && right.is(Blocks.DIAMOND_BLOCK)) return 2;
            if (left.is(Blocks.GOLD_BLOCK) && right.is(Blocks.QUARTZ_BLOCK)) return 3;
        }
        return null;
    }

    public void tick(Minecraft mc) {
        tickCounter++;
        if (currentRoom == null || currentDetector == null || mc.level == null) return;
        if (patternIdentifier == -1) scan(mc);
    }

    private List<int[]> buildSortedList() {
        // Returns list of [leverOrdinal, timeIndex] sorted by activation order
        List<double[]> list = new ArrayList<>();
        for (var entry : solutions.entrySet()) {
            LeverBlock lever = entry.getKey();
            double[] times = entry.getValue();
            for (int i = lever.clickCount; i < times.length; i++) {
                list.add(new double[]{lever.ordinal(), times[i], i});
            }
        }
        list.sort(Comparator.<double[], Boolean>comparing(e -> e[1] != 0.0)
            .thenComparingInt(e -> e[1] == 0.0 ? (int) e[0] : Integer.MAX_VALUE)
            .thenComparingDouble(e -> e[1] != 0.0 ? e[1] : 0.0));
        return list.stream().map(e -> new int[]{(int) e[0], (int) e[2]}).toList();
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        if (patternIdentifier == -1 || solutions.isEmpty() || currentDetector == null || currentRoom == null) return;

        // Pending clicks, sorted by their scheduled time (0-time = pull immediately, in lever order).
        List<double[]> sorted = new ArrayList<>();
        for (var entry : solutions.entrySet()) {
            double[] times = entry.getValue();
            for (int i = entry.getKey().clickCount; i < times.length; i++) {
                sorted.add(new double[]{entry.getKey().ordinal(), times[i]});
            }
        }
        sorted.sort(Comparator.<double[], Boolean>comparing(e -> e[1] != 0.0)
            .thenComparingInt(e -> e[1] == 0.0 ? (int) e[0] : Integer.MAX_VALUE)
            .thenComparingDouble(e -> e[1] != 0.0 ? e[1] : 0.0));
        if (sorted.isEmpty()) return;

        // Box the next lever (green) and the one after (orange); line between them.
        BlockPos nextPos = leverWorld(LeverBlock.values()[(int) sorted.get(0)[0]]);
        DungeonRenderUtil.drawBox(ctx, new AABB(nextPos), 0xFF00FF00, style, true);
        Vec3 nextCenter = Vec3.atCenterOf(nextPos);
        if (sorted.size() > 1) {
            BlockPos secondPos = leverWorld(LeverBlock.values()[(int) sorted.get(1)[0]]);
            if (!secondPos.equals(nextPos)) {
                DungeonRenderUtil.drawBox(ctx, new AABB(secondPos), 0xFFFFA500, style, true);
                DungeonRenderUtil.drawLine(ctx, List.of(nextCenter, Vec3.atCenterOf(secondPos)), 0xFFFFA500, true);
            }
        }

        // Countdown numbers above each pending lever \u2014 clear, billboarded, coloured by urgency.
        List<DungeonRenderUtil.ColoredStringSpec> labels = new ArrayList<>();
        for (var entry : solutions.entrySet()) {
            LeverBlock lever = entry.getKey();
            double[] times = entry.getValue();
            Vec3 center = Vec3.atCenterOf(leverWorld(lever));
            for (int i = lever.clickCount; i < times.length; i++) {
                double remaining = openedWaterTicks == -1
                    ? times[i]
                    : (openedWaterTicks + times[i] * 20 - tickCounter) / 20.0;
                String text = remaining <= 0.05 ? "NOW" : String.format("%.1f", remaining);
                int color = remaining < 2 ? 0xFFFF5555 : remaining < 6 ? 0xFFFFFF55 : 0xFF55FF55;
                double y = center.y + 1.0 + (i - lever.clickCount) * 0.4;
                labels.add(new DungeonRenderUtil.ColoredStringSpec(text, center.x, y, center.z, color));
            }
        }
        DungeonRenderUtil.drawColoredStringsBatched(ctx, labels);
    }

    private BlockPos leverWorld(LeverBlock lever) {
        return currentDetector.relativeToWorld(currentRoom, new BlockPos(lever.cx, lever.cy, lever.cz));
    }

    public void onLeverClick(BlockPos worldPos, int y) {
        if (solutions.isEmpty() || currentDetector == null || currentRoom == null) return;
        for (LeverBlock lever : LeverBlock.values()) {
            if (lever == LeverBlock.NONE) continue;
            BlockPos leverWorld = currentDetector.relativeToWorld(currentRoom,
                new BlockPos(lever.cx, lever.cy, lever.cz));
            if (leverWorld.getX() == worldPos.getX() && leverWorld.getY() == y
                && leverWorld.getZ() == worldPos.getZ()) {
                if (lever == LeverBlock.WATER && openedWaterTicks == -1) {
                    openedWaterTicks = tickCounter;
                }
                lever.clickCount++;
                break;
            }
        }
    }

    public boolean hasSolution() {
        return !solutions.isEmpty();
    }

    public void reset() {
        for (LeverBlock lb : LeverBlock.values()) lb.clickCount = 0;
        patternIdentifier = -1;
        solutions.clear();
        openedWaterTicks = -1;
        tickCounter = 0;
        currentRoom = null;
        currentDetector = null;
    }

    private static Map<String, Map<String, Map<String, Map<String, List<Double>>>>> loadSolutions() {
        try (InputStream is = WaterSolver.class.getResourceAsStream("/assets/horizon/puzzles/waterSolutions.json")) {
            if (is == null) return null;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Map<String, Map<String, Map<String, List<Double>>>>> result = new HashMap<>();
            for (var optEntry : root.entrySet()) {
                Map<String, Map<String, Map<String, List<Double>>>> variants = new HashMap<>();
                for (var varEntry : optEntry.getValue().getAsJsonObject().entrySet()) {
                    Map<String, Map<String, List<Double>>> subvariants = new HashMap<>();
                    for (var subEntry : varEntry.getValue().getAsJsonObject().entrySet()) {
                        Map<String, List<Double>> levers = new HashMap<>();
                        for (var leverEntry : subEntry.getValue().getAsJsonObject().entrySet()) {
                            List<Double> times = new ArrayList<>();
                            for (JsonElement t : leverEntry.getValue().getAsJsonArray()) {
                                times.add(t.getAsDouble());
                            }
                            levers.put(leverEntry.getKey(), times);
                        }
                        subvariants.put(subEntry.getKey(), levers);
                    }
                    variants.put(varEntry.getKey(), subvariants);
                }
                result.put(optEntry.getKey(), variants);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
