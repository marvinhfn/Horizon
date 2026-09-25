package de.horizon.screen;

import java.util.ArrayList;
import java.util.List;
import de.horizon.screen.HorizonConfigScreen.Tab;
import de.horizon.screen.HorizonConfigScreen.DungeonSection;

public final class FeatureRegistry {
    private static final List<FeatureEntry> ENTRIES = new ArrayList<>();
    private FeatureRegistry() {}

    private static void add(String de, String en, String loc, Tab tab, DungeonSection sec, String kwDe, String kwEn) {
        ENTRIES.add(new FeatureEntry(de, en, loc, tab, sec, kwDe, kwEn));
    }

    static {
        // ── GENERAL ────────────────────────────────────────────────────────────────
        add("Sprache", "Language", "General", Tab.GENERAL, null,
            "sprache language deutsch englisch umschalten",
            "language german english switch toggle");
        add("Config Reload", "Config Reload", "General", Tab.GENERAL, null,
            "config reload laden konfiguration",
            "config reload configuration disk");

        // ── HUD ────────────────────────────────────────────────────────────────────
        add("Theme", "Theme", "HUD", Tab.HUD, null,
            "theme farbe hell dunkel rose farbschema",
            "theme color light dark rose family");
        add("Dunkel-Modus", "Dark Mode", "HUD", Tab.HUD, null,
            "dunkel dark mode hell modus theme",
            "dark light mode theme toggle");
        add("HUD bearbeiten", "Edit HUD", "HUD", Tab.HUD, null,
            "hud bearbeiten layout reset positionen",
            "hud edit layout reset positions");
        add("HUD Farbe", "HUD Color", "HUD", Tab.HUD, null,
            "hud farbe accent akzent hex farbwahl",
            "hud color accent hex picker");

        // ── MUSIC CONTROL ──────────────────────────────────────────────────────────
        add("Aktiver Dienst", "Active Service", "Music Control / General", Tab.MUSIC_CONTROL, null,
            "aktiver dienst spotify youtube music service",
            "active service spotify youtube music");
        add("Music Control HUD", "Music Control HUD", "Music Control / General", Tab.MUSIC_CONTROL, null,
            "music control hud inventarsteuerung inventar controls",
            "music control hud inventory controls");
        add("Music HUD", "Music HUD", "Music Control / General", Tab.MUSIC_CONTROL, null,
            "music hud song cover album fortschritt progress play pause ingame spotify",
            "music hud song cover album progress play pause ingame spotify");
        add("Spotify Login", "Spotify Login", "Music Control / Spotify", Tab.MUSIC_CONTROL, null,
            "spotify login logout verbinden authentifizierung",
            "spotify login logout connect authenticate");
        add("YouTube Login", "YouTube Login", "Music Control / Youtube Music", Tab.MUSIC_CONTROL, null,
            "youtube login logout verbinden google musik",
            "youtube login logout connect google music");

        // ── DUNGEON / GENERAL ──────────────────────────────────────────────────────
        add("Party Finder Overlay", "Party Finder Overlay", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL,
            "party finder overlay dungeon general beste zeiten",
            "party finder overlay dungeon general best times");
        add("Rare Room Alerts", "Rare Room Alerts", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL,
            "rare room alerts trinity tomioka duncan dungeon benachrichtigung",
            "rare room alerts trinity tomioka duncan dungeon notification");

        // ── DUNGEON / MOBS ─────────────────────────────────────────────────────────
        add("Non-Starred Mobs verstecken", "Hide Non-Starred Mobs", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "non starred mobs verstecken nametags ohne stern dungeon",
            "hide non starred mobs nametags without star dungeon");
        add("Starred Mobs highlighten", "Highlight Starred Mobs", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "starred mobs highlight glow stern dungeon markieren",
            "starred mobs highlight glow star dungeon");
        add("Starred Mob Farbe", "Starred Mob Color", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "starred mob farbe farbe color glow dungeon",
            "starred mob color glow dungeon");
        add("Fledermaeuse highlighten", "Highlight Bats", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "bats fledermaeuse highlight dungeon markieren",
            "highlight bats dungeon mark");
        add("Fledermaus Farbe", "Bat Color", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "fledermaus farbe bat color dungeon",
            "bat color dungeon");
        add("Fels highlighten", "Highlight Fels", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "fels enderman invisible unsichtbar highlight dungeon",
            "fels enderman invisible highlight dungeon");
        add("Fel Farbe", "Fel Color", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "fel farbe enderman color dungeon",
            "fel color enderman dungeon");
        add("Teammate Glow", "Teammate Glow", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "teammate glow dungeon party klasse archer berserk healer mage tank",
            "teammate glow dungeon party class archer berserk healer mage tank");
        add("Teamkameraden-Farben", "Teammate Colors", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "teamkameraden farben archer berserk healer mage tank klasse farbe",
            "teammate colors archer berserk healer mage tank class color");
        add("Mimic Erkennung", "Mimic Detection", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "mimic erkennung kill zombie baby dungeon f6 f7",
            "mimic detection kill zombie baby dungeon f6 f7");
        add("Mimic Nachricht", "Mimic Message", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "mimic nachricht party chat message killed dungeon",
            "mimic message party chat killed dungeon");
        add("Prince Nachricht", "Prince Message", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS,
            "prince nachricht party chat message killed dungeon bonus score",
            "prince message party chat killed dungeon bonus score");

        // ── DUNGEON / REVIVAL ──────────────────────────────────────────────────────
        add("Revive HUD", "Revive HUD", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "revive hud spirit bonzo phoenix panel",
            "revive hud spirit bonzo phoenix panel");
        add("Catacombs Level", "Catacombs Level", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "catacombs level revive dungeon",
            "catacombs level revive dungeon");
        add("Boss Only", "Boss Only", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "boss only revive nur bosskampf dungeon",
            "boss only revive boss fight dungeon");
        add("Always Visible", "Always Visible", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "always visible revive immer sichtbar dungeon",
            "always visible revive dungeon");
        // ReviveSource entries (static expansion)
        add("Spirit Mask", "Spirit Mask", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "spirit mask revive hud dungeon",
            "spirit mask revive hud dungeon");
        add("Bonzo Mask", "Bonzo Mask", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "bonzo mask revive hud dungeon",
            "bonzo mask revive hud dungeon");
        add("Phoenix Pet", "Phoenix Pet", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL,
            "phoenix pet revive hud dungeon",
            "phoenix pet revive hud dungeon");

        // ── DUNGEON / MAP ──────────────────────────────────────────────────────────
        add("Dungeon Map", "Dungeon Map", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "dungeon map minimap karte dungeon layout groesse",
            "dungeon map minimap scale layout");
        add("Raumnamen", "Room Names", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "raumnamen karte map dungeon zeigt",
            "room names map dungeon shows");
        add("Checkmarks", "Checkmarks", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "checkmarks haken erkundete raeume dungeon karte",
            "checkmarks explored rooms dungeon map");
        add("Spielerkoepfe", "Player Heads", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "spielerkoepfe klassenfarbe karte dungeon kopf",
            "player heads class color map dungeon");
        add("Spielernamen", "Player Names", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "spielernamen karte map dungeon namen",
            "player names map dungeon");
        add("Secret-Anzahl", "Secret Count", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "secret anzahl karte raum dungeon secrets",
            "secret count map room dungeon");
        add("Kartenfarben", "Map Colors", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "kartenfarben hintergrund normal puzzle trap eingang miniboss blood rare farbe",
            "map colors background normal puzzle trap entrance miniboss blood rare color");
        add("Raumnamen-Farben", "Room Name Colors", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "raumnamen farben ungecleart gecleart secrets farbe karte",
            "room name colors uncleared cleared secrets map color");
        add("Leap Menu", "Leap Menu", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "leap menu spirit leap quadrant gui dungeon teleport",
            "leap menu spirit leap quadrant gui dungeon teleport");
        add("Ansage im Party-Chat", "Announce in Party Chat", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "ansage party chat leap dungeon ankuendigen ziel",
            "announce party chat leap dungeon destination");
        add("Leap-Nachricht", "Leap Message", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "leap nachricht message template playername party chat",
            "leap message template playername party chat");
        add("Sortierung", "Sort Mode", "Dungeons / Map", Tab.DUNGEON, DungeonSection.MAP,
            "sortierung klasse quadrant alphabetisch leap dungeon",
            "sort mode class quadrant alphabetical leap dungeon");

        // ── DUNGEON / SECRETS (Clear) ──────────────────────────────────────────────
        add("Secret Waypoints", "Secret Waypoints", "Dungeons / Secrets", Tab.DUNGEON, DungeonSection.SECRETS,
            "secret waypoints chest item essence bat redstone lever dungeon raum",
            "secret waypoints chest item essence bat redstone lever dungeon room");
        add("Durch Waende", "Through Walls", "Dungeons / Secrets", Tab.DUNGEON, DungeonSection.SECRETS,
            "secret waypoints durch waende dungeon bloecke",
            "secret waypoints through walls dungeon blocks");
        add("Waypoint Text", "Waypoint Text", "Dungeons / Secrets", Tab.DUNGEON, DungeonSection.SECRETS,
            "waypoint text kategorie beschriftung secret dungeon",
            "waypoint text category label secret dungeon");
        add("Power/Time HUD", "Power/Time HUD", "Dungeons / Secrets", Tab.DUNGEON, DungeonSection.SECRETS,
            "power time blessing hud tab liste dungeon",
            "power time blessing hud tab list dungeon");
        add("Secret Waypoints Config", "Secret Waypoints Config", "Dungeons / Secrets", Tab.DUNGEON, DungeonSection.SECRETS,
            "secret waypoints chest item wither essence bat redstone lever farbe dungeon config",
            "secret waypoints chest item wither essence bat redstone lever color dungeon config");
        add("Wither Door ESP", "Wither Door ESP", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "wither door esp highlight dungeon tueren",
            "wither door esp highlight dungeon doors");
        add("Blood Door ESP", "Blood Door ESP", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "blood door esp highlight dungeon tueren",
            "blood door esp highlight dungeon doors");
        add("Schluessel Highlight", "Key Highlight", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "schluessel highlight wither blood tracer dungeon key",
            "key highlight wither blood tracer dungeon");
        add("Key aufgesammelt", "Key Collected", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "key aufgesammelt farbe door dungeon",
            "key collected color door dungeon");
        add("Key nicht aufgesammelt", "Key Not Collected", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "key nicht aufgesammelt farbe door dungeon",
            "key not collected color door dungeon");
        add("Dungeon Score", "Dungeon Score", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "dungeon score hud punktzahl geschaetzt overlay",
            "dungeon score hud estimated overlay");
        add("Score-Title", "Score Title", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "score title s plus 270 300 dungeon",
            "score title s plus 270 300 dungeon");
        add("Score im Boss zeigen", "Show Score in Boss", "Dungeons / Clear", Tab.DUNGEON, DungeonSection.SECRETS,
            "score boss zeigen bosskampf sichtbar dungeon",
            "show score in boss fight visible dungeon");

        // ── DUNGEON / PUZZLE SOLVER ────────────────────────────────────────────────
        add("Puzzle Solver", "Puzzle Solver", "Dungeons / Puzzles", Tab.DUNGEON, DungeonSection.PUZZLE_SOLVER,
            "puzzle solver blaze boulder eis quiz wasser creeper beams three weirdos dungeon",
            "puzzle solver blaze boulder ice fill quiz water creeper beams three weirdos dungeon");
        add("Falsche Klicks blockieren (Puzzle)", "Block Wrong Clicks (Puzzle)", "Dungeons / Puzzles", Tab.DUNGEON, DungeonSection.PUZZLE_SOLVER,
            "falsche klicks blockieren quiz antwort dungeon puzzle",
            "block wrong clicks quiz answer dungeon puzzle");
        add("Stil (Puzzle)", "Style (Puzzle)", "Dungeons / Puzzles", Tab.DUNGEON, DungeonSection.PUZZLE_SOLVER,
            "stil gefuellt umriss render dungeon puzzle",
            "style filled outline render dungeon puzzle");

        // ── DUNGEON / TERMINAL SOLVER ──────────────────────────────────────────────
        add("Terminal Solver", "Terminal Solver", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "terminal solver f7 overlay slots markiert klicken",
            "terminal solver f7 overlay highlighted slots click");
        add("Slot-Stil", "Slot Style", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "slot stil rechteck rahmen button terminal",
            "slot style rect bordered button terminal");
        add("Zahlen anzeigen", "Show Numbers", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "zahlen anzeigen klick reihenfolge order terminal",
            "show numbers click order terminal");
        add("Test-Modus", "Test Mode", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "test modus terminal rubix clicked field lokal",
            "test mode terminal rubix clicked field local");
        add("GUI Scale (Terminal)", "GUI Scale (Terminal)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "gui scale terminal overlay skalierung groesse",
            "gui scale terminal overlay scale");
        add("Melody Announce", "Melody Announce", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "melody announce party chat fortschritt ankuendigen",
            "melody announce party chat progress");
        add("Melody-Nachricht", "Melody Message", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "melody nachricht message template party chat terminal",
            "melody message template party chat terminal");
        add("Terminal-Farben", "Terminal Colors", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "terminal farben automatisch hud loesung numbers rubix melody hintergrund rahmen title",
            "terminal colors automatic hud solution numbers rubix melody background border title");
        add("Automatische HUD-Farben", "Automatic HUD Colors", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "automatische hud farben terminal solver ableiten",
            "automatic hud colors terminal solver derive");
        add("Simon Says", "Simon Says", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "simon says goldor device schaltflaechen reihenfolge dungeon",
            "simon says goldor device button sequence dungeon");
        add("Falsche Klicks blockieren (Simon)", "Block Wrong Clicks (Simon)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "falsche klicks blockieren simon says dungeon",
            "block wrong clicks simon says dungeon");
        add("Sneak invertieren (Simon)", "Invert Sneak (Simon)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "sneak invertieren simon says dungeon deaktivieren",
            "invert sneak simon says dungeon disable");
        add("Arrow Align", "Arrow Align", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "arrow align pfeil bilderrahmen klickanzahl dungeon",
            "arrow align item frame click count dungeon");
        add("Arrow Farb-Stil", "Arrow Color Style", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "arrow farb stil dynamisch custom klickanzahl gruen orange rot",
            "arrow color style dynamic custom click count green orange red");
        add("Arrow Textfarbe", "Arrow Text Color", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "arrow textfarbe farbe text dungeon terminal",
            "arrow text color dungeon terminal");
        add("Falsche Klicks blockieren (Arrow)", "Block Wrong Clicks (Arrow)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "falsche klicks blockieren arrow align dungeon",
            "block wrong clicks arrow align dungeon");
        add("Sneak invertieren (Arrow)", "Invert Sneak (Arrow)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "sneak invertieren arrow align dungeon",
            "invert sneak arrow align dungeon");
        add("Arrow Device (I4)", "Arrow Device (I4)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "arrow device i4 sharpshooter smaragd bloecke dungeon",
            "arrow device i4 sharpshooter emerald blocks dungeon");
        add("I4 Done-Text", "I4 Done Text", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "i4 done text welt fertig sharpshooter dungeon",
            "i4 done text world complete sharpshooter dungeon");
        add("I4 Done-Title", "I4 Done Title", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "i4 done title screen sharpshooter dungeon fertig",
            "i4 done title screen sharpshooter dungeon complete");
        add("I4 Done-Farbe", "I4 Done Color", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "i4 done farbe color sharpshooter dungeon",
            "i4 done color sharpshooter dungeon");
        add("I4 Done-Groesse", "I4 Done Size", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "i4 done groesse scale size sharpshooter dungeon",
            "i4 done scale size sharpshooter dungeon");
        add("Terminal-Wegpunkte", "Terminal Waypoints", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "terminal wegpunkte waypoints markiert position dungeon",
            "terminal waypoints position dungeon highlighted");
        add("Typ-Title", "Type Title", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "typ title terminal oeffnen annaehern dungeon",
            "type title terminal open approach dungeon");
        add("Custom Sounds (Terminal)", "Custom Sounds (Terminal)", "Dungeons / Terminal", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER,
            "custom sounds terminal secret simon says lever arrow sharpshooter sound custom",
            "custom sounds terminal secret simon says lever arrow sharpshooter sound custom");

        // ── DUNGEON / BOSS ─────────────────────────────────────────────────────────
        add("Blood Camper", "Blood Camper", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "blood camper timer fortschritt dungeon boss",
            "blood camper timer progress dungeon boss");
        add("Damage Tick Timer", "Damage Tick Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "damage tick timer goldor f7 p3 countdown dungeon",
            "damage tick timer goldor f7 p3 countdown dungeon");
        add("Maxor Timer", "Maxor Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "maxor timer phase countdown p1 f7 dungeon",
            "maxor timer phase countdown p1 f7 dungeon");
        add("Storm Timer", "Storm Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "storm timer phase countdown p2 f7 dungeon",
            "storm timer phase countdown p2 f7 dungeon");
        add("Goldor Timer", "Goldor Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "goldor timer phase countdown p3 f7 dungeon",
            "goldor timer phase countdown p3 f7 dungeon");
        add("Necron Timer", "Necron Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "necron timer phase countdown p4 f7 dungeon",
            "necron timer phase countdown p4 f7 dungeon");
        add("Purple Pad Timer", "Purple Pad Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "purple pad timer countdown f7 p2 dungeon zeitpunkt",
            "purple pad timer countdown f7 p2 dungeon timing");
        add("Pad Timer", "Pad Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "pad timer zweiter purple dungeon f7",
            "pad timer second purple dungeon f7");
        add("Dragon Overlay", "Dragon Overlay", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "dragon overlay spawn prioritaet boxen timer m7 p5 drachen",
            "dragon overlay spawn priority boxes timer m7 p5 dragons");
        add("Dragon Boxes", "Dragon Boxes", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "dragon boxen spawn positionen farbig m7 p5 drachen",
            "dragon boxes spawn positions colored m7 p5 dragons");
        add("Dragon Timer", "Dragon Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "dragon timer countdown spawn m7 p5 drachen",
            "dragon timer countdown spawn m7 p5 dragons");
        add("Spawn Alert", "Spawn Alert", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "spawn alert warnung chat m7 p5 drachen",
            "spawn alert warning chat m7 p5 dragons");
        add("Prioritaet (Dragon)", "Priority (Dragon)", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "prioritaet dragon power kill reihenfolge m7 p5",
            "priority dragon power kill order m7 p5");
        add("Normal Power", "Normal Power", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "normal power schwelle dragon prioritaet m7 p5",
            "normal power threshold dragon priority m7 p5");
        add("Easy Power", "Easy Power", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "easy power niedrigere schwelle purple split dragon m7 p5",
            "easy power lower threshold purple split dragon m7 p5");
        add("Purple Solo-Debuff", "Purple Solo Debuff", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "purple solo debuff healer tank dragon m7 p5",
            "purple solo debuff healer tank dragon m7 p5");
        add("Solo-Debuff auf alle Splits", "Solo Debuff on All Splits", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "solo debuff alle splits dragon m7 p5",
            "solo debuff all splits dragon m7 p5");
        add("Drachen-Leben", "Dragon Health", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "drachen leben health leben dragon box m7 p5",
            "dragon health box m7 p5");
        add("Prioritaet-Tracer", "Priority Tracer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "prioritaet tracer linie prioritaets drachen m7 p5",
            "priority tracer line priority dragon m7 p5");
        add("Rag Axe Notification", "Rag Axe Notification", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "rag axe notification necron m7 phase dungeon title",
            "rag axe notification necron m7 phase dungeon title");
        add("Relic Timer", "Relic Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "relic timer countdown spawn necron m7 dungeon",
            "relic timer countdown spawn necron m7 dungeon");
        add("Relic-Platzier-Timer", "Relic Place Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "relic platzier timer chat platzieren m7 dungeon",
            "relic place timer chat place m7 dungeon");
        add("Spirit Bear Timer", "Spirit Bear Timer", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "spirit bear timer f4 m4 boss spawn dungeon countdown",
            "spirit bear timer f4 m4 boss spawn dungeon countdown");
        add("Spirit Bear Highlight", "Spirit Bear Highlight", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "spirit bear highlight glow f4 m4 dungeon",
            "spirit bear highlight glow f4 m4 dungeon");
        add("Spirit Bear Farbe", "Spirit Bear Color", "Dungeons / Boss", Tab.DUNGEON, DungeonSection.BOSS,
            "spirit bear farbe color glow f4 m4 dungeon",
            "spirit bear color glow f4 m4 dungeon");

        // ── DISPLAY / GENERAL ──────────────────────────────────────────────────────
        add("16:9 Pillarbox", "16:9 Pillarbox", "Anzeige", Tab.DISPLAY, null,
            "pillarbox 16:9 anzeige display monitor ultrawide 32:9 odyssey g9 schwarze balken letterbox",
            "pillarbox 16:9 display monitor ultrawide 32:9 odyssey g9 black bars letterbox");

        // ── DISPLAY / ANIMATIONS ───────────────────────────────────────────────────
        add("Position X", "Position X", "Anzeige / Animationen", Tab.DISPLAY, null,
            "animation hand item position x horizontal",
            "animation hand item position x horizontal");
        add("Position Y", "Position Y", "Anzeige / Animationen", Tab.DISPLAY, null,
            "animation hand item position y vertikal",
            "animation hand item position y vertical");
        add("Position Z", "Position Z", "Anzeige / Animationen", Tab.DISPLAY, null,
            "animation hand item position z tiefe",
            "animation hand item position z depth");
        add("Groesse", "Scale", "Anzeige / Animationen", Tab.DISPLAY, null,
            "animation hand item groesse scale skalierung",
            "animation hand item scale size");
        add("Schlaggeschwindigkeit", "Swing Speed", "Anzeige / Animationen", Tab.DISPLAY, null,
            "animation hand schlaggeschwindigkeit swing speed geschwindigkeit",
            "animation hand swing speed");

        // ── DISPLAY / NO_RENDER ────────────────────────────────────────────────────
        add("Feuer-Overlay", "Fire Overlay", "Anzeige / NoRender", Tab.DISPLAY, null,
            "feuer overlay ausblenden brennen anzeige",
            "fire overlay disable hide burning display");
        add("Front Cam", "Front Cam", "Anzeige / NoRender", Tab.DISPLAY, null,
            "front cam f5 ansicht vorne dritten person deaktivieren",
            "front cam f5 view disable front facing third person");
        add("Soulweaver Gloves", "Soulweaver Gloves", "Anzeige / NoRender", Tab.DISPLAY, null,
            "soulweaver gloves skulls verstecken rendern anzeige",
            "soulweaver gloves skulls hide render display");
        add("HUD bei Tab verstecken", "Hide HUD on Tab", "Anzeige / NoRender", Tab.DISPLAY, null,
            "hud tab verstecken horizon tablist",
            "hide hud tab tablist");
        add("Effekte verstecken", "Hide Effects", "Anzeige / NoRender", Tab.DISPLAY, null,
            "effekte verstecken potion status icons hud inventar",
            "hide effects potion status icons hud inventory");
        add("Hurtcam Intensitaet", "Hurtcam Intensity", "Anzeige / NoRender", Tab.DISPLAY, null,
            "hurtcam intensitaet kamera schwanken schaden",
            "hurtcam intensity camera shake damage");

        // ── DISPLAY / HELPERS ──────────────────────────────────────────────────────
        add("Etherwarp Helper", "Etherwarp Helper", "Anzeige / Helpers", Tab.DISPLAY, null,
            "etherwarp helper teleport ziel aspect of the void dragons anzeige",
            "etherwarp helper teleport destination aspect of the void dragons");
        add("Nur beim Schleichen (Etherwarp)", "Sneak Only (Etherwarp)", "Anzeige / Helpers", Tab.DISPLAY, null,
            "etherwarp nur schleichen sneak box anzeige",
            "etherwarp sneak only box display");
        add("Stil (Etherwarp)", "Style (Etherwarp)", "Anzeige / Helpers", Tab.DISPLAY, null,
            "etherwarp stil render gefuellt umriss ziel box",
            "etherwarp style render filled outline destination box");
        add("Depth Check (Etherwarp)", "Depth Check (Etherwarp)", "Anzeige / Helpers", Tab.DISPLAY, null,
            "etherwarp depth check waende ausblenden ziel",
            "etherwarp depth check walls hide destination");
        add("Etherwarp Sound", "Etherwarp Sound", "Anzeige / Helpers", Tab.DISPLAY, null,
            "etherwarp sound teleport sound abspielen",
            "etherwarp sound teleport play");

        // ── DISPLAY / MISC ─────────────────────────────────────────────────────────
        add("Zeit HUD", "Time HUD", "Anzeige / Misc", Tab.DISPLAY, null,
            "zeit hud uhrzeit clock lokal overlay",
            "time hud clock local overlay");
        add("FPS / TPS / Ping", "FPS / TPS / Ping", "Anzeige / Misc", Tab.DISPLAY, null,
            "fps tps ping performance overlay anzeige",
            "fps tps ping performance overlay");
        add("System HUD", "System HUD", "Anzeige / Misc", Tab.DISPLAY, null,
            "system hud cpu gpu temperatur anzeige",
            "system hud cpu gpu temperature display");
        add("Defense Bar", "Defense Bar", "Anzeige / Misc", Tab.DISPLAY, null,
            "defense bar ruestung armor verstecken anzeige",
            "defense bar armor hide display");
        add("Kompakte Herzen", "Compact Hearts", "Anzeige / Misc", Tab.DISPLAY, null,
            "kompakte herzen hypixel health herz absorption reihe",
            "compact hearts hypixel health absorption row");

        // ── DISPLAY / PARTICLE ─────────────────────────────────────────────────────
        add("Break Particles", "Break Particles", "Anzeige / Particle", Tab.DISPLAY, null,
            "break particles block abbauen partikel anzeige",
            "break particles block breaking display");
        add("Particle Suche", "Particle Search", "Anzeige / Particle", Tab.DISPLAY, null,
            "particle suche filter liste partikel",
            "particle search filter list");

        // ── HELPER ─────────────────────────────────────────────────────────────────
        add("Experimentier-Tisch", "Experimentation Table", "Helper", Tab.HELPER, null,
            "experimentier tisch superpairs solver helper aufgedeckt belohnungen",
            "experimentation table superpairs solver helper revealed rewards");
        add("Croesus Chest Profit", "Croesus Chest Profit", "Helper", Tab.HELPER, null,
            "croesus chest profit dungeon truhen coins bazaar bin preis",
            "croesus chest profit dungeon chests coins bazaar bin price");
        add("Storage Overlay", "Storage Overlay", "Helper", Tab.HELPER, null,
            "storage overlay ender chest backpack durchsuchbar uebersicht",
            "storage overlay ender chest backpack searchable overview");
        add("Bazaar Value Tooltip", "Bazaar Value Tooltip", "Helper", Tab.HELPER, null,
            "bazaar value tooltip buy sell preise item",
            "bazaar value tooltip buy sell prices item");
        add("Auction Value Tooltip", "Auction Value Tooltip", "Helper", Tab.HELPER, null,
            "auction value tooltip lowest bin avg preis item",
            "auction value tooltip lowest bin avg price item");
        add("Craft Value Tooltip", "Craft Value Tooltip", "Helper", Tab.HELPER, null,
            "craft value tooltip preis enchants scrolls sterne gems item",
            "craft value tooltip price enchants scrolls stars gems item");
        add("Stack-Value bei Shift", "Stack Value on Shift", "Helper", Tab.HELPER, null,
            "stack value shift preis stack groesse multipliziert",
            "stack value shift price stack size multiplied");
        add("Enchant Gradient", "Enchant Gradient", "Helper", Tab.HELPER, null,
            "enchant gradient farbverlauf animiert tooltip maxed",
            "enchant gradient animated tooltip maxed");
        add("Gradient Modus", "Gradient Mode", "Helper", Tab.HELPER, null,
            "gradient modus hud akzent eigene farben rainbow verlauf",
            "gradient mode hud accent custom colors rainbow");
        add("Pet Highlight", "Pet Highlight", "Helper", Tab.HELPER, null,
            "pet highlight aktiv despawn pets menue",
            "pet highlight active despawn pets menu");
        add("Sparkling Announce", "Sparkling Announce", "Helper", Tab.HELPER, null,
            "sparkling announce nametag title sound helper",
            "sparkling announce nametag title sound helper");
        add("Title Announce", "Title Announce", "Helper", Tab.HELPER, null,
            "title announce chat message trigger helper liste",
            "title announce chat message trigger helper list");
        add("Chat-Trigger", "Chat Trigger", "Helper", Tab.HELPER, null,
            "chat trigger title announce ausloeser eingabe",
            "chat trigger title announce input");
        add("Title-Text", "Title Text", "Helper", Tab.HELPER, null,
            "title text announce anzeigen eingabe",
            "title text announce display input");

        // ── CHAT / GENERAL ─────────────────────────────────────────────────────────
        add("Guild Chat verstecken", "Hide Guild Chat", "Chat / General", Tab.CHAT, null,
            "guild chat verstecken ausblenden nachrichten",
            "hide guild chat messages");
        add("Bridge verstecken", "Hide Bridge", "Chat / General", Tab.CHAT, null,
            "bridge discord guild bot verstecken ausblenden",
            "hide bridge discord guild bot");
        add("Bridge Bot Name", "Bridge Bot Name", "Chat / General", Tab.CHAT, null,
            "bridge bot name catgirlfc guild discord ingame",
            "bridge bot name catgirlfc guild discord ingame");
        add("Nachrichten kopieren", "Copy Messages", "Chat / General", Tab.CHAT, null,
            "nachrichten kopieren clipboard copy strg rechtsklick modus",
            "copy messages clipboard ctrl right click mode");
        add("Gesamte Nachricht", "Full Message", "Chat / General", Tab.CHAT, null,
            "gesamte nachricht kopieren zeile eintrag chat",
            "full message copy line entry chat");

        // ── CHAT / SPAM FILTERS ────────────────────────────────────────────────────
        add("Anti Spam Gesamt", "Anti Spam All", "Chat / Spam Filters", Tab.CHAT, null,
            "anti spam gesamt dungeon ability noise",
            "anti spam all dungeon ability noise");
        // SpamFilterOption entries (static expansion)
        add("Dungeon Blessings", "Dungeon Blessings", "Chat / Spam Filters", Tab.CHAT, null,
            "blessing chatmeldungen dungeon ausblenden spam",
            "blessing chat messages dungeon hide spam");
        add("Dungeon Pickups", "Dungeon Pickups", "Chat / Spam Filters", Tab.CHAT, null,
            "dungeon pickups keys superboom revive stones spam",
            "dungeon pickups keys superboom revive stones spam");
        add("Dungeon Events", "Dungeon Events", "Chat / Spam Filters", Tab.CHAT, null,
            "dungeon events blood wither door ansagen spam",
            "dungeon events blood wither door announcements spam");
        add("Locked Chest", "Locked Chest", "Chat / Spam Filters", Tab.CHAT, null,
            "locked chest this chest is locked spam dungeon",
            "locked chest this chest is locked spam dungeon");
        add("Boss Messages", "Boss Messages", "Chat / Spam Filters", Tab.CHAT, null,
            "boss messages nachrichten dungeon spam",
            "boss messages dungeon spam");
        add("Blocks in the way", "Blocks in the way", "Chat / Spam Filters", Tab.CHAT, null,
            "blocks in the way teleport portal spam",
            "blocks in the way teleport portal spam");
        add("Ability spam", "Ability spam", "Chat / Spam Filters", Tab.CHAT, null,
            "ability spam wither impact messages dungeon",
            "ability spam wither impact messages dungeon");
        add("Mana Messages", "Mana Messages", "Chat / Spam Filters", Tab.CHAT, null,
            "mana messages nicht genug spam",
            "mana messages not enough spam");
        add("Cooldown Messages", "Cooldown Messages", "Chat / Spam Filters", Tab.CHAT, null,
            "cooldown messages spam allgemein",
            "cooldown messages spam general");
        add("AutoPet", "AutoPet", "Chat / Spam Filters", Tab.CHAT, null,
            "autopet swap messages spam",
            "autopet swap messages spam");
        add("Full HP / Mana", "Full HP / Mana", "Chat / Spam Filters", Tab.CHAT, null,
            "full hp mana status messages spam",
            "full hp mana status messages spam");
        add("Effect Warnings", "Effect Warnings", "Chat / Spam Filters", Tab.CHAT, null,
            "effect warnings potion doppelt spam",
            "effect warnings potion duplicate spam");
        add("Heal Messages", "Heal Messages", "Chat / Spam Filters", Tab.CHAT, null,
            "heal messages heilung spam chat",
            "heal messages healing spam chat");
        add("Warping", "Warping", "Chat / Spam Filters", Tab.CHAT, null,
            "warping messages spam warp",
            "warping messages spam warp");
        add("Sending to Server", "Sending to Server", "Chat / Spam Filters", Tab.CHAT, null,
            "sending to server messages spam",
            "sending to server messages spam");
        add("Profile / Profile ID", "Profile / Profile ID", "Chat / Spam Filters", Tab.CHAT, null,
            "profil id profile messages spam",
            "profile id messages spam");
        add("Guild Join / Leave", "Guild Join / Leave", "Chat / Spam Filters", Tab.CHAT, null,
            "guild join leave beitritt austritt spam",
            "guild join leave messages spam");
        add("Firesales", "Firesales", "Chat / Spam Filters", Tab.CHAT, null,
            "firesale ankuendigungen spam",
            "firesale announcements spam");
        add("Radio Signal", "Radio Signal", "Chat / Spam Filters", Tab.CHAT, null,
            "radio signal nachrichten spam",
            "radio signal messages spam");
        add("Sacks", "Sacks", "Chat / Spam Filters", Tab.CHAT, null,
            "sacks chatmeldungen spam",
            "sacks chat messages spam");
        add("Sea Creatures filtern", "Filter Sea Creatures", "Chat / Spam Filters", Tab.CHAT, null,
            "sea creatures filtern fishing fang nachrichten atoll lotus spam",
            "filter sea creatures fishing catch messages spam");
        add("Elusive Creatures filtern", "Filter Elusive Creatures", "Chat / Spam Filters", Tab.CHAT, null,
            "elusive creatures filtern fishing rare fang nachrichten spam",
            "filter elusive creatures fishing rare catch messages spam");
        add("Trophy Fish filtern", "Filter Trophy Fish", "Chat / Spam Filters", Tab.CHAT, null,
            "trophy fish filtern fishing fang nachrichten spam",
            "filter trophy fish fishing catch messages spam");
        add("Trophy Frogs filtern", "Filter Trophy Frogs", "Chat / Spam Filters", Tab.CHAT, null,
            "trophy frogs filtern fishing fang nachrichten spam",
            "filter trophy frogs fishing catch messages spam");
        add("Diamond Trophies filtern", "Filter Diamond Trophies", "Chat / Spam Filters", Tab.CHAT, null,
            "diamond trophies filtern fishing fang nachrichten spam",
            "filter diamond trophies fishing catch messages spam");
        add("Good/Great/Outstanding filtern", "Filter Good/Great/Outstanding", "Chat / Spam Filters", Tab.CHAT, null,
            "good great outstanding perfect catch filtern fishing spam",
            "filter good great outstanding perfect catch fishing spam");

        // ── CHAT / CHAT COMMANDS ───────────────────────────────────────────────────
        add("Chat Commands", "Chat Commands", "Chat / Chat Commands", Tab.CHAT, null,
            "chat commands befehle party gilde privat ausrufezeichen",
            "chat commands party guild private exclamation");
        add("Party Chat Commands", "Party Chat Commands", "Chat / Chat Commands", Tab.CHAT, null,
            "party chat commands befehle erlauben",
            "party chat commands allow");
        add("Guild Chat Commands", "Guild Chat Commands", "Chat / Chat Commands", Tab.CHAT, null,
            "guild chat commands befehle gilde erlauben",
            "guild chat commands allow");
        add("Private Chat Commands", "Private Chat Commands", "Chat / Chat Commands", Tab.CHAT, null,
            "private chat commands befehle privatnachrichten erlauben",
            "private chat commands allow");

        // ── CHAT / SHORTCUTS ───────────────────────────────────────────────────────
        add("Command Shortcuts", "Command Shortcuts", "Chat / Shortcuts", Tab.CHAT, null,
            "command shortcuts f1 f7 m1 m7 joininstance catacombs master dungeon hub warp",
            "command shortcuts f1 f7 m1 m7 joininstance catacombs master dungeon hub warp");

        // ── SCOREBOARD ─────────────────────────────────────────────────────────────
        add("Custom Scoreboard", "Custom Scoreboard", "Scoreboard", Tab.SCOREBOARD, null,
            "custom scoreboard sidebar hypixel leiste anzeige",
            "custom scoreboard sidebar hypixel bar display");
        add("Globale Zeilenfilter", "Global Line Filters", "Scoreboard", Tab.SCOREBOARD, null,
            "globale zeilenfilter scoreboard standort season uhrzeit datum profil",
            "global line filters scoreboard location season time date profile");
        // SkyBlockIsland entries (static expansion of knownIslands)
        add("Hub (Scoreboard)", "Hub (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "hub scoreboard zeilen island filter",
            "hub scoreboard lines island filter");
        add("Dungeons (Scoreboard)", "Dungeons (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "dungeons scoreboard zeilen island filter",
            "dungeons scoreboard lines island filter");
        add("Garden (Scoreboard)", "Garden (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "garden scoreboard zeilen island filter",
            "garden scoreboard lines island filter");
        add("Dwarven Mines (Scoreboard)", "Dwarven Mines (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "dwarven mines scoreboard zeilen island filter",
            "dwarven mines scoreboard lines island filter");
        add("Crystal Hollows (Scoreboard)", "Crystal Hollows (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "crystal hollows scoreboard zeilen island filter",
            "crystal hollows scoreboard lines island filter");
        add("Crimson Isle (Scoreboard)", "Crimson Isle (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "crimson isle scoreboard zeilen island filter",
            "crimson isle scoreboard lines island filter");
        add("Farming Islands (Scoreboard)", "Farming Islands (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "farming islands scoreboard zeilen island filter",
            "farming islands scoreboard lines island filter");
        add("The Rift (Scoreboard)", "The Rift (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "the rift scoreboard zeilen island filter",
            "the rift scoreboard lines island filter");
        add("Spider's Den (Scoreboard)", "Spider's Den (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "spiders den scoreboard zeilen island filter",
            "spiders den scoreboard lines island filter");
        add("The End (Scoreboard)", "The End (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "the end scoreboard zeilen island filter",
            "the end scoreboard lines island filter");
        add("Dungeon Hub (Scoreboard)", "Dungeon Hub (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "dungeon hub scoreboard zeilen island filter",
            "dungeon hub scoreboard lines island filter");
        add("Private Island (Scoreboard)", "Private Island (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "private island scoreboard zeilen island filter",
            "private island scoreboard lines island filter");
        add("Jerry's Workshop (Scoreboard)", "Jerry's Workshop (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "jerry workshop scoreboard zeilen island filter",
            "jerry workshop scoreboard lines island filter");
        add("Kuudra (Scoreboard)", "Kuudra (Scoreboard)", "Scoreboard", Tab.SCOREBOARD, null,
            "kuudra scoreboard zeilen island filter",
            "kuudra scoreboard lines island filter");

        // ── INVENTORY / GENERAL ────────────────────────────────────────────────────
        add("Wardrobe Keybinds", "Wardrobe Keybinds", "Inventory / General", Tab.INVENTORY, null,
            "wardrobe keybinds pfeiltasten zifferntasten screen inventar",
            "wardrobe keybinds arrow number keys screen inventory");
        add("Loadout Keybinds", "Loadout Keybinds", "Inventory / General", Tab.INVENTORY, null,
            "loadout keybinds pfeiltasten 1-9 screen inventar",
            "loadout keybinds arrow keys 1-9 screen inventory");
        add("Slot Binds", "Slot Binds", "Inventory / General", Tab.INVENTORY, null,
            "slot binds shift klick gebundene slots inventar",
            "slot binds shift click bound inventory slots");
        add("Slot Bind Key", "Slot Bind Key", "Inventory / General", Tab.INVENTORY, null,
            "slot bind key taste inventar setzen",
            "slot bind key inventory create remove");
        add("Scrollbare Tooltips", "Scrollable Tooltips", "Inventory / General", Tab.INVENTORY, null,
            "scrollbare tooltips mausrad strg groesse inventar",
            "scrollable tooltips mouse wheel ctrl size inventory");
        add("Tooltip-Groesse", "Tooltip Scale", "Inventory / General", Tab.INVENTORY, null,
            "tooltip groesse scale standard inventar",
            "tooltip scale default inventory");

        // ── INVENTORY / COMMANDS ───────────────────────────────────────────────────
        add("Command Keybinds", "Command Keybinds", "Inventory / Commands", Tab.INVENTORY, null,
            "command keybinds befehle taste pets equipment wardrobe loadouts stats",
            "command keybinds key pets equipment wardrobe loadouts stats");
        add("/pets Keybind", "/pets Keybind", "Inventory / Commands", Tab.INVENTORY, null,
            "pets keybind taste command inventar",
            "pets keybind key command inventory");
        add("/equipment Keybind", "/equipment Keybind", "Inventory / Commands", Tab.INVENTORY, null,
            "equipment keybind taste command inventar",
            "equipment keybind key command inventory");
        add("/wardrobe Keybind", "/wardrobe Keybind", "Inventory / Commands", Tab.INVENTORY, null,
            "wardrobe keybind taste command inventar",
            "wardrobe keybind key command inventory");
        add("/loadouts Keybind", "/loadouts Keybind", "Inventory / Commands", Tab.INVENTORY, null,
            "loadouts keybind taste command inventar",
            "loadouts keybind key command inventory");
        add("/stats Keybind", "/stats Keybind", "Inventory / Commands", Tab.INVENTORY, null,
            "stats keybind taste command inventar",
            "stats keybind key command inventory");
        add("Eigene Commands", "Custom Commands", "Inventory / Commands", Tab.INVENTORY, null,
            "eigene commands keybind tastenkuerzel custom cmd",
            "custom commands keybind hotkey");
        add("Command hinzufuegen", "Add Command", "Inventory / Commands", Tab.INVENTORY, null,
            "command hinzufuegen eigene custom cmd eingabe",
            "add command custom input");

        // ── INVENTORY / BUTTONS ────────────────────────────────────────────────────
        add("Inventory Buttons", "Inventory Buttons", "Inventory / Inventory Buttons", Tab.INVENTORY, null,
            "inventory buttons inventar konfiguriert layout platzieren",
            "inventory buttons inventory configured layout place");

        // ── FISHING ────────────────────────────────────────────────────────────────
        add("Announce Rare Sea Creatures", "Announce Rare Sea Creatures", "Fishing", Tab.FISHING, null,
            "fishing rare sea creatures elusive announce title sound alert",
            "fishing rare sea creatures elusive announce title sound alert");
        add("Alert Sound", "Alert Sound", "Fishing", Tab.FISHING, null,
            "fishing alert sound rare meow katze custom boo womp",
            "fishing alert sound rare meow cat custom boo womp");
        add("Creature Filter", "Creature Filter", "Fishing", Tab.FISHING, null,
            "fishing creature filter sea creatures toggle aktivieren deaktivieren",
            "fishing creature filter sea creatures toggle enable disable");
        // ElusiveSeaCreature entries (static expansion)
        add("Frog Prince", "Frog Prince", "Fishing", Tab.FISHING, null,
            "fishing frog prince elusive sea creature",
            "fishing frog prince elusive sea creature");
        add("The Loch Emperor", "The Loch Emperor", "Fishing", Tab.FISHING, null,
            "fishing the loch emperor elusive sea creature",
            "fishing the loch emperor elusive sea creature");
        add("Nessie", "Nessie", "Fishing", Tab.FISHING, null,
            "fishing nessie elusive sea creature",
            "fishing nessie elusive sea creature");
        add("Water Hydra", "Water Hydra", "Fishing", Tab.FISHING, null,
            "fishing water hydra elusive sea creature",
            "fishing water hydra elusive sea creature");
        add("Puddle Jumper", "Puddle Jumper", "Fishing", Tab.FISHING, null,
            "fishing puddle jumper elusive sea creature",
            "fishing puddle jumper elusive sea creature");
        add("Alligator", "Alligator", "Fishing", Tab.FISHING, null,
            "fishing alligator elusive sea creature",
            "fishing alligator elusive sea creature");
        add("Agarimoo", "Agarimoo", "Fishing", Tab.FISHING, null,
            "fishing agarimoo elusive sea creature",
            "fishing agarimoo elusive sea creature");
        add("Water Worm", "Water Worm", "Fishing", Tab.FISHING, null,
            "fishing water worm elusive sea creature",
            "fishing water worm elusive sea creature");
        add("Abyssal Miner", "Abyssal Miner", "Fishing", Tab.FISHING, null,
            "fishing abyssal miner elusive sea creature",
            "fishing abyssal miner elusive sea creature");
        add("Thunder", "Thunder", "Fishing", Tab.FISHING, null,
            "fishing thunder elusive sea creature",
            "fishing thunder elusive sea creature");
        add("Fiery Scuttler", "Fiery Scuttler", "Fishing", Tab.FISHING, null,
            "fishing fiery scuttler elusive sea creature",
            "fishing fiery scuttler elusive sea creature");
        add("Lord Jawbus", "Lord Jawbus", "Fishing", Tab.FISHING, null,
            "fishing lord jawbus elusive sea creature",
            "fishing lord jawbus elusive sea creature");
        add("Phantom Fisher", "Phantom Fisher", "Fishing", Tab.FISHING, null,
            "fishing phantom fisher elusive sea creature",
            "fishing phantom fisher elusive sea creature");
        add("Grim Reaper", "Grim Reaper", "Fishing", Tab.FISHING, null,
            "fishing grim reaper elusive sea creature",
            "fishing grim reaper elusive sea creature");
        add("Great White Shark", "Great White Shark", "Fishing", Tab.FISHING, null,
            "fishing great white shark elusive sea creature",
            "fishing great white shark elusive sea creature");
        add("Blue Ringed Octopus", "Blue Ringed Octopus", "Fishing", Tab.FISHING, null,
            "fishing blue ringed octopus elusive sea creature",
            "fishing blue ringed octopus elusive sea creature");
        add("Wiki Tiki", "Wiki Tiki", "Fishing", Tab.FISHING, null,
            "fishing wiki tiki elusive sea creature",
            "fishing wiki tiki elusive sea creature");
        add("Ragnarok", "Ragnarok", "Fishing", Tab.FISHING, null,
            "fishing ragnarok elusive sea creature",
            "fishing ragnarok elusive sea creature");
        add("Plhlegblast", "Plhlegblast", "Fishing", Tab.FISHING, null,
            "fishing plhlegblast elusive sea creature",
            "fishing plhlegblast elusive sea creature");
    }

    public static List<FeatureEntry> all() {
        return ENTRIES;
    }
}
