package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom overlay for the Spirit Leap / Teleport to Player screen.
 * 4 quadrants centered on screen (200x75px each).
 * Player detection from tablist (more reliable than item scanning).
 * Actual leap via slot 11-15 name matching.
 * Class-based default quadrant sorting with priority.
 */
public final class LeapMenuOverlay {
    private static final Pattern LEAP_TITLE = Pattern.compile("spirit\\s+leap|teleport\\s+to\\s+player", Pattern.CASE_INSENSITIVE);
    // Tablist regex: [42] [VIP+] PlayerName ... (Mage XXIV)
    private static final Pattern TABLIST_REGEX = Pattern.compile("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?:\\s+(\\w+))*\\)$");
    private static final String[] CLASS_NAMES = { "Archer", "Berserk", "Healer", "Mage", "Tank" };

    // Class quadrant defaults and priorities
    // Archer=0/top-left, Berserk=1/top-right, Healer=2/bottom-left, Mage=3/bottom-right, Tank=3
    private static final int[] DEFAULT_QUADRANT = { 0, 1, 2, 3, 3 }; // indexed by class ordinal
    private static final int[] CLASS_PRIORITY   = { 2, 0, 2, 2, 1 }; // Berserk highest (0)

    private static final int BOX_WIDTH = 200;
    private static final int BOX_HEIGHT = 75;
    private static final int GAP = 24;

    // Colors (ARGB)
    private static final int BG_COLOR    = 0xBF262626;
    private static final int HOVER_COLOR = 0xBF3A3A3A;
    private static final int DEAD_COLOR  = 0xFFFF5555;

    // Class colors (ARGB)
    private static final int[] CLASS_COLORS = {
        0xFFFFAA00, // Archer - Gold
        0xFFAA0000, // Berserk - Dark Red
        0xFFFF55FF, // Healer - Light Purple
        0xFF55FFFF, // Mage - Aqua
        0xFF00AA00, // Tank - Dark Green
    };

    public record LeapPlayer(String name, int classIndex, boolean dead, PlayerSkin skin) {}

    private List<LeapPlayer> sortedPlayers = List.of();
    private long lastUpdate = 0L;

    public boolean isLeapScreen(AbstractContainerScreen<?> screen) {
        return isLeapScreenTitle(screen);
    }

