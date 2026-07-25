package de.horizon.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DungeonConfig {
    int catacombsLevel = 0;
    boolean dungeonPartyFinderOverlayEnabled = true;
    boolean dungeonRareRoomAlertsEnabled = true;
    boolean hideNonStarredMobs = false;
    boolean highlightStarredMobs = false;
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

    // Door ESP
    boolean witherDoorEspEnabled = true;
    int witherDoorColor = 0xFF1A1A1A;   // near-black (coal)
    boolean bloodDoorEspEnabled = true;
    int bloodDoorColor = 0xFFCC0000;    // dark red
    boolean doorKeyHighlightEnabled = true;
    // Door outline colours by key state (defaults = the values DoorEspService used to hardcode).
    int doorColorHasKey = 0xFF00CC00;   // key collected (green)
    int doorColorNoKey = 0xFFCC0000;    // no key (red)

    // Leap Menu
    boolean leapMenuEnabled = true;
    boolean leapMenuAnnounce = false;
    int leapMenuSortMode = 0; // 0=Quadrant, 1=A-Z Class, 2=A-Z Name

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

    // Command Shortcuts (/f1-/f7, /m1-/m7, /d, /dh)
    boolean commandShortcutsEnabled = true;

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

    // Terminal Solver (custom overlay)
    boolean terminalSolverEnabled = false;
    boolean terminalSolverBlockWrongClicks = false;
    boolean terminalShowNumbers = true;      // Numbers terminal: draw the click count on each slot
    int terminalSlotStyle = 0;               // 0=Rect, 1=Bordered-Rect, 2=Button
    boolean terminalUseHudColor = true;      // derive all solver colours from the HUD accent color (auto offsets)
    // Terminal overlay colours (ARGB hex; alpha matters)
    int termColorSolution        = 0x8200FF00; // generic solution (green, a=130)
    int termColorNumbers1        = 0x8200FF00; // 1st click
    int termColorNumbers2        = 0x8200C800; // 2nd click
    int termColorNumbers3        = 0x82009600; // 3rd click
    int termColorRubixPos        = 0x820072FF; // rubix positive (+)
    int termColorRubixNeg        = 0x82CD0000; // rubix negative (-)
    int termColorMelodyColumn    = 0x82FF00FF; // melody target column
    int termColorMelodyIndicator = 0x82FF7400; // melody current indicator
    int termColorMelodyWrong     = 0x82FF0000; // melody wrong slots
    int termColorBackground      = 0x64000000; // menu background (a=100)
    int termColorBorder          = 0xFFFFFFFF; // menu border
    int termColorTitle           = 0xFFFFFFFF; // title text
    int termColorOverlayText     = 0xFFFFFFFF; // slot overlay text
    // Terminal Waypoints & Titles (in-world)
    boolean terminalWaypointsEnabled = false;
    boolean terminalTitleEnabled = false;

    // Boss Solver
    boolean simonSaysEnabled = true;
    boolean simonSaysBlockWrongClicks = false;
    boolean arrowAlignEnabled = true;
    int arrowAlignColorStyle = 0;             // 0=Dynamic (green/orange/red by clicks), 1=Custom
    int arrowAlignTextColor = 0xFFFFFFFF;     // custom text colour
    boolean arrowAlignBlockWrongClicks = false;
    boolean arrowAlignInvertSneak = false;
    boolean sharpShooterEnabled = true;
    boolean sharpShooterDoneEnabled = true;   // show the "Done" world-text when I4 is complete
    boolean sharpShooterDoneTitleEnabled = true; // also flash a screen title when I4 completes
    int sharpShooterDoneColor = 0xFF55FF55;   // "Done" text colour (default green)
    float sharpShooterDoneScale = 4.0f;       // "Done" text size multiplier (1.0 = old baseline)
    boolean purplePadTimerEnabled = true;

    // Dungeon Map
    boolean dungeonMapEnabled = false;
    boolean mapShowRoomNames = false;
    boolean mapShowCheckmarks = true;
    boolean mapShowPlayerHeads = true;
    boolean mapShowPlayerNames = false;
    boolean mapShowSecretCount = true;

    // Dungeon Map Colors (ARGB hex)
    int mapColorBackground = 0xCC000000; // semi-transparent black
    int mapColorNormal    = 0xFF999999; // gray
    int mapColorPuzzle    = 0xFFCC00CC; // magenta
    int mapColorTrap      = 0xFFFF8800; // orange
    int mapColorEntrance  = 0xFF00CC00; // green
    int mapColorMiniboss  = 0xFFFFFF00; // yellow
    int mapColorBlood     = 0xFFFF0000; // red
    int mapColorRare      = 0xFF00FFFF; // cyan

    // Dungeon Map room-name colours by state (ARGB hex)
    int mapColorNameUncleared = 0xFFAAAAAA; // discovered, not cleared
    int mapColorNameCleared   = 0xFFFFFFFF; // white check (starred mobs cleared)
    int mapColorNameSecrets   = 0xFF55FF55; // green check (all secrets found)

    // Secret Waypoints
    boolean secretWaypointsEnabled = true;
    boolean secretShowChest = true;
    boolean secretShowItem = true;
    boolean secretShowEssence = true;
    boolean secretShowBat = true;
    boolean secretShowRedstone = true;
    boolean secretShowLever = true;
    boolean secretWaypointsThroughWalls = true;
    // Waypoint colours per category (ARGB hex)
    int secretColorChest    = 0xFFFFAA00;
    int secretColorItem     = 0xFF55FFFF;
    int secretColorEssence  = 0xFFAA00AA;
    int secretColorBat      = 0xFFFF5555;
    int secretColorRedstone = 0xFFAA0000;
    int secretColorLever    = 0xFFFFFF55;

    // Blood Camper
    boolean bloodCamperEnabled = true;

    // Dungeon Score
    boolean dungeonScoreEnabled = true;
    boolean dungeonScoreTitle = true; // Title bei S / S+ (270 / 300)
    boolean dungeonScoreShowInBoss = false; // true = Anzeige bleibt im Bosskampf sichtbar; false = verschwindet beim Boss-Start

    // M7 Dragons
    boolean dragonEnabled = true;
    boolean dragonBoxes = true;
    boolean dragonTimer = true;
    boolean dragonSpawnAlert = true;
    boolean dragonPriority = true;
    boolean dragonHealth = true;     // show each dragon's health above its box
    boolean dragonTracer = true;     // draw a tracer line to the priority dragon
    String dragonSplitPrio = "ogrbp";   // priority letters: o=orange, g=green, r=red, b=blue, p=purple
    String dragonNoSplitPrio = "robpg";

    // Relic Timer
    boolean relicTimerEnabled = true;

    // Spirit Bear (F4/M4)
    boolean spiritBearTimerEnabled = true;
    boolean spiritBearHighlightEnabled = true;
    int spiritBearHighlightColor = 0xFF00FFAA;  // aqua-green

    // Mimic Detection
    boolean mimicDetectionEnabled = true;
    boolean mimicMessageEnabled = true;
    boolean princeMessageEnabled = true;

    // Terminal Solver
    float terminalGuiScale = 1.0f;
}
