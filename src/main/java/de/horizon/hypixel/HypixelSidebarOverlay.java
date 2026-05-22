package de.horizon.hypixel;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.hud.HudStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HypixelSidebarOverlay {
    public static final int BAR_HEIGHT = 18;
    public static final int HOTBAR_OFFSET = 24;
    private static final long SNAPSHOT_CACHE_MILLIS = 1500L;

    private static SidebarSnapshot cachedSnapshot;
    private static long cachedSnapshotAt;

    public void render(DrawContext context, MinecraftClient client) {
        SidebarSnapshot snapshot = snapshot(client);
        if (snapshot == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int top = height - BAR_HEIGHT;
        context.fill(0, top, width, height, HudStyle.panel());
        context.fill(0, top, width, top + 1, HudStyle.accent());

        List<String> segments = buildSegments(snapshot);
        if (segments.isEmpty()) {
            return;
        }

        int x = 6;
        int baseline = top + 5;
        for (int index = 0; index < segments.size(); index++) {
            String segment = segments.get(index);
            String suffix = index >= segments.size() - 1 ? "" : "  |  ";
            int remainingWidth = width - x - 6;
            if (remainingWidth <= 0) {
                break;
            }

            String content = trimToWidth(textRenderer, segment, remainingWidth);
            int contentWidth = textRenderer.getWidth(content);
            if (!suffix.isEmpty() && contentWidth + textRenderer.getWidth(suffix) > remainingWidth) {
                suffix = "";
            }

            context.drawTextWithShadow(textRenderer, content, x, baseline, index == 0 ? HudStyle.accent() : HudStyle.text());
            x += contentWidth;
            if (!suffix.isEmpty()) {
                context.drawTextWithShadow(textRenderer, suffix, x, baseline, HudStyle.muted());
                x += textRenderer.getWidth(suffix);
            }
        }
    }

    /**
     * Returns the deduplicated lines of the current scoreboard snapshot keyed by
     * their {@link de.horizon.config.HorizonConfig#scoreboardLineKey(String)} value.
     * Only one entry per key is kept (first wins), so lines that change their
     * numeric values (kill counts, timers, plot numbers, etc.) always produce a
     * single stable entry.
     */
    public static Map<String, String> liveDeduplicatedLines(MinecraftClient client) {
        SidebarSnapshot snap = snapshot(client);
        if (snap == null) {
            snap = cachedSnapshot;
        }
        if (snap == null) {
            return new LinkedHashMap<>();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : snap.lines()) {
            String key = de.horizon.config.HorizonConfig.scoreboardLineKey(line);
            if (!key.isBlank()) {
                result.putIfAbsent(key, line);
            }
        }
        return result;
    }

    /** Returns the island detected from the current (or cached) snapshot. */
    public static SkyBlockIsland liveIsland(MinecraftClient client) {
        SidebarSnapshot snap = snapshot(client);
        if (snap == null) {
            snap = cachedSnapshot;
        }
        if (snap == null) {
            return SkyBlockIsland.UNKNOWN;
        }
        return SkyBlockIsland.detect(snap.title(), snap.lines());
    }

    public static boolean shouldReplaceVanillaSidebar(MinecraftClient client) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon != null && !horizon.getConfigManager().getConfig().isCustomScoreboardEnabled()) {
            return false;
        }
        return snapshot(client) != null;
    }

    public static int lowerHudOffset(MinecraftClient client) {
        return shouldReplaceVanillaSidebar(client) ? HOTBAR_OFFSET : 0;
    }

    private static SidebarSnapshot snapshot(MinecraftClient client) {
        if (!isHypixelSkyBlock(client) || client.world == null) {
            clearCache();
            return null;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return cachedSnapshot(client);
        }

        String title = clean(objective.getDisplayName());
        List<String> lines = new ArrayList<>();
        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        entries.stream()
            .filter(entry -> !entry.hidden())
            .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed())
            .forEach(entry -> {
                String line = lineText(scoreboard, entry);
                if (!line.isBlank()) {
                    lines.add(line);
                }
            });
        if (title.isBlank() && lines.isEmpty()) {
            return cachedSnapshot(client);
        }
        SidebarSnapshot snapshot = new SidebarSnapshot(title, lines);
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon != null) {
            SkyBlockIsland island = SkyBlockIsland.detect(title, lines);
            if (island != SkyBlockIsland.UNKNOWN) {
                horizon.getConfigManager().getConfig().recordScoreboardLines(island.id(), lines);
            }
        }
        cachedSnapshot = snapshot;
        cachedSnapshotAt = now(client);
        return snapshot;
    }

    private static boolean isHypixelSkyBlock(MinecraftClient client) {
        if (client == null || client.player == null) {
            return false;
        }

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            String address = normalize(serverInfo.address);
            if (address.contains("hypixel.net")) {
                return true;
            }
        }

        if (client.world == null) {
            return false;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return false;
        }

        String title = normalize(clean(objective.getDisplayName()));
        if (title.contains("skyblock")) {
            return true;
        }

        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            if (entry.hidden()) {
                continue;
            }
            String normalized = normalize(lineText(scoreboard, entry));
            if (normalized.contains("purse:")
                || normalized.contains("bits:")
                || normalized.contains("the catacombs")
                || normalized.contains("objective")
                || normalized.contains("commissions")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> buildSegments(SidebarSnapshot snapshot) {
        SkyBlockIsland island = SkyBlockIsland.detect(snapshot.title(), snapshot.lines());
        SidebarSnapshot filtered = filterHiddenLines(snapshot, island);
        if (isDungeonSnapshot(snapshot)) {
            return buildDungeonSegments(filtered);
        }

        List<String> segments = new ArrayList<>();
        if (!filtered.title().isBlank()) {
            segments.add(filtered.title());
        }

        List<String> prioritized = new ArrayList<>(filtered.lines().stream()
            .filter(HypixelSidebarOverlay::shouldKeepLine)
            .sorted(Comparator.comparingInt(HypixelSidebarOverlay::priority).reversed())
            .toList());
        if (prioritized.isEmpty()) {
            prioritized.addAll(filtered.lines().stream().filter(HypixelSidebarOverlay::isUsefulFallbackLine).limit(4).toList());
        }

        for (String line : prioritized) {
            if (segments.size() >= 5) {
                break;
            }
            if (!containsNormalized(segments, line)) {
                segments.add(line);
            }
        }
        return segments;
    }

    private static List<String> buildDungeonSegments(SidebarSnapshot snapshot) {
        List<String> segments = new ArrayList<>();
        addPreferredSegment(segments, snapshot.title());
        addMatchingSegment(segments, snapshot.lines(), "the catacombs", "catacombs");
        addMatchingSegment(segments, snapshot.lines(), "time elapsed", "elapsed", "time:");
        addMatchingSegment(segments, snapshot.lines(), "score:");
        addMatchingSegment(segments, snapshot.lines(), "cleared:");
        addMatchingSegment(segments, snapshot.lines(), "secrets found");
        addMatchingSegment(segments, snapshot.lines(), "crypts:");
        addMatchingSegment(segments, snapshot.lines(), "deaths:");

        for (String line : snapshot.lines()) {
            if (segments.size() >= 6) {
                break;
            }
            if (shouldKeepLine(line) && !containsNormalized(segments, line)) {
                segments.add(line);
            }
        }
        return segments;
    }

    private static SidebarSnapshot filterHiddenLines(SidebarSnapshot snapshot, SkyBlockIsland island) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) {
            return snapshot;
        }
        HorizonConfig config = horizon.getConfigManager().getConfig();
        List<String> visible = new ArrayList<>();
        for (String line : snapshot.lines()) {
            String key = HorizonConfig.scoreboardLineKey(line);
            if (config.isScoreboardGlobalLineHidden(key)) {
                continue;
            }
            if (island != SkyBlockIsland.UNKNOWN && config.isScoreboardLineHidden(island.id(), key)) {
                continue;
            }
            visible.add(line);
        }
        return new SidebarSnapshot(snapshot.title(), visible);
    }

    private static boolean shouldKeepLine(String line) {
        String normalized = normalize(line);
        return priority(line) > 0 && isUsefulFallbackLine(line) && !normalized.equals("www.hypixel.net");
    }

    private static boolean isUsefulFallbackLine(String line) {
        String normalized = normalize(line);
        if (normalized.isBlank() || normalized.equals(":")) {
            return false;
        }
        return !normalized.equals("www.hypixel.net")
            && !normalized.equals("skyblock")
            && !normalized.startsWith("www.");
    }

    private static int priority(String line) {
        String normalized = normalize(line);
        if (normalized.contains("purse:") || normalized.contains("piggy:") || normalized.contains("bits:") || normalized.contains("copper:")) {
            return 120;
        }
        if (normalized.contains("score:") || normalized.contains("time elapsed") || normalized.contains("elapsed")) {
            return 116;
        }
        if (normalized.contains("the catacombs") || normalized.contains("secrets found") || normalized.contains("crypts:") || normalized.contains("deaths:") || normalized.contains("cleared:")) {
            return 110;
        }
        if (normalized.contains("objective") || normalized.contains("commissions") || normalized.contains("slayer quest") || normalized.contains("next tier")) {
            return 100;
        }
        if (line.contains("⏣") || normalized.contains("profile:") || normalized.contains("skills:") || normalized.contains("date:") || normalized.contains("time:")) {
            return 90;
        }
        if (normalized.matches(".*\\d.*")) {
            return 50;
        }
        return 0;
    }

    private static boolean containsNormalized(List<String> values, String candidate) {
        String normalizedCandidate = normalize(candidate);
        for (String value : values) {
            if (normalize(value).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private static String lineText(Scoreboard scoreboard, ScoreboardEntry entry) {
        StringBuilder builder = new StringBuilder();
        Team team = scoreboard.getScoreHolderTeam(entry.owner());
        if (team != null) {
            builder.append(clean(team.getPrefix()));
        }
        if (entry.display() != null) {
            builder.append(clean(entry.display()));
        } else {
            builder.append(clean(entry.name()));
        }
        if (team != null) {
            builder.append(clean(team.getSuffix()));
        }
        String value = builder.toString().replace('\u00A0', ' ').trim();
        return value.replaceAll("\\s{2,}", " ");
    }

    private static String trimToWidth(TextRenderer textRenderer, String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }
        if (maxWidth <= textRenderer.getWidth("...")) {
            return "";
        }

        String ellipsis = "...";
        String current = value;
        while (!current.isEmpty() && textRenderer.getWidth(current + ellipsis) > maxWidth) {
            current = current.substring(0, current.length() - 1);
        }
        return current.isEmpty() ? "" : current + ellipsis;
    }

    private static String clean(Text text) {
        return text == null ? "" : text.getString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean isDungeonSnapshot(SidebarSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (normalize(snapshot.title()).contains("catacombs")) {
            return true;
        }
        for (String line : snapshot.lines()) {
            String normalized = normalize(line);
            if (normalized.contains("the catacombs")
                || normalized.contains("time elapsed")
                || normalized.contains("score:")
                || normalized.contains("secrets found")) {
                return true;
            }
        }
        return false;
    }

    private static void addMatchingSegment(List<String> segments, List<String> lines, String... needles) {
        for (String line : lines) {
            String normalized = normalize(line);
            for (String needle : needles) {
                if (normalized.contains(needle) && !containsNormalized(segments, line)) {
                    segments.add(line);
                    return;
                }
            }
        }
    }

    private static void addPreferredSegment(List<String> segments, String value) {
        if (value != null && !value.isBlank() && !containsNormalized(segments, value)) {
            segments.add(value);
        }
    }

    private static SidebarSnapshot cachedSnapshot(MinecraftClient client) {
        if (cachedSnapshot == null) {
            return null;
        }
        if (now(client) - cachedSnapshotAt > SNAPSHOT_CACHE_MILLIS) {
            clearCache();
            return null;
        }
        return cachedSnapshot;
    }

    private static long now(MinecraftClient client) {
        return System.currentTimeMillis();
    }

    private static void clearCache() {
        cachedSnapshot = null;
        cachedSnapshotAt = 0L;
    }

    private record SidebarSnapshot(String title, List<String> lines) {
    }
}
