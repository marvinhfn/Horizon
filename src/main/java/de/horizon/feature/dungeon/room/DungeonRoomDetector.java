package de.horizon.feature.dungeon.room;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.feature.dungeon.DungeonStateService;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects dungeon rooms using legacy block ID hashing.
 * Coordinate system: dungeon corner at (-200, -200), room size 31, door size 1.
 */
public final class DungeonRoomDetector {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");

    // Coordinate system constants
    private static final int CORNER_X = -200;
    private static final int CORNER_Z = -200;
    private static final int ROOM_SIZE = 31;
    private static final int HALF_ROOM_SIZE = ROOM_SIZE / 2; // 15
    private static final int HALF_COMBINED_SIZE = (ROOM_SIZE + 1) / 2; // 16

    private static final int SCAN_INTERVAL_TICKS = 8;
    private static final Map<Integer, RoomTemplate> CORE_TO_ROOM = loadRooms();
    private static final Map<String, Integer> SECRETS_BY_NAME = loadSecretCounts();

    // Rotation markers: blue terracotta at corner positions relative to room center
    // Index 0 = 0°, 1 = 90°, 2 = 180°, 3 = 270°
    private static final int[][] CORNER_OFFSETS = {
        {-HALF_ROOM_SIZE, -HALF_ROOM_SIZE}, // 0° (NW corner)
        { HALF_ROOM_SIZE, -HALF_ROOM_SIZE}, // 90° (NE corner)
        { HALF_ROOM_SIZE,  HALF_ROOM_SIZE}, // 180° (SE corner)
        {-HALF_ROOM_SIZE,  HALF_ROOM_SIZE}, // 270° (SW corner)
    };

    private Optional<DetectedDungeonRoom> currentRoom = Optional.empty();
    private long ticks;
    private int scanCooldown;
    private String recentQuizHint = "";
    private int recentQuizHintTicks;
    private String recentWeirdosHint = "";
    private int recentWeirdosHintTicks;

    public void tick(Minecraft client, DungeonStateService dungeonState) {
        ticks++;
        if (recentQuizHintTicks > 0) recentQuizHintTicks--;
        if (recentWeirdosHintTicks > 0) recentWeirdosHintTicks--;

        if (client == null || client.level == null || client.player == null
                || dungeonState == null || !dungeonState.isInDungeon() || dungeonState.isInBoss()) {
            currentRoom = Optional.empty();
            return;
        }
        if (scanCooldown-- > 0) return;
        scanCooldown = SCAN_INTERVAL_TICKS;
        currentRoom = scan(client);
    }

    public void handleChatMessage(String rawMessage) {
        String normalized = normalize(rawMessage);
        if (normalized.contains("what skyblock year is it")
            || normalized.contains("what is the status of")
            || normalized.contains("how many fairy souls")
            || normalized.contains("which of these")
            || normalized.contains("what is the name of")
            || normalized.contains("which villager")) {
            recentQuizHint = "Quiz";
            recentQuizHintTicks = 20 * 45;
        }
        if (normalized.contains("reward is in my chest")
            || normalized.contains("i always tell the truth")
            || normalized.contains("at least one of them is lying")
            || normalized.contains("both of them are telling the truth")) {
            recentWeirdosHint = "Three Weirdos";
            recentWeirdosHintTicks = 20 * 45;
        }
    }

    public Optional<DetectedDungeonRoom> currentRoom() {
        return currentRoom;
    }

    public boolean isCurrentPuzzle(String puzzleName) {
        return currentRoom.map(room -> room.isPuzzle(puzzleName)).orElse(false);
    }

    // ── Coordinate Conversion ──────────────────────────────────────────────

    /**
     * Convert relative (component) position to world position.
     * Rotation system: 0=0°, 90=90°CW, 180=180°, 270=270°CW.
     * The corner position is the blue terracotta corner of the room.
     */
    public BlockPos relativeToWorld(BlockPos relative) {
        return currentRoom.map(room -> relativeToWorld(room, relative)).orElse(relative);
    }

