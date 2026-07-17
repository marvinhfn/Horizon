# Horizon Projektuebersicht

## Kurzbeschreibung

Horizon ist ein Hypixel-SkyBlock-Mod fuer Fabric auf Basis von Minecraft `26.1.2`. Der Schwerpunkt liegt auf Ingame-Quality-of-Life-Funktionen fuer Dungeons, Profilinformationen, HUD-Anpassungen und mehreren Overlay-Systemen. Ergaenzt wird der Client durch ein optionales Java-Backend, das sensible API-Zugriffe serverseitig kapselt.

## Repository-Struktur

- `src/`: Client-Mod mit Commands, HUD, Overlays, Config-Screen und Integrationen
- `backend/`: separates Backend fuer Dev-Auth und SkyBlock-Profildaten
- `src/main/resources/assets/horizon/`: Datenbasis fuer Dungeon-Raeume, Puzzle-Loesungen und Texturen

## Client-Funktionen

- Konfigurationsscreen und HUD-Layout direkt im Spiel
- Profile Viewer fuer den eigenen oder einen beliebigen Spielernamen
- Dungeon-Unterstuetzung mit Raumdatenbank, Solver-Overlays und Alerts
- Revival-Tracking fuer typische Death-Save-Quellen wie Spirit Mask oder Bonzo Mask
- Performance-, Ping-, TPS-, Uhrzeit- und Systemstatistik-HUDs
- Chat-Tabs, Chat-Filter und Particle-Filter
- Spotify- und YouTube-Music-Steuerung innerhalb von Inventar-Screens
- Eigene Hypixel-Statusleiste als Ersatz fuer die Sidebar
- Inventory Buttons mit konfigurierbarem Layout
- Fishing Alerts fuer seltene Sea Creatures
- 16:9 Pillarbox-Modus fuer Ultrawide-Monitore

## Technische Basis

- Java 25
- Fabric Loom 1.15
- Mojang Official Mappings (MC 26.1.2 ist unobfuscated)
- Fabric API 0.151.0+26.1.2
- OSHI fuer Systemmetriken
- Javalin im Backend (Java 21)

## Relevante Klassen

- `de.horizon.HorizonClient`: zentraler Client-Entrypoint
- `de.horizon.config.ConfigManager`: Laden und Speichern der Konfiguration
- `de.horizon.screen.HorizonConfigScreen`: Ingame-Einstellungen
- `de.horizon.screen.PlayerProfileScreen`: Profildarstellung
- `de.horizon.feature.dungeon.DungeonSolverOverlay`: Dungeon- und Puzzle-Overlays
- `de.horizon.feature.dungeon.room.DungeonRoomDetector`: Raum-Erkennung auf Datenbasis
- `de.horizon.hypixel.HypixelProfileService`: Profil- und Dungeondaten im Client
- `de.horizon.api.profile.HorizonProfileGateway`: Anbindung an das Horizon-Backend
- `de.horizon.spotify.SpotifyService`: Spotify-Authentifizierung und Playback
- `de.horizon.youtube.YoutubeService`: YouTube-Music-Integration
- `de.horizon.backend.HorizonBackendApplication`: Startpunkt des Backends

## Build und Ausfuehrung

Client:

```bash
./gradlew build --rerun-tasks
```

Backend:

```bash
cd backend
../gradlew build --rerun-tasks
```

## Verbindliche Arbeitsroutine fuer jeden Prompt

Bei jeder Arbeitsanfrage gilt diese Reihenfolge:

1. Dateien lesen, aendern oder neue Dateien anlegen.
2. Anschliessend beide Builds ausfuehren, damit die Artefakte in die jeweiligen Build-Ordner geschrieben werden, so wie es die vorhandenen Gradle-Builddateien vorsehen:

```bash
./gradlew build --rerun-tasks
cd backend
../gradlew build --rerun-tasks
```

Hinweise fuer die Ausfuehrung:

- Der Root-Build baut fuer Minecraft 26.1.2 und fuehrt dabei die in `build.gradle` hinterlegte Kopier-/Deploy-Logik aus.
- Der Backend-Build schreibt sein Jar ebenfalls gemaess `backend/build.gradle` in den Backend-Build-Ordner.
- Auf NixOS werden die Builds ueber `nix-shell` ausgefuehrt: `nix-shell -p temurin-bin-25 --run "./gradlew build --rerun-tasks"` fuer den Client, `nix-shell -p temurin-bin-21 temurin-bin-25 --run "cd backend && ../gradlew build --rerun-tasks"` fuer das Backend.

## Konfiguration

Die Client-Konfiguration wird lokal gespeichert. Das Backend liest seine Werte aus Umgebungsvariablen oder aus einer nicht versionierten `backend/.env`.

Belegte Backend-Variablen:

