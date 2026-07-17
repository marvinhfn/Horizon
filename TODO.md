# Horizon Migration Roadmap

## Arbeitsregeln

- Statuslogik: Ein Arbeitspaket durchlaeuft die Stati `[ ]` -> `[-]` -> `[x]`.
- Testing markieren: Wenn die Umsetzung eines Arbeitspakets fertig ist, beide Builds erfolgreich gelaufen sind und das Backend wieder gestartet wurde, wird das Paket von `[ ]` auf `[-]` gesetzt. `[-]` bedeutet `testing`.
- Erledigt markieren: Ein Arbeitspaket wird erst dann von `[-]` auf `[x]` gesetzt, wenn der Nutzer danach `naechstes Paket` schreibt. Damit bestaetigt der Nutzer, dass das vorherige Paket ausreichend getestet wurde und als abgeschlossen gilt.
- Nicht doppelt anfangen: Bereits mit `[x]` markierte Pakete werden nicht erneut begonnen. Pakete mit `[-]` gelten als in Testing. Neue Arbeit wird immer mit dem ersten Paket im Status `[ ]` begonnen.
- Kommando "naechstes Paket": Wenn der Nutzer nur `naechstes Paket` schreibt, wird zuerst das zuletzt bearbeitete Paket von `[-]` auf `[x]` gesetzt. Danach beginne ich mit dem ersten noch offenen Arbeitspaket in dieser Datei, von oben nach unten.
- Abschlussroutine pro Paket: Sobald der Nutzer mit `naechstes Paket` das vorherige Testing-Paket freigibt und es auf `[x]` gesetzt wird, mache ich fuer dieses zuletzt abgeschlossene Arbeitspaket den `git commit` und `git push`, bevor ich das naechste Arbeitspaket beginne.
- Paketgroesse: Pakete sollen so umgesetzt werden, dass sie fachlich zusammenhaengen und sinnvoll in einem Commit landbar sind.
- Fuege jedes Paket in die entsprechenden Reiter im Konfigurationsmenu ein. Erstelle sinvolle Unterreiter bei Bedarf.
- Ueberschreiben statt ueberspringen: Bestehende Horizon-Features, die mit einem Migrationspaket ueberlappen, werden ueberschrieben und neu implementiert — nicht uebersprungen.
- Herkunft: Im gesamten Horizon-Projekt duerfen keine Namen der Quellprojekte vorkommen. Alle Bezeichner, Kommentare, Logs und Config-Texte muessen so formuliert sein, als waeren die Funktionen von Beginn an Teil von Horizon.
- Kommando "naechstes Paket Bug:XXX": Wenn der Nutzer `naechstes Paket Bug:XXX` schreibt, wird das aktuelle Testing-Paket auf `[x]` gesetzt, committed und gepusht. Der Text nach `Bug:` wird als neuer Eintrag in die Bug-Fixes-Liste aufgenommen. Danach wird das naechste offene Arbeitspaket begonnen.

## Legende

- `[ ]` offen
- `[-]` testing
- `[x]` erledigt

---

## Bereits abgeschlossene Pakete

- [x] Paket DUN-01: Party Finder Tooltip-/Overlay-System
- [x] Paket CHAT-00: Chat Tabs
- [x] Paket CHAT-00A1: Chat Click Position Fix
- [x] Paket FISH-01: Fishing Rare Sea Creature Alerts (19 Creatures, Title + Sound, Filter-Dropdown)
- [x] Paket INV-01: Inventory Buttons (Layout-Editor, Button Config, Item Picker, Farming Tool Rebind)
- [x] Paket SCO-00: Scoreboard Konfigurationsreiter
- [x] Paket SCO-01: Custom Scoreboard Toggle
- [x] Paket SCO-02: Island-abhaengige Zeilenanzeige
- [x] Paket SCO-03: Scoreboard Config Qualitaet
- [x] Paket SCO-04: Dungeon-Klassen im Scoreboard
- [x] Paket MUC-00: Spotify ohne Developer Dashboard
- [x] Paket MUC-01: Music Control General-Tab, OAuth2, YouTube Playlist Overlay
- [x] Paket CHAT-00A: Boss Messages Hider, Rag Axe Notification
- [x] Paket CHAT-00B: Warping/Server/Profile Spam Filters
- [x] Paket DUN-04: Fancy Minimap Erweiterungen (Player Heads, Color, Labels, Checkmarks, Outline/Blur)

