package de.horizon.mixin;

import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes the map's decoration map WITH its keys. Vanilla {@code getDecorations()} returns only the
 * values, but the key (e.g. {@code icon-0}, {@code icon-1}) identifies which party member a marker
 * belongs to — needed to place teammate heads deterministically on the dungeon map.
 */
@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {
    @Accessor("decorations")
    Map<String, MapDecoration> getDecorationsMap();
}
