package de.horizon.api.profile;

import java.util.List;
import java.util.Map;

public record HorizonAccessoryStorage(
    String selectedPower,
    int highestMagicalPower,
    int bagUpgradesPurchased,
    List<String> unlockedPowers,
    Map<String, String> tuning
) {
    public static HorizonAccessoryStorage empty() {
        return new HorizonAccessoryStorage("", 0, 0, List.of(), Map.of());
    }
}
