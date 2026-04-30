package de.horizon.api.profile;

import java.util.List;

public record HorizonDungeonData(
    String selectedClass,
    int secrets,
    List<HorizonDungeonClass> classes,
    List<HorizonDungeonFloor> floors
) {
    public static HorizonDungeonData empty() {
        return new HorizonDungeonData("", 0, List.of(), List.of());
    }
}
