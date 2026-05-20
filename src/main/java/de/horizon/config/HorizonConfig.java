package de.horizon.config;

import de.horizon.feature.chat.ChatCopyMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HorizonConfig {
    private static final String DEFAULT_HUD_ACCENT_COLOR = "#75E7CA";

    private boolean reviveHudEnabled = true;
    private String hudAccentColor = DEFAULT_HUD_ACCENT_COLOR;
    private int catacombsLevel = 0;
    private boolean spiritMaskEnabled = true;
    private boolean bonzoMaskEnabled = true;
    private boolean phoenixPetEnabled = true;
    private boolean reviveHudOnlyInBoss = false;
    private boolean reviveHudAlwaysVisible = false;
    private boolean dungeonPartyFinderOverlayEnabled = true;
    private boolean dungeonRareRoomAlertsEnabled = true;
    private boolean terminalCorrectAllEnabled = true;
    private boolean terminalNavigateMazeEnabled = true;
    private boolean terminalClickInOrderEnabled = true;
    private boolean terminalStartsWithEnabled = true;
    private boolean terminalSelectAllColorEnabled = true;
    private boolean terminalSameColorEnabled = true;
    private boolean puzzleWaterBoardEnabled = true;
    private boolean puzzleThreeWeirdosEnabled = true;
    private boolean puzzleBlazeEnabled = true;
    private boolean puzzleIceFillEnabled = true;
    private boolean puzzleQuizEnabled = true;
    private boolean puzzleTicTacToeEnabled = true;
    private boolean puzzleCreeperBeamsEnabled = true;
    private boolean puzzleBoulderEnabled = true;
    private boolean puzzleIcePathEnabled = true;
    private boolean puzzleTeleportMazeEnabled = true;
    private boolean spotifyInventoryControlsEnabled = true;
    private String spotifyClientId = "";
    private int spotifyRedirectPort = 43821;
    private String spotifyAccessToken = "";
    private String spotifyRefreshToken = "";
    private long spotifyTokenExpiresAt = 0L;
    private String spotifyConnectedAccount = "";
    private String hypixelApiKey = "";
    private boolean horizonBackendEnabled = false;
    private String horizonBackendBaseUrl = "https://api.horizon.local";
    private String horizonBackendAccessToken = "";
    private long horizonBackendTokenExpiresAt = 0L;
    private String horizonBackendAudience = "horizon-profile-api";
    private boolean timeHudEnabled = false;
    private boolean performanceHudEnabled = false;
    private boolean systemHudEnabled = false;
    private boolean solverDebugHudEnabled = false;
    private boolean hideDefenseBar = false;
    private boolean antiSpamEnabled = false;
    private boolean hideBlocksInTheWayMessages = true;
    private boolean hideAbilityMessages = true;
    private boolean hideManaMessages = true;
    private boolean hideCooldownMessages = true;
    private boolean hideBlessingMessages = true;
    private boolean hideDungeonPickupMessages = true;
    private boolean hideAutoPetMessages = false;
    private boolean hideFullStatusMessages = true;
    private boolean hideEffectMessages = true;
    private boolean hideHealingMessages = false;
    private boolean hideDungeonEventMessages = false;
    private boolean hideLockedChestMessages = true;
    private boolean compactHypixelHealthEnabled = true;
    private boolean customScoreboardEnabled = true;
    private String chatBridgeBotName = "catgirlfc";
    private boolean chatBridgeHidden = false;
    private ChatCopyMode chatCopyMode = ChatCopyMode.OFF;
    private boolean chatCopyFullMessage = true;
    private final Map<String, Boolean> particleStates = new HashMap<>();
    private final Map<String, HudPosition> hudPositions = new HashMap<>();
    private Map<String, Set<String>> scoreboardHiddenKeys = new HashMap<>();
    private Map<String, Map<String, String>> scoreboardKnownLines = new HashMap<>();
    private Set<String> scoreboardGlobalHiddenKeys = new HashSet<>();

    public boolean isReviveHudEnabled() {
        return reviveHudEnabled;
    }

    public void setReviveHudEnabled(boolean reviveHudEnabled) {
        this.reviveHudEnabled = reviveHudEnabled;
    }

    public String getHudAccentColor() {
        return normalizeHudAccentColor(hudAccentColor);
    }

    public void setHudAccentColor(String hudAccentColor) {
        this.hudAccentColor = normalizeHudAccentColor(hudAccentColor);
    }

    public int getCatacombsLevel() {
        return catacombsLevel;
    }

    public void setCatacombsLevel(int catacombsLevel) {
        this.catacombsLevel = Math.max(0, Math.min(50, catacombsLevel));
    }

    public boolean isSpiritMaskEnabled() {
        return spiritMaskEnabled;
    }

    public void setSpiritMaskEnabled(boolean spiritMaskEnabled) {
        this.spiritMaskEnabled = spiritMaskEnabled;
    }

    public boolean isBonzoMaskEnabled() {
        return bonzoMaskEnabled;
    }

    public void setBonzoMaskEnabled(boolean bonzoMaskEnabled) {
        this.bonzoMaskEnabled = bonzoMaskEnabled;
    }

    public boolean isPhoenixPetEnabled() {
        return phoenixPetEnabled;
    }

    public void setPhoenixPetEnabled(boolean phoenixPetEnabled) {
        this.phoenixPetEnabled = phoenixPetEnabled;
    }

    public boolean isReviveHudOnlyInBoss() {
        return reviveHudOnlyInBoss;
    }

    public void setReviveHudOnlyInBoss(boolean reviveHudOnlyInBoss) {
        this.reviveHudOnlyInBoss = reviveHudOnlyInBoss;
    }

    public boolean isReviveHudAlwaysVisible() {
        return reviveHudAlwaysVisible;
    }

    public void setReviveHudAlwaysVisible(boolean reviveHudAlwaysVisible) {
        this.reviveHudAlwaysVisible = reviveHudAlwaysVisible;
    }

    public boolean isDungeonPartyFinderOverlayEnabled() {
        return dungeonPartyFinderOverlayEnabled;
    }

    public void setDungeonPartyFinderOverlayEnabled(boolean dungeonPartyFinderOverlayEnabled) {
        this.dungeonPartyFinderOverlayEnabled = dungeonPartyFinderOverlayEnabled;
    }

    public boolean isDungeonRareRoomAlertsEnabled() {
        return dungeonRareRoomAlertsEnabled;
    }

    public void setDungeonRareRoomAlertsEnabled(boolean dungeonRareRoomAlertsEnabled) {
        this.dungeonRareRoomAlertsEnabled = dungeonRareRoomAlertsEnabled;
    }

    public boolean isTerminalCorrectAllEnabled() {
        return terminalCorrectAllEnabled;
    }

    public void setTerminalCorrectAllEnabled(boolean terminalCorrectAllEnabled) {
        this.terminalCorrectAllEnabled = terminalCorrectAllEnabled;
    }

    public boolean isTerminalNavigateMazeEnabled() {
        return terminalNavigateMazeEnabled;
    }

    public void setTerminalNavigateMazeEnabled(boolean terminalNavigateMazeEnabled) {
        this.terminalNavigateMazeEnabled = terminalNavigateMazeEnabled;
    }

    public boolean isTerminalClickInOrderEnabled() {
        return terminalClickInOrderEnabled;
    }

    public void setTerminalClickInOrderEnabled(boolean terminalClickInOrderEnabled) {
        this.terminalClickInOrderEnabled = terminalClickInOrderEnabled;
    }

    public boolean isTerminalStartsWithEnabled() {
        return terminalStartsWithEnabled;
    }

    public void setTerminalStartsWithEnabled(boolean terminalStartsWithEnabled) {
        this.terminalStartsWithEnabled = terminalStartsWithEnabled;
    }

    public boolean isTerminalSelectAllColorEnabled() {
        return terminalSelectAllColorEnabled;
    }

    public void setTerminalSelectAllColorEnabled(boolean terminalSelectAllColorEnabled) {
        this.terminalSelectAllColorEnabled = terminalSelectAllColorEnabled;
    }

    public boolean isTerminalSameColorEnabled() {
        return terminalSameColorEnabled;
    }

    public void setTerminalSameColorEnabled(boolean terminalSameColorEnabled) {
        this.terminalSameColorEnabled = terminalSameColorEnabled;
    }

    public boolean isPuzzleWaterBoardEnabled() {
        return puzzleWaterBoardEnabled;
    }

    public void setPuzzleWaterBoardEnabled(boolean puzzleWaterBoardEnabled) {
        this.puzzleWaterBoardEnabled = puzzleWaterBoardEnabled;
    }

    public boolean isPuzzleThreeWeirdosEnabled() {
        return puzzleThreeWeirdosEnabled;
    }

    public void setPuzzleThreeWeirdosEnabled(boolean puzzleThreeWeirdosEnabled) {
        this.puzzleThreeWeirdosEnabled = puzzleThreeWeirdosEnabled;
    }

    public boolean isPuzzleBlazeEnabled() {
        return puzzleBlazeEnabled;
    }

    public void setPuzzleBlazeEnabled(boolean puzzleBlazeEnabled) {
        this.puzzleBlazeEnabled = puzzleBlazeEnabled;
    }

    public boolean isPuzzleIceFillEnabled() {
        return puzzleIceFillEnabled;
    }

    public void setPuzzleIceFillEnabled(boolean puzzleIceFillEnabled) {
        this.puzzleIceFillEnabled = puzzleIceFillEnabled;
    }

    public boolean isPuzzleQuizEnabled() {
        return puzzleQuizEnabled;
    }

    public void setPuzzleQuizEnabled(boolean puzzleQuizEnabled) {
        this.puzzleQuizEnabled = puzzleQuizEnabled;
    }

    public boolean isPuzzleTicTacToeEnabled() {
        return puzzleTicTacToeEnabled;
    }

    public void setPuzzleTicTacToeEnabled(boolean puzzleTicTacToeEnabled) {
        this.puzzleTicTacToeEnabled = puzzleTicTacToeEnabled;
    }

    public boolean isPuzzleCreeperBeamsEnabled() {
        return puzzleCreeperBeamsEnabled;
    }

    public void setPuzzleCreeperBeamsEnabled(boolean puzzleCreeperBeamsEnabled) {
        this.puzzleCreeperBeamsEnabled = puzzleCreeperBeamsEnabled;
    }

    public boolean isPuzzleBoulderEnabled() {
        return puzzleBoulderEnabled;
    }

    public void setPuzzleBoulderEnabled(boolean puzzleBoulderEnabled) {
        this.puzzleBoulderEnabled = puzzleBoulderEnabled;
    }

    public boolean isPuzzleIcePathEnabled() {
        return puzzleIcePathEnabled;
    }

    public void setPuzzleIcePathEnabled(boolean puzzleIcePathEnabled) {
        this.puzzleIcePathEnabled = puzzleIcePathEnabled;
    }

    public boolean isPuzzleTeleportMazeEnabled() {
        return puzzleTeleportMazeEnabled;
    }

    public void setPuzzleTeleportMazeEnabled(boolean puzzleTeleportMazeEnabled) {
        this.puzzleTeleportMazeEnabled = puzzleTeleportMazeEnabled;
    }

    public boolean isSpotifyInventoryControlsEnabled() {
        return spotifyInventoryControlsEnabled;
    }

    public void setSpotifyInventoryControlsEnabled(boolean spotifyInventoryControlsEnabled) {
        this.spotifyInventoryControlsEnabled = spotifyInventoryControlsEnabled;
    }

    public String getSpotifyClientId() {
        return spotifyClientId;
    }

    public void setSpotifyClientId(String spotifyClientId) {
        this.spotifyClientId = spotifyClientId == null ? "" : spotifyClientId.trim();
    }

    public int getSpotifyRedirectPort() {
        return spotifyRedirectPort;
    }

    public void setSpotifyRedirectPort(int spotifyRedirectPort) {
        this.spotifyRedirectPort = Math.max(1024, Math.min(65535, spotifyRedirectPort));
    }

    public String getSpotifyAccessToken() {
        return spotifyAccessToken;
    }

    public void setSpotifyAccessToken(String spotifyAccessToken) {
        this.spotifyAccessToken = spotifyAccessToken == null ? "" : spotifyAccessToken;
    }

    public String getSpotifyRefreshToken() {
        return spotifyRefreshToken;
    }

    public void setSpotifyRefreshToken(String spotifyRefreshToken) {
        this.spotifyRefreshToken = spotifyRefreshToken == null ? "" : spotifyRefreshToken;
    }

    public long getSpotifyTokenExpiresAt() {
        return spotifyTokenExpiresAt;
    }

    public void setSpotifyTokenExpiresAt(long spotifyTokenExpiresAt) {
        this.spotifyTokenExpiresAt = spotifyTokenExpiresAt;
    }

    public String getSpotifyConnectedAccount() {
        return spotifyConnectedAccount;
    }

    public void setSpotifyConnectedAccount(String spotifyConnectedAccount) {
        this.spotifyConnectedAccount = spotifyConnectedAccount == null ? "" : spotifyConnectedAccount;
    }

    public String getHypixelApiKey() {
        return hypixelApiKey;
    }

    public void setHypixelApiKey(String hypixelApiKey) {
        this.hypixelApiKey = hypixelApiKey == null ? "" : hypixelApiKey.trim();
    }

    public boolean isHorizonBackendEnabled() {
        return horizonBackendEnabled;
    }

    public void setHorizonBackendEnabled(boolean horizonBackendEnabled) {
        this.horizonBackendEnabled = horizonBackendEnabled;
    }

    public String getHorizonBackendBaseUrl() {
        return horizonBackendBaseUrl == null || horizonBackendBaseUrl.isBlank() ? "https://api.horizon.local" : horizonBackendBaseUrl.trim();
    }

    public void setHorizonBackendBaseUrl(String horizonBackendBaseUrl) {
        this.horizonBackendBaseUrl = horizonBackendBaseUrl == null ? "https://api.horizon.local" : horizonBackendBaseUrl.trim();
    }

    public String getHorizonBackendAccessToken() {
        return horizonBackendAccessToken == null ? "" : horizonBackendAccessToken;
    }

    public void setHorizonBackendAccessToken(String horizonBackendAccessToken) {
        this.horizonBackendAccessToken = horizonBackendAccessToken == null ? "" : horizonBackendAccessToken;
    }

    public long getHorizonBackendTokenExpiresAt() {
        return horizonBackendTokenExpiresAt;
    }

    public void setHorizonBackendTokenExpiresAt(long horizonBackendTokenExpiresAt) {
        this.horizonBackendTokenExpiresAt = horizonBackendTokenExpiresAt;
    }

    public String getHorizonBackendAudience() {
        return horizonBackendAudience == null || horizonBackendAudience.isBlank() ? "horizon-profile-api" : horizonBackendAudience.trim();
    }

    public void setHorizonBackendAudience(String horizonBackendAudience) {
        this.horizonBackendAudience = horizonBackendAudience == null ? "horizon-profile-api" : horizonBackendAudience.trim();
    }

    public boolean isTimeHudEnabled() {
        return timeHudEnabled;
    }

    public void setTimeHudEnabled(boolean timeHudEnabled) {
        this.timeHudEnabled = timeHudEnabled;
    }

    public boolean isPerformanceHudEnabled() {
        return performanceHudEnabled;
    }

    public void setPerformanceHudEnabled(boolean performanceHudEnabled) {
        this.performanceHudEnabled = performanceHudEnabled;
    }

    public boolean isSystemHudEnabled() {
        return systemHudEnabled;
    }

    public void setSystemHudEnabled(boolean systemHudEnabled) {
        this.systemHudEnabled = systemHudEnabled;
    }

    public boolean isSolverDebugHudEnabled() {
        return solverDebugHudEnabled;
    }

    public void setSolverDebugHudEnabled(boolean solverDebugHudEnabled) {
        this.solverDebugHudEnabled = solverDebugHudEnabled;
    }

    public boolean isHideDefenseBar() {
        return hideDefenseBar;
    }

    public void setHideDefenseBar(boolean hideDefenseBar) {
        this.hideDefenseBar = hideDefenseBar;
    }

    public boolean isAntiSpamEnabled() {
        return antiSpamEnabled;
    }

    public void setAntiSpamEnabled(boolean antiSpamEnabled) {
        this.antiSpamEnabled = antiSpamEnabled;
    }

    public boolean isHideBlocksInTheWayMessages() {
        return hideBlocksInTheWayMessages;
    }

    public void setHideBlocksInTheWayMessages(boolean hideBlocksInTheWayMessages) {
        this.hideBlocksInTheWayMessages = hideBlocksInTheWayMessages;
    }

    public boolean isHideAbilityMessages() {
        return hideAbilityMessages;
    }

    public void setHideAbilityMessages(boolean hideAbilityMessages) {
        this.hideAbilityMessages = hideAbilityMessages;
    }

    public boolean isHideManaMessages() {
        return hideManaMessages;
    }

    public void setHideManaMessages(boolean hideManaMessages) {
        this.hideManaMessages = hideManaMessages;
    }

    public boolean isHideCooldownMessages() {
        return hideCooldownMessages;
    }

    public void setHideCooldownMessages(boolean hideCooldownMessages) {
        this.hideCooldownMessages = hideCooldownMessages;
    }

    public boolean isHideBlessingMessages() {
        return hideBlessingMessages;
    }

    public void setHideBlessingMessages(boolean hideBlessingMessages) {
        this.hideBlessingMessages = hideBlessingMessages;
    }

    public boolean isHideDungeonPickupMessages() {
        return hideDungeonPickupMessages;
    }

    public void setHideDungeonPickupMessages(boolean hideDungeonPickupMessages) {
        this.hideDungeonPickupMessages = hideDungeonPickupMessages;
    }

    public boolean isHideAutoPetMessages() {
        return hideAutoPetMessages;
    }

    public void setHideAutoPetMessages(boolean hideAutoPetMessages) {
        this.hideAutoPetMessages = hideAutoPetMessages;
    }

    public boolean isHideFullStatusMessages() {
        return hideFullStatusMessages;
    }

    public void setHideFullStatusMessages(boolean hideFullStatusMessages) {
        this.hideFullStatusMessages = hideFullStatusMessages;
    }

    public boolean isHideEffectMessages() {
        return hideEffectMessages;
    }

    public void setHideEffectMessages(boolean hideEffectMessages) {
        this.hideEffectMessages = hideEffectMessages;
    }

    public boolean isHideHealingMessages() {
        return hideHealingMessages;
    }

    public void setHideHealingMessages(boolean hideHealingMessages) {
        this.hideHealingMessages = hideHealingMessages;
    }

    public boolean isHideDungeonEventMessages() {
        return hideDungeonEventMessages;
    }

    public void setHideDungeonEventMessages(boolean hideDungeonEventMessages) {
        this.hideDungeonEventMessages = hideDungeonEventMessages;
    }

    public boolean isHideLockedChestMessages() {
        return hideLockedChestMessages;
    }

    public void setHideLockedChestMessages(boolean hideLockedChestMessages) {
        this.hideLockedChestMessages = hideLockedChestMessages;
    }

    public boolean isCompactHypixelHealthEnabled() {
        return compactHypixelHealthEnabled;
    }

    public void setCompactHypixelHealthEnabled(boolean compactHypixelHealthEnabled) {
        this.compactHypixelHealthEnabled = compactHypixelHealthEnabled;
    }

    public boolean isCustomScoreboardEnabled() {
        return customScoreboardEnabled;
    }

    public void setCustomScoreboardEnabled(boolean customScoreboardEnabled) {
        this.customScoreboardEnabled = customScoreboardEnabled;
    }

    public String getChatBridgeBotName() {
        return chatBridgeBotName == null || chatBridgeBotName.isBlank() ? "catgirlfc" : chatBridgeBotName.trim();
    }

    public void setChatBridgeBotName(String chatBridgeBotName) {
        this.chatBridgeBotName = chatBridgeBotName == null ? "catgirlfc" : chatBridgeBotName.trim();
    }

    public boolean isChatBridgeHidden() {
        return chatBridgeHidden;
    }

    public void setChatBridgeHidden(boolean chatBridgeHidden) {
        this.chatBridgeHidden = chatBridgeHidden;
    }

    public ChatCopyMode getChatCopyMode() {
        return chatCopyMode == null ? ChatCopyMode.OFF : chatCopyMode;
    }

    public void setChatCopyMode(ChatCopyMode chatCopyMode) {
        this.chatCopyMode = chatCopyMode == null ? ChatCopyMode.OFF : chatCopyMode;
    }

    public boolean isChatCopyFullMessage() {
        return chatCopyFullMessage;
    }

    public void setChatCopyFullMessage(boolean chatCopyFullMessage) {
        this.chatCopyFullMessage = chatCopyFullMessage;
    }

    public Map<String, Boolean> getParticleStates() {
        return particleStates;
    }

    public Map<String, HudPosition> getHudPositions() {
        return hudPositions;
    }

    public static String scoreboardLineKey(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String clean = line.replaceAll("§[0-9a-fklmnorA-FK-LMN-OR]", "").trim();
        if (clean.contains("⏣")) {
            return "location";
        }
        // Time line: starts with 1-2 digits followed by colon (e.g. "3:45 PM", "12:00")
        if (clean.matches("\\d{1,2}:\\d{2}.*")) {
            return "time";
        }
        // Date line: contains ordinal number suffix (e.g. "Early Spring 3rd", "Late Summer 28th")
        if (clean.toLowerCase(Locale.ROOT).matches(".*\\d+(st|nd|rd|th).*")) {
            return "date";
        }
        int colon = clean.indexOf(':');
        if (colon > 0) {
            String key = clean.substring(0, colon).toLowerCase(Locale.ROOT).trim();
            // Guard: if key is a bare 1-2 digit number it's a time line without AM/PM suffix
            if (key.matches("\\d{1,2}")) {
                return "time";
            }
            return key;
        }
        return clean.toLowerCase(Locale.ROOT).trim();
    }

    public boolean isScoreboardLineHidden(String islandId, String lineKey) {
        Set<String> hidden = scoreboardHiddenKeys.get(islandId);
        return hidden != null && hidden.contains(lineKey);
    }

    public void toggleScoreboardLine(String islandId, String lineKey) {
        Set<String> hidden = scoreboardHiddenKeys.computeIfAbsent(islandId, k -> new HashSet<>());
        if (!hidden.remove(lineKey)) {
            hidden.add(lineKey);
        }
    }

    public void recordScoreboardLines(String islandId, List<String> lines) {
        Map<String, String> known = scoreboardKnownLines.computeIfAbsent(islandId, k -> new LinkedHashMap<>());
        for (String line : lines) {
            String key = scoreboardLineKey(line);
            if (!key.isBlank()) {
                known.put(key, line);
            }
        }
    }

    public Map<String, String> getScoreboardKnownLines(String islandId) {
        return scoreboardKnownLines.getOrDefault(islandId, new LinkedHashMap<>());
    }

    public Map<String, Set<String>> getScoreboardHiddenKeys() {
        return scoreboardHiddenKeys;
    }

    public boolean isScoreboardGlobalLineHidden(String lineKey) {
        return scoreboardGlobalHiddenKeys.contains(lineKey);
    }

    public void toggleScoreboardGlobalLine(String lineKey) {
        if (!scoreboardGlobalHiddenKeys.remove(lineKey)) {
            scoreboardGlobalHiddenKeys.add(lineKey);
        }
    }

    public Set<String> getScoreboardGlobalHiddenKeys() {
        return scoreboardGlobalHiddenKeys;
    }

    private String normalizeHudAccentColor(String value) {
        if (value == null) {
            return DEFAULT_HUD_ACCENT_COLOR;
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        if (!trimmed.matches("[0-9A-F]{6}")) {
            return DEFAULT_HUD_ACCENT_COLOR;
        }
        return "#" + trimmed;
    }
}
