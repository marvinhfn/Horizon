package de.horizon.hud;

import com.mojang.blaze3d.platform.NativeImage;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.map.DungeonMapService;
import de.horizon.feature.dungeon.map.DungeonMapService.PlayerMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

import java.util.*;

/**
 * HUD element that renders the dungeon map as a minimap.
 * Uses configurable room colors mapped from Hypixel map color base IDs.
 * Only visible during dungeon clear (not in boss).
 */
public final class DungeonMapHudElement implements HudElement {
    private static final String ID = "dungeon_map";
    private static final int DEFAULT_X = 5;
    private static final int DEFAULT_Y = 5;
    private static final Identifier MAP_TEXTURE_ID = Identifier.fromNamespaceAndPath("horizon", "dungeon_map_live");
    private static final int MAP_TEX_SIZE = 128;

    // Hypixel dungeon map base color IDs (packedId >> 2)
    private static final int BASE_GREEN   = 27;
    private static final int BASE_MAGENTA = 16;
    private static final int BASE_ORANGE  = 15;
    private static final int BASE_YELLOW  = 18;
    private static final int BASE_RED     = 28;
    private static final int BASE_CYAN    = 23;
    private static final int BASE_BROWN   = 26;

    private static final double[] SHADE_MULT = { 180.0 / 255.0, 220.0 / 255.0, 1.0, 135.0 / 255.0 };

    private final DungeonMapService mapService;
    private final DungeonStateService dungeonStateService;

    private DynamicTexture mapTexture = null;
    private byte[] lastUploadedColors = null;
    private int lastConfigHash = 0;

    // Per-player smooth position tracking (by player name, lowercase)
    private final Map<String, float[]> smoothPositions = new HashMap<>();
    private static final float HEAD_LERP = 0.08f;

    // Cached own-player marker index (stable across frames until marker list changes)
    private int cachedOwnMarkerIdx = -1;
    private int lastMarkerCount = -1;

    public DungeonMapHudElement(DungeonMapService mapService, DungeonStateService dungeonStateService) {
        this.mapService = mapService;
        this.dungeonStateService = dungeonStateService;
    }

