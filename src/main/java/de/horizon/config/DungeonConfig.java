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
    String leapMenuMessage = "[HRZN] Watch the sunset with me {playername}!";
    int leapMenuSortMode = 0; // 0=Quadrant, 1=A-Z Class, 2=A-Z Name

    // Etherwarp Helper
    boolean etherwarpEnabled = true;
    boolean etherwarpDepthCheck = false;
    boolean etherwarpSoundEnabled = true;
    boolean etherwarpSneakOnly = true;
    int etherwarpRenderStyle = 2; // 0=Filled, 1=Outline, 2=FilledOutline
    int etherwarpSoundIndex = 0;  // 0=Experience Orb Pickup, 1=Chorus Fruit
    float etherwarpSoundVolume = 0.5f;
    float etherwarpSoundPitch = 1.0f;

    // Wardrobe Keybinds
    boolean wardrobeKeybindsEnabled = true;
    boolean loadoutKeybindsEnabled = true;

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
    int commandKeybindLoadouts = -1;
    int commandKeybindStats = -1;

    // User-defined command keybinds (own command text + key)
    public static final class CustomCmd {
        public String command = "";
        public int key = -1;
    }
    java.util.List<CustomCmd> customCommandKeybinds = new java.util.ArrayList<>();

    // Tick Timer
    boolean tickTimerEnabled = true;
    boolean tickTimerSendToChat = false;
    boolean tickTimerMaxor = true;
    boolean tickTimerStorm = true;
    boolean tickTimerGoldor = true;
    boolean tickTimerNecron = true;

    // Puzzle Solver
    boolean puzzleSolverEnabled = true;
    int puzzleSolverStyle = 2; // 0=Filled, 1=Outline, 2=FilledOutline
    boolean puzzleBlockWrongClicks = true; // cancel right-clicks on wrong quiz answer blocks

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
    boolean simonSaysInvertSneak = false; // sneak overrides the block (default: sneak = allow click)
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
    boolean padTimerEnabled = true;

    // Configurable action sounds. Default = a note-block sound; enabled by default.
    public static final class CustomSound {
        public String sound = "block.note_block.pling";
        public float volume = 1.0f;
        public float pitch = 1.0f;
        public boolean enabled = true;
        public CustomSound() {}
        public CustomSound(String s, float v, float p) { this.sound = s; this.volume = v; this.pitch = p; }
    }
    CustomSound secretSound = new CustomSound();
    CustomSound terminalClickSound = new CustomSound();
    CustomSound simonSaysSound = new CustomSound();
    CustomSound leverSound = new CustomSound();
    CustomSound arrowAlignSound = new CustomSound();
    CustomSound sharpShooterSound = new CustomSound();
    CustomSound etherwarpSound = new CustomSound(); // default note_block.pling (see CustomSound ctor)

    // Melody terminal progress announce ({%} = progress, {coords} = player coords)
    boolean melodyAnnounceEnabled = false;
    String melodyAnnounceMessage = "Melody {%}";

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
    boolean secretWaypointText = true; // draw the category label text above each waypoint box
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
    boolean blessingHudEnabled = false;
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
    // Power-based priority (used when dragonPriority is on)
    float dragonNormalPower = 0f;       // power threshold for the "power" order
    float dragonEasyPower = 0f;         // lower threshold, applies when a Purple/Soul is in the split
    int dragonSoloDebuff = 0;           // who takes the purple solo-debuff: 0 = Tank, 1 = Healer
    boolean dragonSoloDebuffOnAll = true;

    // Relic Timer
    boolean relicTimerEnabled = true;
    boolean relicPlaceTimerEnabled = true;  // chat report of how long each relic took to place

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
