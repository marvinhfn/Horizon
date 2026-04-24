package de.horizon.api.profile;

public record HorizonInventoryItem(
    String itemId,
    String displayName,
    String rarity,
    int count,
    String lore,
    String iconTexture,
    boolean enchanted
) {
    public static HorizonInventoryItem empty() {
        return new HorizonInventoryItem("", "", "", 0, "", "", false);
    }

    public boolean isEmpty() {
        return count <= 0 || displayName.isBlank();
    }
}