    @Override public String id() { return ID; }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return DEFAULT_X; }
    @Override public int defaultY() { return DEFAULT_Y; }
    @Override public int width(Minecraft mc, HudPosition pos) { return (int)(MAP_TEX_SIZE * pos.getScale()); }
    @Override public int height(Minecraft mc, HudPosition pos) { return (int)(MAP_TEX_SIZE * pos.getScale()); }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isDungeonMapEnabled();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Minecraft mc, HudPosition pos, boolean editMode) {
        if (!editMode) {
            if (!mapService.hasData()) return;
            if (!dungeonStateService.isInDungeon()) return;
            if (dungeonStateService.isInBoss()) return;
        }

        float scale = (float) pos.getScale();
        int renderSize = (int)(MAP_TEX_SIZE * scale);
        int x = pos.getX();
        int y = pos.getY();

        // Background
        HorizonConfig config = de.horizon.HorizonClient.getInstance() != null
            ? de.horizon.HorizonClient.getInstance().getConfigManager().getConfig() : null;
        int bgColor = config != null ? config.getMapColorBackground() : 0xCC000000;
        ctx.fill(x - 1, y - 1, x + renderSize + 1, y + renderSize + 1, bgColor);

        byte[] colors = mapService.getMapColors();
        if (editMode || colors == null) {
            ctx.fill(x, y, x + renderSize, y + renderSize, 0xFF334455);
            if (mc != null) ctx.centeredText(mc.font, "\u00a77Map", x + renderSize / 2, y + renderSize / 2 - 3, 0xFFAAAAAA);
            return;
        }

        // Init/update DynamicTexture
        if (mapTexture == null) {
            mapTexture = new DynamicTexture("horizon_dungeon_map", MAP_TEX_SIZE, MAP_TEX_SIZE, false);
            mc.getTextureManager().register(MAP_TEXTURE_ID, mapTexture);
            lastUploadedColors = null;
        }

        int configHash = config != null ? colorConfigHash(config) : 0;

        if (colors != lastUploadedColors || configHash != lastConfigHash) {
            uploadMapColors(colors, config);
            lastUploadedColors = colors;
            lastConfigHash = configHash;
        }

        // Render via matrix scale to avoid UV tiling
        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);
        int sx = Math.round(x / scale);
        int sy = Math.round(y / scale);
        ctx.blit(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE_ID, sx, sy,
                 0f, 0f, MAP_TEX_SIZE, MAP_TEX_SIZE, MAP_TEX_SIZE, MAP_TEX_SIZE);
        ctx.pose().popMatrix();

        // Player markers — match own player by rotation (cached), others by relative position
        var markers = mapService.getPlayerMarkers();
        if (markers.isEmpty()) return;

        int scaleSize = 1 << mapService.getScale();

        boolean[] markerMatched = new boolean[markers.size()];
        Map<Integer, AbstractClientPlayer> markerToPlayer = new HashMap<>();

        // Reset cache when marker count changes (player join/leave/death)
        if (markers.size() != lastMarkerCount) {
            cachedOwnMarkerIdx = -1;
            lastMarkerCount = markers.size();
        }

        if (mc.player != null) {
            // Step 1: Match own player — use cached index or find by rotation
            int ownIdx = cachedOwnMarkerIdx;
            if (ownIdx < 0 || ownIdx >= markers.size()) {
                float playerYaw = mc.player.getYRot();
                int playerRotUnit = (int)(((playerYaw % 360 + 360) % 360) / 22.5f) & 0xF;

                int bestIdx = -1;
                int bestRotDiff = Integer.MAX_VALUE;
                for (int i = 0; i < markers.size(); i++) {
                    int markerRot = Byte.toUnsignedInt(markers.get(i).rotation()) & 0xF;
                    int diff = Math.abs(markerRot - playerRotUnit);
                    diff = Math.min(diff, 16 - diff);
                    if (diff < bestRotDiff) {
                        bestRotDiff = diff;
                        bestIdx = i;
                    }
                }
                if (bestIdx >= 0) {
                    ownIdx = bestIdx;
                    cachedOwnMarkerIdx = bestIdx;
                }
            }

            if (ownIdx >= 0 && ownIdx < markers.size()) {
                markerMatched[ownIdx] = true;
                markerToPlayer.put(ownIdx, mc.player);

                // Step 2: Match other players using relative position offsets
                PlayerMarker ownMarker = markers.get(ownIdx);
                float ownMapX = ownMarker.mapX();
                float ownMapZ = ownMarker.mapY();
                double ownWorldX = mc.player.getX();
                double ownWorldZ = mc.player.getZ();

                List<AbstractClientPlayer> players = mc.level != null
                    ? new ArrayList<>(mc.level.players()) : new ArrayList<>();

                for (AbstractClientPlayer player : players) {
                    if (player == mc.player) continue;
                    float expectedMapX = (float)((player.getX() - ownWorldX) / scaleSize * 2.0) + ownMapX;
                    float expectedMapZ = (float)((player.getZ() - ownWorldZ) / scaleSize * 2.0) + ownMapZ;

                    int closestIdx = -1;
                    float closestDist = Float.MAX_VALUE;
                    for (int i = 0; i < markers.size(); i++) {
                        if (markerMatched[i]) continue;
                        float dx = markers.get(i).mapX() - expectedMapX;
                        float dz = markers.get(i).mapY() - expectedMapZ;
                        float dist = dx * dx + dz * dz;
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestIdx = i;
                        }
                    }
                    if (closestIdx >= 0 && closestDist < 400) {
                        markerMatched[closestIdx] = true;
                        markerToPlayer.put(closestIdx, player);
                    }
                }
            }
        }

        // Render each marker
        for (int i = 0; i < markers.size(); i++) {
            PlayerMarker marker = markers.get(i);
            float nx = (marker.mapX() + 128f) / 256f;
            float ny = (marker.mapY() + 128f) / 256f;
            float rawX = x + nx * renderSize;
            float rawY = y + ny * renderSize;

            AbstractClientPlayer player = markerToPlayer.get(i);
            if (player == null) {
                // Unmatched marker — render as yellow dot
                ctx.fill((int) rawX - 2, (int) rawY - 2, (int) rawX + 2, (int) rawY + 2, 0xFFFFFF00);
                continue;
            }

            String playerName = player.getName().getString();
            String key = playerName.toLowerCase(Locale.ROOT);

            // Smooth position
            float[] smooth = smoothPositions.computeIfAbsent(key, k -> new float[]{Float.NaN, Float.NaN});
            if (Float.isNaN(smooth[0])) {
                smooth[0] = rawX;
                smooth[1] = rawY;
            } else {
                smooth[0] += (rawX - smooth[0]) * HEAD_LERP;
                smooth[1] += (rawY - smooth[1]) * HEAD_LERP;
            }

            // Find skin texture
            Identifier skinTex = player.getSkin().body().texturePath();
            if (skinTex == null) {
                ctx.fill((int) smooth[0] - 2, (int) smooth[1] - 2, (int) smooth[0] + 2, (int) smooth[1] + 2, 0xFFFFFF00);
                continue;
            }

            // Rotation: use marker rotation (22.5 degrees per unit)
            float headYaw = (Byte.toUnsignedInt(marker.rotation()) & 0xF) * 22.5f;

            ctx.pose().pushMatrix();
            ctx.pose().translate(smooth[0], smooth[1]);
            ctx.pose().rotate((float) Math.toRadians(headYaw + 180f));
            ctx.pose().scale(2f, 2f);
            ctx.blit(RenderPipelines.GUI_TEXTURED, skinTex,
                -4, -4, 8f, 8f, 8, 8, 64, 64);
            ctx.pose().popMatrix();
        }
    }

    private void uploadMapColors(byte[] colors, HorizonConfig config) {
        NativeImage img = mapTexture.getPixels();
        if (img == null) return;
        for (int i = 0; i < MAP_TEX_SIZE * MAP_TEX_SIZE; i++) {
            int packedId = Byte.toUnsignedInt(colors[i]);
            if (packedId == 0) {
                img.setPixelABGR(i % MAP_TEX_SIZE, i / MAP_TEX_SIZE, 0);
                continue;
            }

            int baseColor = packedId >> 2;
            int shade = packedId & 3;
            int customArgb = getCustomColor(baseColor, config);

            int argb;
            if (customArgb != 0) {
                argb = applyShade(customArgb, shade);
            } else {
                argb = MapColor.getColorFromPackedId(packedId);
            }

            // ARGB -> ABGR for NativeImage
            int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
            img.setPixelABGR(i % MAP_TEX_SIZE, i / MAP_TEX_SIZE, abgr);
        }
        mapTexture.upload();
    }

    private static int getCustomColor(int baseColorId, HorizonConfig config) {
        if (config == null) return 0;
        return switch (baseColorId) {
            case BASE_BROWN   -> config.getMapColorNormal();
            case BASE_GREEN   -> config.getMapColorEntrance();
            case BASE_MAGENTA -> config.getMapColorPuzzle();
            case BASE_ORANGE  -> config.getMapColorTrap();
            case BASE_YELLOW  -> config.getMapColorMiniboss();
            case BASE_RED     -> config.getMapColorBlood();
            case BASE_CYAN    -> config.getMapColorRare();
            default -> 0;
        };
    }

    private static int applyShade(int argb, int shade) {
        double mult = SHADE_MULT[shade];
        int a = (argb >> 24) & 0xFF;
        int r = (int)(((argb >> 16) & 0xFF) * mult);
        int g = (int)(((argb >> 8) & 0xFF) * mult);
        int b = (int)((argb & 0xFF) * mult);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int colorConfigHash(HorizonConfig config) {
        int h = config.getMapColorBackground();
        h = h * 31 + config.getMapColorNormal();
        h = h * 31 + config.getMapColorPuzzle();
        h = h * 31 + config.getMapColorTrap();
        h = h * 31 + config.getMapColorEntrance();
        h = h * 31 + config.getMapColorMiniboss();
        h = h * 31 + config.getMapColorBlood();
        h = h * 31 + config.getMapColorRare();
        return h;
    }
}
