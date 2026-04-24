package de.horizon.api.profile;

public record HorizonSlayerBoss(
    String id,
    String displayName,
    int level,
    long experience,
    int kills
) {
}