## In Testing

---

## Bug Fixes

- [ ] Fairy Door ESP: Wird nicht angezeigt oder an invaliden Stellen

---

## Phase 1 — Dungeon Visuals & Mob Features

- [x] Paket P1-01: Starred Mob ESP & Teammate Glow (StarMobESP mit Box/Outline/Glow-Modi, class-based Player Glow nach Dungeon-Klasse mit konfigurierbaren Farben, Entity Glow Mixin erweitern, Config Dungeon > Mobs Unterreiter)
- [x] Paket P1-02: Blood Door & Wither Door ESP (Wither-Door und Blood-Door als farbige Box/Outline rendern, Door-Key-Highlight wenn Spieler Schluessel im Inventar hat, Config Dungeon > Doors Unterreiter)
- [x] Paket P1-04: F4/M4 Spirit Bear Timer & Highlight (Spirit Bear Spawn-Timer HUD, Entity-Highlight per Glow, Config in Dungeon > Floor Specials)
- [ ] Paket P1-05: Mimic Detection & Message (Mimic-Kill-Erkennung per Chat + Entity-Scan, Prince-Nachricht ins Party-Chat, Config Toggles in Dungeon > Mobs)

## Phase 2 — Dungeon Map & Secrets

- [ ] Paket P2-01: Fancy Dungeon Map Grundsystem (vollstaendige Map mit Room-Scanning, Door-Erkennung, Room-State-Tracking, Room-Name-Overlay, Checkmarks fuer cleared/explored, eigener Map-Renderer mit DungeonInfo-Datenstruktur, Room/Door/Tile Klassen, Config Dungeon > Map)
- [ ] Paket P2-02: Fancy Map Player Heads & Decorations (Spielerkoepfe statt Marker, Klassen-Farben als Fallback, Room-Labels, Wither-Door/Blood-Door auf Map, Secret-Count pro Raum, Config Optionen)
- [ ] Paket P2-03: Score Calculator & HUD (Score-Berechnung aus Map-Daten: Skill/Explore/Speed/Bonus mit Secrets-Tracking, Score-Title bei S/S+, Score-Sound, Config Dungeon > Score)
- [ ] Paket P2-04: Secret Waypoints Grundsystem (Secret-Positionen aus externer Datenbasis laden, Waypoints fuer Chest/Wither Essence/Bat/Item/Lever/Redstone, Room-Rotation beruecksichtigen, Config Dungeon > Secrets)
- [ ] Paket P2-05: Secret Hitboxes & Tracker (visuelle Hitbox-Overlays fuer interagierbare Secrets, Player-Secrets-Tracker pro Spieler, Cleared-State pro Raum, Config Dungeon > Secrets)
- [ ] Paket P2-06: Dungeon Waypoint Commands (Custom Waypoints per Chat-Command setzen/loeschen, Waypoint-Sharing per Party-Chat, Config Dungeon > Waypoints)

## Phase 3 — Dungeon Run Helpers

