package de.horizon.feature.dungeon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DungeonMapService {
    private byte[] cachedColors;
    private final List<PlayerDot> playerDots = new ArrayList<>();
    private boolean dirty = true;

    public record PlayerDot(float pixelX, float pixelZ, int argbColor) {}

    public void tick(MinecraftClient client, DungeonStateService dungeonState) {
        if (client == null || client.player == null || client.world == null) {
            reset();
            return;
        }
        if (!dungeonState.isInDungeon()) {
            reset();
            return;
        }

        MapState mapState = findDungeonMapState(client);
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
            float pz = (deco.z() + 128) / 2.0f;
            playerDots.add(new PlayerDot(px, pz, 0xFFFFFFFF));
        }
    }

    private MapState findDungeonMapState(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        // Hotbar first (slots 0-8), then rest of inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof FilledMapItem) {
                MapState state = FilledMapItem.getMapState(stack, world);
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
