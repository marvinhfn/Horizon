package de.horizon.feature.dungeon.puzzle;

import com.google.gson.*;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
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

    // Wool block types for Y=57 detection (fallback)
    private static final Block[] WOOL_BLOCKS = {
        Blocks.PURPLE_WOOL, Blocks.ORANGE_WOOL, Blocks.BLUE_WOOL, Blocks.LIME_WOOL, Blocks.RED_WOOL
    };

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

        // Detect subvariant: which 3 wool positions are extended (missing)
        String extendedSlots = detectExtendedSlots(mc);
        if (extendedSlots == null) return;

        // Detect variant from specific block positions
        // Try primary positions first (Z=27), then fallback positions (Z=26)
        Integer variant = detectVariantPrimary(mc);
        if (variant == null) variant = detectVariantFallback(mc);
        if (variant == null) return;

        patternIdentifier = variant;

        // Build solutions from JSON (use standard "false" solutions)
        solutions.clear();
        if (SOLUTIONS == null) return;
        var optMap = SOLUTIONS.get("false");
        if (optMap == null) return;
        var varMap = optMap.get(String.valueOf(patternIdentifier));
        if (varMap == null) return;
        var leverMap = varMap.get(extendedSlots);
        if (leverMap == null) return;

        for (var entry : leverMap.entrySet()) {
            LeverBlock lever = LeverBlock.fromKey(entry.getKey());
            if (lever == LeverBlock.NONE) continue;
            double[] times = entry.getValue().stream().mapToDouble(Double::doubleValue).toArray();
            solutions.put(lever, times);
        }
    }

    /** Detect extended wool positions. Returns 3-char string or null. */
    private String detectExtendedSlots(Minecraft mc) {
        // Method 1: check Y=56 for AIR
        StringBuilder extended56 = new StringBuilder();
        for (WoolColor wc : WoolColor.values()) {
            BlockPos worldPos = currentDetector.relativeToWorld(currentRoom,
                new BlockPos(wc.cx, 56, wc.cz));
            if (mc.level.getBlockState(worldPos).isAir()) {
                extended56.append(wc.ordinal());
            }
        }
        if (extended56.length() == 3) return extended56.toString();

        // Method 2: Check Y=57 for present wool, compute complement
        Set<Integer> present = new TreeSet<>();
        for (WoolColor wc : WoolColor.values()) {
            BlockPos worldPos = currentDetector.relativeToWorld(currentRoom,
                new BlockPos(wc.cx, 57, wc.cz));
            var state = mc.level.getBlockState(worldPos);
            for (int c = 0; c < WOOL_BLOCKS.length; c++) {
                if (state.is(WOOL_BLOCKS[c])) {
                    present.add(c);
                    break;
                }
            }
        }
        // Extended = all colors minus present colors
        Set<Integer> allColors = new TreeSet<>(Set.of(0, 1, 2, 3, 4));
        allColors.removeAll(present);
        if (allColors.size() == 3) {
            StringBuilder sb = new StringBuilder();
            for (int c : allColors) sb.append(c);
            return sb.toString();
        }

        // Method 3: Y=57, check for AIR directly
        StringBuilder extended57 = new StringBuilder();
        for (WoolColor wc : WoolColor.values()) {
            BlockPos worldPos = currentDetector.relativeToWorld(currentRoom,
                new BlockPos(wc.cx, 57, wc.cz));
            if (mc.level.getBlockState(worldPos).isAir()) {
                extended57.append(wc.ordinal());
            }
        }
        if (extended57.length() == 3) return extended57.toString();

        return null;
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
        if (patternIdentifier == -1) {
            scan(mc);
        }
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

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // Build sorted solution list for tracer/line
        List<double[]> sortedEntries = new ArrayList<>();
        for (var entry : solutions.entrySet()) {
            LeverBlock lever = entry.getKey();
            double[] times = entry.getValue();
            for (int i = lever.clickCount; i < times.length; i++) {
                sortedEntries.add(new double[]{lever.ordinal(), times[i]});
            }
        }
        sortedEntries.sort(Comparator.<double[], Boolean>comparing(e -> e[1] != 0.0)
            .thenComparingInt(e -> e[1] == 0.0 ? (int) e[0] : Integer.MAX_VALUE)
            .thenComparingDouble(e -> e[1] != 0.0 ? e[1] : 0.0));

        if (sortedEntries.isEmpty()) return;

        // First lever in sorted order
        LeverBlock firstLever = LeverBlock.values()[(int) sortedEntries.get(0)[0]];
        BlockPos firstWorldPos = currentDetector.relativeToWorld(currentRoom,
            new BlockPos(firstLever.cx, firstLever.cy, firstLever.cz));
        Vec3 firstCenter = Vec3.atCenterOf(firstWorldPos);

        // Tracer from crosshair to first lever
        if (mc.player != null) {
            Vec3 eyePos = mc.player.getEyePosition(1.0f);
            DungeonRenderUtil.drawLine(ctx, List.of(eyePos, firstCenter), 0xFF00FF00, true);
        }

        // Line between first and second lever if different
        if (sortedEntries.size() > 1) {
            LeverBlock secondLever = LeverBlock.values()[(int) sortedEntries.get(1)[0]];
            BlockPos secondWorldPos = currentDetector.relativeToWorld(currentRoom,
                new BlockPos(secondLever.cx, secondLever.cy, secondLever.cz));
            if (!firstWorldPos.equals(secondWorldPos)) {
                Vec3 secondCenter = Vec3.atCenterOf(secondWorldPos);
                DungeonRenderUtil.drawLine(ctx, List.of(firstCenter, secondCenter), 0xFFFFA500, true);
            }
        }

        // Render text at each lever position
        for (var entry : solutions.entrySet()) {
            LeverBlock lever = entry.getKey();
            double[] times = entry.getValue();
            BlockPos leverWorld = currentDetector.relativeToWorld(currentRoom,
                new BlockPos(lever.cx, lever.cy, lever.cz));
            Vec3 leverCenter = Vec3.atCenterOf(leverWorld);

            for (int i = lever.clickCount; i < times.length; i++) {
                double time = times[i];
                int timeInTicks = (int)(time * 20);
                String text;
                if (openedWaterTicks == -1) {
                    text = timeInTicks == 0 ? "\u00a7a\u00a7lCLICK ME!" : "\u00a7e" + time + "s";
                } else {
                    int remaining = openedWaterTicks + timeInTicks - tickCounter;
                    if (remaining > 0) {
                        text = "\u00a7e" + String.format("%.1fs", remaining / 20f);
                    } else {
                        text = "\u00a7a\u00a7lCLICK ME!";
                    }
                }
                DungeonRenderUtil.drawString(ctx, text,
                    leverCenter.x, leverCenter.y + (i - lever.clickCount) * 0.5 + 1.0, leverCenter.z);
            }
        }
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
