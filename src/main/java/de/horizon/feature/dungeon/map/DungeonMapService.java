package de.horizon.feature.dungeon.map;

import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import de.horizon.feature.dungeon.room.RoomType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and holds the structured dungeon layout ({@link DungeonInfo}).
 *
 * <p>Map-authoritative, following Odin's {@code MapScan}: the Hypixel dungeon
 * map item is the source of truth for which rooms exist, their type, their
 * checkmark state and the doors. The world scan ({@link DungeonRoomDetector})
 * is used only to attach a room name to each grid room by hashing its blocks.
 *
 * <p>The output {@link DungeonInfo} is an 11x11 grid: rooms sit on even/even
 * coords, doors/separators on the positions between them.
 */
public final class DungeonMapService {

    private static final int MAP = 128;
    private static final int ROOM_SPACING = 4;
    private static final int SCAN_INTERVAL_TICKS = 5;

    // World coords: room (tileX,tileZ) center = -185 + 32*tile.
    private static final int WORLD_START = -185;
    private static final int WORLD_STEP = 32;

    private final DungeonInfo dungeonInfo = new DungeonInfo();

    private byte[] mapColors = null;
    private List<PlayerMarker> playerMarkers = new ArrayList<>();
    private int centerX = 0;
    private int centerZ = 0;
    private byte scale = 0;
    private int scanCooldown = 0;

    // Map calibration (pixel grid on the 128x128 map).
    private int roomSizePx = 16;
    private int roomGap = 20;
    private int mapStartX = 5;
    private int mapStartY = 5;
    private boolean layoutInit = false;
    private int lastFloor = -1;

    // ── Update ───────────────────────────────────────────────────────────────

    /** Rebuilds the room/door model from the current Hypixel map + world names. */
    public void scan(Minecraft client, DungeonRoomDetector detector, int floor) {
        if (client == null || client.level == null || client.player == null || detector == null) return;
        if (scanCooldown-- > 0) return;
        scanCooldown = SCAN_INTERVAL_TICKS;

        if (floor != lastFloor) {
            initClient(floor);
            lastFloor = floor;
            layoutInit = false;
        }

        byte[] colors = this.mapColors;
        // A valid dungeon map has an empty (0) border in the top-left corner.
        if (colors == null || colors.length < MAP * MAP || colors[0] != 0) return;
        if (!layoutInit) {
            layoutInit = initLayout(colors);
            if (!layoutInit) return;
        }
        buildGrid(client, detector, colors);
    }

    /** Floor-based calibration defaults (Odin DungeonScan.initClient), refined by initLayout. */
    private void initClient(int floor) {
        roomSizePx = floor <= 3 ? 18 : 16;
        roomGap = roomSizePx + ROOM_SPACING;
        mapStartX = floor <= 1 ? 22 : floor <= 3 ? 11 : 5;
        mapStartY = switch (floor) {
            case 0 -> 22;
            case 4 -> 16;
            default -> (floor >= 1 && floor <= 3) ? 11 : 5;
        };
    }

    /** Refines calibration from the map itself: the entrance green run (Odin MapScan.initLayout). */
    private boolean initLayout(byte[] colors) {
        for (int index = 0; index < colors.length; index++) {
            if (colors[index] != (byte) 30) continue; // ENTRANCE map colour
            int end = index;
            while (end < colors.length && colors[end] == (byte) 30) end++;
            int length = end - index;
            if (length == 16 || length == 18) {
                roomSizePx = length;
                roomGap = length + ROOM_SPACING;
                mapStartX = (index % MAP) % roomGap;
                mapStartY = (index / MAP) % roomGap;
                if (mapStartX == 0) mapStartX = 22;
                if (mapStartY == 0) mapStartY = 22;
                return true;
            }
        }
        return false;
    }

    // ── Grid build from the map ────────────────────────────────────────────

    private void buildGrid(Minecraft client, DungeonRoomDetector detector, byte[] colors) {
        int half = roomSizePx / 2;
        int connectionGap = roomSizePx + ROOM_SPACING / 2; // Odin: roomSize + 2

        for (int tz = 0; tz <= 5; tz++) {
            for (int tx = 0; tx <= 5; tx++) {
                int ox = mapStartX + tx * roomGap;
                int oz = mapStartY + tz * roomGap;
                int gx = tx * 2, gz = tz * 2;

                // Room existence + type from the corner pixel; checkmark from the center.
                int corner = getPx(colors, ox, oz);
                RoomType type = roomType(corner);
                if (corner != 0 && type != null) {
                    int center = getPx(colors, ox + half, oz + half);
                    setRoom(client, detector, gx, gz, tx, tz, corner, type, stateFromCheck(corner, center, type));
                } else {
                    dungeonInfo.set(gx, gz, null); // undiscovered / unknown → hide
                }

                // Horizontal door / connection to the east room.
                if (tx < 5) {
                    int center = getPx(colors, ox + connectionGap, oz + half);
                    int side = getPx(colors, ox + connectionGap, oz + half - 4);
                    setGap(gx + 1, gz, center, side, true);
                }
                // Vertical door / connection to the south room.
                if (tz < 5) {
                    int center = getPx(colors, ox + half, oz + connectionGap);
                    int side = getPx(colors, ox + half - 4, oz + connectionGap);
                    setGap(gx, gz + 1, center, side, false);
                }
            }
        }

        // 2x2 room centers: fill when both bordering gaps are same-room separators.
        for (int gz = 1; gz <= 9; gz += 2) {
            for (int gx = 1; gx <= 9; gx += 2) {
                DungeonTile north = dungeonInfo.get(gx, gz - 1);
                DungeonTile west = dungeonInfo.get(gx - 1, gz);
                boolean sepN = north instanceof DungeonRoom r && r.isSeparator();
                boolean sepW = west instanceof DungeonRoom r && r.isSeparator();
                if (sepN && sepW && dungeonInfo.get(gx - 1, gz - 1) instanceof DungeonRoom nw) {
                    DungeonRoom sep = new DungeonRoom(gx, gz, nw.type(), nw.name());
                    sep.setSeparator(true);
                    sep.setMapColorId(nw.mapColorId());
                    sep.setState(nw.state());
                    dungeonInfo.set(gx, gz, sep);
                } else {
                    dungeonInfo.set(gx, gz, null);
                }
            }
        }
    }

