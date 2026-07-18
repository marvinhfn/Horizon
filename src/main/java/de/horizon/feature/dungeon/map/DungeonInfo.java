package de.horizon.feature.dungeon.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured model of a dungeon layout built from world scanning.
 * Holds an 11x11 tile grid where rooms sit on even/even coordinates and
 * doors on the positions in between. Later dungeon features (secrets, score,
 * labels) attach their data to the {@link DungeonRoom} instances stored here.
 */
public final class DungeonInfo {
    /** Side length of the tile grid (6 rooms + 5 doors per axis). */
    public static final int GRID = 11;
    /** Number of rooms per axis. */
    public static final int ROOMS = 6;

    private final DungeonTile[][] tiles = new DungeonTile[GRID][GRID];

    public DungeonTile get(int tileX, int tileZ) {
        if (tileX < 0 || tileZ < 0 || tileX >= GRID || tileZ >= GRID) return null;
        return tiles[tileX][tileZ];
    }

    public void set(int tileX, int tileZ, DungeonTile tile) {
        if (tileX < 0 || tileZ < 0 || tileX >= GRID || tileZ >= GRID) return;
        tiles[tileX][tileZ] = tile;
    }

    public DungeonRoom room(int tileX, int tileZ) {
        return get(tileX, tileZ) instanceof DungeonRoom room ? room : null;
    }

    public DungeonDoor door(int tileX, int tileZ) {
        return get(tileX, tileZ) instanceof DungeonDoor door ? door : null;
    }

    public List<DungeonRoom> rooms() {
        List<DungeonRoom> result = new ArrayList<>();
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                if (tiles[x][z] instanceof DungeonRoom room) result.add(room);
            }
        }
        return result;
    }

    public List<DungeonDoor> doors() {
        List<DungeonDoor> result = new ArrayList<>();
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                if (tiles[x][z] instanceof DungeonDoor door) result.add(door);
            }
        }
        return result;
    }

    public boolean isEmpty() {
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                if (tiles[x][z] != null) return false;
            }
        }
        return true;
    }

    public void clear() {
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                tiles[x][z] = null;
            }
        }
    }
}