- [ ] Paket P3-01: Run Splits Widget (Start/Blood/Boss/Clear-Zeitpunkte als HUD, M:SS-Format, Total Time, Lag-Lost, Wither-Door-Zaehler, frei platzierbares HUD-Element, Config Dungeon > Splits)
- [ ] Paket P3-02: Blessing Display HUD (aktive Dungeon-Blessings als HUD-Element anzeigen: Power/Life/Stone/Wisdom/Time mit Stufe, Scoreboard-/Tablist-Parser, Config Dungeon > General)
- [ ] Paket P3-03: Boss Bar Health Display (Boss-HP als dedizierte Anzeige statt Vanilla-Bossbar, formatierte HP-Zahl, Config Dungeon > Boss)
- [ ] Paket P3-04: Chest Profit Calculator (Dungeon-Chest-Inhalt bewerten, Gewinn/Verlust anzeigen, Lore-Parsing fuer Item-Preise, API-Preisabfrage, Config Dungeon > Loot)
- [ ] Paket P3-05: Salvage Overlay & Sellable Items (Salvage-Helper im Salvage-Screen: markiert salvageable Items, Sellable Dungeon Item Highlighter fuer NPC-verkaufbare Items, Config Dungeon > Loot)
- [ ] Paket P3-06: Reparty & Requeue (Reparty-Command /rp, Auto-Requeue als manuell bestaetigter Button, Party-Command-Formatter, Config Dungeon > Party)
- [ ] Paket P3-07: Breaker/Stonk Helper (AOTV/Etherwarp Stonk-Spot Prediction, Breaker-Animation-Helper, Config Dungeon > Helpers)

## Phase 4 — Floor 7 / Master Mode Specials

- [ ] Paket P4-01: F7/M7 Phase Titles (Phase-Uebergangs-Titel: Maxor/Storm/Goldor/Necron, P3 Terminal-Reformat, Config Dungeon > Boss)
- [ ] Paket P4-02: Maxor Crystal Waypoints (Crystal-Positionen als Waypoints, Spawn-Timer, Config Dungeon > Boss)
- [ ] Paket P4-03: Melody Alert & Display (Melody-Terminal erkannt → Alert/Sound, Melody-Fortschritt als HUD, Config Dungeon > Terminal)
- [ ] Paket P4-04: Terminal Hitboxes & Titles (Terminal-Positionen als Waypoints im Raum, Terminal-Typ als Title bei Approach, Config Dungeon > Terminal)
- [ ] Paket P4-05: I4 Device Helper (Lights-On-Device Hinweis-Overlay mit Loesungsvorschau, kein Auto-Click, Config Dungeon > Boss)
- [ ] Paket P4-06: Wither King ESP & Debuff Timer (WitherKing-Entity Highlight, Debuff-Timer/Reticle HUD fuer aktive Debuffs, Config Dungeon > Boss)
- [ ] Paket P4-07: Livid Solver (korrekten Livid per Farbe/Wool identifizieren, Highlight, Config Dungeon > Boss)
- [ ] Paket P4-08: Terracotta Timer (Terracotta-Phase Timer HUD, Config Dungeon > Boss)

## Phase 5 — Party Finder & Dungeon Economy

- [ ] Paket P5-01: Party Finder Erweiterungen (Join-Stats, PB-Zeitformatierung M:SS, Tablist-Party-Sync, Requirements/Class-Checks, Config Dungeon > Party Finder)
- [ ] Paket P5-02: Croesus Helper (Croesus-Chest-Status tracken: geoeffnet/nicht geoeffnet, Chest-Highlights, Config Dungeon > Loot)
- [ ] Paket P5-03: Dungeon Class & Ability Helpers (Ability Keybinds, Ult Reminder, Architect Draft Reminder, Config Dungeon > Class)
- [ ] Paket P5-04: Dungeon Refill Helper (Sack-/Twilight-Arrow-Refill Status als HUD/Chat-Hinweis, manuell klickbarer /gfs-Command, Config Dungeon > Helpers)

## Phase 6 — Puzzle & Device Solver Ueberarbeitung

