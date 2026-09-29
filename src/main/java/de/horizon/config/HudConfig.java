package de.horizon.config;

import java.util.HashMap;
import java.util.Map;

public final class HudConfig {
    boolean reviveHudEnabled = true;
    String hudAccentColor = "#75E7CA";
    String themeFamily = "rose";
    String themeMode = "light";
    boolean reviveHudOnlyInBoss = false;
    boolean reviveHudAlwaysVisible = false;
    boolean spiritMaskEnabled = true;
    boolean bonzoMaskEnabled = true;
    boolean phoenixPetEnabled = true;
    boolean compactHypixelHealthEnabled = true;
    boolean hideDefenseBar = false;
    boolean timeHudEnabled = false;
    boolean performanceHudEnabled = false;
    boolean systemHudEnabled = false;
    boolean musicHudEnabled = false;
    boolean miningAbilityTimerEnabled = false;
    boolean miningAbilityReadyTitleEnabled = false;
    int miningAbilityReadyColor = 0xFFFF66AA; // rose
    boolean commissionHudEnabled = false;
    Map<String, HudPosition> hudPositions = new HashMap<>();
}
