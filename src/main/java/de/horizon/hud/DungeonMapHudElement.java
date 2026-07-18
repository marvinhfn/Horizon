package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.map.DoorType;
import de.horizon.feature.dungeon.map.DungeonDoor;
import de.horizon.feature.dungeon.map.DungeonInfo;
import de.horizon.feature.dungeon.map.DungeonMapService;
import de.horizon.feature.dungeon.map.DungeonRoom;
import de.horizon.feature.dungeon.map.DungeonTile;
import de.horizon.feature.dungeon.map.RoomState;
import de.horizon.feature.dungeon.room.RoomType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the structured dungeon layout ({@link DungeonInfo}) built by
 * {@link DungeonMapService} from world scanning, laid out like the NoammAddons
 * dungeon map: rooms as coloured squares, doors as short connectors, multi-cell
 * rooms merged by full-width connectors, plus room-name labels and player heads.
 * Only visible during the dungeon clear phase (not in boss).
 */
public final class DungeonMapHudElement implements HudElement {
    private static final String ID = "dungeon_map";
    private static final int DEFAULT_X = 5;
    private static final int DEFAULT_Y = 5;

    private static final int BOX = 128;         // logical HUD box (before scale)
    private static final int ROOM_SIZE = 16;    // room cell size in px
    private static final int CONNECTOR = 4;     // door / gap thickness in px
    private static final int CELL = ROOM_SIZE + CONNECTOR; // room-to-room stride (20)
    private static final int HALF_ROOM = ROOM_SIZE / 2;    // 8
    private static final int CONTENT = 5 * CELL + ROOM_SIZE;      // full grid extent (116)
    private static final int MAP_OFFSET = (BOX - CONTENT) / 2;    // centring offset (6)
    private static final int DOORWAY_OFFSET = 5; // small door bar centring (ROOM_SIZE == 16)

    // World<->pixel mapping (matches DungeonMapService grid: first room center at -185).
    private static final int START = -185;
    private static final int ROOM_STEP = 32; // world blocks between room centers

    private final DungeonMapService mapService;
    private final DungeonStateService dungeonStateService;

    public DungeonMapHudElement(DungeonMapService mapService, DungeonStateService dungeonStateService) {
        this.mapService = mapService;
        this.dungeonStateService = dungeonStateService;
    }

