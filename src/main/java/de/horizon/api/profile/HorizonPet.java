package de.horizon.api.profile;

public record HorizonPet(
    String type,
    String displayName,
    String tier,
    int level,
    boolean active,
    String heldItem
) {
}
