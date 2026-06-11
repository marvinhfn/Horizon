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
import net.minecraft.world.level.chunk.LevelChunk;

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

public final class DungeonRoomDetector {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private static final int ROOM_SIZE_SHIFT = 5;
    private static final int START = -185;
    private static final int SCAN_INTERVAL_TICKS = 8;
    private static final Map<Integer, RoomTemplate> CORE_TO_ROOM = loadRooms();
    private static final List<RotationMarker> ROTATION_MARKERS = List.of(
        new RotationMarker(Direction.NORTH, 15, 15),
        new RotationMarker(Direction.WEST, 15, -15),
        new RotationMarker(Direction.SOUTH, -15, -15),
        new RotationMarker(Direction.EAST, -15, 15)
    );

    private Optional<DetectedDungeonRoom> currentRoom = Optional.empty();
    private long ticks;
    private int scanCooldown;
    private String recentQuizHint = "";
    private int recentQuizHintTicks;
    private String recentWeirdosHint = "";
    private int recentWeirdosHintTicks;
    private RoomCenter lastRoomCenter;

    public void tick(Minecraft client, DungeonStateService dungeonState) {
        ticks++;
        if (recentQuizHintTicks > 0) {
            recentQuizHintTicks--;
        }
        if (recentWeirdosHintTicks > 0) {
            recentWeirdosHintTicks--;
        }
        if (client == null || client.level == null || client.player == null || dungeonState == null || !dungeonState.isInDungeon() || dungeonState.isInBoss()) {
            currentRoom = Optional.empty();
            lastRoomCenter = null;
            return;
        }
        if (scanCooldown-- > 0) {
            return;
        }
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

    public BlockPos relativeToWorld(BlockPos relative) {
        return currentRoom.map(room -> relativeToWorld(room, relative)).orElse(relative);
    }

    public BlockPos relativeToWorld(DetectedDungeonRoom room, BlockPos relative) {
        BlockPos rotated = switch (room.rotation()) {
            case NORTH -> new BlockPos(-relative.getX(), relative.getY(), -relative.getZ());
            case WEST -> new BlockPos(-relative.getZ(), relative.getY(), relative.getX());
            case EAST -> new BlockPos(relative.getZ(), relative.getY(), -relative.getX());
            case SOUTH -> relative;
            default -> relative;
        };
        return rotated.offset(room.origin().getX(), 0, room.origin().getZ());
    }

    public BlockPos worldToRelative(BlockPos world) {
        return currentRoom.map(room -> worldToRelative(room, world)).orElse(world);
    }

    public BlockPos worldToRelative(DetectedDungeonRoom room, BlockPos world) {
        BlockPos delta = world.subtract(new BlockPos(room.origin().getX(), world.getY(), room.origin().getZ()));
        return switch (room.rotation()) {
            case NORTH -> new BlockPos(-delta.getX(), delta.getY(), -delta.getZ());
            case WEST -> new BlockPos(delta.getZ(), delta.getY(), -delta.getX());
            case EAST -> new BlockPos(-delta.getZ(), delta.getY(), delta.getX());
            case SOUTH -> delta;
            default -> delta;
        };
    }

    private Optional<DetectedDungeonRoom> scan(Minecraft client) {
        RoomCenter roomCenter = roomCenter(client.player.blockPosition().getX(), client.player.blockPosition().getZ());
        lastRoomCenter = roomCenter;
        LevelChunk chunk = client.level.getChunk(roomCenter.x() >> 4, roomCenter.z() >> 4);
        int roomHeight = topLayerOfRoom(roomCenter, chunk);
        int core = coreAtHeight(roomCenter, roomHeight, chunk);
        RoomTemplate template = CORE_TO_ROOM.get(core);
        if (template != null) {
            List<RoomCenter> components = findRoomComponents(client, roomCenter, roomHeight, template.cores());
            RoomScan roomScan = resolveRotation(client, template, components, roomHeight);
            if (roomScan != null) {
                return Optional.of(new DetectedDungeonRoom(roomScan.name(), roomScan.type(), roomScan.origin(), roomScan.rotation(), 99, ticks));
            }
            return Optional.of(new DetectedDungeonRoom(template.name(), template.type(), new BlockPos(roomCenter.x(), roomHeight, roomCenter.z()), Direction.SOUTH, 84, ticks));
        }

        if (recentQuizHintTicks > 0) {
            return Optional.of(new DetectedDungeonRoom(recentQuizHint, RoomType.PUZZLE, new BlockPos(roomCenter.x(), roomHeight, roomCenter.z()), Direction.SOUTH, 60, ticks));
        }
        if (recentWeirdosHintTicks > 0) {
            return Optional.of(new DetectedDungeonRoom(recentWeirdosHint, RoomType.PUZZLE, new BlockPos(roomCenter.x(), roomHeight, roomCenter.z()), Direction.SOUTH, 60, ticks));
        }
        return Optional.empty();
    }

    private List<RoomCenter> findRoomComponents(Minecraft client, RoomCenter start, int roomHeight, Set<Integer> cores) {
        List<RoomCenter> components = new ArrayList<>();
        List<RoomCenter> queue = new ArrayList<>();
        Set<RoomCenter> visited = new java.util.HashSet<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            RoomCenter current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            LevelChunk chunk = client.level.getChunk(current.x() >> 4, current.z() >> 4);
            if (!cores.contains(coreAtHeight(current, roomHeight, chunk))) {
                continue;
            }
            components.add(current);
            queue.add(new RoomCenter(current.x() + 32, current.z()));
            queue.add(new RoomCenter(current.x() - 32, current.z()));
            queue.add(new RoomCenter(current.x(), current.z() + 32));
            queue.add(new RoomCenter(current.x(), current.z() - 32));
        }
        components.sort(Comparator.comparingInt(RoomCenter::x).thenComparingInt(RoomCenter::z));
        return components;
    }

