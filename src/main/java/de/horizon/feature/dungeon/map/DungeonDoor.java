package de.horizon.feature.dungeon.map;

/**
 * A door/passage tile connecting two adjacent rooms.
 * A horizontal door (odd tileX) connects the rooms to its west and east,
 * a vertical door (odd tileZ) connects the rooms to its north and south.
 */
public final class DungeonDoor implements DungeonTile {
    private final int tileX;
    private final int tileZ;
    private DoorType type;
    private boolean opened;
    private RoomState state = RoomState.DISCOVERED;

    public DungeonDoor(int tileX, int tileZ, DoorType type, boolean opened) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.type = type == null ? DoorType.UNKNOWN : type;
        this.opened = opened;
    }

    @Override public int tileX() { return tileX; }
    @Override public int tileZ() { return tileZ; }
    @Override public RoomState state() { return state; }
    @Override public void setState(RoomState state) { if (state != null) this.state = state; }

    /** True if this door connects two horizontally adjacent rooms. */
    public boolean horizontal() { return (tileX & 1) == 1; }

    public DoorType type() { return type; }
    public void setType(DoorType type) { if (type != null) this.type = type; }

    public boolean opened() { return opened; }
    public void setOpened(boolean opened) { this.opened = opened; }
}
