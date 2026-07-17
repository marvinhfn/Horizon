package de.horizon.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dungeon Score estimator based on the community scoring formula.
 * Parses tab list for stats and scoreboard sidebar for cleared%/time.
 *
 * Score = Skill (max 100) + Exploration (max 100) + Speed (max 100) + Bonus (max 5..18)
 * S  = 270+
 * S+ = 300+
 */
public final class DungeonScoreService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    // Scoreboard sidebar patterns
    private static final Pattern CLEARED_PATTERN = Pattern.compile("Cleared: (\\d+)%");
    private static final Pattern TIME_PATTERN = Pattern.compile("Time Elapsed: (?:(\\d+)h)? ?(?:(\\d+)m)? ?(\\d+)s");

    // Tab list patterns
    private static final Pattern TAB_SECRETS_PCT = Pattern.compile("Secrets Found: ([\\d.]+)%");
    private static final Pattern TAB_SECRETS_COUNT = Pattern.compile("Secrets Found: (\\d+)$");
    private static final Pattern TAB_COMPLETED_ROOMS = Pattern.compile("Completed Rooms: (\\d+)");
    private static final Pattern TAB_DEATHS = Pattern.compile("Team Deaths: (\\d+)");
    private static final Pattern TAB_CRYPTS = Pattern.compile("Crypts: (\\d+)");
    private static final Pattern TAB_PUZZLES_COUNT = Pattern.compile("Puzzles: \\((\\d+)\\)");
    private static final Pattern TAB_PUZZLE_STATUS = Pattern.compile("(.+): \\[(\u2726|\u2714|\u2716)\\]");

    // Required secret percentages per floor index [E=0, F1=1, ..., F7=7]
    private static final double[] REQUIRED_PERCENT = { 0.3, 0.3, 0.4, 0.5, 0.6, 0.7, 0.85, 1.0 };
    // Required speed (seconds) for 100 points
    private static final int[] SPEED_NORMAL = { 1200, 600, 600, 600, 720, 600, 720, 840 };
    private static final int[] SPEED_MASTER = { 480, 480, 480, 480, 480, 480, 480, 900 };

    // Parsed state
    private int clearedPercent = 0;
    private int secretsFound = 0;
    private double secretsFoundPercent = 0.0;
    private int completedRooms = 0;
    private int deaths = 0;
    private int crypts = 0;
    private int totalPuzzles = 0;
    private int completedPuzzles = 0;
    private int elapsedSeconds = 0;
    private boolean mimicKilled = false;
    private boolean bloodCleared = false;
    private boolean active = false;

    // Total rooms histogram
    private final Map<Integer, Integer> totalRoomsHisto = new HashMap<>();

    public void tick(Minecraft mc, DungeonStateService state) {
        if (mc == null || mc.level == null || !state.isInDungeon()) {
            if (active && !state.isInDungeon()) reset();
            return;
        }
        if (!active) active = true;

        parseScoreboard(mc);
        parseTabList(mc);
    }

    public void handleChatMessage(String rawMessage) {
        if (rawMessage == null) return;
        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip();
        String lower = plain.toLowerCase(Locale.ROOT);
        // Mimic kill via party chat (community convention)
        if (lower.contains("mimic killed!") || plain.contains("$SKYTILS-DUNGEON-SCORE-MIMIC$")) {
            mimicKilled = true;
        }
        // Blood cleared
        if (lower.contains("[boss] the watcher:") && lower.contains("you have proven yourself")) {
            bloodCleared = true;
        }
    }

    private void parseScoreboard(Minecraft mc) {
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append(clean(objective.getDisplayName())).append('\n');
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;
            sb.append(clean(entry.ownerName()));
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            if (team != null) {
                sb.append(' ').append(clean(team.getPlayerPrefix()));
                sb.append(' ').append(clean(team.getPlayerSuffix()));
            }
            if (entry.display() != null) {
                sb.append(' ').append(clean(entry.display()));
            }
            sb.append('\n');
        }
        String text = sb.toString();

        Matcher m = CLEARED_PATTERN.matcher(text);
        if (m.find()) clearedPercent = Integer.parseInt(m.group(1));

        m = TIME_PATTERN.matcher(text);
        if (m.find()) {
            int h = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int min = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            int sec = Integer.parseInt(m.group(3));
            elapsedSeconds = h * 3600 + min * 60 + sec;
        }
    }

    private void parseTabList(Minecraft mc) {
        if (mc.player == null || mc.player.connection == null) return;

        int puzzlesDone = 0;
        boolean foundPuzzles = false;

        for (PlayerInfo info : mc.player.connection.getListedOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;
            String text = FORMATTING.matcher(display.getString()).replaceAll("").strip();
            if (text.isEmpty()) continue;

            Matcher m;

            m = TAB_SECRETS_PCT.matcher(text);
            if (m.find()) {
                secretsFoundPercent = Double.parseDouble(m.group(1));
                continue;
            }

            m = TAB_SECRETS_COUNT.matcher(text);
            if (m.find()) {
                secretsFound = Integer.parseInt(m.group(1));
                continue;
            }

            m = TAB_COMPLETED_ROOMS.matcher(text);
            if (m.find()) {
                completedRooms = Integer.parseInt(m.group(1));
                continue;
            }

            m = TAB_DEATHS.matcher(text);
            if (m.find()) {
                deaths = Integer.parseInt(m.group(1));
                continue;
            }

            m = TAB_CRYPTS.matcher(text);
            if (m.find()) {
                crypts = Integer.parseInt(m.group(1));
                continue;
            }

            m = TAB_PUZZLES_COUNT.matcher(text);
            if (m.find()) {
                totalPuzzles = Integer.parseInt(m.group(1));
                foundPuzzles = true;
                continue;
            }

            m = TAB_PUZZLE_STATUS.matcher(text);
            if (m.find()) {
                if ("\u2714".equals(m.group(2))) { // checkmark
                    puzzlesDone++;
                }
            }
        }

        if (foundPuzzles || puzzlesDone > 0) {
            completedPuzzles = puzzlesDone;
        }
    }

    // --- Derived values ---

    private int getTotalSecrets() {
        if (secretsFound == 0 || secretsFoundPercent == 0.0) return 0;
        return (int) (100.0 / secretsFoundPercent * secretsFound + 0.5);
    }

    /** Total rooms estimated via histogram mode of 100*completedRooms/clearedPercent */
    private int getTotalRooms() {
        if (clearedPercent == 0 || completedRooms == 0) return 0;
        int guess = (int) (100.0 * completedRooms / clearedPercent + 0.5);
        totalRoomsHisto.merge(guess, 1, Integer::sum);
        return totalRoomsHisto.entrySet().stream()
            .max(Comparator.comparingInt(e -> e.getValue() * 1000 + e.getKey()))
            .map(Map.Entry::getKey)
            .orElse(0);
    }

    private double getRequiredPercent(int floor, boolean master) {
        if (master) return 1.0;
        if (floor < 0 || floor > 7) return 1.0;
        return REQUIRED_PERCENT[floor];
    }

    private int getRequiredSpeed(int floor, boolean master) {
        if (floor < 0 || floor > 7) return 600;
        return master ? SPEED_MASTER[floor] : SPEED_NORMAL[floor];
    }

    private boolean isEntrance(int floor, boolean master) {
        return floor == 0 && !master;
    }

    private int applyEntrance(double score, int floor, boolean master) {
        return (int) (score * (isEntrance(floor, master) ? 0.7 : 1.0));
    }

    // --- Score components ---

    public int getSkillScore(int floor, boolean master, boolean inBoss) {
        int totalRooms = getTotalRooms();
        int actualCompleted = completedRooms + (inBoss ? 0 : 1);
        double actualClearPercent = totalRooms > 0 ? Math.min((double) actualCompleted / totalRooms, 1.0) : 0.0;

        int deathPenalty = deaths * 2;
        int puzzlePenalty = 10 * Math.max(0, totalPuzzles - completedPuzzles);
        int totalPenalty = deathPenalty + puzzlePenalty;

        double skill = Math.max(20.0 + actualClearPercent * 80.0 - totalPenalty, 20.0);
        return applyEntrance(skill, floor, master);
    }

    public int getExplorationScore(int floor, boolean master, boolean inBoss) {
        // Secret score (max 40)
        int totalSecrets = getTotalSecrets();
        double reqPct = getRequiredPercent(floor, master);
        int totalSecretsRequired = totalSecrets > 0 ? (int) Math.ceil(reqPct * totalSecrets) : 0;
        double actualSecretPercent = totalSecretsRequired > 0
            ? Math.min((double) secretsFound / totalSecretsRequired, 1.0) : 0.0;
        double secretScore = actualSecretPercent * 40.0;

        // Room clear score (max 60)
        int totalRooms = getTotalRooms();
        int actualCompleted = completedRooms + (inBoss ? 0 : 1);
        double actualClearPercent = totalRooms > 0 ? Math.min((double) actualCompleted / totalRooms, 1.0) : 0.0;
        double roomClearScore = actualClearPercent * 60.0;

        return applyEntrance(secretScore, floor, master) + applyEntrance(roomClearScore, floor, master);
    }

    public int getSpeedScore(int floor, boolean master) {
        int requiredSpeed = getRequiredSpeed(floor, master);
        int overtime = elapsedSeconds - requiredSpeed;

        double speed;
        if (overtime < 12) speed = 100.0;
        else if (overtime < 120) speed = 100.0 - overtime / 12.0;
        else if (overtime < 360) speed = 91.0 - overtime / 24.0;
        else if (overtime < 660) speed = 92.0 - overtime / 30.0;
        else if (overtime < 3090) speed = 86.5 - overtime / 40.0;
        else speed = 0.0;

        return applyEntrance(Math.max(0, speed), floor, master);
    }

    public int getBonusScore(int floor, boolean master) {
        double bonus = Math.min(crypts, 5) + (mimicKilled ? 2 : 0);
        return applyEntrance(bonus, floor, master);
    }

    public int getTotalScore(int floor, boolean master, boolean inBoss) {
        return getSkillScore(floor, master, inBoss)
            + getExplorationScore(floor, master, inBoss)
            + getSpeedScore(floor, master)
            + getBonusScore(floor, master);
    }

    public String getGrade(int floor, boolean master, boolean inBoss) {
        int total = getTotalScore(floor, master, inBoss);
        if (total >= 300) return "S+";
        if (total >= 270) return "S";
        if (total >= 230) return "A";
        if (total >= 160) return "B";
        return "C";
    }

    // --- Getters ---

    public boolean isActive() { return active; }
    public int getClearedPercent() { return clearedPercent; }
    public int getSecretsFound() { return secretsFound; }
    public int getDeaths() { return deaths; }
    public int getCrypts() { return crypts; }
    public int getElapsedSeconds() { return elapsedSeconds; }

    private String clean(Component text) {
        return text == null ? "" : FORMATTING.matcher(text.getString()).replaceAll("");
    }

    private String clean(String raw) {
        return FORMATTING.matcher(raw == null ? "" : raw).replaceAll("");
    }

    public void reset() {
        clearedPercent = 0;
        secretsFound = 0;
        secretsFoundPercent = 0.0;
        completedRooms = 0;
        deaths = 0;
        crypts = 0;
        totalPuzzles = 0;
        completedPuzzles = 0;
        elapsedSeconds = 0;
        mimicKilled = false;
        bloodCleared = false;
        active = false;
        totalRoomsHisto.clear();
    }
}