    public BlockPos relativeToWorld(DetectedDungeonRoom room, BlockPos relative) {
        int rx = relative.getX(), rz = relative.getZ();
        int wx, wz;
        // Inverse rotation (360 - deg) to convert component→world
        switch (room.rotationDeg()) {
            case 0:   wx = rx;  wz = rz;  break;
            case 90:  wx = -rz; wz = rx;  break;  // 360-90=270: (-z, x)
            case 180: wx = -rx; wz = -rz; break;  // 360-180=180: (-x, -z)
            case 270: wx = rz;  wz = -rx; break;  // 360-270=90: (z, -x)
            default:  wx = rx;  wz = rz;
        }
        return new BlockPos(
            wx + room.origin().getX(),
            relative.getY(),
            wz + room.origin().getZ()
        );
    }

    public BlockPos worldToRelative(BlockPos world) {
        return currentRoom.map(room -> worldToRelative(room, world)).orElse(world);
    }

    public BlockPos worldToRelative(DetectedDungeonRoom room, BlockPos world) {
        int dx = world.getX() - room.origin().getX();
        int dz = world.getZ() - room.origin().getZ();
        int rx, rz;
        // Forward rotation to convert world→component
        switch (room.rotationDeg()) {
            case 0:   rx = dx;  rz = dz;  break;
            case 90:  rx = dz;  rz = -dx; break;  // (z, -x)
            case 180: rx = -dx; rz = -dz; break;  // (-x, -z)
            case 270: rx = -dz; rz = dx;  break;  // (-z, x)
            default:  rx = dx;  rz = dz;
        }
        return new BlockPos(rx, world.getY(), rz);
    }

    /**
     * Returns the room type at the given room center coordinates by hashing the ceiling.
     */
    public RoomType getRoomTypeAt(Minecraft client, int centerX, int centerZ) {
        if (client == null || client.level == null) return RoomType.UNKNOWN;
        if (!isChunkLoaded(client, centerX, centerZ)) return RoomType.UNKNOWN;
        int hash = hashCeiling(client, centerX, centerZ);
        RoomTemplate template = CORE_TO_ROOM.get(hash);
        return template != null ? template.type() : RoomType.UNKNOWN;
    }

    /**
     * Returns the highest non-air block Y in the column at (x, z), or -1 for a
     * pure void column. Used to detect whether a room is present regardless of
     * its floor height. Mirrors the internal room-existence scan.
     */
    public int highestBlockY(Minecraft client, int x, int z) {
        if (client == null || client.level == null) return -1;
        if (!isChunkLoaded(client, x, z)) return -1;
        return getHighestY(client, x, z);
    }

    /**
     * Returns the room template name at the given room center coordinates, or an
     * empty string when the ceiling hash is unknown / the chunk is not loaded.
     */
    public String getRoomNameAt(Minecraft client, int centerX, int centerZ) {
        if (client == null || client.level == null) return "";
        if (!isChunkLoaded(client, centerX, centerZ)) return "";
        RoomTemplate template = CORE_TO_ROOM.get(hashCeiling(client, centerX, centerZ));
        return template != null ? template.name() : "";
    }

    /**
     * Returns the room center coordinates for all cells of the room at the player's position.
     * For 1x1 rooms returns a single entry, for 1x2/2x2/L-shaped rooms returns multiple.
     * Each entry is an int[2] = {centerX, centerZ}.
     */
    public List<int[]> getRoomCellsAt(Minecraft client, int playerX, int playerZ) {
        int cx = (playerX - CORNER_X) / HALF_COMBINED_SIZE;
        int cz = (playerZ - CORNER_Z) / HALF_COMBINED_SIZE;
        if (cx < 0 || cx > 10 || cz < 0 || cz > 10) return List.of();

        int roomCx = cx & ~1;
        int roomCz = cz & ~1;
        int worldCenterX = CORNER_X + HALF_ROOM_SIZE + HALF_COMBINED_SIZE * roomCx;
        int worldCenterZ = CORNER_Z + HALF_ROOM_SIZE + HALF_COMBINED_SIZE * roomCz;

        if (!isChunkLoaded(client, worldCenterX, worldCenterZ)) return List.of();

        int roomHeight = getHighestY(client, worldCenterX, worldCenterZ);
        if (roomHeight <= 0) return List.of();

        int hash = hashCeiling(client, worldCenterX, worldCenterZ);
        RoomTemplate template = CORE_TO_ROOM.get(hash);

        if (template != null) {
            return findComponents(client, worldCenterX, worldCenterZ, roomHeight, template.cores());
        }

        return List.of(new int[]{worldCenterX, worldCenterZ});
    }

