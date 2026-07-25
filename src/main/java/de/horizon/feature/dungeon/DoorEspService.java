package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import de.horizon.feature.dungeon.room.RoomType;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Door ESP: shows doors on the boundary of the player's current room.
 * Supports multi-cell rooms (1x2, 2x2, L-shaped) via DungeonRoomDetector BFS.
 * Uses dungeon map pixel data to validate passages (walls vs real connections).
 * Closed doors are outlined green/red based on key count.
 * Open passages to fairy rooms are outlined in pink (only if never had a door).
 */
public final class DoorEspService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    // Dungeon grid constants
    private static final int CORNER = -200;
    private static final int GRID_STEP = 32; // room 31 + gap 1
    private static final int HALF_ROOM = 15;
    private static final int MIN_CENTER = CORNER + HALF_ROOM;            // -185
    private static final int MAX_CENTER = CORNER + 5 * GRID_STEP + HALF_ROOM; // -25

    // Chat regex patterns
    private static final Pattern KEY_PICKUP = Pattern.compile(
        "RIGHT CLICK on .+ to open it\\. This key can only be used to open (\\d+) door!");
    private static final Pattern WITHER_DOOR_OPENED = Pattern.compile(
        "^(?:\\[.+?] )?\\w+ opened a WITHER door!$");
    private static final String BLOOD_DOOR_OPENED = "The BLOOD DOOR has been opened!";

    // Colors
    private static final int COLOR_HAS_KEY = 0xFF00CC00;
    private static final int COLOR_NO_KEY  = 0xFFCC0000;
    private static final int COLOR_FAIRY   = 0xFFFF55FF;
    private static final int KEY_BOX_COLOR = 0x60FFAA00;

    public record DungeonDoor(int x, int z, boolean isBlood, boolean isFairyPassage) {}

    private final List<DungeonDoor> doors = new ArrayList<>();
    private final Set<Long> everSeenDoorPositions = new HashSet<>();
    private boolean fairyVisited = false;
    private int doorKeys = 0;
    private Entity keyEntity;
    private long lastScanTick = -1;

    public void handleChatMessage(String raw) {
        String plain = FORMATTING_CODES.matcher(raw).replaceAll("");

        Matcher keyMatch = KEY_PICKUP.matcher(plain);
        if (keyMatch.find()) {
            doorKeys += Integer.parseInt(keyMatch.group(1));
            return;
        }

        if (WITHER_DOOR_OPENED.matcher(plain).matches()) {
            doorKeys--;
            return;
        }

        if (plain.contains(BLOOD_DOOR_OPENED)) {
            doorKeys--;
        }
    }

    public void tick(Minecraft mc, boolean inDungeon, boolean inBoss, DungeonRoomDetector roomDetector) {
        if (!inDungeon || inBoss || mc == null || mc.level == null || mc.player == null) {
            return;
        }

        long currentTick = mc.level.getGameTime();
        if (currentTick == lastScanTick) return;
        lastScanTick = currentTick;

        scanKeyEntities(mc);
        scanRoomBoundary(mc, roomDetector);
    }

    private void scanKeyEntities(Minecraft mc) {
        keyEntity = null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand as)) continue;
            if (as.getCustomName() == null) continue;
            if (!e.isAlive()) continue;
            String name = FORMATTING_CODES.matcher(as.getCustomName().getString()).replaceAll("");
            if ("Wither Key".equals(name) || "Blood Key".equals(name)) {
                keyEntity = as;
                break;
            }
        }
    }

    /**
     * Uses DungeonRoomDetector to find all cells of the player's current room,
     * then checks all boundary positions for closed doors or open fairy passages.
     */
    private void scanRoomBoundary(Minecraft mc, DungeonRoomDetector roomDetector) {
        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();

        List<int[]> roomCells = roomDetector.getRoomCellsAt(mc, px, pz);
        // Transient miss (edge/door, unloaded chunk, hash mismatch): keep the last
        // door set instead of clearing it, so the render doesn't flicker out.
        if (roomCells.isEmpty()) return;

        // Track fairy room visits — once visited, stop showing fairy outlines
        if (!fairyVisited) {
            RoomType playerRoomType = roomDetector.getRoomTypeAt(mc, roomCells.get(0)[0], roomCells.get(0)[1]);
            if (playerRoomType == RoomType.FAIRY) {
                fairyVisited = true;
            }
        }

        Set<Long> cellSet = new HashSet<>();
        for (int[] cell : roomCells) {
            cellSet.add(cellKey(cell[0], cell[1]));
        }

        List<DungeonDoor> newDoors = new ArrayList<>();
        for (int[] cell : roomCells) {
            int cx = cell[0], cz = cell[1];
            checkBoundary(mc, roomDetector, cellSet, newDoors, cx, cz, cx + 32, cz);
            checkBoundary(mc, roomDetector, cellSet, newDoors, cx, cz, cx - 32, cz);
            checkBoundary(mc, roomDetector, cellSet, newDoors, cx, cz, cx, cz + 32);
            checkBoundary(mc, roomDetector, cellSet, newDoors, cx, cz, cx, cz - 32);
        }
        doors.clear();
        doors.addAll(newDoors);
    }

    private void checkBoundary(Minecraft mc, DungeonRoomDetector roomDetector,
                               Set<Long> roomCells, List<DungeonDoor> out, int cx, int cz, int nx, int nz) {
        if (roomCells.contains(cellKey(nx, nz))) return;
        if (nx < MIN_CENTER || nx > MAX_CENTER || nz < MIN_CENTER || nz > MAX_CENTER) return;

        int gapX = (cx + nx) / 2;
        int gapZ = (cz + nz) / 2;
        long gapKey = cellKey(gapX, gapZ);

        var state = mc.level.getBlockState(new BlockPos(gapX, 69, gapZ));
        if (state.is(Blocks.COAL_BLOCK)) {
            everSeenDoorPositions.add(gapKey);
            out.add(new DungeonDoor(gapX, gapZ, false, false));
        } else if (state.is(Blocks.RED_TERRACOTTA)) {
            everSeenDoorPositions.add(gapKey);
            out.add(new DungeonDoor(gapX, gapZ, true, false));
        } else if (!fairyVisited && everSeenDoorPositions.contains(gapKey)) {
            RoomType adjType = roomDetector.getRoomTypeAt(mc, nx, nz);
            if (adjType == RoomType.FAIRY) {
                out.add(new DungeonDoor(gapX, gapZ, false, true));
            }
        }
    }

    private static long cellKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config, boolean inDungeon, boolean inBoss) {
        if (!inDungeon || inBoss) return;
        if (!config.isWitherDoorEspEnabled()) return;

        int baseColor = doorKeys > 0 ? config.getDoorColorHasKey() : config.getDoorColorNoKey();
        int fillColor = (baseColor & 0x00FFFFFF) | 0x40000000;
        int outlineColor = baseColor | 0xFF000000;

        int fairyFill = (COLOR_FAIRY & 0x00FFFFFF) | 0x40000000;
        int fairyOutline = COLOR_FAIRY;

        for (DungeonDoor door : doors) {
            if (!door.isFairyPassage && door.isBlood && !config.isBloodDoorEspEnabled()) continue;

            AABB box = new AABB(
                door.x - 1.0, 69.0, door.z - 1.0,
                door.x + 2.0, 73.0, door.z + 2.0
            );

            if (door.isFairyPassage) {
                DungeonRenderUtil.drawBox(ctx, box, fairyFill, 0, true);
                DungeonRenderUtil.drawBox(ctx, box, fairyOutline, 1, true, 7.5f);
            } else {
                DungeonRenderUtil.drawBox(ctx, box, fillColor, 0, true);
                DungeonRenderUtil.drawBox(ctx, box, outlineColor, 1, true, 7.5f);
            }
        }

        if (config.isDoorKeyHighlightEnabled() && keyEntity != null && keyEntity.isAlive()) {
            renderKeyHighlight(ctx, keyEntity);
        }
    }

    private void renderKeyHighlight(LevelRenderContext ctx, Entity entity) {
        double kx = entity.getX();
        double ky = entity.getY() + 1.2;
        double kz = entity.getZ();

        AABB keyBox = new AABB(kx - 0.4, ky, kz - 0.4, kx + 0.4, ky + 0.8, kz + 0.4);
        DungeonRenderUtil.drawBox(ctx, keyBox, KEY_BOX_COLOR, 2, true);

        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        Vec3 keyPos = new Vec3(kx, ky + 0.4, kz);
        Vec3 dir = keyPos.subtract(cam);
        double len = dir.length();
        Vec3 tracerStart = len > 1.0 ? cam.add(dir.scale(0.5 / len)) : cam;
        int tracerColor = (KEY_BOX_COLOR & 0x00FFFFFF) | 0xCC000000;
        DungeonRenderUtil.drawLine(ctx, List.of(tracerStart, keyPos), tracerColor, true, 2.5f);
    }

    public void reset() {
        doors.clear();
        everSeenDoorPositions.clear();
        fairyVisited = false;
        keyEntity = null;
        doorKeys = 0;
        lastScanTick = -1;
    }
}
