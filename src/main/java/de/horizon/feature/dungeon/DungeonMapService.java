package de.horizon.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DungeonMapService {
    private byte[] cachedColors;
    private final List<PlayerDot> playerDots = new ArrayList<>();
    private boolean dirty = true;

    public record PlayerDot(float pixelX, float pixelZ, int argbColor) {}

    public void tick(Minecraft client, DungeonStateService dungeonState) {
        if (client == null || client.player == null || client.level == null) {
            reset();
            return;
        }
        if (!dungeonState.isInDungeon()) {
            reset();
            return;
        }

        MapItemSavedData mapState = findDungeonMapItemSavedData(client);
        if (mapState == null) {
            return;
        }

        byte[] colors = mapState.colors;
        if (colors != null && (cachedColors == null || !Arrays.equals(cachedColors, colors))) {
            cachedColors = colors.clone();
            dirty = true;
        }

        playerDots.clear();
        for (MapDecoration deco : mapState.getDecorations()) {
            // signed byte -128..127 → pixel 0..127
            float px = (deco.x() + 128) / 2.0f;
            float pz = (deco.y() + 128) / 2.0f;
            playerDots.add(new PlayerDot(px, pz, 0xFFFFFFFF));
        }
    }

    private MapItemSavedData findDungeonMapItemSavedData(Minecraft client) {
        var player = client.player;
        var world = client.level;
        // Hotbar first (slots 0-8), then rest of inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MapItem) {
                MapItemSavedData state = MapItem.getSavedData(stack, world);
                if (state != null) {
                    return state;
                }
            }
        }
        return null;
    }

    public byte[] getCachedColors() {
        return cachedColors;
    }

    public List<PlayerDot> getPlayerDots() {
        return playerDots;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    private void reset() {
        if (cachedColors != null) {
            cachedColors = null;
            dirty = true;
        }
        playerDots.clear();
    }
}
