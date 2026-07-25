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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DungeonStateService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");
    private static final Pattern FLOOR_ROMAN = Pattern.compile("floor\\s+(i{1,3}v?|vi{0,3}|iv|ix)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_MASTER = Pattern.compile("master\\s+mode.*?m([1-7])", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_MASTER_ALT = Pattern.compile("m([1-7])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_F_NUMBER = Pattern.compile("\\bf([1-7])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_DIGIT = Pattern.compile("floor\\s*:?\\s*([0-7])", Pattern.CASE_INSENSITIVE);

    public enum F7Phase { NONE, P1, P2, P3, P4, P5 }

    private boolean inDungeon;
    private boolean inBoss;
    // True only once the actual floor-boss fight begins (a "[BOSS] <Name>:" chat line that
    // is NOT the Watcher). The blood room is NOT the boss, so this stays false through
    // blood — the score HUD uses it so the calc only disappears when the boss starts.
    private boolean bossFightStarted;
    private int currentFloor = 0;
    private boolean isMasterMode = false;
    // Only true once we positively identify the Entrance floor. Prevents the
    // score's 0.7 entrance factor from wrongly applying when the floor is simply
    // not detected yet (default 0). See DungeonScoreService.isEntrance.
    private boolean entranceConfirmed = false;
    private F7Phase f7Phase = F7Phase.NONE;
    private int ticksSinceDungeonSeen = Integer.MAX_VALUE;
    private int ticksSinceBossSeen = Integer.MAX_VALUE;

    public void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            reset();
            return;
        }

        String scoreboardText = sidebarText(client);
        String normalized = normalize(scoreboardText);
        detectFloorFromScoreboard(normalized);

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
            || normalized.contains("blessing of")
            || bossDetected
            || inBoss;

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
        String plain = clean(rawMessage);
        String normalized = plain.toLowerCase(Locale.ROOT);
        if ((normalized.contains("[boss]") || normalized.contains("boss room"))
            && !normalized.contains("the watcher")) {
            inDungeon = true;
            inBoss = true;
            bossFightStarted = true; // a real floor boss is speaking (not the blood Watcher)
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
        // F7/M7 phase detection via boss chat lines
        if (currentFloor == 7 && inBoss) {
            handleF7PhaseChatLine(plain, normalized);
        }
    }

    private void handleF7PhaseChatLine(String plain, String normalized) {
        // P1 – Maxor
        if (normalized.contains("[boss] maxor:") || plain.contains("[BOSS] Maxor:")) {
            if (f7Phase == F7Phase.NONE) f7Phase = F7Phase.P1;
        }
        // P2 – Storm
        if (normalized.contains("[boss] storm:") || plain.contains("[BOSS] Storm:")) {
            if (f7Phase == F7Phase.NONE || f7Phase == F7Phase.P1) f7Phase = F7Phase.P2;
        }
        // P3 – Goldor starts when he says his greeting
        if (normalized.contains("goldor: who dares trespass")) {
            f7Phase = F7Phase.P3;
        }
        // P3 ends when the core entrance opens
        if (normalized.contains("the core entrance is opening")) {
            if (f7Phase == F7Phase.P3) f7Phase = F7Phase.P4;
        }
        // P4 – Necron
        if (normalized.contains("[boss] necron:") || plain.contains("[BOSS] Necron:")) {
            if (f7Phase != F7Phase.P5) f7Phase = F7Phase.P4;
        }
        // P5 – Giant / final phase
        if (normalized.contains("the wither king is respawning") || normalized.contains("[boss] necron: you were right")) {
            f7Phase = F7Phase.P5;
        }
    }

    public void handleLocationPacket(String rawPayload) {
        String normalized = normalize(rawPayload);
        // "dungeon_hub" is NOT a dungeon — only actual catacombs instances count
        if ((normalized.contains("catacombs") || normalized.contains("\"dungeon\""))
            && !normalized.contains("dungeon_hub")) {
            inDungeon = true;
            ticksSinceDungeonSeen = 0;
        }
        if (normalized.contains("\"boss\"") || normalized.contains("boss_room")) {
            inBoss = true;
            ticksSinceBossSeen = 0;
        }
    }

    /** Called on world change (disconnect/reconnect) to fully reset dungeon state. */
    public void onWorldChange() {
        reset();
    }

    public boolean isInDungeon() {
        return inDungeon;
    }

    public boolean isInBoss() {
        return inBoss;
    }

    /** True once the actual floor-boss fight has started (not the blood room). */
    public boolean isBossFightStarted() {
        return bossFightStarted;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public boolean isMasterMode() {
        return isMasterMode;
    }

    /** True only when the Entrance floor was positively detected (not an undetected default). */
    public boolean isEntranceFloor() {
        return entranceConfirmed;
    }

    public F7Phase getF7Phase() {
        return f7Phase;
    }

    public boolean isF7() {
        return currentFloor == 7;
    }

    public boolean isInStormPhase() {
        return isF7() && f7Phase == F7Phase.P2;
    }

    public boolean isInGoldorPhase() {
        return isF7() && f7Phase == F7Phase.P3;
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

    private String clean(String raw) {
        return FORMATTING_CODES.matcher(raw == null ? "" : raw).replaceAll("").strip();
    }

    private String normalize(String value) {
        return FORMATTING_CODES.matcher(value == null ? "" : value)
            .replaceAll("")
            .strip()
            .toLowerCase(Locale.ROOT);
    }

    private void detectFloorFromScoreboard(String normalized) {
        if (!normalized.contains("catacombs")) return;
        // Entrance floor: positively identified so the score's entrance factor
        // only applies here, never on an undetected floor.
        if (normalized.contains("entrance") || normalized.contains("(f0)") || normalized.contains("floor 0")) {
            isMasterMode = false;
            currentFloor = 0;
            entranceConfirmed = true;
            return;
        }
        // Master mode: "m7" etc. (only when "master" is present)
        Matcher masterAlt = FLOOR_MASTER_ALT.matcher(normalized);
        if (normalized.contains("master") && masterAlt.find()) {
            isMasterMode = true;
            currentFloor = Integer.parseInt(masterAlt.group(1));
            entranceConfirmed = false;
            return;
        }
        // Normal floor: "floor vii", "floor vi", etc.
        Matcher roman = FLOOR_ROMAN.matcher(normalized);
        if (roman.find()) {
            isMasterMode = normalized.contains("master");
            currentFloor = romanToInt(roman.group(1));
            entranceConfirmed = false;
            return;
        }
        // "(f7)", "f7" — sidebar location format
        Matcher fNum = FLOOR_F_NUMBER.matcher(normalized);
        if (fNum.find()) {
            isMasterMode = false;
            currentFloor = Integer.parseInt(fNum.group(1));
            entranceConfirmed = false;
            return;
        }
        // Digit format: "floor: 7", "floor 7"
        Matcher digit = FLOOR_DIGIT.matcher(normalized);
        if (digit.find()) {
            int f = Integer.parseInt(digit.group(1));
            currentFloor = f;
            entranceConfirmed = f == 0;
            return;
        }
        // Master mode fallback without "master" keyword: "m7" in sidebar
        masterAlt.reset();
        if (masterAlt.find()) {
            isMasterMode = true;
            currentFloor = Integer.parseInt(masterAlt.group(1));
            entranceConfirmed = false;
        }
    }

    private static int romanToInt(String roman) {
        return switch (roman.toUpperCase(Locale.ROOT)) {
            case "I"   -> 1;
            case "II"  -> 2;
            case "III" -> 3;
            case "IV"  -> 4;
            case "V"   -> 5;
            case "VI"  -> 6;
            case "VII" -> 7;
            default    -> 0;
        };
    }

    private void reset() {
        inDungeon = false;
        inBoss = false;
        bossFightStarted = false;
        currentFloor = 0;
        isMasterMode = false;
        entranceConfirmed = false;
        f7Phase = F7Phase.NONE;
        ticksSinceDungeonSeen = Integer.MAX_VALUE;
        ticksSinceBossSeen = Integer.MAX_VALUE;
    }
}