- [ ] Paket P6-01: Puzzle Solver komplett entfernen und neu aufbauen (alle bestehenden Puzzle-Solver entfernen, neue Architektur mit einheitlichem Interface, Room-Gate-System, Render-Pipeline)
- [ ] Paket P6-02: Puzzle Solver Set A (Three Weirdos, Blaze, Creeper Beams, Quiz mit Timer, Config Dungeon > Puzzles mit individuellen Toggles)
- [ ] Paket P6-03: Puzzle Solver Set B (Tic-Tac-Toe, Ice Path, Ice Fill mit Background-Solve, Config Dungeon > Puzzles)
- [ ] Paket P6-04: Puzzle Solver Set C (Waterboard mit schnellerem Scanner, Boulder, Teleport Maze, Silverfish, Config Dungeon > Puzzles)
- [ ] Paket P6-05: Terminal Solver komplett entfernen und neu aufbauen (neue Terminal-Architektur: Order, Coloured Items, Item Name, Same Color, Wrong-Click Blocking, Hide Wrong Items, Config Dungeon > Terminal)
- [ ] Paket P6-06: Device Solver Set (Simon Says, Arrow Align, Target Practice, Melody Display/Alert, Lights On, Config Dungeon > Devices)

## Phase 7 — General QOL Features

- [ ] Paket P7-01: Auction Price Input & Search Calculator (Preis-Eingabefeld mit Kurzformaten wie 1.5m/500k, Search-Calculator mit x-Multiplikator, Config Misc > Auction)
- [ ] Paket P7-02: Inventory Search (Suchfeld ueber Inventar-Screens, Item-Name-Matching, nicht matchende Items ausgegraut, Config Inventory > General)
- [ ] Paket P7-03: Item Protection (Items als geschuetzt markieren, Drop/Sell/Salvage blockieren, visuelle Markierung, Config Inventory > Protection)
- [ ] Paket P7-04: Storage Overlay (Ender Chest / Backpack Vorschau ohne oeffnen, Page-Navigation, Config Inventory > Storage)
- [ ] Paket P7-05: Cake Numbers (New Year Cake Nummern als Slot-Text, fehlende Cakes markieren, Config Inventory > Slot Text)
- [ ] Paket P7-06: Reparty Command (/rp) (Party auflösen und neu einladen, Config Chat > Commands)
- [ ] Paket P7-07: Command Shortcuts (konfigurierbare Chat-Kommando-Aliase, z.B. /wh → /warp hub, Config Misc > Shortcuts)

## Phase 8 — Visual & Rendering Features

- [ ] Paket P8-01: Item Rarity Background (farbiger Hintergrund nach Item-Seltenheit in Inventar-Slots, Config Display > Items)
- [ ] Paket P8-02: Enchant Colors (farbige Enchantment-Namen nach Stufe/Typ, Config Display > Items)
- [ ] Paket P8-03: Damage Splash Formatting (Damage-Zahlen kompakter/farbiger, Hide-Option, Config Display > Rendering)
- [ ] Paket P8-04: Gyrokinetic Wand Circle Overlay (Kreis-Vorschau fuer Gyro-Wand Radius, Config Display > Helpers)
- [ ] Paket P8-05: Mask Timers HUD (Spirit Mask, Bonzo Mask, Phoenix Pet Cooldown-Timer als HUD, erweitert bestehendes Revival-HUD, Config HUD)
- [ ] Paket P8-06: Warp Cooldown Display (Warp-Cooldown Timer nach /warp, Config Display > General)
- [ ] Paket P8-07: Spring Boots HUD (Spring-Boots Ladebalken/Timer, Config Display > General)
- [ ] Paket P8-08: Freeze Display (Frozen/Stunned Timer HUD, Config Display > General)
- [ ] Paket P8-09: Pet Display HUD (aktives Pet als HUD-Element, Config HUD)
- [ ] Paket P8-10: CPS Display HUD (Clicks-per-Second Zaehler, Config HUD)
- [ ] Paket P8-11: Render Optimizer (unnoetige Entity-Renders in Dungeons reduzieren, Armor-Stand-Culling, Config Display > Performance)
- [ ] Paket P8-12: Block Overlay Optionen (Block-Highlight Farbe/Stil anpassen, Config Display > Rendering)
- [ ] Paket P8-13: Time Changer (Client-seitige Tageszeit aendern, Config Display > General)