    private void setRoom(Minecraft client, DungeonRoomDetector detector, int gx, int gz,
                         int tx, int tz, int corner, RoomType type, RoomState state) {
        int wX = WORLD_START + WORLD_STEP * tx;
        int wZ = WORLD_START + WORLD_STEP * tz;
        String name = detector.getRoomNameAt(client, wX, wZ);

        DungeonRoom room = dungeonInfo.room(gx, gz);
        if (room == null || room.isSeparator()) {
            room = new DungeonRoom(gx, gz, type, name);
            dungeonInfo.set(gx, gz, room);
        } else {
            room.setType(type);
            if (!name.isEmpty()) room.setName(name); // keep last known name when chunk unloaded
        }
        room.setMapColorId(corner);
        room.setState(state);
    }

    /** A between-cell is a door (narrow, side pixel empty) or a same-room separator (side pixel filled). */
    private void setGap(int gx, int gz, int center, int side, boolean horizontal) {
        if (center == 0) { dungeonInfo.set(gx, gz, null); return; }
        if (side == 0) {
            DungeonDoor door = new DungeonDoor(gx, gz, doorType(center), false);
            door.setState(RoomState.DISCOVERED);
            dungeonInfo.set(gx, gz, door);
            return;
        }
        // Connection between two cells of the same room → separator inheriting a neighbour.
        DungeonRoom src = horizontal ? dungeonInfo.room(gx - 1, gz) : dungeonInfo.room(gx, gz - 1);
        if (src == null) src = horizontal ? dungeonInfo.room(gx + 1, gz) : dungeonInfo.room(gx, gz + 1);
        if (src == null) { dungeonInfo.set(gx, gz, null); return; }
        DungeonRoom sep = new DungeonRoom(gx, gz, src.type(), src.name());
        sep.setSeparator(true);
        sep.setMapColorId(src.mapColorId());
        sep.setState(src.state());
        dungeonInfo.set(gx, gz, sep);
    }

    // ── Map colour → type / state (Odin RoomType / MapCheckmark) ──────────────

    private static RoomType roomType(int mapColor) {
        return switch (mapColor) {
            case 30 -> RoomType.ENTRANCE;
            case 82 -> RoomType.FAIRY;
            case 18 -> RoomType.BLOOD;
            case 66 -> RoomType.PUZZLE;
            case 63, 74, 62 -> RoomType.NORMAL; // normal / champion / trap / rare
            default -> null;                         // 85 unknown, 0 undiscovered → not a room
        };
    }

    private static DoorType doorType(int mapColor) {
        return switch (mapColor) {
            case 119 -> DoorType.WITHER;
            case 18  -> DoorType.BLOOD;
            case 30  -> DoorType.ENTRANCE;
            case 82  -> DoorType.NORMAL; // fairy passage rendered like normal
            default  -> DoorType.NORMAL;
        };
    }

    /** Center pixel gives the checkmark; equal to the room body colour means "entered, no check". */
    private static RoomState stateFromCheck(int corner, int center, RoomType type) {
        if (center == corner) return RoomState.DISCOVERED;
        return switch (center) {
            case 34 -> RoomState.CLEARED;                                    // white check
            case 30 -> type == RoomType.ENTRANCE ? RoomState.DISCOVERED : RoomState.GREEN; // green check
            case 18 -> RoomState.FAILED;                                     // red (failed puzzle)
            case 119 -> RoomState.UNOPENED;                                  // question mark
            default -> RoomState.DISCOVERED;
        };
    }

    private static int getPx(byte[] colors, int x, int z) {
        if (x < 0 || x >= MAP || z < 0 || z >= MAP) return 0;
        return colors[z * MAP + x] & 0xFF;
    }

    public DungeonInfo getDungeonInfo() {
        return dungeonInfo;
    }

    // ── Packet map data ────────────────────────────────────────────────────

    /** Called by ClientPlayNetworkHandlerMixin when a dungeon map data packet arrives. */
    public void onMapData(byte[] colors, Iterable<MapDecoration> decorations, int centerX, int centerZ, byte scale) {
        if (colors != null && colors.length == 128 * 128) {
            this.mapColors = colors.clone();
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.scale = scale;
        playerMarkers.clear();
        if (decorations != null) {
            for (MapDecoration dec : decorations) {
                playerMarkers.add(new PlayerMarker(dec.x(), dec.y(), dec.rot(),
                    dec.name().map(net.minecraft.network.chat.Component::getString).orElse("")));
            }
        }
    }

    public byte[] getMapColors() { return mapColors; }
    public List<PlayerMarker> getPlayerMarkers() { return playerMarkers; }
    public boolean hasData() { return mapColors != null; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public byte getScale() { return scale; }

    public void reset() {
        mapColors = null;
        playerMarkers.clear();
        centerX = 0;
        centerZ = 0;
        scale = 0;
        scanCooldown = 0;
        dungeonInfo.clear();
        roomSizePx = 16;
        roomGap = 20;
        mapStartX = 5;
        mapStartY = 5;
        layoutInit = false;
        lastFloor = -1;
    }

    public record PlayerMarker(byte mapX, byte mapY, byte rotation, String name) {}
}
