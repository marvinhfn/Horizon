package de.horizon.feature.dungeon.puzzle;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Blaze puzzle solver.
 *
 * Detection: checks for cobblestone at y=118 in the room.
 * - hasPlatform (cobblestone found = "Lower Blaze", entrance at bottom): sort ascending (lowest HP first).
 * - !hasPlatform (no cobblestone = "Higher Blaze", entrance at top): sort descending (highest HP first).
 */
public final class BlazeSolver {
    private static final Pattern HP_PATTERN = Pattern.compile("\\[Lv\\d+] . Blaze [\\d,]+/([\\d,]+)");
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    private static final int COLOR_FIRST  = 0xAA00FF44;
    private static final int COLOR_SECOND = 0xAAFFA500;
    private static final int COLOR_THIRD  = 0xAAFF1111;

    // ArmorStand entityId -> maxHP
    private final ConcurrentHashMap<Integer, Integer> entityHpMap = new ConcurrentHashMap<>();
    private List<BlazeEntry> sortedBlazes = List.of();
    private boolean hasPlatform = false;

    private record BlazeEntry(Entity entity, int maxHp) {}

    /**
     * Detects whether the blaze room has a cobblestone platform at y=118.
     * Scans a small area around the room origin for cobblestone blocks.
     * hasPlatform = true → Lower Blaze (entrance bottom, ascending sort)
     * hasPlatform = false → Higher Blaze (entrance top, descending sort)
     */
    public void detectPlatform(Minecraft mc, BlockPos roomOrigin) {
        if (mc == null || mc.level == null || roomOrigin == null) return;
        hasPlatform = false;
        // Scan full room (±15 blocks around origin) at y=118 for cobblestone
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                BlockPos pos = new BlockPos(roomOrigin.getX() + dx, 118, roomOrigin.getZ() + dz);
                if (mc.level.getBlockState(pos).getBlock() == Blocks.COBBLESTONE) {
                    hasPlatform = true;
                    return;
                }
            }
        }
    }

    public boolean hasPlatform() { return hasPlatform; }

    public void tick(Minecraft mc) {
        if (mc == null || mc.level == null) {
            sortedBlazes = List.of();
            return;
        }

        // Scan ArmorStand entities for blaze HP names
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getType() != EntityType.ARMOR_STAND) continue;
            String name = e.getCustomName() != null ? e.getCustomName().getString() : "";
            name = FORMATTING.matcher(name).replaceAll("");
            Matcher m = HP_PATTERN.matcher(name);
            if (m.find()) {
                try {
                    int maxHp = Integer.parseInt(m.group(1).replace(",", ""));
                    entityHpMap.put(e.getId(), maxHp);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Build sorted blaze list: ArmorStand ID - 1 = actual Blaze entity
        List<BlazeEntry> blazes = new ArrayList<>();
        for (var entry : entityHpMap.entrySet()) {
            Entity blaze = mc.level.getEntity(entry.getKey() - 1);
            if (blaze == null || !blaze.isAlive()) continue;
            if (blaze.getType() == EntityType.ARMOR_STAND) continue;
            blazes.add(new BlazeEntry(blaze, entry.getValue()));
        }

        // Sort ascending by max HP, then reverse if no platform
        blazes.sort(Comparator.comparingInt(BlazeEntry::maxHp));
        if (!hasPlatform) {
            Collections.reverse(blazes);
        }

        sortedBlazes = blazes;
    }

    public void renderWorld(LevelRenderContext ctx, int style, Minecraft mc) {
        if (sortedBlazes.isEmpty()) return;

        int[] colors = {COLOR_FIRST, COLOR_SECOND, COLOR_THIRD};
        for (int i = 0; i < Math.min(sortedBlazes.size(), 3); i++) {
            Entity e = sortedBlazes.get(i).entity;
            int color = colors[i];
            DungeonRenderUtil.drawBox(ctx, e.getBoundingBox(), color, style, false);
        }

        if (sortedBlazes.size() >= 2) {
            List<Vec3> points = new ArrayList<>();
            for (int i = 0; i < Math.min(sortedBlazes.size(), 3); i++) {
                points.add(sortedBlazes.get(i).entity.getBoundingBox().getCenter());
            }
            for (int i = 0; i < points.size() - 1; i++) {
                int color = colors[i];
                DungeonRenderUtil.drawLine(ctx, List.of(points.get(i), points.get(i + 1)), color, false);
            }
        }
    }

    public void reset() {
        sortedBlazes = List.of();
        entityHpMap.clear();
    }
}
