package de.horizon.api.profile;

public record HorizonPet(
    String type,
    String displayName,
    String tier,
    int level,
    double experience,
    boolean active,
    String heldItem,
    String heldItemDisplayName,
    int candyUsed,
    boolean soulbound,
    String skin,
    String skinDisplayName,
    String minecraftItemId,
    String iconTexture,
    String iconTextureSignature
) {
}