    @Override public String id() { return ID; }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return DEFAULT_X; }
    @Override public int defaultY() { return DEFAULT_Y; }
    @Override public int width(Minecraft mc, HudPosition pos) { return (int)(BOX * pos.getScale()); }
    @Override public int height(Minecraft mc, HudPosition pos) { return (int)(BOX * pos.getScale()); }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isDungeonMapEnabled();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Minecraft mc, HudPosition pos, boolean editMode) {
        DungeonInfo info = mapService.getDungeonInfo();
        if (!editMode) {
            if (!dungeonStateService.isInDungeon() || dungeonStateService.isInBoss()) return;
            if (info.isEmpty()) return;
        }

        HorizonConfig config = de.horizon.HorizonClient.getInstance() != null
            ? de.horizon.HorizonClient.getInstance().getConfigManager().getConfig() : null;

        float scale = (float) pos.getScale();
        float bx = pos.getX() / scale;
        float by = pos.getY() / scale;

        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);

        int bg = config != null ? config.getMapColorBackground() : 0xCC000000;
        ctx.fill((int) bx, (int) by, (int) bx + BOX, (int) by + BOX, bg);

        if (editMode && info.isEmpty()) {
            ctx.centeredText(mc.font, "§7Map", (int)(bx + BOX / 2f), (int)(by + BOX / 2f - 4), 0xFFAAAAAA);
            ctx.pose().popMatrix();
            return;
        }

        float ox = bx + MAP_OFFSET;
        float oy = by + MAP_OFFSET;
        ctx.pose().pushMatrix();
        ctx.pose().translate(ox, oy);

        renderTiles(ctx, info, config);
        renderUnknownRooms(ctx, mc, info);
        if (config != null && config.isMapShowRoomNames() && mc != null) {
            renderNames(ctx, mc, info, config);
        }
        renderPlayers(ctx, mc);

        ctx.pose().popMatrix();
        ctx.pose().popMatrix();
    }

    // ── Tiles ──────────────────────────────────────────────────────────────

    private void renderTiles(GuiGraphicsExtractor ctx, DungeonInfo info, HorizonConfig config) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                DungeonTile tile = info.get(x, z);
                if (tile == null) continue;

                boolean xEven = (x & 1) == 0;
                boolean zEven = (z & 1) == 0;

                // Rooms show once entered. A door shows once at least one side is
                // entered (the room behind it is drawn as a "?" placeholder). A
                // same-room separator shows only when both its cells are entered.
                boolean visible;
                if (xEven && zEven) visible = tile instanceof DungeonRoom && isRoomEntered(tile.state());
                else if (tile instanceof DungeonDoor) visible = doorVisible(info, x, z);
                else visible = bothRoomsEntered(info, x, z);
                if (!visible) continue;

                int color = tileColor(tile, config);
                int xo = (x >> 1) * CELL;
                int yo = (z >> 1) * CELL;

                if (xEven && zEven) {
                    if (tile instanceof DungeonRoom) {
                        ctx.fill(xo, yo, xo + ROOM_SIZE, yo + ROOM_SIZE, color);
                    }
                } else if (!xEven && !zEven) {
                    // 2x2 room center — fill the whole joining block.
                    ctx.fill(xo, yo, xo + CELL, yo + CELL, color);
                } else {
                    drawConnector(ctx, xo, yo, tile instanceof DungeonDoor, !xEven, color);
                }
            }
        }
    }

    // ── Unknown "?" rooms (path hints) ────────────────────────────────────────

    private static final int UNKNOWN_ROOM_COLOR = 0xAA2A2A2A; // dark transparent gray
    private static final int UNKNOWN_MARK_COLOR = 0xFFCCCCCC;

    /**
     * Draws the room behind each discovered door as a 1x1 gray "?" placeholder,
     * until that room is entered (then {@link #renderTiles} draws it for real).
     */
    private void renderUnknownRooms(GuiGraphicsExtractor ctx, Minecraft mc, DungeonInfo info) {
        boolean[][] drawn = new boolean[11][11];
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                if (!(info.get(x, z) instanceof DungeonDoor) || !doorVisible(info, x, z)) continue;
                boolean horizontal = (x & 1) == 1;
                int[][] sides = horizontal
                    ? new int[][]{{x - 1, z}, {x + 1, z}}
                    : new int[][]{{x, z - 1}, {x, z + 1}};
                for (int[] s : sides) {
                    int cx = s[0], cz = s[1];
                    if (cx < 0 || cx > 10 || cz < 0 || cz > 10 || drawn[cx][cz]) continue;
                    if (roomEntered(info, cx, cz)) continue; // real room already drawn
                    drawn[cx][cz] = true;
                    int xo = (cx >> 1) * CELL;
                    int yo = (cz >> 1) * CELL;
                    ctx.fill(xo, yo, xo + ROOM_SIZE, yo + ROOM_SIZE, UNKNOWN_ROOM_COLOR);
                    if (mc != null) {
                        ctx.centeredText(mc.font, "?", xo + ROOM_SIZE / 2,
                            yo + ROOM_SIZE / 2 - mc.font.lineHeight / 2, UNKNOWN_MARK_COLOR);
                    }
                }
            }
        }
    }

    private void drawConnector(GuiGraphicsExtractor ctx, int x, int y, boolean doorway, boolean vertical, int color) {
        int len = doorway ? 6 : ROOM_SIZE;
        int x1 = vertical ? x + ROOM_SIZE : x;
        int y1 = vertical ? y : y + ROOM_SIZE;
        if (doorway) {
            if (vertical) y1 += DOORWAY_OFFSET; else x1 += DOORWAY_OFFSET;
        }
        int w = vertical ? CONNECTOR : len;
        int h = vertical ? len : CONNECTOR;
        ctx.fill(x1, y1, x1 + w, y1 + h, color);
    }

    // ── Names ──────────────────────────────────────────────────────────────

    private void renderNames(GuiGraphicsExtractor ctx, Minecraft mc, DungeonInfo info, HorizonConfig config) {
        boolean[][] visited = new boolean[11][11];
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                DungeonTile tile = info.get(x, z);
                if (!(tile instanceof DungeonRoom) || visited[x][z]) continue;

                // Flood-fill the connected room blob (room cells + their separators);
                // separate rooms are split by door cells / empty gaps.
                List<int[]> cells = new ArrayList<>();
                DungeonRoom main = floodRoom(info, visited, x, z, cells);
                if (main == null || main.name().isEmpty() || main.type() == RoomType.ENTRANCE) continue;
                if (!isRoomEntered(main.state())) continue;

                drawRoomName(ctx, mc, main, cells, config);
            }
        }
    }

    /** BFS over adjacent room/separator cells; returns the main (named) room cell. */
    private DungeonRoom floodRoom(DungeonInfo info, boolean[][] visited, int sx, int sz, List<int[]> cells) {
        DungeonRoom main = null;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sz});
        visited[sx][sz] = true;
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int cx = c[0], cz = c[1];
            DungeonTile t = info.get(cx, cz);
            if (!(t instanceof DungeonRoom room)) continue;
            cells.add(c);
            if (!room.isSeparator() && !room.name().isEmpty() && main == null) main = room;
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = cx + d[0], nz = cz + d[1];
                if (nx < 0 || nx > 10 || nz < 0 || nz > 10 || visited[nx][nz]) continue;
                if (info.get(nx, nz) instanceof DungeonRoom) {
                    visited[nx][nz] = true;
                    queue.add(new int[]{nx, nz});
                }
            }
        }
        return main;
    }

    private void drawRoomName(GuiGraphicsExtractor ctx, Minecraft mc, DungeonRoom room, List<int[]> cells, HorizonConfig config) {
        // Pixel bounding box of the whole (possibly multi-cell) room.
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
        for (int[] c : cells) {
            int xo = (c[0] >> 1) * CELL;
            int yo = (c[1] >> 1) * CELL;
            int w = (c[0] & 1) == 0 ? ROOM_SIZE : CELL;
            int h = (c[1] & 1) == 0 ? ROOM_SIZE : CELL;
            left = Math.min(left, xo);
            top = Math.min(top, yo);
            right = Math.max(right, xo + w);
            bottom = Math.max(bottom, yo + h);
        }
        float cx = (left + right) / 2f;
        float cy = (top + bottom) / 2f;
        float boxW = (right - left) - 1f;       // small padding so text never touches the edge
        float boxH = (bottom - top) - 1f;

        String[] lines = room.name().split(" ");
        int maxLineW = 1;
        for (String line : lines) maxLineW = Math.max(maxLineW, mc.font.width(line));
        float totalH = lines.length * mc.font.lineHeight;

        // Scale so the name fits entirely within the room bounds (never overflows).
        float tscale = Math.min(boxW / maxLineW, boxH / totalH);
        tscale = Math.max(0.25f, Math.min(1.0f, tscale));

        int color = nameColor(room.state(), config);
        float lineH = mc.font.lineHeight * tscale;
        float startY = cy - lines.length * lineH / 2f;
        for (int i = 0; i < lines.length; i++) {
            ctx.pose().pushMatrix();
            ctx.pose().translate(cx, startY + i * lineH);
            ctx.pose().scale(tscale, tscale);
            ctx.centeredText(mc.font, lines[i], 0, 0, color);
            ctx.pose().popMatrix();
        }
    }

    // ── Player heads ─────────────────────────────────────────────────────────

    private void renderPlayers(GuiGraphicsExtractor ctx, Minecraft mc) {
        if (mc == null || mc.level == null) return;
        var connection = mc.getConnection();
        if (connection == null) return;
        List<AbstractClientPlayer> players = new ArrayList<>(mc.level.players());
        for (AbstractClientPlayer player : players) {
            // Only real players — dungeon mobs are player-type NPCs with version-2
            // UUIDs and are absent from the tab list.
            if (player.getUUID().version() != 4) continue;
            if (connection.getPlayerInfo(player.getUUID()) == null) continue;
            float[] px = worldToPixel(player.getX(), player.getZ());
            if (px == null) continue;

            Identifier skin = player.getSkin().body().texturePath();
            float yaw = player.getYRot();

            ctx.pose().pushMatrix();
            ctx.pose().translate(px[0], px[1]);
            ctx.pose().rotate((float) Math.toRadians(yaw + 180f));
            if (skin != null) {
                ctx.pose().scale(1.4f, 1.4f);
                ctx.blit(RenderPipelines.GUI_TEXTURED, skin, -4, -4, 8f, 8f, 8, 8, 64, 64);
            } else {
                ctx.fill(-2, -2, 2, 2, 0xFFFFFF00);
            }
            ctx.pose().popMatrix();
        }
    }

    private static float[] worldToPixel(double worldX, double worldZ) {
        float mx = (float)((worldX - START) / ROOM_STEP) * CELL + HALF_ROOM;
        float mz = (float)((worldZ - START) / ROOM_STEP) * CELL + HALF_ROOM;
        if (mx < -CELL || mx > CONTENT + CELL || mz < -CELL || mz > CONTENT + CELL) return null;
        mx = Math.max(0, Math.min(CONTENT, mx));
        mz = Math.max(0, Math.min(CONTENT, mz));
        return new float[]{ mx, mz };
    }

    // ── Colours ──────────────────────────────────────────────────────────────

    private static int tileColor(DungeonTile tile, HorizonConfig cfg) {
        if (tile instanceof DungeonRoom room) return roomColor(room.mapColorId(), cfg);
        if (tile instanceof DungeonDoor door) return doorColor(door.type(), cfg);
        return 0xFF999999;
    }

    /** Colours by the Hypixel map corner colour so trap/champion/rare all render distinctly. */
    private static int roomColor(int mapColorId, HorizonConfig cfg) {
        int color = switch (mapColorId) {
            case 30 -> cfg != null ? cfg.getMapColorEntrance() : 0xFF00CC00; // entrance
            case 18 -> cfg != null ? cfg.getMapColorBlood() : 0xFFFF0000;    // blood
            case 66 -> cfg != null ? cfg.getMapColorPuzzle() : 0xFFCC00CC;   // puzzle
            case 62 -> cfg != null ? cfg.getMapColorTrap() : 0xFFFF6600;     // trap
            case 74 -> cfg != null ? cfg.getMapColorMiniboss() : 0xFFCCCC00; // champion/miniboss
            case 82 -> 0xFFFF55FF;                                           // fairy
            default -> cfg != null ? cfg.getMapColorNormal() : 0xFF999999;   // normal / rare
        };
        return color; // honour the configured alpha (transparency slider)
    }

    private static int doorColor(DoorType type, HorizonConfig cfg) {
        int color = switch (type) {
            case WITHER   -> 0xFF2A2A2A;
            case BLOOD    -> cfg != null ? cfg.getMapColorBlood() : 0xFFFF0000;
            case ENTRANCE -> cfg != null ? cfg.getMapColorEntrance() : 0xFF00CC00;
            case GOLD     -> 0xFFFFD700;
            default       -> cfg != null ? cfg.getMapColorNormal() : 0xFFAAAAAA;
        };
        return color; // honour the configured alpha (transparency slider)
    }

    private static int nameColor(RoomState state, HorizonConfig config) {
        return switch (state) {
            case GREEN   -> config.getMapColorNameSecrets();   // all secrets found (green check)
            case CLEARED -> config.getMapColorNameCleared();   // starred mobs cleared (white check)
            case FAILED  -> 0xFFFF5555;
            default      -> config.getMapColorNameUncleared(); // discovered, not cleared
        };
    }

    /** A door is drawn once at least one of the rooms it connects has been entered. */
    private static boolean doorVisible(DungeonInfo info, int x, int z) {
        if ((x & 1) == 1) return roomEntered(info, x - 1, z) || roomEntered(info, x + 1, z);
        return roomEntered(info, x, z - 1) || roomEntered(info, x, z + 1);
    }

    /** A same-room separator is drawn only once both its cells have been entered. */
    private static boolean bothRoomsEntered(DungeonInfo info, int x, int z) {
        boolean xOdd = (x & 1) == 1;
        boolean zOdd = (z & 1) == 1;
        if (xOdd && !zOdd) return roomEntered(info, x - 1, z) && roomEntered(info, x + 1, z);
        if (!xOdd && zOdd) return roomEntered(info, x, z - 1) && roomEntered(info, x, z + 1);
        // 2x2 center — require all four surrounding rooms entered.
        return roomEntered(info, x - 1, z - 1) && roomEntered(info, x + 1, z - 1)
            && roomEntered(info, x - 1, z + 1) && roomEntered(info, x + 1, z + 1);
    }

    private static boolean roomEntered(DungeonInfo info, int x, int z) {
        if (x < 0 || x > 10 || z < 0 || z > 10) return false;
        return info.get(x, z) instanceof DungeonRoom room && isRoomEntered(room.state());
    }

    private static boolean isRoomEntered(RoomState state) {
        return state == RoomState.DISCOVERED || state == RoomState.CLEARED
            || state == RoomState.GREEN || state == RoomState.FAILED;
    }
}
