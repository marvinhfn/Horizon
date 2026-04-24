package de.horizon.api.profile;

public record HorizonInventorySlot(
    int index,
    int x,
    int y,
    HorizonInventoryItem item
) {
}
