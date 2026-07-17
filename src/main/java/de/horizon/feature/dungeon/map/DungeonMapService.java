package de.horizon.feature.dungeon.map;

import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes dungeon map data received via map item data packets.
 * Stores room/door layout derived from the 128x128 map pixel colors.
 */
public final class DungeonMapService {

    private byte[] mapColors = null;
    private List<PlayerMarker> playerMarkers = new ArrayList<>();
    private int centerX = 0;
    private int centerZ = 0;
    private byte scale = 0;

    /** Called by ClientPlayNetworkHandlerMixin when a dungeon map data packet arrives. */
    public void onMapData(byte[] colors, Iterable<MapDecoration> decorations, int centerX, int centerZ, byte scale) {
        if (colors != null && colors.length == 128 * 128) {
            this.mapColors = colors.clone();
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.scale = scale;
        playerMarkers.clear();
        if (decorations != null) {
            for (MapDecoration dec : decorations) {
                playerMarkers.add(new PlayerMarker(dec.x(), dec.y(), dec.rot(),
                    dec.name().map(net.minecraft.network.chat.Component::getString).orElse("")));
            }
        }
    }

    public byte[] getMapColors() { return mapColors; }
    public List<PlayerMarker> getPlayerMarkers() { return playerMarkers; }
    public boolean hasData() { return mapColors != null; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public byte getScale() { return scale; }

    public void reset() {
        mapColors = null;
        playerMarkers.clear();
        centerX = 0;
        centerZ = 0;
        scale = 0;
    }

    public record PlayerMarker(byte mapX, byte mapY, byte rotation, String name) {}
}
