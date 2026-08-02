package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import net.minecraft.ChatFormatting;
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
    private static final Pattern TAB_CRYPTS = Pattern.compile("Crypts: (\\d+)");

    // Deaths come straight from the tab "Team Deaths: N" line (the tab value is
    // authoritative and cumulative).
    private static final Pattern TAB_TEAM_DEATHS = Pattern.compile("Team Deaths: (\\d+)");
    private static final Pattern TAB_PUZZLES_COUNT = Pattern.compile("Puzzles: \\((\\d+)\\)");
    private static final Pattern TAB_PUZZLE_STATUS = Pattern.compile("(.+): \\[(\u2726|\u2714|\u2716)\\]");

    // Required secret percentages per floor index [E=0, F1=1, ..., F7=7]
    private static final double[] REQUIRED_PERCENT = { 0.3, 0.3, 0.4, 0.5, 0.6, 0.7, 0.85, 1.0 };
    // Required speed (seconds) for 100 points, indexed by floor [E=0, F1..F7]
    private static final int[] SPEED_NORMAL = { 600, 600, 600, 600, 720, 600, 720, 840 };
    private static final int[] SPEED_MASTER = { 480, 480, 480, 480, 480, 480, 600, 840 };

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

    // Total-rooms estimate via a histogram of guesses: each time
    // completedRooms or clearedPercent changes we push guess = round(100*rooms/clear)
    // and take the mode (tie-break to the higher room count). Far more stable than an
    // instantaneous divide, which is what made the early score jump around.
    private final Map<Integer, Integer> totalRoomHisto = new LinkedHashMap<>();
    private int totalRoomsCached = 0;
    private int lastHistoCompleted = -1;
    private int lastHistoCleared = -1;

    // Milestone alerts (fired once per run when the total score reaches S / S+).
    private boolean announcedS = false;
    private boolean announcedSPlus = false;

    public void tick(Minecraft mc, DungeonStateService state) {
        if (mc == null || mc.level == null || !state.isInDungeon()) {
            if (active && !state.isInDungeon()) reset();
            return;
        }
        if (!active) active = true;

        parseScoreboard(mc);
        parseTabList(mc);
        updateTotalRooms();
        checkScoreMilestones(mc, state);
    }

    /** Fires a title + sound the first time the run reaches an S (270) or S+ (300) score. */
    private void checkScoreMilestones(Minecraft mc, DungeonStateService state) {
        int floor = state.getCurrentFloor();
        boolean master = state.isMasterMode();
        boolean inBoss = state.isInBoss();
        int total = getTotalScore(floor, master, inBoss);

        if (total >= 300 && !announcedSPlus) {
            announcedSPlus = true;
            announcedS = true;
            logScoreBreakdown(floor, master, inBoss, total);
            announce(mc, "S+", 300);
        } else if (total >= 270 && !announcedS) {
            announcedS = true;
            logScoreBreakdown(floor, master, inBoss, total);
            announce(mc, "S", 270);
        }
    }

    /**
     * Logs the full score breakdown once per milestone. Lets us reconcile the
     * estimate against Hypixel's real end-score and spot which bonus input (mimic,
     * prince, crypts, paul) is off when the totals disagree.
     */
    private void logScoreBreakdown(int floor, boolean master, boolean inBoss, int total) {
        de.horizon.HorizonMod.LOGGER.info(
            "[DungeonScore] total={} skill={} expl={} speed={} bonus={} "
                + "(crypts={} mimic={} prince={} paul={}) "
                + "rooms={}/{} secrets%={} deaths={} puzzles={}/{} time={}s floor={}{}",
            total, getSkillScore(floor, master, inBoss), getExplorationScore(floor, master, inBoss),
            getSpeedScore(floor, master), getBonusScore(floor, master),
            Math.min(crypts, 5), (isMimicKilled() && floor > 5) ? 2 : 0,
            isPrinceKilled() ? 1 : 0, paulScoreBonus() ? 10 : 0,
            actualCompletedRooms(inBoss), totalRoomsCached, secretsFoundPercent,
            deaths, completedPuzzles, totalPuzzles, elapsedSeconds, floor, master ? "(M)" : "");
    }

    private void announce(Minecraft mc, String grade, int score) {
        HorizonConfig config = de.horizon.HorizonClient.getInstance() != null
            ? de.horizon.HorizonClient.getInstance().getConfigManager().getConfig() : null;
        if (config == null || !config.isDungeonScoreEnabled()) return;

        if (config.isDungeonScoreTitle() && mc.gui != null) {
            ChatFormatting color = grade.equals("S+") ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            mc.gui.setTitle(Component.literal(grade + " Score!").withStyle(color, ChatFormatting.BOLD));
            mc.gui.setSubtitle(Component.literal(score + "+ Punkte").withStyle(ChatFormatting.GRAY));
            mc.gui.setTimes(5, 40, 10);
        }
    }

    public void handleChatMessage(String rawMessage) {
        if (rawMessage == null) return;
        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip();
        String lower = plain.toLowerCase(Locale.ROOT);
        // Deaths are read from the tab (Team Deaths).
        // Mimic kill via party chat (community convention)
        if (lower.contains("mimic killed!")) {
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

            m = TAB_CRYPTS.matcher(text);
            if (m.find()) {
                crypts = Integer.parseInt(m.group(1));
                continue;
            }

            m = TAB_TEAM_DEATHS.matcher(text);
            if (m.find()) {
                deaths = Integer.parseInt(m.group(1));
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
    // The score formula uses a derived-state model: a histogram total-rooms
    // estimate, full room projection (blood + boss
    // room always counted, so the live score never dips when they clear), count-based
    // secret score, and the staged speed curve. All components are truncated to int.

    private double getRequiredPercent(int floor, boolean master) {
        if (master) return 1.0;
        if (floor < 0 || floor > 7) return 1.0;
        return REQUIRED_PERCENT[floor];
    }

    private int getRequiredSpeed(int floor, boolean master) {
        if (floor < 0 || floor > 7) return 600;
        return master ? SPEED_MASTER[floor] : SPEED_NORMAL[floor];
    }

    /**
     * Pushes a new total-rooms guess into the histogram whenever completedRooms or
     * clearedPercent changes, then takes the mode (tie-break to the higher room count).
     * guess = round(100 * completedRooms / clearedPercent).
     */
    private void updateTotalRooms() {
        if (completedRooms <= 0 || clearedPercent <= 0) return;
        // Sample once per genuine change of either input (a room clear or a %-tick) —
        // the change-guard keeps polling jitter out.
        if (completedRooms == lastHistoCompleted && clearedPercent == lastHistoCleared) return;
        lastHistoCompleted = completedRooms;
        lastHistoCleared = clearedPercent;
        int guess = (int) (100.0 * completedRooms / clearedPercent + 0.5);
        if (guess <= 0) return;
        totalRoomHisto.merge(guess, 1, Integer::sum);
        int best = 0;
        int bestRank = -1;
        for (Map.Entry<Integer, Integer> e : totalRoomHisto.entrySet()) {
            int rank = e.getValue() * 1000 + e.getKey();
            if (rank > bestRank) {
                bestRank = rank;
                best = e.getKey();
            }
        }
        totalRoomsCached = best;
    }

    /**
     * Completed rooms with the blood room and boss room both projected ahead: each is
     * counted from the start of the run and swaps to a real completed room once cleared
     * / entered. Because +1 projection and +1 real room cancel exactly at the moment of
     * transition, the live score stays smooth (no dip).
     */
    private int actualCompletedRooms(boolean inBoss) {
        return completedRooms + (inBoss ? 0 : 1) + (bloodCleared ? 0 : 1);
    }

    /** min(actualCompletedRooms / totalRooms, 1.0), or 0 before any room data exists. */
    private double actualClearPercent(boolean inBoss) {
        if (totalRoomsCached <= 0) return 0.0;
        return Math.min((double) actualCompletedRooms(inBoss) / totalRoomsCached, 1.0);
    }

    /** Entrance floor scores everything at 0.7 (no master entrance exists). */
    private double entranceFactor(int floor, boolean master) {
        return (!master && floor == 0) ? 0.7 : 1.0;
    }

    /** Raw secret score (0..40): estimate total secrets, require the floor %, ratio × 40. */
    private double secretScoreRaw(int floor, boolean master) {
        if (secretsFound == 0 || secretsFoundPercent == 0.0) return 0.0;
        int totalSecrets = (int) (100.0 / secretsFoundPercent * secretsFound + 0.5);
        int required = (int) Math.ceil(getRequiredPercent(floor, master) * totalSecrets);
        if (required == 0) return 0.0;
        return Math.min((double) secretsFound / required, 1.0) * 40.0;
    }

    private int deathPenalty() {
        return deaths == 0 ? 0 : 2 * deaths - 1;
    }

    private int puzzlePenalty() {
        return 10 * Math.max(0, totalPuzzles - completedPuzzles);
    }

    // --- Score components ---

    public int getSkillScore(int floor, boolean master, boolean inBoss) {
        double penalty = deathPenalty() + puzzlePenalty();
        double skill = Math.max(20.0 + actualClearPercent(inBoss) * 80.0 - penalty, 20.0);
        return (int) (skill * entranceFactor(floor, master));
    }

    public int getExplorationScore(int floor, boolean master, boolean inBoss) {
        double ef = entranceFactor(floor, master);
        int secretScore = (int) (secretScoreRaw(floor, master) * ef);
        int roomScore = (int) (actualClearPercent(inBoss) * 60.0 * ef);
        return secretScore + roomScore;
    }

    public int getSpeedScore(int floor, boolean master) {
        int overtime = elapsedSeconds - getRequiredSpeed(floor, master);
        double s;
        if (overtime < 12) s = 100.0;
        else if (overtime < 120) s = 100.0 - overtime / 12.0;
        else if (overtime < 360) s = 91.0 - overtime / 24.0;
        else if (overtime < 660) s = 92.0 - overtime / 30.0;
        else if (overtime < 3090) s = 86.5 - overtime / 40.0;
        else s = 0.0;
        return (int) (s * entranceFactor(floor, master));
    }

    public int getBonusScore(int floor, boolean master) {
        int bonus = Math.min(crypts, 5);
        if (isMimicKilled() && floor > 5) bonus += 2; // mimics only exist on F6/F7 (+ master)
        if (isPrinceKilled()) bonus += 1;
        if (paulScoreBonus()) bonus += 10; // Mayor Paul EZPZ perk (auto-detected)
        return (int) (bonus * entranceFactor(floor, master));
    }

    /** Mimic kill from either the party-chat convention or the entity-death detector. */
    private boolean isMimicKilled() {
        if (mimicKilled) return true;
        de.horizon.HorizonClient client = de.horizon.HorizonClient.getInstance();
        return client != null && client.getMimicService() != null
            && client.getMimicService().isMimicKilled();
    }

    private boolean isPrinceKilled() {
        de.horizon.HorizonClient client = de.horizon.HorizonClient.getInstance();
        return client != null && client.getMimicService() != null
            && client.getMimicService().isPrinceKilled();
    }

    private boolean paulScoreBonus() {
        de.horizon.HorizonClient client = de.horizon.HorizonClient.getInstance();
        return client != null && client.getMayorService() != null
            && client.getMayorService().hasDungeonScoreBonus();
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
        announcedS = false;
        announcedSPlus = false;
        totalRoomHisto.clear();
        totalRoomsCached = 0;
        lastHistoCompleted = -1;
        lastHistoCleared = -1;
    }
}
