package de.horizon.api.profile;

public record HorizonAccessory(
    String id,
    String displayName,
    String rarity,
    String enrichment,
    boolean active
) {
}
