package de.horizon.feature.waypoint;

/** A single user waypoint. Serialized to disk as-is (public fields, GSON-friendly). */
public final class Waypoint {
    public int x, y, z;
    /** Non-null → x/y/z are RELATIVE to this dungeon room (survives per-run room placement). */
    public String room = null;
    public String name = "Waypoint";
    public String group = "Default";
    /** 0 = Outlined, 1 = Box, 2 = Outline + Box. */
    public int type = 0;
    public boolean throughWalls = true;
    public boolean beacon = false;
    public int color = 0xFF55FF55; // ARGB

    public Waypoint() {}

    public Waypoint(int x, int y, int z, String name) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name;
    }

    public double centerX() { return x + 0.5; }
    public double centerY() { return y; }
    public double centerZ() { return z + 0.5; }
}
