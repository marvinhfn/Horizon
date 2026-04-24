package de.horizon.feature.dungeon.room;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Locale;

public record DetectedDungeonRoom(
    String name,
    RoomType type,
    BlockPos origin,
    Direction rotation,
    int confidence,
    long detectedAtTick
) {
    public boolean isPuzzle(String puzzleName) {
        return type == RoomType.PUZZLE && normalized(name).equals(normalized(puzzleName));
    }

    public boolean containsName(String value) {
        return normalized(name).contains(normalized(value));
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }
}
