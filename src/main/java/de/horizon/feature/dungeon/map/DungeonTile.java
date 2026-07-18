package de.horizon.feature.dungeon.map;

/**
 * Base type for a single cell in the dungeon tile grid.
 * The grid is 11x11: rooms live on even/even coordinates, doors on the
 * positions in between (exactly one odd coordinate).
 */
public sealed interface DungeonTile permits DungeonRoom, DungeonDoor {
    int tileX();
    int tileZ();
    RoomState state();
    void setState(RoomState state);
}
