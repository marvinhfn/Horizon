package de.horizon.api.profile;

public record HorizonSkill(
    String id,
    String displayName,
    int level,
    double progress,
    double experience
) {
}
