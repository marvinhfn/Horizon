package de.horizon.hypixel;

import java.util.List;
import java.util.Map;

public record HypixelDungeonStats(
    String username,
    String uuid,
    String selectedProfile,
    int catacombsLevel,
    int secretsFound,
    int totalCompletions,
    Map<String, Double> fastestSPlusSeconds,
    int skyblockLevel,
    double averageSkillLevel,
    long totalSlayerXp,
    double purse,
    double bank,
    double personalBank,
    double networth,
    double nonCosmeticNetworth,
    Map<String, Integer> skillLevels,
    Map<String, Float> skillProgress,
    Map<String, Integer> slayerLevels,
    Map<String, Double> networthByType,
    List<String> profileNames
) {
    public HypixelDungeonStats(
        String username,
        String uuid,
        String selectedProfile,
        int catacombsLevel,
        int secretsFound,
        int totalCompletions,
        Map<String, Double> fastestSPlusSeconds
    ) {
        this(
            username,
            uuid,
            selectedProfile,
            catacombsLevel,
            secretsFound,
            totalCompletions,
            fastestSPlusSeconds,
            0,
            0.0D,
            0L,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );
    }

    public double fastestSPlus(String floorKey) {
        return fastestSPlusSeconds.getOrDefault(floorKey, -1.0D);
    }
}
