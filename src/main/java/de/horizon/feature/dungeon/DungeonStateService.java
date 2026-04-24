package de.horizon.feature.dungeon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DungeonStateService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private boolean inDungeon;
    private boolean inBoss;
    private int ticksSinceDungeonSeen = Integer.MAX_VALUE;
    private int ticksSinceBossSeen = Integer.MAX_VALUE;

    public void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
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

    private String sidebarText(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(clean(objective.getDisplayName()));
        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        for (ScoreboardEntry entry : entries) {
            if (!entry.hidden()) {
                builder.append('\n').append(clean(entry.name()));
                Team team = scoreboard.getScoreHolderTeam(entry.owner());
                if (team != null) {
                    builder.append(' ').append(clean(team.getPrefix()));
                    builder.append(' ').append(clean(team.getSuffix()));
                    builder.append(' ').append(clean(team.decorateName(Text.literal(entry.owner()))));
                }
                if (entry.display() != null) {
                    builder.append(' ').append(clean(entry.display()));
                }
                builder.append(' ').append(entry.owner());
            }
        }
        return builder.toString();
    }

    private String clean(Text text) {
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