## Phase 9 — Camera, Movement & Input

- [ ] Paket P9-01: Camera Tweaks (Third-Person Distanz, FOV-Lock, Viewmodel-Einstellungen, Config Display > Camera)
- [ ] Paket P9-02: Auto Sprint (Toggle-Sprint, Config Misc > Movement)
- [ ] Paket P9-03: No Rotate / No Cursor Reset (Server-Rotation blockieren, Cursor-Position in GUIs beibehalten, Config Misc > Tweaks)
- [ ] Paket P9-04: Loadout Keybinds (schneller Loadout-Wechsel per Hotkey, Config Inventory > Keybinds)

## Phase 10 — Chat & Sound Features

- [ ] Paket P10-01: Extended Spam Filters (Teleport Pad, Molten Wave, Show, Sky Mall, Lottery, Sack/Essence Spam, einzelne Kategorien schaltbar, Config Chat > Spam Filters)
- [ ] Paket P10-02: Sound Manager (individuelle Sound-Lautstaerke pro SkyBlock-Event, Mute-Liste, Config Misc > Sound)
- [ ] Paket P10-03: Mono Audio & Arrow Hit Sound (Mono-Audio-Option, Arrow-Treffer-Sound, Config Misc > Sound)
- [ ] Paket P10-04: NameTag Tweaks (NameTag-Formatierung, Level/Class/Health in NameTags, Config Display > NameTags)
- [ ] Paket P10-05: Chat Message Copy (Ctrl-Click kopiert Nachricht, Config Chat > General)

## Phase 11 — Fishing Features (Alerts)

- [ ] Paket P11-01: Bait Alerts (Bait-Wechsel, Bait-Vorrat niedrig, Fishing Bag deaktiviert, Config Fishing > Alerts)
- [ ] Paket P11-02: Consumable Alerts (Spirit Mask Activation, Deployable Expired, Salt Expired, Thunder Bottle Charged, Blizzard consumed, Config Fishing > Alerts)
- [ ] Paket P11-03: Spawn & Location Alerts (Golden Fish Spawn, Reindrake, Hotspot Gone, Wormhole Gone, Nessie Destination, Puddle Jumper Timer, Config Fishing > Alerts)
- [ ] Paket P11-04: Catch Alerts Erweiterung (Rare Drop Alert mit MF-Info, Trophy Fish/Frog Discovered, Worm the Fish, Lootshare Alert, Pet Level Up, Config Fishing > Alerts)
- [ ] Paket P11-05: Player Death Alert (eigener Tod beim Fischen, Armor-Check-Warnung wenn keine Fishing-Ruestung, Config Fishing > Alerts)

## Phase 12 — Fishing Features (Trackers & Overlays)

- [ ] Paket P12-01: Fishing Profit Tracker (Gewinn/Verlust pro Session, Drop-Tracking mit API-Preisen, Reset/Pause, Config Fishing > Trackers)
- [ ] Paket P12-02: Sea Creature Tracker (gefangene Sea Creatures zaehlen, pro Stunde, HP-Tracker fuer aktive Creatures, Config Fishing > Trackers)
- [ ] Paket P12-03: Bait & Consumable Tracker (aktive Baits zaehlen, Consumable-Timer: Deployables, Chum Bucket, Config Fishing > Trackers)
- [ ] Paket P12-04: Location-spezifische Tracker (Bayou, Crimson Isle, Lotus Atoll, Galatea Water, Jerry Workshop, Magma Core, Abandoned Quarry, Config Fishing > Location Trackers)
- [ ] Paket P12-05: Fishing Hook Timer & Barn Timer (Hook-Timer seit Auswerfen, Barn-Fishing-Timer, Rain-Timer, Hotspot-Tracker, Config Fishing > Timers)
- [ ] Paket P12-06: Fishing Festival & Treasure Tracker (Fishing Festival Event Tracker, Treasure Fishing Tracker, Archfiend Dice Profit, Config Fishing > Event Trackers)
- [ ] Paket P12-07: Nearby Entities Counter (Anzahl naher Entities beim Fischen, Config Fishing > General)

