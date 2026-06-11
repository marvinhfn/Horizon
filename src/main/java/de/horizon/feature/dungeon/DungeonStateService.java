package de.horizon.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DungeonStateService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private boolean inDungeon;
    private boolean inBoss;
    private int ticksSinceDungeonSeen = Integer.MAX_VALUE;
    private int ticksSinceBossSeen = Integer.MAX_VALUE;

    public void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            reset();
            return;
        }

        String scoreboardText = sidebarText(client);
        String normalized = normalize(scoreboardText);
        boolean dungeonDetected = normalized.contains("the catacombs")
            || normalized.contains("catacombs")
            || normalized.contains("dungeon cleared")
            || normalized.contains("cleared:")
            || normalized.contains("dungeon starts")
            || normalized.contains("secrets found")
            || normalized.contains("crypts:")
            || normalized.contains("deaths:")
            || normalized.contains("score:")
            || normalized.contains("dungeon buff")
            || normalized.contains("blessing of");

        if (dungeonDetected) {
            inDungeon = true;
            ticksSinceDungeonSeen = 0;
        } else {
            ticksSinceDungeonSeen++;
        }

        if (ticksSinceDungeonSeen > 120) {
            inDungeon = false;
            inBoss = false;
        }

        boolean bossDetected = normalized.contains("boss")
            || normalized.contains("maxor")
            || normalized.contains("storm")
            || normalized.contains("goldor")
            || normalized.contains("necron")
            || normalized.contains("sadan")
            || normalized.contains("thorn")
            || normalized.contains("livid")
            || normalized.contains("professor")
            || normalized.contains("bonzo");

        if (inDungeon && bossDetected) {
            inBoss = true;
            ticksSinceBossSeen = 0;
        } else {
            ticksSinceBossSeen++;
        }

        if (!inDungeon || ticksSinceBossSeen > 3600) {
            inBoss = false;
        }
    }

    public void handleChatMessage(String rawMessage) {
        String normalized = normalize(rawMessage);
        if (normalized.contains("[boss]") || normalized.contains("boss room")) {
            inDungeon = true;
            inBoss = true;
            ticksSinceDungeonSeen = 0;
            ticksSinceBossSeen = 0;
        }
        if (normalized.contains("dungeon starts in") || normalized.contains("dungeon starts")) {
            inDungeon = true;
            inBoss = false;
            ticksSinceDungeonSeen = 0;
            ticksSinceBossSeen = Integer.MAX_VALUE;
        }
        if (normalized.contains("dungeon complete") || normalized.contains("team score:") || normalized.contains("you were kicked while joining that server")) {
            reset();
        }
    }

    public void handleLocationPacket(String rawPayload) {
        String normalized = normalize(rawPayload);
        if (normalized.contains("catacombs") || normalized.contains("\"dungeon\"") || normalized.contains("dungeon_hub")) {
            inDungeon = true;
            ticksSinceDungeonSeen = 0;
        }
        if (normalized.contains("\"boss\"") || normalized.contains("boss_room")) {
            inBoss = true;
            ticksSinceBossSeen = 0;
        }
    }

    public boolean isInDungeon() {
        return inDungeon;
    }

    public boolean isInBoss() {
        return inBoss;
    }

    private String sidebarText(Minecraft client) {
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(clean(objective.getDisplayName()));
        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);
        for (PlayerScoreEntry entry : entries) {
            if (!entry.isHidden()) {
                builder.append('\n').append(clean(entry.ownerName()));
                PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                if (team != null) {
                    builder.append(' ').append(clean(team.getPlayerPrefix()));
                    builder.append(' ').append(clean(team.getPlayerSuffix()));
                    builder.append(' ').append(clean(team.getFormattedName(Component.literal(entry.owner()))));
                }
                if (entry.display() != null) {
                    builder.append(' ').append(clean(entry.display()));
                }
                builder.append(' ').append(entry.owner());
            }
        }
        return builder.toString();
    }

    private String clean(Component text) {
        return text == null ? "" : text.getString();
    }

    private String normalize(String value) {
        return FORMATTING_CODES.matcher(value == null ? "" : value)
            .replaceAll("")
            .strip()
            .toLowerCase(Locale.ROOT);
    }

    private void reset() {
        inDungeon = false;
        inBoss = false;
        ticksSinceDungeonSeen = Integer.MAX_VALUE;
        ticksSinceBossSeen = Integer.MAX_VALUE;
    }
}