    public static boolean isLeapScreenTitle(AbstractContainerScreen<?> screen) {
        if (screen == null) return false;
        String title = screen.getTitle().getString();
        return LEAP_TITLE.matcher(title).find();
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx,
                       int mouseX, int mouseY, HorizonConfig config) {
        if (!config.isLeapMenuEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        refreshFromTablist(mc, config);

        int halfW = screen.width / 2;
        int halfH = screen.height / 2;

        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int nearX = col == 0 ? halfW - GAP : halfW + GAP;
            int nearY = row == 0 ? halfH - GAP : halfH + GAP;
            int localX = col == 0 ? -BOX_WIDTH : 0;
            int localY = row == 0 ? -BOX_HEIGHT : 0;

            int boxX = nearX + localX;
            int boxY = nearY + localY;

            // Hover detection based on screen quadrant
            boolean hovered = (col == 0 ? mouseX < halfW : mouseX >= halfW)
                           && (row == 0 ? mouseY < halfH : mouseY >= halfH);

            // Background
            ctx.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, hovered ? HOVER_COLOR : BG_COLOR);

            if (i < sortedPlayers.size()) {
                LeapPlayer player = sortedPlayers.get(i);
                int classColor = player.classIndex >= 0 && player.classIndex < CLASS_COLORS.length
                    ? CLASS_COLORS[player.classIndex] : 0xFFFFFFFF;

                // Player face
                int faceSize = (int)(BOX_HEIGHT * 0.65);
                if (player.skin != null) {
                    net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(
                        ctx, player.skin, boxX + 8, boxY + (BOX_HEIGHT - faceSize) / 2, faceSize);
                }

                // Name
                int textX = boxX + 14 + faceSize;
                int nameY = boxY + (int)(BOX_HEIGHT / 2.5) - mc.font.lineHeight / 2;
                ctx.text(mc.font, player.name, textX, nameY, classColor, true);

                // Class or DEAD
                int classY = boxY + (int)(BOX_HEIGHT / 1.7) - mc.font.lineHeight / 2;
                if (player.dead) {
                    ctx.text(mc.font, "DEAD", textX, classY, DEAD_COLOR, true);
                } else if (player.classIndex >= 0 && player.classIndex < CLASS_NAMES.length) {
                    ctx.text(mc.font, CLASS_NAMES[player.classIndex], textX, classY, 0xFFFFFFFF, true);
                }
            }
        }
    }

    /**
     * Returns the slot index to click for a given mouse position, or -1.
     * Searches slots 11-15 for matching player name.
     */
    public int getClickedSlot(AbstractContainerScreen<?> screen, int mouseX, int mouseY, HorizonConfig config) {
        if (!config.isLeapMenuEnabled()) return -1;

        int halfW = screen.width / 2;
        int halfH = screen.height / 2;
        int quadrant = (mouseY >= halfH ? 2 : 0) + (mouseX >= halfW ? 1 : 0);

        if (quadrant >= sortedPlayers.size()) return -1;
        LeapPlayer player = sortedPlayers.get(quadrant);
        if (player.dead) return -1;

        // Find the slot matching this player's name (slots 11-15)
        var menu = screen.getMenu();
        for (int i = 11; i < Math.min(16, menu.slots.size()); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            String displayName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
            if (displayName == null) continue;
            // Skip the first word (rank prefix or color code artifact)
            int spaceIdx = displayName.indexOf(' ');
            String cleanName = spaceIdx >= 0 ? displayName.substring(spaceIdx + 1).trim() : displayName.trim();
            if (cleanName.equalsIgnoreCase(player.name)) {
                return i;
            }
        }
        // Fallback: also search slots 20-25 (some leap UIs use different layouts)
        for (int i = 20; i < Math.min(26, menu.slots.size()); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            String displayName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
            if (displayName == null) continue;
            int spaceIdx = displayName.indexOf(' ');
            String cleanName = spaceIdx >= 0 ? displayName.substring(spaceIdx + 1).trim() : displayName.trim();
            if (cleanName.equalsIgnoreCase(player.name)) {
                return i;
            }
        }
        return -1;
    }

    public int getQuadrantIndex(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        int halfW = screen.width / 2;
        int halfH = screen.height / 2;
        return (mouseY >= halfH ? 2 : 0) + (mouseX >= halfW ? 1 : 0);
    }

    public String getLeapAnnouncementMessage(int quadrantIndex, HorizonConfig config) {
        if (!config.isLeapMenuAnnounce()) return null;
        if (quadrantIndex < 0 || quadrantIndex >= sortedPlayers.size()) return null;
        return "/pc Leaping to " + sortedPlayers.get(quadrantIndex).name;
    }

    private void refreshFromTablist(Minecraft mc, HorizonConfig config) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 250L) return;
        lastUpdate = now;

        if (mc.player == null || mc.getConnection() == null) return;
        String selfName = mc.player.getName().getString();

        List<LeapPlayer> players = new ArrayList<>();
        Collection<PlayerInfo> playerInfos = mc.getConnection().getListedOnlinePlayers();

        for (PlayerInfo info : playerInfos) {
            if (info.getTabListDisplayName() == null) continue;
            String tabLine = ChatFormatting.stripFormatting(info.getTabListDisplayName().getString());
            if (tabLine == null) continue;
            Matcher m = TABLIST_REGEX.matcher(tabLine.trim());
            if (!m.matches()) continue;

            String name = m.group(2);
            String classStr = m.group(3);

            // Skip self
            if (name.equalsIgnoreCase(selfName)) continue;

            boolean dead = "DEAD".equalsIgnoreCase(classStr);
            int classIndex = -1;
            if (!dead) {
                for (int i = 0; i < CLASS_NAMES.length; i++) {
                    if (CLASS_NAMES[i].equalsIgnoreCase(classStr)) {
                        classIndex = i;
                        break;
                    }
                }
            }

            PlayerSkin skin = info.getSkin();
            players.add(new LeapPlayer(name, classIndex, dead, skin));
        }

        sortedPlayers = sortPlayers(players, config.getLeapMenuSortMode());
    }

    private List<LeapPlayer> sortPlayers(List<LeapPlayer> raw, int mode) {
        return switch (mode) {
            case 1 -> raw.stream()
                .sorted(Comparator.comparingInt((LeapPlayer p) -> p.classIndex < 0 ? 99 : p.classIndex)
                    .thenComparing(LeapPlayer::name))
                .toList();
            case 2 -> raw.stream()
                .sorted(Comparator.comparing(LeapPlayer::name))
                .toList();
            default -> quadrantSorting(raw);
        };
    }

    /**
     * Class-based quadrant sorting: each class has a default quadrant and priority.
     * First pass: place players in their class's default quadrant (sorted by priority).
     * Second pass: fill empty quadrants with overflow.
     */
    private List<LeapPlayer> quadrantSorting(List<LeapPlayer> players) {
        // Sort by class priority (Berserk=0, Tank=1, others=2)
        List<LeapPlayer> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparingInt(p -> {
            if (p.classIndex >= 0 && p.classIndex < CLASS_PRIORITY.length)
                return CLASS_PRIORITY[p.classIndex];
            return 99;
        }));

        LeapPlayer[] result = new LeapPlayer[4];
        List<LeapPlayer> overflow = new ArrayList<>();

        for (LeapPlayer p : sorted) {
            if (p.classIndex >= 0 && p.classIndex < DEFAULT_QUADRANT.length) {
                int quadrant = DEFAULT_QUADRANT[p.classIndex];
                if (result[quadrant] == null) {
                    result[quadrant] = p;
                    continue;
                }
            }
            overflow.add(p);
        }

        List<LeapPlayer> finalList = new ArrayList<>();
        int overflowIdx = 0;
        for (int i = 0; i < 4; i++) {
            if (result[i] != null) {
                finalList.add(result[i]);
            } else if (overflowIdx < overflow.size()) {
                finalList.add(overflow.get(overflowIdx++));
            }
        }
        return finalList;
    }

    public void reset() {
        sortedPlayers = List.of();
        lastUpdate = 0L;
    }
}
