package de.horizon.api.profile;

import java.util.List;

public record HorizonStoragePage(
    String id,
    String title,
    int columns,
    int rows,
    HorizonInventoryItem buttonItem,
    List<HorizonInventorySlot> slots
) {
}