    // ── Scanning ────────────────────────────────────────────────────────────

    private Optional<DetectedDungeonRoom> scan(Minecraft client) {
        int px = client.player.blockPosition().getX();
        int pz = client.player.blockPosition().getZ();

        // Convert player world position to component position
        int cx = (px - CORNER_X) / HALF_COMBINED_SIZE;
        int cz = (pz - CORNER_Z) / HALF_COMBINED_SIZE;

        // Component must be a valid room position (even indices only)
        if (cx < 0 || cx > 10 || cz < 0 || cz > 10) return Optional.empty();
        // Snap to room grid (even positions)
        int roomCx = cx & ~1;
        int roomCz = cz & ~1;

        // Convert back to world center
        int worldCenterX = CORNER_X + HALF_ROOM_SIZE + HALF_COMBINED_SIZE * roomCx;
        int worldCenterZ = CORNER_Z + HALF_ROOM_SIZE + HALF_COMBINED_SIZE * roomCz;

        if (!isChunkLoaded(client, worldCenterX, worldCenterZ)) return Optional.empty();

        int roomHeight = getHighestY(client, worldCenterX, worldCenterZ);
        if (roomHeight <= 0) return Optional.empty();

        int core = hashCeiling(client, worldCenterX, worldCenterZ);
        RoomTemplate template = CORE_TO_ROOM.get(core);

        if (template != null) {
            // Find all room components (for multi-component rooms like 1x2, L-shape etc.)
            List<int[]> components = findComponents(client, worldCenterX, worldCenterZ, roomHeight, template.cores());
            RoomScan roomScan = resolveRotation(client, components, roomHeight, template);
            if (roomScan != null) {
                return Optional.of(new DetectedDungeonRoom(
                    template.name(), template.type(), roomScan.origin(), roomScan.rotation(), roomScan.rotationDeg(), 99, ticks));
            }
            // Fallback: no rotation found, use room center with default rotation
            return Optional.of(new DetectedDungeonRoom(
                template.name(), template.type(),
                new BlockPos(worldCenterX - HALF_ROOM_SIZE, roomHeight, worldCenterZ - HALF_ROOM_SIZE),
                Direction.SOUTH, 0, 84, ticks));
        }

        // Chat-based fallback for Quiz and Three Weirdos
        if (recentQuizHintTicks > 0) {
            return Optional.of(new DetectedDungeonRoom(recentQuizHint, RoomType.PUZZLE,
                new BlockPos(worldCenterX - HALF_ROOM_SIZE, roomHeight, worldCenterZ - HALF_ROOM_SIZE),
                Direction.SOUTH, 0, 60, ticks));
        }
        if (recentWeirdosHintTicks > 0) {
            return Optional.of(new DetectedDungeonRoom(recentWeirdosHint, RoomType.PUZZLE,
                new BlockPos(worldCenterX - HALF_ROOM_SIZE, roomHeight, worldCenterZ - HALF_ROOM_SIZE),
                Direction.SOUTH, 0, 60, ticks));
        }

        // Final fallback: return "Unknown" room with rotation detection (enables puzzle auto-detection)
        if (roomHeight > 0) {
            List<int[]> defaultComponent = List.of(new int[]{worldCenterX, worldCenterZ});
            RoomScan fallbackScan = resolveRotation(client, defaultComponent, roomHeight,
                new RoomTemplate("Unknown", RoomType.UNKNOWN, Set.of()));
            if (fallbackScan != null) {
                return Optional.of(new DetectedDungeonRoom("Unknown", RoomType.UNKNOWN,
                    fallbackScan.origin(), fallbackScan.rotation(), fallbackScan.rotationDeg(), 10, ticks));
            }
            return Optional.of(new DetectedDungeonRoom("Unknown", RoomType.UNKNOWN,
                new BlockPos(worldCenterX - HALF_ROOM_SIZE, roomHeight, worldCenterZ - HALF_ROOM_SIZE),
                Direction.SOUTH, 0, 5, ticks));
        }
        return Optional.empty();
    }

