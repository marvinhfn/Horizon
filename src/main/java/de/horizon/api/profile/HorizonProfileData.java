package de.horizon.api.profile;

import java.util.List;
import java.util.Map;

public record HorizonProfileData(
    String playerName,
    String playerUuid,
    String profileId,
    String profileName,
    String gameMode,
    int skyblockLevel,
    int catacombsLevel,
    double purse,
    double bank,
    double networth,
    List<String> profileNames,
    List<HorizonStoragePage> storages,
    List<HorizonAccessory> accessories,
    List<HorizonPet> pets,
    List<HorizonSkill> skills,
    List<HorizonSlayerBoss> slayers,
    Map<String, String> metadata
) {
    public static HorizonProfileData empty(String playerName) {
        return new HorizonProfileData(
            playerName,
            "",
            "",
            "",
            "",
            0,
            0,
            0.0D,
            0.0D,
            0.0D,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of()
        );
    }
}