## Phase 13 — Fishing Features (Chat, Rendering, Items)

- [ ] Paket P13-01: Compact Catch Messages (Sea-Creature-Chat kompakter darstellen, Rare-Catch-Nachricht hervorheben, Config Fishing > Chat)
- [ ] Paket P13-02: Catch Sharing & Hotspot Messages (Rare Catch in All-Chat teilen, Hotspot-Location klickbar im Party-Chat, Config Fishing > Chat)
- [ ] Paket P13-03: Trophy Messages (Trophy Fish/Frog Discovered Chat-Nachricht formatieren, Config Fishing > Chat)
- [ ] Paket P13-04: Hide Other Hooks & Players (andere Spieler-Angelhaken ausblenden, Spieler in der Naehe des Bobbers ausblenden, Config Fishing > Rendering)
- [ ] Paket P13-05: Rare Mob Highlight (seltene Sea Creatures per Glow/Box hervorheben, Config Fishing > Rendering)
- [ ] Paket P13-06: Fishing Sound Mutes (Jade Dragon Sound, Reindrake Gifts muten, Config Fishing > Sound)
- [ ] Paket P13-07: Fishing Item Tooltips & Slot Text (Compacted Rod Part Progress, Expertise Kill Count, Moby Duck/Thunder Bottle Progress, Auto Recomb Flag, Config Fishing > Items)
- [ ] Paket P13-08: Fishing Personal Bests (Blizzard, Double Hook, Moby Duck PBs tracken und anzeigen, Config Fishing > Personal Bests)

## Phase 14 — Item & Tooltip Features

- [ ] Paket P14-01: Item Tooltip Erweiterungen (NPC-Preis, Bazaar-Preis, AH-Preis in Tooltips, Config Inventory > Tooltips)
- [ ] Paket P14-02: Slot Text Set 1 (Catacombs Level, Pet Level, Enchant Book Level, Minion Tier als Slot-Text, Config Inventory > Slot Text)
- [ ] Paket P14-03: Slot Text Set 2 (Potion Level, Rancher Speed, Attribute Shard Level, Wardrobe Slot, Config Inventory > Slot Text)
- [ ] Paket P14-04: Scrollable Tooltip (lange Tooltips scrollbar machen, Config Inventory > General)

## Phase 15 — Etherwarp & Teleport Erweiterung

- [ ] Paket P15-01: Etherwarp Erweiterung (Etherwarp-Prediction verbessern, Ender-Pearl Wand/Floor/Ceiling Warning, Config Display > Helpers)
- [ ] Paket P15-02: Instant Transmission Helper (AOTV Teleport-Vorschau, Config Display > Helpers)

## Phase 16 — Dungeon Floor Specials

- [ ] Paket P16-01: F3/M3 Guardian Health & Bounds (Guardian HP-Anzeige, Arena-Grenzen visualisieren, Config Dungeon > Floor Specials)
- [ ] Paket P16-02: F5/M5 Livid Identifier (korrekten Livid hervorheben, Config Dungeon > Floor Specials)
- [ ] Paket P16-03: Dungeon Run Alerts (Blood-Ready, Watcher-Ready per Chat-Erkennung, Overlay-/Sound-Warnung, Config Dungeon > General)

## Phase 17 — Profile Viewer Erweiterung

- [ ] Paket P17-01: Skills & Slayer Tabs (Skill-Levels und Slayer-Daten im Profile Viewer, Config Profile)
- [ ] Paket P17-02: Catacombs Tab (Catacombs-Stats, Klassen-Levels, Floor-PBs im Profile Viewer, Config Profile)
- [ ] Paket P17-03: Inventory & Storage Preview (Inventory, Ender Chest, Backpack im Profile Viewer, Config Profile)
- [ ] Paket P17-04: Wardrobe, Pets & Accessories (Wardrobe-Sets, Pets, Accessory Bag im Profile Viewer, Config Profile)

