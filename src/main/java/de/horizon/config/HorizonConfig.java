package de.horizon.config;

import de.horizon.Lang;
import de.horizon.feature.chat.ChatCopyMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class HorizonConfig {
    final HudConfig hud;
    final DungeonConfig dungeon;
    final SpotifyConfig spotify;
    final ChatConfig chat;
    final MiscConfig misc;
    final AntiSpamConfig antiSpam;
    final ParticleConfig particle;
    final ScoreboardConfig scoreboard;

    HorizonConfig(HudConfig hud, DungeonConfig dungeon, SpotifyConfig spotify, ChatConfig chat,
                  MiscConfig misc, AntiSpamConfig antiSpam, ParticleConfig particle, ScoreboardConfig scoreboard) {
        this.hud = hud;
        this.dungeon = dungeon;
        this.spotify = spotify;
        this.chat = chat;
        this.misc = misc;
        this.antiSpam = antiSpam;
        this.particle = particle;
        this.scoreboard = scoreboard;
    }

    // ── HUD ──────────────────────────────────────────────────────────────────

    public boolean isReviveHudEnabled() { return hud.reviveHudEnabled; }
    public void setReviveHudEnabled(boolean v) { hud.reviveHudEnabled = v; }

    public String getHudAccentColor() { return normalizeHudAccentColor(hud.hudAccentColor); }
    public void setHudAccentColor(String v) { hud.hudAccentColor = normalizeHudAccentColor(v); }

    public boolean isReviveHudOnlyInBoss() { return hud.reviveHudOnlyInBoss; }
    public void setReviveHudOnlyInBoss(boolean v) { hud.reviveHudOnlyInBoss = v; }

    public boolean isReviveHudAlwaysVisible() { return hud.reviveHudAlwaysVisible; }
    public void setReviveHudAlwaysVisible(boolean v) { hud.reviveHudAlwaysVisible = v; }

    public boolean isSpiritMaskEnabled() { return hud.spiritMaskEnabled; }
    public void setSpiritMaskEnabled(boolean v) { hud.spiritMaskEnabled = v; }

    public boolean isBonzoMaskEnabled() { return hud.bonzoMaskEnabled; }
    public void setBonzoMaskEnabled(boolean v) { hud.bonzoMaskEnabled = v; }

    public boolean isPhoenixPetEnabled() { return hud.phoenixPetEnabled; }
    public void setPhoenixPetEnabled(boolean v) { hud.phoenixPetEnabled = v; }

    public boolean isCompactHypixelHealthEnabled() { return hud.compactHypixelHealthEnabled; }
    public void setCompactHypixelHealthEnabled(boolean v) { hud.compactHypixelHealthEnabled = v; }

    public boolean isHideDefenseBar() { return hud.hideDefenseBar; }
    public void setHideDefenseBar(boolean v) { hud.hideDefenseBar = v; }

    public boolean isTimeHudEnabled() { return hud.timeHudEnabled; }
    public void setTimeHudEnabled(boolean v) { hud.timeHudEnabled = v; }

    public boolean isPerformanceHudEnabled() { return hud.performanceHudEnabled; }
    public void setPerformanceHudEnabled(boolean v) { hud.performanceHudEnabled = v; }

    public boolean isSystemHudEnabled() { return hud.systemHudEnabled; }
    public void setSystemHudEnabled(boolean v) { hud.systemHudEnabled = v; }

    public boolean isSolverDebugHudEnabled() { return hud.solverDebugHudEnabled; }
    public void setSolverDebugHudEnabled(boolean v) { hud.solverDebugHudEnabled = v; }

    public Map<String, HudPosition> getHudPositions() { return hud.hudPositions; }

    // ── DUNGEON ───────────────────────────────────────────────────────────────

    public int getCatacombsLevel() { return dungeon.catacombsLevel; }
    public void setCatacombsLevel(int v) { dungeon.catacombsLevel = Math.max(0, Math.min(50, v)); }

    public boolean isDungeonPartyFinderOverlayEnabled() { return dungeon.dungeonPartyFinderOverlayEnabled; }
    public void setDungeonPartyFinderOverlayEnabled(boolean v) { dungeon.dungeonPartyFinderOverlayEnabled = v; }

    public boolean isDungeonRareRoomAlertsEnabled() { return dungeon.dungeonRareRoomAlertsEnabled; }
    public void setDungeonRareRoomAlertsEnabled(boolean v) { dungeon.dungeonRareRoomAlertsEnabled = v; }

    public boolean isTerminalCorrectAllEnabled() { return dungeon.terminalCorrectAllEnabled; }
    public void setTerminalCorrectAllEnabled(boolean v) { dungeon.terminalCorrectAllEnabled = v; }

    public boolean isTerminalNavigateMazeEnabled() { return dungeon.terminalNavigateMazeEnabled; }
    public void setTerminalNavigateMazeEnabled(boolean v) { dungeon.terminalNavigateMazeEnabled = v; }

    public boolean isTerminalClickInOrderEnabled() { return dungeon.terminalClickInOrderEnabled; }
    public void setTerminalClickInOrderEnabled(boolean v) { dungeon.terminalClickInOrderEnabled = v; }

    public boolean isTerminalStartsWithEnabled() { return dungeon.terminalStartsWithEnabled; }
    public void setTerminalStartsWithEnabled(boolean v) { dungeon.terminalStartsWithEnabled = v; }

    public boolean isTerminalSelectAllColorEnabled() { return dungeon.terminalSelectAllColorEnabled; }
    public void setTerminalSelectAllColorEnabled(boolean v) { dungeon.terminalSelectAllColorEnabled = v; }

    public boolean isTerminalSameColorEnabled() { return dungeon.terminalSameColorEnabled; }
    public void setTerminalSameColorEnabled(boolean v) { dungeon.terminalSameColorEnabled = v; }

    public boolean isPuzzleWaterBoardEnabled() { return dungeon.puzzleWaterBoardEnabled; }
    public void setPuzzleWaterBoardEnabled(boolean v) { dungeon.puzzleWaterBoardEnabled = v; }

    public boolean isPuzzleThreeWeirdosEnabled() { return dungeon.puzzleThreeWeirdosEnabled; }
    public void setPuzzleThreeWeirdosEnabled(boolean v) { dungeon.puzzleThreeWeirdosEnabled = v; }

    public boolean isPuzzleBlazeEnabled() { return dungeon.puzzleBlazeEnabled; }
    public void setPuzzleBlazeEnabled(boolean v) { dungeon.puzzleBlazeEnabled = v; }

    public boolean isPuzzleIceFillEnabled() { return dungeon.puzzleIceFillEnabled; }
    public void setPuzzleIceFillEnabled(boolean v) { dungeon.puzzleIceFillEnabled = v; }

    public boolean isPuzzleQuizEnabled() { return dungeon.puzzleQuizEnabled; }
    public void setPuzzleQuizEnabled(boolean v) { dungeon.puzzleQuizEnabled = v; }

    public boolean isPuzzleTicTacToeEnabled() { return dungeon.puzzleTicTacToeEnabled; }
    public void setPuzzleTicTacToeEnabled(boolean v) { dungeon.puzzleTicTacToeEnabled = v; }

    public boolean isPuzzleCreeperBeamsEnabled() { return dungeon.puzzleCreeperBeamsEnabled; }
    public void setPuzzleCreeperBeamsEnabled(boolean v) { dungeon.puzzleCreeperBeamsEnabled = v; }

    public boolean isPuzzleBoulderEnabled() { return dungeon.puzzleBoulderEnabled; }
    public void setPuzzleBoulderEnabled(boolean v) { dungeon.puzzleBoulderEnabled = v; }

    public boolean isPuzzleIcePathEnabled() { return dungeon.puzzleIcePathEnabled; }
    public void setPuzzleIcePathEnabled(boolean v) { dungeon.puzzleIcePathEnabled = v; }

    public boolean isPuzzleTeleportMazeEnabled() { return dungeon.puzzleTeleportMazeEnabled; }
    public void setPuzzleTeleportMazeEnabled(boolean v) { dungeon.puzzleTeleportMazeEnabled = v; }

    // ── SPOTIFY ───────────────────────────────────────────────────────────────

    public boolean isSpotifyInventoryControlsEnabled() { return spotify.spotifyInventoryControlsEnabled; }
    public void setSpotifyInventoryControlsEnabled(boolean v) { spotify.spotifyInventoryControlsEnabled = v; }

    public String getSpotifyClientId() { return spotify.spotifyClientId; }
    public void setSpotifyClientId(String v) { spotify.spotifyClientId = v == null ? "" : v.trim(); }

    public int getSpotifyRedirectPort() { return spotify.spotifyRedirectPort; }
    public void setSpotifyRedirectPort(int v) { spotify.spotifyRedirectPort = Math.max(1024, Math.min(65535, v)); }

    public String getSpotifyAccessToken() { return spotify.spotifyAccessToken; }
    public void setSpotifyAccessToken(String v) { spotify.spotifyAccessToken = v == null ? "" : v; }

    public String getSpotifyRefreshToken() { return spotify.spotifyRefreshToken; }
    public void setSpotifyRefreshToken(String v) { spotify.spotifyRefreshToken = v == null ? "" : v; }

    public long getSpotifyTokenExpiresAt() { return spotify.spotifyTokenExpiresAt; }
    public void setSpotifyTokenExpiresAt(long v) { spotify.spotifyTokenExpiresAt = v; }

    public String getSpotifyConnectedAccount() { return spotify.spotifyConnectedAccount; }
    public void setSpotifyConnectedAccount(String v) { spotify.spotifyConnectedAccount = v == null ? "" : v; }

    // ── CHAT ──────────────────────────────────────────────────────────────────

    public String getChatBridgeBotName() {
        return chat.chatBridgeBotName == null || chat.chatBridgeBotName.isBlank() ? "catgirlfc" : chat.chatBridgeBotName.trim();
    }
    public void setChatBridgeBotName(String v) { chat.chatBridgeBotName = v == null ? "catgirlfc" : v.trim(); }

    public boolean isChatBridgeHidden() { return chat.chatBridgeHidden; }
    public void setChatBridgeHidden(boolean v) { chat.chatBridgeHidden = v; }

    public ChatCopyMode getChatCopyMode() { return chat.chatCopyMode == null ? ChatCopyMode.OFF : chat.chatCopyMode; }
    public void setChatCopyMode(ChatCopyMode v) { chat.chatCopyMode = v == null ? ChatCopyMode.OFF : v; }

    public boolean isChatCopyFullMessage() { return chat.chatCopyFullMessage; }
    public void setChatCopyFullMessage(boolean v) { chat.chatCopyFullMessage = v; }

    // ── MISC ──────────────────────────────────────────────────────────────────

    public Lang.Language getLanguage() { return misc.language == null ? Lang.Language.DE : misc.language; }
    public void setLanguage(Lang.Language v) { misc.language = v == null ? Lang.Language.DE : v; }

    public String getHypixelApiKey() { return misc.hypixelApiKey == null ? "" : misc.hypixelApiKey; }
    public void setHypixelApiKey(String v) { misc.hypixelApiKey = v == null ? "" : v.trim(); }

    public boolean isHorizonBackendEnabled() { return misc.horizonBackendEnabled; }
    public void setHorizonBackendEnabled(boolean v) { misc.horizonBackendEnabled = v; }

    public String getHorizonBackendBaseUrl() {
        return misc.horizonBackendBaseUrl == null || misc.horizonBackendBaseUrl.isBlank() ? "https://api.horizon.local" : misc.horizonBackendBaseUrl.trim();
    }
    public void setHorizonBackendBaseUrl(String v) {
        misc.horizonBackendBaseUrl = v == null ? "https://api.horizon.local" : v.trim();
    }

    public String getHorizonBackendAccessToken() { return misc.horizonBackendAccessToken == null ? "" : misc.horizonBackendAccessToken; }
    public void setHorizonBackendAccessToken(String v) { misc.horizonBackendAccessToken = v == null ? "" : v; }

    public long getHorizonBackendTokenExpiresAt() { return misc.horizonBackendTokenExpiresAt; }
    public void setHorizonBackendTokenExpiresAt(long v) { misc.horizonBackendTokenExpiresAt = v; }

    public String getHorizonBackendAudience() {
        return misc.horizonBackendAudience == null || misc.horizonBackendAudience.isBlank() ? "horizon-profile-api" : misc.horizonBackendAudience.trim();
    }
    public void setHorizonBackendAudience(String v) {
        misc.horizonBackendAudience = v == null ? "horizon-profile-api" : v.trim();
    }

    // ── ANTI SPAM ─────────────────────────────────────────────────────────────

    public boolean isAntiSpamEnabled() { return antiSpam.antiSpamEnabled; }
    public void setAntiSpamEnabled(boolean v) { antiSpam.antiSpamEnabled = v; }

    public boolean isHideBlocksInTheWayMessages() { return antiSpam.hideBlocksInTheWayMessages; }
    public void setHideBlocksInTheWayMessages(boolean v) { antiSpam.hideBlocksInTheWayMessages = v; }

    public boolean isHideAbilityMessages() { return antiSpam.hideAbilityMessages; }
    public void setHideAbilityMessages(boolean v) { antiSpam.hideAbilityMessages = v; }

    public boolean isHideManaMessages() { return antiSpam.hideManaMessages; }
    public void setHideManaMessages(boolean v) { antiSpam.hideManaMessages = v; }

    public boolean isHideCooldownMessages() { return antiSpam.hideCooldownMessages; }
    public void setHideCooldownMessages(boolean v) { antiSpam.hideCooldownMessages = v; }

    public boolean isHideBlessingMessages() { return antiSpam.hideBlessingMessages; }
    public void setHideBlessingMessages(boolean v) { antiSpam.hideBlessingMessages = v; }

    public boolean isHideDungeonPickupMessages() { return antiSpam.hideDungeonPickupMessages; }
    public void setHideDungeonPickupMessages(boolean v) { antiSpam.hideDungeonPickupMessages = v; }

    public boolean isHideAutoPetMessages() { return antiSpam.hideAutoPetMessages; }
    public void setHideAutoPetMessages(boolean v) { antiSpam.hideAutoPetMessages = v; }

    public boolean isHideFullStatusMessages() { return antiSpam.hideFullStatusMessages; }
    public void setHideFullStatusMessages(boolean v) { antiSpam.hideFullStatusMessages = v; }

    public boolean isHideEffectMessages() { return antiSpam.hideEffectMessages; }
    public void setHideEffectMessages(boolean v) { antiSpam.hideEffectMessages = v; }

    public boolean isHideHealingMessages() { return antiSpam.hideHealingMessages; }
    public void setHideHealingMessages(boolean v) { antiSpam.hideHealingMessages = v; }

    public boolean isHideDungeonEventMessages() { return antiSpam.hideDungeonEventMessages; }
    public void setHideDungeonEventMessages(boolean v) { antiSpam.hideDungeonEventMessages = v; }

    public boolean isHideLockedChestMessages() { return antiSpam.hideLockedChestMessages; }
    public void setHideLockedChestMessages(boolean v) { antiSpam.hideLockedChestMessages = v; }

    // ── PARTICLE ──────────────────────────────────────────────────────────────

    public Map<String, Boolean> getParticleStates() { return particle.particleStates; }

    // ── SCOREBOARD ────────────────────────────────────────────────────────────

    public boolean isCustomScoreboardEnabled() { return scoreboard.customScoreboardEnabled; }
    public void setCustomScoreboardEnabled(boolean v) { scoreboard.customScoreboardEnabled = v; }

    public Map<String, String> getScoreboardKnownLines(String islandId) {
        return scoreboard.getKnownLines(islandId);
    }

    public void recordScoreboardLines(String islandId, List<String> lines) {
        scoreboard.recordLines(islandId, lines);
    }

    public boolean isScoreboardLineHidden(String islandId, String lineKey) {
        return scoreboard.isLineEffectivelyHidden(islandId, lineKey);
    }

    public boolean isScoreboardLineEffectivelyHidden(String islandId, String lineKey) {
        return scoreboard.isLineEffectivelyHidden(islandId, lineKey);
    }

    public void toggleScoreboardLine(String islandId, String lineKey) {
        scoreboard.toggleLine(islandId, lineKey);
    }

    public boolean isScoreboardGlobalLineHidden(String lineKey) {
        return scoreboard.isGlobalLineHidden(lineKey);
    }

    public void toggleScoreboardGlobalLine(String lineKey) {
        scoreboard.toggleGlobalLine(lineKey);
    }

    public void reorderScoreboardLine(String islandId, String key, int newIndex) {
        scoreboard.reorderLine(islandId, key, newIndex);
    }

    public Map<String, Set<String>> getScoreboardHiddenKeys() {
        return scoreboard.scoreboardHiddenKeys;
    }

    public Set<String> getScoreboardGlobalHiddenKeys() {
        return scoreboard.scoreboardGlobalHiddenKeys;
    }

    /**
     * Returns a human-readable display label for a scoreboard line key.
     * Dynamic components (numbers, timers) are stripped – the label is stable.
     */
    public static String formatScoreboardKeyLabel(String key) {
        if (key == null || key.isBlank()) return key;
        switch (key) {
            case "location":    return "Location (⏣)";
            case "server_code": return "Date";
            case "timer":       return "Timer";
            case "time":        return "Time";
            case "season":      return "Season";
            case "slayer quest": return "Slayer Quest";
            case "combat exp":  return "Slayer Quest Combat EXP";
            case "next tier":   return "Slayer Quest Next Tier";
            default: {
                // Title-case: "farming contest" → "Farming Contest"
                String[] words = key.replace('_', ' ').split(" ");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) sb.append(word.substring(1));
                }
                return sb.toString();
            }
        }
    }

    /** Returns the ARGB text color that matches the in-game scoreboard style for a given line key. */
    public static int scoreboardKeyColor(String key) {
        if (key == null) return 0xFFFFFFFF;
        switch (key) {
            case "purse": case "piggy":                             return 0xFFFFAA00; // gold
            case "bits": case "motes":                              return 0xFF55FFFF; // aqua
            case "copper":                                          return 0xFFFF7700; // orange
            case "stardust":                                        return 0xFF55FF55; // green
            case "location":                                        return 0xFFFFFF55; // yellow
            case "slayer quest": case "combat exp": case "next tier": return 0xFFFF55FF; // light purple
            case "kills":                                           return 0xFFFFFF55; // yellow
            case "deaths":                                          return 0xFFFF5555; // red
            case "secrets found": case "score": case "cleared":
            case "the catacombs": case "crypts":                    return 0xFFFFFF55; // yellow
            case "profile": case "skills": case "class":            return 0xFFAAAAAA; // gray
            case "season": case "time": case "server_code":
            case "timer": case "date":                              return 0xFFAAAAAA; // gray
            default:                                                return 0xFFFFFFFF; // white
        }
    }

    // ── UTILITY ───────────────────────────────────────────────────────────────

    private static final Pattern P_COLOR_CODE       = Pattern.compile("§[0-9a-fklmnorA-FK-LMN-OR]");
    private static final Pattern P_TIME_START       = Pattern.compile("\\d{1,2}:\\d{2}.*");
    private static final Pattern P_SEASON           = Pattern.compile(".*(spring|summer|autumm?|fall|winter).*");
    private static final Pattern P_ORDINAL          = Pattern.compile(".*\\d+(st|nd|rd|th).*");
    private static final Pattern P_SERVER_CODE      = Pattern.compile("\\d{2}/\\d{2}/\\d{2}.*");
    private static final Pattern P_LEADING_NONSYM   = Pattern.compile("^[^\\w]+");
    private static final Pattern P_TIMER_HMS        = Pattern.compile("\\d+[hms](\\s+\\d+[hms])*");
    private static final Pattern P_BARE_DIGITS      = Pattern.compile("\\d{1,2}");
    private static final Pattern P_PAREN_PROGRESS   = Pattern.compile("^\\([\\d.,]+[kKmMbBtT]?/[\\d.,]+[kKmMbBtT]?\\)\\s*");
    private static final Pattern P_LEADING_FRACTION = Pattern.compile("^\\d[\\d.,]*/\\d[\\d.,]*\\s+");
    private static final Pattern P_LEADING_NONALNUM = Pattern.compile("^[^a-zA-Z0-9]+");
    private static final Pattern P_TRAILING_TIMER   = Pattern.compile("\\s+\\d+[hms](\\d+[hms])*$");
    private static final Pattern P_TRAILING_CLOCK   = Pattern.compile("\\s+\\d{1,2}:\\d{2}(:\\d{2})?$");
    private static final Pattern P_TRAILING_FRAC    = Pattern.compile("\\s+\\d+/\\d+$");
    private static final Pattern P_TRAILING_NUMS    = Pattern.compile("(\\s+x?[\\d,.]+[kKmMbBtT]?)+$");
    private static final Pattern P_TRAILING_SYMBOLS = Pattern.compile("[^a-zA-Z0-9\\s]+$");
    private static final Pattern P_TRAILING_SEP     = Pattern.compile("[\\s\\-/|]+$");

    public static String scoreboardLineKey(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String clean = P_COLOR_CODE.matcher(line).replaceAll("").trim();
        if (clean.contains("⏣")) {
            return "location";
        }
        // Time line: starts with 1-2 digits followed by colon (e.g. "3:45 PM", "12:00")
        if (P_TIME_START.matcher(clean).matches()) {
            return "time";
        }
        // Season line: contains a SkyBlock season word (e.g. "Autum 22", "Early Spring", "Late Summer 3rd")
        String cleanLower = clean.toLowerCase(Locale.ROOT);
        if (P_SEASON.matcher(cleanLower).matches()) {
            return "season";
        }
        // Ordinal date line (fallback for lines like "3rd" without season word)
        if (P_ORDINAL.matcher(cleanLower).matches()) {
            return "season";
        }
        // Server code / date line: starts with MM/DD/YY — server code suffix ignored for key
        if (P_SERVER_CODE.matcher(clean).matches()) {
            return "server_code";
        }
        // Timer / countdown: symbol-prefixed time (e.g. "⏰ 0:37:52") or h/m/s format (e.g. "1h 30m 20s")
        // Strip leading non-word characters to reveal the numeric content
        String afterSymbols = P_LEADING_NONSYM.matcher(clean).replaceFirst("").trim();
        if (!afterSymbols.isEmpty()) {
            if (P_TIME_START.matcher(afterSymbols).matches()
                    || P_TIMER_HMS.matcher(afterSymbols).matches()) {
                return "timer";
            }
        }
        int colon = clean.indexOf(':');
        if (colon > 0) {
            String key = clean.substring(0, colon).toLowerCase(Locale.ROOT).trim();
            // Guard: if key is a bare 1-2 digit number it's a time line without AM/PM suffix
            if (P_BARE_DIGITS.matcher(key).matches()) {
                return "time";
            }
            if (key.length() <= 1) return "";
            return key;
        }

        // ── Dynamic-value normalization ───────────────────────────────────────
        // Strip changing numeric values so lines like "2/70 Kills" and "3/77 Kills"
        // always produce the same stable key ("kills"), preventing duplicate toggle
        // entries in the config screen for progress counters, timers, plot numbers, etc.
        String s = clean;
        // Remove parenthesised progress prefix: "(70/2.4k) " or "(3/10) "
        s = P_PAREN_PROGRESS.matcher(s).replaceFirst("");
        // Remove leading numeric fraction: "2/70 " or "3/77 "
        s = P_LEADING_FRACTION.matcher(s).replaceFirst("");
        // Remove leading non-alphanumeric characters (emoji, colour symbols, etc.)
        s = P_LEADING_NONALNUM.matcher(s).replaceFirst("").trim();
        // Remove trailing h/m/s timer: "3m9s", "1h30m20s", "45s"
        s = P_TRAILING_TIMER.matcher(s).replaceFirst("").trim();
        // Remove trailing clock: " 0:37:52", " 3:45"
        s = P_TRAILING_CLOCK.matcher(s).replaceFirst("").trim();
        // Remove trailing fraction: " 3/5"
        s = P_TRAILING_FRAC.matcher(s).replaceFirst("").trim();
        // Iteratively remove trailing numeric tokens (x1, 19, 1.2k, 1,234,567, etc.)
        // and trailing symbol/emoji characters until the string stabilises
        String prev;
        do {
            prev = s;
            s = P_TRAILING_NUMS.matcher(s).replaceFirst("").trim();
            s = P_TRAILING_SYMBOLS.matcher(s).replaceFirst("").trim();
        } while (!s.equals(prev));
        // Remove any leftover trailing punctuation/separators (" - ", " / ", etc.)
        s = P_TRAILING_SEP.matcher(s).replaceFirst("").trim();

        String dynamicKey = s.toLowerCase(Locale.ROOT);
        String finalKey = (!dynamicKey.isBlank() && !dynamicKey.equals(cleanLower.trim()))
            ? dynamicKey : cleanLower.trim();
        if (finalKey.length() <= 1) return "";
        if (isSlayerBossKey(finalKey)) return "slayer quest";
        return finalKey;
    }

    private static boolean isSlayerBossKey(String key) {
        return key.contains("sven") || key.contains("tarantula") || key.contains("revenant")
            || key.contains("voidgloom") || key.contains("inferno") || key.contains("riftstalker");
    }

    private String normalizeHudAccentColor(String value) {
        if (value == null) return "#75E7CA";
        String trimmed = value.trim().toUpperCase();
        if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
        if (!trimmed.matches("[0-9A-F]{6}")) return "#75E7CA";
        return "#" + trimmed;
    }
}
