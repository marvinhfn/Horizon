package de.horizon.config;

import de.horizon.Lang;
import de.horizon.feature.chat.ChatCopyMode;
import de.horizon.feature.fishing.ElusiveSeaCreature;
import de.horizon.feature.fishing.FishingAlertSound;
import de.horizon.feature.inventory.InventoryButton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HorizonConfig {
    final HudConfig hud;
    final DungeonConfig dungeon;
    final SpotifyConfig spotify;
    final YoutubeConfig youtube;
    final ChatConfig chat;
    final MiscConfig misc;
    final AntiSpamConfig antiSpam;
    final ParticleConfig particle;
    final ScoreboardConfig scoreboard;
    final InventoryButtonConfig inventoryButtons;
    final FishingConfig fishing;
    final DisplayConfig display;

    HorizonConfig(HudConfig hud, DungeonConfig dungeon, SpotifyConfig spotify, YoutubeConfig youtube, ChatConfig chat,
                  MiscConfig misc, AntiSpamConfig antiSpam, ParticleConfig particle, ScoreboardConfig scoreboard,
                  InventoryButtonConfig inventoryButtons, FishingConfig fishing, DisplayConfig display) {
        this.hud = hud;
        this.dungeon = dungeon;
        this.spotify = spotify;
        this.youtube = youtube;
        this.chat = chat;
        this.misc = misc;
        this.antiSpam = antiSpam;
        this.particle = particle;
        this.scoreboard = scoreboard;
        this.inventoryButtons = inventoryButtons;
        this.fishing = fishing;
        this.display = display;
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

    public Map<String, HudPosition> getHudPositions() { return hud.hudPositions; }

    // ── DUNGEON ───────────────────────────────────────────────────────────────

    public int getCatacombsLevel() { return dungeon.catacombsLevel; }
    public void setCatacombsLevel(int v) { dungeon.catacombsLevel = Math.max(0, Math.min(50, v)); }

    public boolean isDungeonPartyFinderOverlayEnabled() { return dungeon.dungeonPartyFinderOverlayEnabled; }
    public void setDungeonPartyFinderOverlayEnabled(boolean v) { dungeon.dungeonPartyFinderOverlayEnabled = v; }

    public boolean isDungeonRareRoomAlertsEnabled() { return dungeon.dungeonRareRoomAlertsEnabled; }
    public void setDungeonRareRoomAlertsEnabled(boolean v) { dungeon.dungeonRareRoomAlertsEnabled = v; }

    public boolean isHideNonStarredMobsEnabled() { return dungeon.hideNonStarredMobs; }
    public void setHideNonStarredMobsEnabled(boolean v) { dungeon.hideNonStarredMobs = v; }

    public boolean isHighlightStarredMobsEnabled() { return dungeon.highlightStarredMobs; }
    public void setHighlightStarredMobsEnabled(boolean v) { dungeon.highlightStarredMobs = v; }
    public int getStarredMobColor() { return dungeon.starredMobColor; }
    public void setStarredMobColor(int v) { dungeon.starredMobColor = v; }
    public boolean isHighlightBatsEnabled() { return dungeon.highlightBats; }
    public void setHighlightBatsEnabled(boolean v) { dungeon.highlightBats = v; }
    public int getBatHighlightColor() { return dungeon.batHighlightColor; }
    public void setBatHighlightColor(int v) { dungeon.batHighlightColor = v; }
    public boolean isHighlightFelsEnabled() { return dungeon.highlightFels; }
    public void setHighlightFelsEnabled(boolean v) { dungeon.highlightFels = v; }
    public int getFelHighlightColor() { return dungeon.felHighlightColor; }
    public void setFelHighlightColor(int v) { dungeon.felHighlightColor = v; }
    public boolean isTeammateGlowEnabled() { return dungeon.teammateGlowEnabled; }
    public void setTeammateGlowEnabled(boolean v) { dungeon.teammateGlowEnabled = v; }
    public int getClassColorArcher() { return dungeon.classColorArcher; }
    public void setClassColorArcher(int v) { dungeon.classColorArcher = v; }
    public int getClassColorBerserk() { return dungeon.classColorBerserk; }
    public void setClassColorBerserk(int v) { dungeon.classColorBerserk = v; }
    public int getClassColorHealer() { return dungeon.classColorHealer; }
    public void setClassColorHealer(int v) { dungeon.classColorHealer = v; }
    public int getClassColorMage() { return dungeon.classColorMage; }
    public void setClassColorMage(int v) { dungeon.classColorMage = v; }
    public int getClassColorTank() { return dungeon.classColorTank; }
    public void setClassColorTank(int v) { dungeon.classColorTank = v; }

    public int getClassColor(de.horizon.feature.dungeon.TeammateGlowService.DungeonClass dc) {
        return switch (dc) {
            case ARCHER  -> dungeon.classColorArcher;
            case BERSERK -> dungeon.classColorBerserk;
            case HEALER  -> dungeon.classColorHealer;
            case MAGE    -> dungeon.classColorMage;
            case TANK    -> dungeon.classColorTank;
        };
    }

    public void setClassColor(de.horizon.feature.dungeon.TeammateGlowService.DungeonClass dc, int color) {
        switch (dc) {
            case ARCHER  -> dungeon.classColorArcher = color;
            case BERSERK -> dungeon.classColorBerserk = color;
            case HEALER  -> dungeon.classColorHealer = color;
            case MAGE    -> dungeon.classColorMage = color;
            case TANK    -> dungeon.classColorTank = color;
        }
    }

    public boolean isRagAxeNotificationEnabled() { return dungeon.ragAxeNotificationEnabled; }
    public void setRagAxeNotificationEnabled(boolean v) { dungeon.ragAxeNotificationEnabled = v; }

    public boolean isWitherDoorEspEnabled() { return dungeon.witherDoorEspEnabled; }
    public void setWitherDoorEspEnabled(boolean v) { dungeon.witherDoorEspEnabled = v; }
    public int getWitherDoorColor() { return dungeon.witherDoorColor; }
    public void setWitherDoorColor(int v) { dungeon.witherDoorColor = v; }
    public boolean isBloodDoorEspEnabled() { return dungeon.bloodDoorEspEnabled; }
    public void setBloodDoorEspEnabled(boolean v) { dungeon.bloodDoorEspEnabled = v; }
    public int getBloodDoorColor() { return dungeon.bloodDoorColor; }
    public void setBloodDoorColor(int v) { dungeon.bloodDoorColor = v; }
    public boolean isDoorKeyHighlightEnabled() { return dungeon.doorKeyHighlightEnabled; }
    public void setDoorKeyHighlightEnabled(boolean v) { dungeon.doorKeyHighlightEnabled = v; }

    public boolean isLeapMenuEnabled() { return dungeon.leapMenuEnabled; }
    public void setLeapMenuEnabled(boolean v) { dungeon.leapMenuEnabled = v; }

    public boolean isLeapMenuAnnounce() { return dungeon.leapMenuAnnounce; }
    public void setLeapMenuAnnounce(boolean v) { dungeon.leapMenuAnnounce = v; }

    public int getLeapMenuSortMode() { return dungeon.leapMenuSortMode; }
    public void setLeapMenuSortMode(int v) { dungeon.leapMenuSortMode = Math.max(0, Math.min(2, v)); }

    public boolean isEtherwarpEnabled() { return dungeon.etherwarpEnabled; }
    public void setEtherwarpEnabled(boolean v) { dungeon.etherwarpEnabled = v; }

    public boolean isEtherwarpDepthCheck() { return dungeon.etherwarpDepthCheck; }
    public void setEtherwarpDepthCheck(boolean v) { dungeon.etherwarpDepthCheck = v; }

    public boolean isEtherwarpSoundEnabled() { return dungeon.etherwarpSoundEnabled; }
    public void setEtherwarpSoundEnabled(boolean v) { dungeon.etherwarpSoundEnabled = v; }

    public boolean isEtherwarpSneakOnly() { return dungeon.etherwarpSneakOnly; }
    public void setEtherwarpSneakOnly(boolean v) { dungeon.etherwarpSneakOnly = v; }

    public int getEtherwarpRenderStyle() { return dungeon.etherwarpRenderStyle; }
    public void setEtherwarpRenderStyle(int v) { dungeon.etherwarpRenderStyle = Math.max(0, Math.min(2, v)); }

    public int getEtherwarpSoundIndex() { return dungeon.etherwarpSoundIndex; }
    public void setEtherwarpSoundIndex(int v) { dungeon.etherwarpSoundIndex = Math.max(0, Math.min(1, v)); }

    public float getEtherwarpSoundVolume() { return dungeon.etherwarpSoundVolume; }
    public void setEtherwarpSoundVolume(float v) { dungeon.etherwarpSoundVolume = (float) Math.max(0.0, Math.min(2.0, v)); }

    public float getEtherwarpSoundPitch() { return dungeon.etherwarpSoundPitch; }
    public void setEtherwarpSoundPitch(float v) { dungeon.etherwarpSoundPitch = (float) Math.max(0.0, Math.min(2.0, v)); }

    public boolean isWardrobeKeybindsEnabled() { return dungeon.wardrobeKeybindsEnabled; }
    public void setWardrobeKeybindsEnabled(boolean v) { dungeon.wardrobeKeybindsEnabled = v; }

    public boolean isSlotBindsEnabled() { return dungeon.slotBindsEnabled; }
    public void setSlotBindsEnabled(boolean v) { dungeon.slotBindsEnabled = v; }

    public java.util.Map<Integer, Integer> getSlotBinds() { return dungeon.slotBinds; }

    public int getSlotBindKey() { return dungeon.slotBindKey; }
    public void setSlotBindKey(int v) { dungeon.slotBindKey = v; }

    public boolean isCommandShortcutsEnabled() { return dungeon.commandShortcutsEnabled; }
    public void setCommandShortcutsEnabled(boolean v) { dungeon.commandShortcutsEnabled = v; }

    public boolean isChatCommandsEnabled() { return dungeon.chatCommandsEnabled; }
    public void setChatCommandsEnabled(boolean v) { dungeon.chatCommandsEnabled = v; }

    public boolean isChatCommandsParty() { return dungeon.chatCommandsParty; }
    public void setChatCommandsParty(boolean v) { dungeon.chatCommandsParty = v; }

    public boolean isChatCommandsGuild() { return dungeon.chatCommandsGuild; }
    public void setChatCommandsGuild(boolean v) { dungeon.chatCommandsGuild = v; }

    public boolean isChatCommandsPrivate() { return dungeon.chatCommandsPrivate; }
    public void setChatCommandsPrivate(boolean v) { dungeon.chatCommandsPrivate = v; }

    public boolean isChatCommandEnabled(String cmd) { return !dungeon.chatCommandsDisabled.contains(cmd); }
    public void setChatCommandEnabled(String cmd, boolean v) {
        if (v) dungeon.chatCommandsDisabled.remove(cmd);
        else dungeon.chatCommandsDisabled.add(cmd);
    }

    public int getCommandKeybindPets() { return dungeon.commandKeybindPets; }
    public void setCommandKeybindPets(int v) { dungeon.commandKeybindPets = v; }

    public int getCommandKeybindEquipment() { return dungeon.commandKeybindEquipment; }
    public void setCommandKeybindEquipment(int v) { dungeon.commandKeybindEquipment = v; }

    public int getCommandKeybindWardrobe() { return dungeon.commandKeybindWardrobe; }
    public void setCommandKeybindWardrobe(int v) { dungeon.commandKeybindWardrobe = v; }

    public boolean isTickTimerEnabled() { return dungeon.tickTimerEnabled; }
    public void setTickTimerEnabled(boolean v) { dungeon.tickTimerEnabled = v; }
    public boolean isTickTimerSendToChat() { return dungeon.tickTimerSendToChat; }
    public void setTickTimerSendToChat(boolean v) { dungeon.tickTimerSendToChat = v; }

    public boolean isPuzzleSolverEnabled() { return dungeon.puzzleSolverEnabled; }
    public void setPuzzleSolverEnabled(boolean v) { dungeon.puzzleSolverEnabled = v; }
    public int getPuzzleSolverStyle() { return dungeon.puzzleSolverStyle; }
    public void setPuzzleSolverStyle(int v) { dungeon.puzzleSolverStyle = Math.max(0, Math.min(2, v)); }

    public boolean isTerminalSolverEnabled() { return dungeon.terminalSolverEnabled; }
    public void setTerminalSolverEnabled(boolean v) { dungeon.terminalSolverEnabled = v; }
    public boolean isTerminalSolverBlockWrongClicks() { return dungeon.terminalSolverBlockWrongClicks; }
    public void setTerminalSolverBlockWrongClicks(boolean v) { dungeon.terminalSolverBlockWrongClicks = v; }
    public boolean isTerminalSolverCustomMode() { return dungeon.terminalSolverCustomMode; }
    public void setTerminalSolverCustomMode(boolean v) { dungeon.terminalSolverCustomMode = v; }

    public boolean isSimonSaysEnabled() { return dungeon.simonSaysEnabled; }
    public void setSimonSaysEnabled(boolean v) { dungeon.simonSaysEnabled = v; }
    public boolean isSimonSaysBlockWrongClicks() { return dungeon.simonSaysBlockWrongClicks; }
    public void setSimonSaysBlockWrongClicks(boolean v) { dungeon.simonSaysBlockWrongClicks = v; }
    public boolean isArrowAlignEnabled() { return dungeon.arrowAlignEnabled; }
    public void setArrowAlignEnabled(boolean v) { dungeon.arrowAlignEnabled = v; }
    public boolean isSharpShooterEnabled() { return dungeon.sharpShooterEnabled; }
    public void setSharpShooterEnabled(boolean v) { dungeon.sharpShooterEnabled = v; }
    public boolean isPurplePadTimerEnabled() { return dungeon.purplePadTimerEnabled; }
    public void setPurplePadTimerEnabled(boolean v) { dungeon.purplePadTimerEnabled = v; }

    public boolean isDungeonMapEnabled() { return dungeon.dungeonMapEnabled; }
    public void setDungeonMapEnabled(boolean v) { dungeon.dungeonMapEnabled = v; }

    public int getMapColorBackground() { return dungeon.mapColorBackground; }
    public void setMapColorBackground(int v) { dungeon.mapColorBackground = v; }
    public int getMapColorNormal()   { return dungeon.mapColorNormal; }
    public void setMapColorNormal(int v) { dungeon.mapColorNormal = v; }
    public int getMapColorPuzzle()   { return dungeon.mapColorPuzzle; }
    public void setMapColorPuzzle(int v) { dungeon.mapColorPuzzle = v; }
    public int getMapColorTrap()     { return dungeon.mapColorTrap; }
    public void setMapColorTrap(int v) { dungeon.mapColorTrap = v; }
    public int getMapColorEntrance() { return dungeon.mapColorEntrance; }
    public void setMapColorEntrance(int v) { dungeon.mapColorEntrance = v; }
    public int getMapColorMiniboss() { return dungeon.mapColorMiniboss; }
    public void setMapColorMiniboss(int v) { dungeon.mapColorMiniboss = v; }
    public int getMapColorBlood()    { return dungeon.mapColorBlood; }
    public void setMapColorBlood(int v) { dungeon.mapColorBlood = v; }
    public int getMapColorRare()     { return dungeon.mapColorRare; }
    public void setMapColorRare(int v) { dungeon.mapColorRare = v; }

    public boolean isBloodCamperEnabled() { return dungeon.bloodCamperEnabled; }
    public void setBloodCamperEnabled(boolean v) { dungeon.bloodCamperEnabled = v; }

    public boolean isDungeonScoreEnabled() { return dungeon.dungeonScoreEnabled; }
    public void setDungeonScoreEnabled(boolean v) { dungeon.dungeonScoreEnabled = v; }

    public boolean isDragonEnabled() { return dungeon.dragonEnabled; }
    public void setDragonEnabled(boolean v) { dungeon.dragonEnabled = v; }
    public boolean isDragonBoxes() { return dungeon.dragonBoxes; }
    public void setDragonBoxes(boolean v) { dungeon.dragonBoxes = v; }
    public boolean isDragonTimer() { return dungeon.dragonTimer; }
    public void setDragonTimer(boolean v) { dungeon.dragonTimer = v; }
    public boolean isDragonSpawnAlert() { return dungeon.dragonSpawnAlert; }
    public void setDragonSpawnAlert(boolean v) { dungeon.dragonSpawnAlert = v; }
    public boolean isDragonPriority() { return dungeon.dragonPriority; }
    public void setDragonPriority(boolean v) { dungeon.dragonPriority = v; }
    public String getDragonSplitPrio() { return dungeon.dragonSplitPrio == null ? "ogrbp" : dungeon.dragonSplitPrio; }
    public void setDragonSplitPrio(String v) { dungeon.dragonSplitPrio = v == null ? "ogrbp" : v; }
    public String getDragonNoSplitPrio() { return dungeon.dragonNoSplitPrio == null ? "robpg" : dungeon.dragonNoSplitPrio; }
    public void setDragonNoSplitPrio(String v) { dungeon.dragonNoSplitPrio = v == null ? "robpg" : v; }

    public boolean isRelicTimerEnabled() { return dungeon.relicTimerEnabled; }
    public void setRelicTimerEnabled(boolean v) { dungeon.relicTimerEnabled = v; }

    public boolean isSpiritBearTimerEnabled() { return dungeon.spiritBearTimerEnabled; }
    public void setSpiritBearTimerEnabled(boolean v) { dungeon.spiritBearTimerEnabled = v; }
    public boolean isSpiritBearHighlightEnabled() { return dungeon.spiritBearHighlightEnabled; }
    public void setSpiritBearHighlightEnabled(boolean v) { dungeon.spiritBearHighlightEnabled = v; }
    public int getSpiritBearHighlightColor() { return dungeon.spiritBearHighlightColor; }
    public void setSpiritBearHighlightColor(int v) { dungeon.spiritBearHighlightColor = v; }

    public float getTerminalGuiScale() { return dungeon.terminalGuiScale; }
    public void setTerminalGuiScale(float v) { dungeon.terminalGuiScale = (float) clamp(v, 0.5, 3.0); }

    // ── SPOTIFY ───────────────────────────────────────────────────────────────

    public boolean isSpotifyInventoryControlsEnabled() { return spotify.spotifyInventoryControlsEnabled; }
    public void setSpotifyInventoryControlsEnabled(boolean v) { spotify.spotifyInventoryControlsEnabled = v; }

    public String getActiveMusicService() { return spotify.activeMusicService == null || spotify.activeMusicService.isBlank() ? "SPOTIFY" : spotify.activeMusicService; }
    public void setActiveMusicService(String v) { spotify.activeMusicService = v == null ? "SPOTIFY" : v; }

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

    // ── YOUTUBE MUSIC ─────────────────────────────────────────────────────────

    public int getYoutubeRedirectPort() { return youtube.youtubeRedirectPort; }
    public void setYoutubeRedirectPort(int v) { youtube.youtubeRedirectPort = Math.max(1024, Math.min(65535, v)); }

    public String getYoutubeAccessToken() { return youtube.youtubeAccessToken == null ? "" : youtube.youtubeAccessToken; }
    public void setYoutubeAccessToken(String v) { youtube.youtubeAccessToken = v == null ? "" : v; }

    public String getYoutubeRefreshToken() { return youtube.youtubeRefreshToken == null ? "" : youtube.youtubeRefreshToken; }
    public void setYoutubeRefreshToken(String v) { youtube.youtubeRefreshToken = v == null ? "" : v; }

    public long getYoutubeTokenExpiresAt() { return youtube.youtubeTokenExpiresAt; }
    public void setYoutubeTokenExpiresAt(long v) { youtube.youtubeTokenExpiresAt = v; }

    public String getYoutubeConnectedAccount() { return youtube.youtubeConnectedAccount == null ? "" : youtube.youtubeConnectedAccount; }
    public void setYoutubeConnectedAccount(String v) { youtube.youtubeConnectedAccount = v == null ? "" : v; }

    // ── CHAT ──────────────────────────────────────────────────────────────────

    public String getChatBridgeBotName() {
        return chat.chatBridgeBotName == null || chat.chatBridgeBotName.isBlank() ? "catgirlfc" : chat.chatBridgeBotName.trim();
    }
    public void setChatBridgeBotName(String v) { chat.chatBridgeBotName = v == null ? "catgirlfc" : v.trim(); }

    public boolean isChatBridgeHidden() { return chat.chatBridgeHidden; }
    public void setChatBridgeHidden(boolean v) { chat.chatBridgeHidden = v; }

    public boolean isGuildChatHidden() { return chat.guildChatHidden; }
    public void setGuildChatHidden(boolean v) { chat.guildChatHidden = v; }

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

    public boolean isHideBossMessages() { return antiSpam.hideBossMessages; }
    public void setHideBossMessages(boolean v) { antiSpam.hideBossMessages = v; }

    public boolean isHideWarpingMessages() { return antiSpam.hideWarpingMessages; }
    public void setHideWarpingMessages(boolean v) { antiSpam.hideWarpingMessages = v; }

    public boolean isHideSendingToServerMessages() { return antiSpam.hideSendingToServerMessages; }
    public void setHideSendingToServerMessages(boolean v) { antiSpam.hideSendingToServerMessages = v; }

    public boolean isHideProfileMessages() { return antiSpam.hideProfileMessages; }
    public void setHideProfileMessages(boolean v) { antiSpam.hideProfileMessages = v; }

    public boolean isHideGuildJoinLeaveMessages() { return antiSpam.hideGuildJoinLeaveMessages; }
    public void setHideGuildJoinLeaveMessages(boolean v) { antiSpam.hideGuildJoinLeaveMessages = v; }

    public boolean isHideFiresaleMessages() { return antiSpam.hideFiresaleMessages; }
    public void setHideFiresaleMessages(boolean v) { antiSpam.hideFiresaleMessages = v; }

    public boolean isHideRadioSignalMessages() { return antiSpam.hideRadioSignalMessages; }
    public void setHideRadioSignalMessages(boolean v) { antiSpam.hideRadioSignalMessages = v; }

    public boolean isHideSacksMessages() { return antiSpam.hideSacksMessages; }
    public void setHideSacksMessages(boolean v) { antiSpam.hideSacksMessages = v; }

    public boolean isHideSeaCreatureMessages() { return antiSpam.hideSeaCreatureMessages; }
    public void setHideSeaCreatureMessages(boolean v) { antiSpam.hideSeaCreatureMessages = v; }

    public boolean isHideElusiveSeaCreatureMessages() { return antiSpam.hideElusiveSeaCreatureMessages; }
    public void setHideElusiveSeaCreatureMessages(boolean v) { antiSpam.hideElusiveSeaCreatureMessages = v; }

    public boolean isHideTrophyFishMessages() { return antiSpam.hideTrophyFishMessages; }
    public void setHideTrophyFishMessages(boolean v) { antiSpam.hideTrophyFishMessages = v; }

    public boolean isHideTrophyFrogMessages() { return antiSpam.hideTrophyFrogMessages; }
    public void setHideTrophyFrogMessages(boolean v) { antiSpam.hideTrophyFrogMessages = v; }

    public boolean isHideFishingDiamondTrophies() { return antiSpam.hideFishingDiamondTrophies; }
    public void setHideFishingDiamondTrophies(boolean v) { antiSpam.hideFishingDiamondTrophies = v; }

    public boolean isHideGoodGreatOutstandingMessages() { return antiSpam.hideGoodGreatOutstandingMessages; }
    public void setHideGoodGreatOutstandingMessages(boolean v) { antiSpam.hideGoodGreatOutstandingMessages = v; }

    // ── PARTICLE ──────────────────────────────────────────────────────────────

    public boolean isBreakParticlesEnabled() { return particle.breakParticlesEnabled; }
    public void setBreakParticlesEnabled(boolean v) { particle.breakParticlesEnabled = v; }
    public Map<String, Boolean> getParticleStates() { return particle.particleStates; }

    // ── INVENTORY BUTTONS ─────────────────────────────────────────────────────

    public boolean isInventoryButtonsEnabled() { return inventoryButtons.inventoryButtonsEnabled; }
    public void setInventoryButtonsEnabled(boolean v) { inventoryButtons.inventoryButtonsEnabled = v; }

    public List<InventoryButton> getInventoryButtons() { return inventoryButtons.buttons; }

    // ── FISHING ───────────────────────────────────────────────────────────────

    public boolean isFishingRareAlertEnabled() { return fishing.fishingRareAlertEnabled; }
    public void setFishingRareAlertEnabled(boolean v) { fishing.fishingRareAlertEnabled = v; }

    public FishingAlertSound getFishingAlertSound() {
        return fishing.fishingAlertSound == null ? FishingAlertSound.RARE : fishing.fishingAlertSound;
    }
    public void setFishingAlertSound(FishingAlertSound v) {
        fishing.fishingAlertSound = v == null ? FishingAlertSound.RARE : v;
    }

    public boolean isFishingCreatureEnabled(String id) { return !fishing.disabledCreatures.contains(id); }
    public void toggleFishingCreature(String id) {
        if (!fishing.disabledCreatures.remove(id)) {
            fishing.disabledCreatures.add(id);
        }
    }

    public int fishingDisabledCount() {
        int count = 0;
        for (ElusiveSeaCreature c : ElusiveSeaCreature.values()) {
            if (!isFishingCreatureEnabled(c.id())) count++;
        }
        return count;
    }

    // ── SCOREBOARD ────────────────────────────────────────────────────────────

    public boolean isCustomScoreboardEnabled() { return scoreboard.customScoreboardEnabled; }
    public void setCustomScoreboardEnabled(boolean v) { scoreboard.customScoreboardEnabled = v; }

    public Map<String, String> getScoreboardKnownLines(String islandId) {
        return scoreboard.getKnownLines(islandId);
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
            case "archer":      return "Archer";
            case "mage":        return "Mage";
            case "tank":        return "Tank";
            case "berserk":         return "Berserk";
            case "healer":          return "Healer";
            case "www.hypixel.net": return "www.hypixel.net";
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
            case "archer":                                          return 0xFFFF5555; // red
            case "mage":                                            return 0xFF55FFFF; // aqua
            case "tank":                                            return 0xFF55FF55; // green
            case "berserk":                                         return 0xFFFFAA00; // gold
            case "healer":                                          return 0xFFFF55FF; // light purple
            case "profile": case "skills": case "class":            return 0xFFAAAAAA; // gray
            case "season": case "time": case "server_code":
            case "timer": case "date":                              return 0xFFAAAAAA; // gray
            default:                                                return 0xFFFFFFFF; // white
        }
    }

    // ── DISPLAY ───────────────────────────────────────────────────────────────

    public boolean isPillarboxEnabled() { return display.pillarboxEnabled; }
    public void setPillarboxEnabled(boolean v) { display.pillarboxEnabled = v; }

    public double getItemPositionX() { return display.animation.itemPositionX; }
    public void setItemPositionX(double v) { display.animation.itemPositionX = clamp(v, -1.5, 1.5); }

    public double getItemPositionY() { return display.animation.itemPositionY; }
    public void setItemPositionY(double v) { display.animation.itemPositionY = clamp(v, -1.5, 1.5); }

    public double getItemPositionZ() { return display.animation.itemPositionZ; }
    public void setItemPositionZ(double v) { display.animation.itemPositionZ = clamp(v, -1.5, 1.5); }

    public double getItemScale() { return display.animation.itemScale; }
    public void setItemScale(double v) { display.animation.itemScale = clamp(v, 0.1, 2.0); }

    public double getSwingSpeed() { return display.animation.swingSpeed; }
    public void setSwingSpeed(double v) { display.animation.swingSpeed = clamp(v, 0.1, 4.0); }

    public boolean isFireOverlayDisabled() { return display.fireOverlayDisabled; }
    public void setFireOverlayDisabled(boolean v) { display.fireOverlayDisabled = v; }

    public float getHurtCamIntensity() { return display.hurtCamIntensity; }
    public void setHurtCamIntensity(float v) { display.hurtCamIntensity = (float) clamp(v, 0.0, 1.0); }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ── UTILITY ───────────────────────────────────────────────────────────────

    private static final Pattern P_COLOR_CODE       = Pattern.compile("§[0-9a-zA-Z]");
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
    private static final Pattern P_FIRST_ALPHA      = Pattern.compile("[a-z]+");

    public static String scoreboardLineKey(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String clean = P_COLOR_CODE.matcher(line).replaceAll("").trim();
        if (clean.contains("⏣")) {
            return "location";
        }
        // Dungeon floor line: "The Catacombs M7", "The Catacombs - Floor VII", etc.
        if (clean.toLowerCase(Locale.ROOT).startsWith("the catacombs")) {
            return "the catacombs";
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
        // Dungeon team-member HP lines contain ❤ with a numeric HP value.
        // Map them to a stable class key so player names never pollute the config.
        if (clean.contains("❤")) {
            String lower = clean.toLowerCase(Locale.ROOT);
            Matcher m = P_FIRST_ALPHA.matcher(lower);
            if (m.find()) {
                switch (m.group()) {
                    case "archer":  return "archer";
                    case "mage":    return "mage";
                    case "tank":    return "tank";
                    case "berserk": case "bers": return "berserk";
                    case "healer":  return "healer";
                }
            }
            return ""; // HP line without identifiable class → skip
        }
        // All "Plot" lines (Plot - 2, Plot - 19, etc.) collapse to a single stable key
        if (cleanLower.startsWith("plot")) {
            return "plot";
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
