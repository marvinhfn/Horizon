package de.horizon.hud;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonMapService;
import de.horizon.feature.dungeon.DungeonStateService;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class DungeonMapHudElement implements HudElement {
    public static final String ID = "dungeon_map";

    private static final int MAP_PIXELS = 128;
    private static final int BORDER = 1;

    private final DungeonMapService mapService;
    private final DungeonStateService stateService;

    private DynamicTexture mapTexture;
    private Identifier mapTextureId;

    public DungeonMapHudElement(DungeonMapService mapService, DungeonStateService stateService) {
        this.mapService = mapService;
        this.stateService = stateService;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isDungeonMapEnabled();
    }

    @Override
    public boolean isMovable() {
        return true;
    }

    @Override
    public int defaultX() {
        return 4;
    }

    @Override
    public int defaultY() {
        return 4;
    }

    @Override
    public int width(Minecraft client, HudPosition position) {
        return (int) Math.ceil((MAP_PIXELS + BORDER * 2) * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        return (int) Math.ceil((MAP_PIXELS + BORDER * 2) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        HorizonConfig config = HorizonClient.getInstance() == null ? null
                : HorizonClient.getInstance().getConfigManager().getConfig();
        if (config == null) {
            return;
        }

        if (!editorMode && !stateService.isInDungeon()) {
            return;
        }

        ensureTexture(client);

        float scale = (float) position.getScale();
        int totalSize = MAP_PIXELS + BORDER * 2;

        var matrices = drawContext.pose();
        matrices.pushMatrix();
        matrices.translate(position.getX(), position.getY());
        matrices.scale(scale, scale);

        // Dark background
        drawContext.fill(0, 0, totalSize, totalSize, 0xCC060B11);

        // Refresh texture if map data changed
        if (mapService.isDirty()) {
            uploadTexture(mapService.getCachedColors());
            mapService.clearDirty();
        }
        if (editorMode && mapTexture != null && mapService.getCachedColors() == null) {
            uploadCheckerboard();
        }

        // Draw the map texture
        drawContext.blit(mapTextureId, BORDER, BORDER, MAP_PIXELS, MAP_PIXELS,
                0.0f, 0.0f, 1.0f, 1.0f);

        // Player dots
        if (config.isDungeonMapShowPlayers()) {
            for (DungeonMapService.PlayerDot dot : mapService.getPlayerDots()) {
                int dotX = BORDER + Math.round(dot.pixelX() * MAP_PIXELS / 128.0f);
                int dotY = BORDER + Math.round(dot.pixelZ() * MAP_PIXELS / 128.0f);
                drawContext.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, dot.argbColor());
                drawContext.fill(dotX, dotY, dotX + 1, dotY + 1, 0xFF000000);
            }
        }

        // Outline
        if (config.isDungeonMapOutlineEnabled()) {
            drawContext.outline(0, 0, totalSize, totalSize, HudStyle.border());
        }

        matrices.popMatrix();
    }

    private void ensureTexture(Minecraft client) {
        if (mapTexture == null) {
            mapTexture = new DynamicTexture("horizon_dungeon_map", MAP_PIXELS, MAP_PIXELS, true);
            mapTextureId = Identifier.fromNamespaceAndPath("horizon", "dungeon_map_hud");
            client.getTextureManager().register(mapTextureId, mapTexture);
        }
    }

    private void uploadTexture(byte[] colors) {
        if (mapTexture == null) {
            return;
        }
        NativeImage image = mapTexture.getPixels();
        if (image == null) {
            return;
        }
        // BLACK render color for empty/null maps so the texture is opaque
        int fallback = MapColor.getColorFromPackedId(MapColor.COLOR_BLACK.id * 4 + 2);
        for (int y = 0; y < MAP_PIXELS; y++) {
            for (int x = 0; x < MAP_PIXELS; x++) {
                int colorByte = colors != null ? (colors[x + y * MAP_PIXELS] & 0xFF) : 0;
                int color = colorByte == 0 ? fallback : MapColor.getColorFromPackedId(colorByte);
                image.setPixel(x, y, color);
            }
        }
        mapTexture.upload();
    }

    private void uploadCheckerboard() {
        if (mapTexture == null) {
            return;
        }
        NativeImage image = mapTexture.getPixels();
        if (image == null) {
            return;
        }
        for (int y = 0; y < MAP_PIXELS; y++) {
            for (int x = 0; x < MAP_PIXELS; x++) {
                boolean checker = ((x / 16 + y / 16) % 2 == 0);
                image.setPixel(x, y, checker ? MapColor.getColorFromPackedId(35 * 4 + 2) : MapColor.getColorFromPackedId(29 * 4 + 1));
            }
        }
        mapTexture.upload();
    }
}
