package de.horizon.config;

import de.horizon.feature.fishing.FishingAlertSound;
import java.util.HashSet;
import java.util.Set;

public final class FishingConfig {
    boolean fishingRareAlertEnabled = true;
    Set<String> disabledCreatures = new HashSet<>();
    FishingAlertSound fishingAlertSound = FishingAlertSound.RARE;
}