- `HORIZON_PORT`
- `HORIZON_BASE_URL`
- `HORIZON_DEV_AUTH_SECRET`
- `HYPIXEL_API_KEY`
- `HYPIXEL_APP_NAME`

## Git-Richtlinien

Commits und Pushes werden ausschliesslich unter dem GitHub-Profil des Repository-Inhabers durchgefuehrt. Kein anderer Contributor (z.B. Co-Authored-By von Claude) darf in Commit-Messages eingetragen werden. Commits immer ohne fremde Autorenangaben erstellen.

## Ziel des Dokuments

Diese Datei beschreibt den Aufbau und den aktuellen Zweck des Projekts. Sie ist als interne Projektreferenz gedacht und nicht als Nutzeranleitung oder Arbeitsanweisung formuliert.

---

## Aktueller Arbeitsstand (Stand: 2026-07-17)

### Was in den letzten Sessions implementiert wurde

Alle Dungeon-Features wurden komplett ueberarbeitet und auf MC 26.1.2 angepasst. Beide Builds (Client + Backend) sind sauber.

**Ueberarbeitete Klassen:**

- `de.horizon.feature.dungeon.room.DungeonRoomDetector` — Komplett neu geschrieben mit Dungeon-Koordinatensystem (Corner -200/-200, Room 31, Door 1) und Legacy-Block-ID-Hashing
- `de.horizon.feature.dungeon.room.LegacyBlockRegistry` — Neu erstellt: Mappt moderne Block-Registry-Namen auf Legacy-Numeric-IDs fuer Room-Hash-Berechnung
- `de.horizon.feature.dungeon.terminal.TerminalSolverService` — Komplett neu geschrieben: LEGACY_NAMES Map (90+ Eintraege), COLOR_FIXES, DataComponents.ENCHANTMENT_GLINT_OVERRIDE statt hasFoil(), ORDER-Animation-Kompensation, RUBIX-Held-Item-Kompensation
- `de.horizon.feature.dungeon.boss.SimonSaysService` — Neu geschrieben: Paket-basierte Erkennung (SEA_LANTERN bei x=111), Grid-Reset-Detection via Section-Updates, Start-Button-Logik mit Solution-Trimming
- `de.horizon.feature.dungeon.boss.ArrowAlignService` — Neu geschrieben: 9 bekannte Loesungen (37-Element-Arrays), Entity-Scan fuer Item-Frames bei x=-2, Klick-Berechnung als 3D-Text
- `de.horizon.hud.DungeonMapHudElement` — Farb-Konvertierung auf MapColor.getColorFromPackedId() umgestellt, ARGB→ABGR korrekt
- `de.horizon.feature.dungeon.LeapMenuOverlay` — Unterstuetzt jetzt auch "Teleport to Player" (Haunted-Variante)
- `src/main/resources/assets/horizon/dungeons/rooms.json` — Mit korrekten Legacy-ID-basierten Core-Hashes aktualisiert

**Behobene Probleme:**

- Raumerkennung: Block.toString() durch LegacyBlockRegistry.getLegacyId() ersetzt → Hashes stimmen jetzt mit rooms.json ueberein
- Terminal "Starts With": LEGACY_NAMES Map fuer Hypixel-Item-Namen (z.B. "Oak Wood Planks" statt "Oak Planks")
- Terminal Completion: DataComponents.ENCHANTMENT_GLINT_OVERRIDE statt hasFoil()
- Dungeon-Map Farben: Manuelle Palette durch MapColor.getColorFromPackedId() ersetzt
- Simon Says: Polling durch Block-Update-Events ersetzt, korrekte Sea-Lantern-Erkennung
- Arrow Align: 37-Element-Arrays fuer Loesungen und korrekte Frame-Position (x=-2, y=120-124, z=75-79)

### Bekannte offene Punkte

**1. Dungeon-Map Rendering — In-Game-Verifizierung noetig**

- MapColor.getColorFromPackedId() wird jetzt verwendet (kanonische API), aber noch nicht in-game getestet ob Farben korrekt angezeigt werden
- Datei: `src/main/java/de/horizon/hud/DungeonMapHudElement.java`

**2. Raumerkennung — In-Game-Verifizierung noetig**

- LegacyBlockRegistry und rooms.json sollten theoretisch funktionieren, muss aber in-game getestet werden
- Dateien: `src/main/java/de/horizon/feature/dungeon/room/DungeonRoomDetector.java`, `src/main/java/de/horizon/feature/dungeon/room/LegacyBlockRegistry.java`

**3. Simon Says / Arrow Align — Koordinaten noch nicht in-game verifiziert**

- Koordinaten sind vermutlich korrekt, aber in-game-Test steht aus
- Simon Says: Grid x=111, Buttons x=110, Start-Button (110,121,91)
- Arrow Align: Frames x=-2, y=120-124, z=75-79

**4. Arrow Device (F4/S4) — Nicht implementiert**

- Betrifft Floor 4 / Sonderfloor S4
