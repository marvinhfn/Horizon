package de.horizon.feature.dungeon.map;

/**
 * Clear/discovery state of a dungeon tile, derived from the Hypixel dungeon
 * map's checkmark colours. Ordinal order goes from most to least progressed.
 */
public enum RoomState {
    /** All secrets found — green checkmark on the Hypixel map. */
    GREEN,
    /** Every starred mob dead — white checkmark on the Hypixel map. */
    CLEARED,
    /** Entered but not cleared. */
    DISCOVERED,
    /** Puzzle failed. */
    FAILED,
    /** Visible on the map but not opened/entered yet. */
    UNOPENED,
    /** Not entered — hidden from the map. */
    UNDISCOVERED
}