    /**
     * Hash the ceiling column at (x, z) using legacy block IDs.
     * Scans from y=140 down to y=12, converting each block to its legacy ID.
     * Skips iron bars and chests (mapped to "0" in hash string).
     */
    private int hashCeiling(Minecraft client, int x, int z) {
        StringBuilder str = new StringBuilder(256);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = 140; y >= 12; y--) {
            mutable.set(x, y, z);
            BlockState state = client.level.getBlockState(mutable);
            Block block = state.getBlock();

            Integer legacyId = LegacyBlockRegistry.getLegacyId(state);
            if (legacyId == null) continue;

            // Iron bars and chests are mapped to "0" (ignored in hash)
            if (block == Blocks.IRON_BARS || block == Blocks.CHEST) {
                str.append("0");
                continue;
            }
            str.append(legacyId);
        }

        return str.toString().hashCode();
    }

    private int getHighestY(Minecraft client, int x, int z) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = 256; y >= 0; y--) {
            mutable.set(x, y, z);
            BlockState state = client.level.getBlockState(mutable);
            if (state.isAir() || state.is(Blocks.GOLD_BLOCK)) continue;
            return y;
        }
        return -1;
    }

    /**
     * Find all room components that belong to the same room (BFS through adjacent room cells).
     */
    private List<int[]> findComponents(Minecraft client, int startX, int startZ, int roomHeight, Set<Integer> cores) {
        List<int[]> components = new ArrayList<>();
        List<int[]> queue = new ArrayList<>();
        Set<Long> visited = new java.util.HashSet<>();

        queue.add(new int[]{startX, startZ});
        while (!queue.isEmpty()) {
            int[] current = queue.removeFirst();
            long key = ((long) current[0] << 32) | (current[1] & 0xFFFFFFFFL);
            if (!visited.add(key)) continue;
            if (!isChunkLoaded(client, current[0], current[1])) continue;

            int hash = hashCeiling(client, current[0], current[1]);
            if (!cores.contains(hash)) continue;

            components.add(current);

            // Adjacent rooms are 32 blocks apart (room size 31 + 1 door)
            queue.add(new int[]{current[0] + 32, current[1]});
            queue.add(new int[]{current[0] - 32, current[1]});
            queue.add(new int[]{current[0], current[1] + 32});
            queue.add(new int[]{current[0], current[1] - 32});
        }
        components.sort(Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[1]));
        return components;
    }

    /**
     * Find the blue terracotta corner marker to determine room rotation.
     * Returns the corner position and rotation degree.
     */
    private RoomScan resolveRotation(Minecraft client, List<int[]> components, int roomHeight, RoomTemplate template) {
        if (template.name().equalsIgnoreCase("Fairy") && !components.isEmpty()) {
            int[] c = components.get(0);
            return new RoomScan(template.name(), template.type(),
                new BlockPos(c[0] - HALF_ROOM_SIZE, roomHeight, c[1] - HALF_ROOM_SIZE),
                Direction.SOUTH, 0);
        }

        for (int[] comp : components) {
            for (int i = 0; i < CORNER_OFFSETS.length; i++) {
                int cx = comp[0] + CORNER_OFFSETS[i][0];
                int cz = comp[1] + CORNER_OFFSETS[i][1];

                if (!isChunkLoaded(client, cx, cz)) continue;
                BlockState state = client.level.getBlockState(new BlockPos(cx, roomHeight, cz));
                if (state.is(Blocks.BLUE_TERRACOTTA)) {
                    int rotDeg = i * 90;
                    Direction dir = switch (rotDeg) {
                        case 0   -> Direction.SOUTH;
                        case 90  -> Direction.WEST;
                        case 180 -> Direction.NORTH;
                        case 270 -> Direction.EAST;
                        default  -> Direction.SOUTH;
                    };
                    return new RoomScan(template.name(), template.type(),
                        new BlockPos(cx, roomHeight, cz), dir, rotDeg);
                }
            }
        }
        return null;
    }

    private boolean isChunkLoaded(Minecraft client, int x, int z) {
        return client.level.getChunkSource().hasChunk(x >> 4, z >> 4);
    }

    // ── Room Data Loading ───────────────────────────────────────────────────

    private static Map<Integer, RoomTemplate> loadRooms() {
        Map<Integer, RoomTemplate> result = new HashMap<>();
        try (InputStream stream = DungeonRoomDetector.class.getResourceAsStream("/assets/horizon/dungeons/rooms.json")) {
            if (stream == null) return result;
            JsonArray rooms = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : rooms) {
                if (!element.isJsonObject()) continue;
                JsonObject room = element.getAsJsonObject();
                String name = room.get("name").getAsString();
                RoomType type = roomType(room.has("type") ? room.get("type").getAsString() : "UNKNOWN");
                Set<Integer> cores = new java.util.HashSet<>();
                for (JsonElement core : room.getAsJsonArray("cores")) {
                    cores.add(core.getAsInt());
                }
                RoomTemplate template = new RoomTemplate(name, type, cores);
                for (Integer core : cores) {
                    result.put(core, template);
                }
            }
        } catch (IOException | RuntimeException ignored) {}
        return result;
    }

    /** Maps room names (lower-case) to their total secret count from the room database. */
    private static Map<String, Integer> loadSecretCounts() {
        Map<String, Integer> result = new HashMap<>();
        try (InputStream stream = DungeonRoomDetector.class.getResourceAsStream("/assets/horizon/dungeons/rooms.json")) {
            if (stream == null) return result;
            JsonArray rooms = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : rooms) {
                if (!element.isJsonObject()) continue;
                JsonObject room = element.getAsJsonObject();
                if (!room.has("name") || !room.has("secrets")) continue;
                result.put(room.get("name").getAsString().toLowerCase(Locale.ROOT), room.get("secrets").getAsInt());
            }
        } catch (IOException | RuntimeException ignored) {}
        return result;
    }

    /** Total secrets a room contains, or -1 when the room name is unknown. */
    public int getSecretCountForRoom(String roomName) {
        if (roomName == null || roomName.isEmpty()) return -1;
        return SECRETS_BY_NAME.getOrDefault(roomName.toLowerCase(Locale.ROOT), -1);
    }

    private static RoomType roomType(String rawType) {
        return switch (rawType.toLowerCase(Locale.ROOT)) {
            case "puzzle" -> RoomType.PUZZLE;
            case "normal", "rare", "trap", "champion", "yellow" -> RoomType.NORMAL;
            case "blood" -> RoomType.BLOOD;
            case "fairy" -> RoomType.FAIRY;
            case "entrance" -> RoomType.ENTRANCE;
            default -> RoomType.UNKNOWN;
        };
    }

    private String normalize(String value) {
        return FORMATTING_CODES.matcher(value == null ? "" : value).replaceAll("").strip().toLowerCase(Locale.ROOT);
    }

    private record RoomTemplate(String name, RoomType type, Set<Integer> cores) {}
    private record RoomScan(String name, RoomType type, BlockPos origin, Direction rotation, int rotationDeg) {}
}