## Phase 18 — Health & Status Bars

- [ ] Paket P18-01: Health/Mana/Defense Bars (SkyBlock Health + Mana + Defense als Custom Bars, Config HUD > Bars)
- [ ] Paket P18-02: Speed/XP Bars (Speed + Skill-XP als Custom Bars, Config HUD > Bars)
- [ ] Paket P18-03: Bar Platzierung & Anchors (freie Platzierung aller Bars, Anchor-System, Config HUD > Bars)

## Phase 19 — Mining Features

- [ ] Paket P19-01: Crystal Hollows Map (CH-Map mit Spielerposition, wichtige Locations, Config Mining > Crystal Hollows)
- [ ] Paket P19-02: Commission HUD & Powder HUD (aktive Commissions + Powder als HUD, Config Mining > HUD)
- [ ] Paket P19-03: Metal Detector & Nucleus Helper (Metal Detector Solver, Nucleus Waypoints, Config Mining > Helpers)
- [ ] Paket P19-04: Cold Overlay & Corpse Finder (Cold-Overlay-Anpassung, Corpse-Waypoints, Config Mining > Glacite)

## Phase 20 — Slayer Features

- [ ] Paket P20-01: Vampire Helper (Effigy Timer, Melon Hit, Twinclaws Ice, Steak Stake Timer, Config Slayer > Vampire)
- [ ] Paket P20-02: Enderman Helper (Beacon Highlight, Yang Glyph, Nukekubi Heads, Laser Timer, Config Slayer > Enderman)
- [ ] Paket P20-03: Blaze Helper (Attunement Display, Fire Pillar Countdown, Config Slayer > Blaze)
- [ ] Paket P20-04: General Slayer HUD (Kill Time, PBs, Boss Highlighting, Spawn Alerts, Config Slayer > General)

## Phase 21 — Garden Features

- [ ] Paket P21-01: Farming HUD (Counter, Crops/min, Coins/h, Blocks/s, Level, XP/h, Config Garden > HUD)
- [ ] Paket P21-02: Visitor Helper (Buy-Shortcuts, Clipboard-Copy, Config Garden > Visitors)
- [ ] Paket P21-03: Pest Highlighter & Plot Widget (Pest-Entity Highlight, Garden Plots als Widget, Config Garden > General)

## Phase 22 — Miscellaneous Solvers & Helpers

- [ ] Paket P22-01: Experiments Solvers (Chronomatron, Ultrasequencer, Superpairs, Config Misc > Solvers)
- [ ] Paket P22-02: Mythological Ritual Helper (Diana Burrow Waypoints, Inquisitor Alert, Config Misc > Solvers)
- [ ] Paket P22-03: Chocolate Factory & Hoppity Helper (Chocolate Factory Overlay, Hoppity Egg Finder, Config Misc > Events)

## Phase 23 — Iris/Shader Compatibility & Performance

- [ ] Paket P23-01: Iris Shader Compatibility (Fullbright kompatibel mit Iris/Sodium, Render-Pipeline Anpassungen, Config Display > Performance)
- [ ] Paket P23-02: Resource Pack Compatibility (Texture-Fixes, Pack-Erkennung, Config Display > Performance)

## Phase 24 — Cleanup & Polish

- [ ] Paket P24-01: Config-Kategorien bereinigen (alle Reiter/Unterreiter konsistent benennen, leere Sektionen entfernen, Reihenfolge optimieren)
- [ ] Paket P24-02: Naming Audit (sicherstellen dass keine Quellprojekt-Namen im Code, Logs, Config oder GUI verbleiben)
- [ ] Paket P24-03: Performance Audit (Tick-Performance messen, unnoetige Berechnungen eliminieren, Render-Optimierungen)
- [ ] Paket P24-04: Feature Interaction Tests (sicherstellen dass alle Features zusammenarbeiten, keine Konflikte zwischen Overlays/HUDs/Mixins)
