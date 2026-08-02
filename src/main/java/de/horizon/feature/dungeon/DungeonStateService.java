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
    // Strip § followed by ANY character — Hypixel embeds non-standard per-line identifiers
    // (e.g. §u) inside scoreboard lines, and the standard [0-9a-fk-or] set would leave them in.
    private static final Pattern FORMATTING_CODES = Pattern.compile("\\u00a7.");
    private static final Pattern FLOOR_ROMAN = Pattern.compile("floor\\s+(i{1,3}v?|vi{0,3}|iv|ix)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_MASTER = Pattern.compile("master\\s+mode.*?m([1-7])", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_MASTER_ALT = Pattern.compile("m([1-7])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_F_NUMBER = Pattern.compile("\\bf([1-7])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_DIGIT = Pattern.compile("floor\\s*:?\\s*([0-7])", Pattern.CASE_INSENSITIVE);
    // Exact sidebar title format: "The Catacombs (F7)" / "The Catacombs (M7)" / "(F0)".
    private static final Pattern FLOOR_PAREN = Pattern.compile("\\(([fm])([0-7])\\)", Pattern.CASE_INSENSITIVE);

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
    private String lastSidebar = "";

    // Dungeon blessings, parsed from the tab-list footer ("Blessing of Power XII", "Blessing of Time V").
    private static final Pattern BLESSING_POWER = Pattern.compile("Blessing of Power (X{0,3}(?:IX|IV|V?I{0,3}))");
    private static final Pattern BLESSING_TIME = Pattern.compile("Blessing of Time (X{0,3}(?:IX|IV|V?I{0,3}))");
    private int powerBlessing = 0;
    private int timeBlessing = 0;
    private boolean hasTimeBlessing = false;

    public void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            // Transient world-transfer / loading gap (e.g. the boss warp) — PRESERVE state so the map,
            // puzzle solver and world renders don't vanish. A real disconnect resets via the
            // DISCONNECT event.
            return;
        }

        String scoreboardText = sidebarText(client);
        String normalized = normalize(scoreboardText);
        boolean loaded = !normalized.isBlank();
        lastSidebar = normalized;
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

        // Positive (non-boss) dungeon keywords only — used both to arm inDungeon and, by their
        // absence on a LOADED sidebar, to detect that we've actually left.
        boolean positiveDungeon = normalized.contains("the catacombs")
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

        // Any current sign we're still inside a dungeon (inBoss self-sustains the boss room, whose
        // sidebar carries no "catacombs" line).
        if (positiveDungeon || bossDetected || inBoss) {
            inDungeon = true;
        }

        // LATCH: only count "we left" when the sidebar is actually LOADED and shows no dungeon/boss
        // signal at all. An empty sidebar (warp/loading) or a blood "kill mobs" phase doesn't count,
        // so the state survives those transitions. Real exits (hub) clear within ~6s.
        boolean loadedNonDungeon = loaded && !positiveDungeon && !bossDetected;
        if (loadedNonDungeon) {
            ticksSinceDungeonSeen++;
        } else {
            ticksSinceDungeonSeen = 0;
        }
        if (ticksSinceDungeonSeen > 120) {
            inDungeon = false;
        }

        // inBoss is POSITION-based: you're in the boss room when your coordinates are inside the current
        // floor's boss-room bounds (fixed per floor). This is deterministic — during clear you're never
        // in those bounds, so the map/renders show; in the boss room they hide. No chat/scoreboard race.
        inBoss = inDungeon && isInBossRoom(client);
        if (!inBoss) f7Phase = F7Phase.NONE; // phase only meaningful inside the boss
    }

    // Per-floor boss-room bounds (floors 1..7). If the player is inside, we're in the boss room.
    private static final net.minecraft.world.phys.AABB[] BOSS_ROOM_BOUNDS = {
        new net.minecraft.world.phys.AABB(-14, 55, 49, -72, 146, -40),   // F1
        new net.minecraft.world.phys.AABB(-40, 99, -40, 24, 54, 59),     // F2
        new net.minecraft.world.phys.AABB(-40, 118, -40, 42, 64, 37),    // F3
        new net.minecraft.world.phys.AABB(-40, 112, -40, 50, 53, 47),    // F4
        new net.minecraft.world.phys.AABB(-40, 112, -8, 50, 53, 118),    // F5
        new net.minecraft.world.phys.AABB(-40, 51, -8, 22, 110, 134),    // F6
        new net.minecraft.world.phys.AABB(-8, 0, -8, 134, 254, 147),     // F7
    };

    private boolean isInBossRoom(Minecraft client) {
        if (client.player == null) return false;
        int floor = currentFloor;
        if (floor < 1 || floor > 7) return false;
        return BOSS_ROOM_BOUNDS[floor - 1].contains(client.player.position());
    }

    // A REAL floor boss greeting — "[BOSS] <name>:". Used only to mark bossFightStarted (for f7 phase
    // logic); inBoss itself is now POSITION-based (isInBossRoom), so a leaked boss line can't set it.
    private static final Pattern BOSS_GREETING = Pattern.compile(
        "\\[boss\\] (bonzo|scarf|the professor|professor|thorn|livid|sadan|maxor|storm|goldor|necron|wither king)");

    public void handleChatMessage(String rawMessage) {
        String plain = clean(rawMessage);
        String normalized = plain.toLowerCase(Locale.ROOT);
        if (BOSS_GREETING.matcher(normalized).find()) {
            bossFightStarted = true; // a real floor boss is speaking (not the blood Watcher)
        }
        if (normalized.contains("dungeon starts in") || normalized.contains("dungeon starts")) {
            // Fresh run — clear per-run phase state (state now survives the boss warp, so this is the
            // authoritative "new instance" reset rather than the JOIN event).
            inDungeon = true;
            inBoss = false;
            bossFightStarted = false;
            f7Phase = F7Phase.NONE;
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

    /**
     * A warp / server transfer happened (boss warp, entering/leaving the dungeon). Clears the boss
     * latch — a non-Watcher boss chat line re-arms it, and it then stays true until the next warp.
     */
    public void onWarp() {
        inBoss = false;
        bossFightStarted = false;
        f7Phase = F7Phase.NONE;
        // Also drop inDungeon: after the warp the scoreboard/location re-arms it within a tick or two
        // IF the destination is a dungeon; if it's the hub/another island it stays false, so the map
        // no longer lingers for a few seconds in the hub after finishing a run.
        inDungeon = false;
        ticksSinceDungeonSeen = Integer.MAX_VALUE;
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

    /** Last normalized sidebar text (for diagnostics). */
    public String getLastSidebar() {
        return lastSidebar;
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
            if (entry.isHidden()) continue;
            // Hypixel renders each sidebar line as team-prefix + an invisible owner identifier
            // + team-suffix. Concatenate them WITHOUT any separator: a word like "Catacombs" is
            // often split across the prefix/suffix boundary, so an injected space would break it
            // (turning "The Catacombs" into "The Catac ombs" and defeating the floor detection).
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            String line = team != null
                ? clean(team.getPlayerPrefix()) + entry.owner() + clean(team.getPlayerSuffix())
                : entry.owner();
            builder.append('\n').append(line);
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
        // Exact sidebar title "The Catacombs (F7)" / "(M7)" — the most reliable source.
        Matcher paren = FLOOR_PAREN.matcher(normalized);
        if (paren.find()) {
            isMasterMode = paren.group(1).equalsIgnoreCase("m") || normalized.contains("master");
            currentFloor = Integer.parseInt(paren.group(2));
            entranceConfirmed = currentFloor == 0;
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
        powerBlessing = 0;
        timeBlessing = 0;
        hasTimeBlessing = false;
    }

    // ── Blessings (from the tab-list footer) ─────────────────────────────────────

    /** Parses the current Power/Time blessing levels from the tab-list footer text. */
    public void onTabFooter(String footer) {
        if (footer == null) return;
        Matcher m = BLESSING_POWER.matcher(footer);
        powerBlessing = m.find() ? parseRomanNumeral(m.group(1)) : 0;
        Matcher t = BLESSING_TIME.matcher(footer);
        timeBlessing = t.find() ? parseRomanNumeral(t.group(1)) : 0;
        hasTimeBlessing = footer.contains("Blessing of Time");
    }

    public int getPowerBlessing() { return powerBlessing; }
    public int getTimeBlessing() { return timeBlessing; }
    public boolean hasTimeBlessing() { return hasTimeBlessing; }

    private static int parseRomanNumeral(String roman) {
        if (roman == null || roman.isEmpty()) return 0;
        int total = 0, prev = 0;
        for (int i = roman.length() - 1; i >= 0; i--) {
            int v = switch (Character.toUpperCase(roman.charAt(i))) {
                case 'I' -> 1; case 'V' -> 5; case 'X' -> 10;
                case 'L' -> 50; case 'C' -> 100; default -> 0;
            };
            if (v < prev) total -= v; else { total += v; prev = v; }
        }
        return total;
    }
}
