package de.horizon.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DungeonConfig {
    int catacombsLevel = 0;
    boolean dungeonPartyFinderOverlayEnabled = true;
    boolean dungeonRareRoomAlertsEnabled = true;
    boolean hideNonStarredMobs = false;
    boolean highlightStarredMobs = false;
    boolean starredMobGlowThroughWalls = true;
    int starredMobColor = 0xFFFFFF00;  // yellow
    boolean highlightBats = true;
    int batHighlightColor = 0xFF00FF00;  // green
    boolean highlightFels = false;
    int felHighlightColor = 0xFFFFAACC;  // pink
    boolean teammateGlowEnabled = true;
    int classColorArcher  = 0xFFAA0000;  // Dark Red
    int classColorBerserk = 0xFFFFAA00;  // Gold
    int classColorHealer  = 0xFFAA00AA;  // Dark Purple
    int classColorMage    = 0xFF00AAAA;  // Dark Aqua
    int classColorTank    = 0xFF00AA00;  // Dark Green
    boolean ragAxeNotificationEnabled = true;

    // Leap Menu
    boolean leapMenuEnabled = true;
    boolean leapMenuAnnounce = false;
    int leapMenuSortMode = 0; // 0=Odin, 1=A-Z Class, 2=A-Z Name

    // Etherwarp Helper
    boolean etherwarpEnabled = true;
    boolean etherwarpDepthCheck = false;
    boolean etherwarpSoundEnabled = true;
    boolean etherwarpSneakOnly = true;
    int etherwarpRenderStyle = 2; // 0=Filled, 1=Outline, 2=FilledOutline
    int etherwarpSoundIndex = 0;  // 0=Ender Dragon Hurt, 1=Chorus Fruit
    float etherwarpSoundVolume = 1.0f;
    float etherwarpSoundPitch = 0.53968257f;

    // Wardrobe Keybinds
    boolean wardrobeKeybindsEnabled = true;

    // Slot Binds
    boolean slotBindsEnabled = true;
    Map<Integer, Integer> slotBinds = new LinkedHashMap<>();
    int slotBindKey = -1; // GLFW key code, -1 = none

    // Chat Commands
    boolean chatCommandsEnabled = true;
    boolean chatCommandsParty = true;
    boolean chatCommandsGuild = true;
    boolean chatCommandsPrivate = false;
    java.util.Set<String> chatCommandsDisabled = new java.util.HashSet<>();

    // Command Keybinds (GLFW key codes, -1 = none)
    int commandKeybindPets = -1;
    int commandKeybindEquipment = -1;
    int commandKeybindWardrobe = -1;

    // Tick Timer
    boolean tickTimerEnabled = true;
    boolean tickTimerSendToChat = false;

    // Puzzle Solver
    boolean puzzleSolverEnabled = true;
    int puzzleSolverStyle = 2; // 0=Filled, 1=Outline, 2=FilledOutline

    // Terminal Solver
    boolean terminalSolverEnabled = false;
    boolean terminalSolverBlockWrongClicks = false;
    boolean terminalSolverCustomMode = false; // true = fully hide non-relevant slots

    // Boss Solver
    boolean simonSaysEnabled = true;
    boolean simonSaysBlockWrongClicks = false;
    boolean arrowAlignEnabled = true;
    boolean sharpShooterEnabled = true;
    boolean purplePadTimerEnabled = true;

    // Dungeon Map
    boolean dungeonMapEnabled = false;

    // Dungeon Map Colors (ARGB hex)
    int mapColorBackground = 0xCC000000; // semi-transparent black
    int mapColorNormal    = 0xFF999999; // gray
    int mapColorPuzzle    = 0xFFCC00CC; // magenta
    int mapColorTrap      = 0xFFFF8800; // orange
    int mapColorEntrance  = 0xFF00CC00; // green
    int mapColorMiniboss  = 0xFFFFFF00; // yellow
    int mapColorBlood     = 0xFFFF0000; // red
    int mapColorRare      = 0xFF00FFFF; // cyan

    // Blood Camper
    boolean bloodCamperEnabled = true;

    // Dungeon Score
    boolean dungeonScoreEnabled = true;

    // M7 Dragons
    boolean dragonEnabled = true;
    boolean dragonBoxes = true;
    boolean dragonTimer = true;
    boolean dragonSpawnAlert = true;
    boolean dragonPriority = true;
    String dragonSplitPrio = "ogrbp";   // priority letters: o=orange, g=green, r=red, b=blue, p=purple
    String dragonNoSplitPrio = "robpg";

    // Relic Timer
    boolean relicTimerEnabled = true;

    // Terminal Solver
    float terminalGuiScale = 1.0f;
}
