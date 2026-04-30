package de.horizon.api.profile;

public record HorizonDungeonClass(
    String id,
    String displayName,
    int level,
    double experience,
    boolean selected
) {
}
