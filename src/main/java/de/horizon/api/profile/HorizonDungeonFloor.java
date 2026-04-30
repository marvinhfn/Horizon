package de.horizon.api.profile;

public record HorizonDungeonFloor(
    String id,
    String displayName,
    int completions,
    int fastestTimeMs,
    int fastestSPlusTimeMs,
    int bestScore
) {
}
