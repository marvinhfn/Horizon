package de.horizon.feature.dungeon.map;

import de.horizon.feature.dungeon.room.RoomType;

/**
 * A single room cell on the dungeon map grid.
 * A room always occupies one tile position; multi-tile rooms are represented
 * as several adjacent room cells of the same type joined by internal passages.
 */
public final class DungeonRoom implements DungeonTile {
    private final int tileX;
    private final int tileZ;
    private RoomType type;
    private String name;
    private RoomState state;
    private int secretsFound;
    private int secretsTotal;
    private boolean separator;
    private int mapColorId; // Hypixel map corner colour byte (room type)

    public DungeonRoom(int tileX, int tileZ, RoomType type, String name) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.type = type == null ? RoomType.UNKNOWN : type;
        this.name = name == null ? "" : name;
        this.state = RoomState.DISCOVERED;
        this.secretsFound = 0;
        this.secretsTotal = -1;
        this.separator = false;
    }

    /**
     * A separator cell is the filler tile that visually joins the sub-tiles of a
     * multi-cell room (2x2 center or the gap between two cells of the same room).
     * It carries its parent room's type/name but is not a room of its own.
     */
    public boolean isSeparator() { return separator; }
    public void setSeparator(boolean separator) { this.separator = separator; }

    public int mapColorId() { return mapColorId; }
    public void setMapColorId(int mapColorId) { this.mapColorId = mapColorId; }

    @Override public int tileX() { return tileX; }
    @Override public int tileZ() { return tileZ; }

    /** Room grid column (0..5). */
    public int roomCol() { return tileX / 2; }
    /** Room grid row (0..5). */
    public int roomRow() { return tileZ / 2; }

    public RoomType type() { return type; }
    public void setType(RoomType type) { if (type != null) this.type = type; }

    public String name() { return name; }
    public void setName(String name) { if (name != null) this.name = name; }

    public RoomState state() { return state; }
    public void setState(RoomState state) { if (state != null) this.state = state; }

    public int secretsFound() { return secretsFound; }
    public int secretsTotal() { return secretsTotal; }
    public void setSecrets(int found, int total) {
        this.secretsFound = found;
        this.secretsTotal = total;
    }
}
