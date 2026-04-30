package de.horizon.api.profile;

public record HorizonInventoryItem(
    String itemId,
    String minecraftItemId,
    String displayName,
    String rarity,
    int count,
    String lore,
    String iconTexture,
    String iconTextureSignature,
    int leatherColor,
    boolean enchanted
) {
    public static HorizonInventoryItem empty() {
        return new HorizonInventoryItem("", "", "", "", 0, "", "", "", -1, false);
    }

    public boolean isEmpty() {
        return count <= 0 || (displayName.isBlank() && itemId.isBlank() && minecraftItemId.isBlank() && iconTexture.isBlank());
    }
}