    private RoomScan resolveRotation(Minecraft client, RoomTemplate template, List<RoomCenter> components, int roomHeight) {
        if (template.name().equalsIgnoreCase("Fairy") && !components.isEmpty()) {
            RoomCenter component = components.get(0);
            return new RoomScan(template.name(), template.type(), new BlockPos(component.x() - 15, roomHeight, component.z() - 15), Direction.SOUTH);
        }

        for (RotationMarker marker : ROTATION_MARKERS) {
            for (RoomCenter component : components) {
                BlockPos clayPos = new BlockPos(component.x() + marker.offsetX(), roomHeight, component.z() + marker.offsetZ());
                BlockState state = client.level.getBlockState(clayPos);
                if (state.is(Blocks.BLUE_TERRACOTTA) && clayMarkerLooksValid(client, clayPos)) {
                    return new RoomScan(template.name(), template.type(), clayPos, marker.rotation());
                }
            }
        }
        return null;
    }

    private boolean clayMarkerLooksValid(Minecraft client, BlockPos clayPos) {
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            Block block = client.level.getBlockState(clayPos.relative(direction)).getBlock();
            if (block != Blocks.AIR && block != Blocks.BLUE_TERRACOTTA) {
                return false;
            }
        }
        return true;
    }

    private RoomCenter roomCenter(int posX, int posZ) {
        int roomX = (posX - START + (1 << (ROOM_SIZE_SHIFT - 1))) >> ROOM_SIZE_SHIFT;
        int roomZ = (posZ - START + (1 << (ROOM_SIZE_SHIFT - 1))) >> ROOM_SIZE_SHIFT;
        return new RoomCenter((roomX << ROOM_SIZE_SHIFT) + START, (roomZ << ROOM_SIZE_SHIFT) + START);
    }

    private int topLayerOfRoom(RoomCenter roomCenter, LevelChunk chunk) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = 160; y >= 12; y--) {
            mutable.set(roomCenter.x(), y, roomCenter.z());
            BlockState state = chunk.getBlockState(mutable);
            if (!state.isAir()) {
                return state.is(Blocks.GOLD_BLOCK) ? y - 1 : y;
            }
        }
        return 0;
    }

    private int coreAtHeight(RoomCenter roomCenter, int roomHeight, LevelChunk chunk) {
        StringBuilder builder = new StringBuilder(150);
        int clampedHeight = Math.max(11, Math.min(140, roomHeight));
        builder.append("0".repeat(Math.max(0, 140 - clampedHeight)));
        int bedrock = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = clampedHeight; y >= 12; y--) {
            mutable.set(roomCenter.x(), y, roomCenter.z());
            Block block = chunk.getBlockState(mutable).getBlock();
            if (block == Blocks.AIR && bedrock >= 2 && y < 69) {
                builder.append("0".repeat(Math.max(0, y - 11)));
                break;
            }
            if (block == Blocks.BEDROCK) {
                bedrock++;
            } else {
                bedrock = 0;
                if (block == Blocks.OAK_PLANKS || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                    continue;
                }
            }
            builder.append(block);
        }
        return builder.toString().hashCode();
    }

    private static Map<Integer, RoomTemplate> loadRooms() {
        Map<Integer, RoomTemplate> result = new HashMap<>();
        try (InputStream stream = DungeonRoomDetector.class.getResourceAsStream("/assets/horizon/dungeons/rooms.json")) {
            if (stream == null) {
                return result;
            }
            JsonArray rooms = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : rooms) {
                if (!element.isJsonObject()) {
                    continue;
                }
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
        } catch (IOException | RuntimeException ignored) {
            return result;
        }
        return result;
    }

    private static RoomType roomType(String rawType) {
        return switch (rawType.toUpperCase(Locale.ROOT)) {
            case "PUZZLE" -> RoomType.PUZZLE;
            case "NORMAL", "RARE", "TRAP", "CHAMPION" -> RoomType.NORMAL;
            case "BLOOD" -> RoomType.BLOOD;
            case "FAIRY" -> RoomType.FAIRY;
            default -> RoomType.UNKNOWN;
        };
    }

    private String clean(String value) {
        return FORMATTING_CODES.matcher(value == null ? "" : value).replaceAll("").strip();
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private record RoomTemplate(String name, RoomType type, Set<Integer> cores) {
    }

    private record RoomCenter(int x, int z) {
    }

    private record RotationMarker(Direction rotation, int offsetX, int offsetZ) {
    }

    private record RoomScan(String name, RoomType type, BlockPos origin, Direction rotation) {
    }
}
