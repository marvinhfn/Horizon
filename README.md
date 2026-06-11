# Horizon

Horizon ist ein clientseitiger Fabric-Mod fuer Hypixel SkyBlock auf Minecraft `26.1.2`. Das Projekt kombiniert Ingame-Overlays, HUD-Elemente, Dungeon-Hilfen, Profilansichten und eine optionale Backend-Anbindung fuer serverseitig geladene Profildaten.

## Projektstand

Der Mod ist fuer Minecraft `26.1.2` ausgelegt. Gebaut wird mit Fabric Loom 1.15, Mojang Official Mappings und Java 25.

## Funktionsumfang

- Ingame-Konfiguration ueber `/horizon` und den Hotkey `H`
- Profilansicht ueber `/hv` und `/hv <spieler>`
- Verschiebbares HUD-System mit Zeit-, Performance-, System- und Revive-Anzeigen
- Dungeon-Funktionen mit Raum-Erkennung, Puzzle-Overlays und Alerts
- Party-Finder-Overlay fuer Dungeon-Runs
- Ersetzende Hypixel-Sidebar als kompakte Statusleiste
- Chat-Tabs und Chat-Filter fuer SkyBlock-spezifische Stoerquellen
- Particle-Filter mit einzeln konfigurierbaren Partikeltypen
- Inventory Buttons mit frei konfigurierbarem Layout
- Fishing Alerts fuer seltene Sea Creatures
- Spotify- und YouTube-Music-Steuerung direkt aus Inventar-Screens
- 16:9 Pillarbox-Modus fuer Ultrawide-Monitore
- Farming Tool Rebind und Squeaky Mousemat Unterstuetzung
- Optionale Backend-Anbindung fuer authentifizierte Profil- und Inventardaten

## Architektur

Das Repository besteht aus zwei Teilen:

- `src/`: Fabric-Client-Mod mit allen Ingame-Funktionen
- `backend/`: kleines Javalin-Backend fuer Dev-Auth und serverseitige Hypixel-Profilabfragen

Wichtige Einstiegspunkte:

- `src/main/java/de/horizon/HorizonClient.java`: registriert Commands, Keybinds, Tick-Hooks, HUDs und Overlays
- `src/main/java/de/horizon/screen/HorizonConfigScreen.java`: textorientierte Ingame-Konfiguration
- `src/main/java/de/horizon/screen/PlayerProfileScreen.java`: Profilansicht im Client
- `src/main/java/de/horizon/feature/dungeon/`: Dungeon-State, Solver und Alerts
- `src/main/java/de/horizon/spotify/`: Spotify-Integration im Inventar
- `src/main/java/de/horizon/youtube/`: YouTube-Music-Integration im Inventar
- `backend/src/main/java/de/horizon/backend/HorizonBackendApplication.java`: Startpunkt des Backends

## Build

Client kompilieren und bauen:

```bash
./gradlew build --rerun-tasks
```

Fuer lokale Entwicklung kann der Build die Mod-Datei direkt in eine PrismLauncher-Instanz kopieren. Der Zielpfad laesst sich ueber `-PprismModsDir=...` ueberschreiben.

## Backend

Das Backend ist fuer lokale oder eigene Deployments gedacht und haelt sensible Server-Konfiguration ausserhalb des Clients. Aktuell stellt es drei Endpunkte bereit:

- `GET /health`
- `POST /v1/auth/token`
- `GET /v1/skyblock/profile?player=<name>`

Konfiguration erfolgt ueber Umgebungsvariablen oder lokal ueber `backend/.env`. Eine Vorlage liegt in `backend/.env.example`.

Start:

```bash
cd backend
../gradlew run
```

## Hinweise zum Repository

- `CLAUDE.md` dient als projektinterne Referenz und ist bewusst nicht fuer das Public-Repository vorgesehen.
- Lokale `.env`-, Key-, Zertifikats- und Laufzeitdateien sollten nicht versioniert werden.
- Bereits veroeffentlichte sensible Dateien oder Inhalte muessen zusaetzlich aus der Git-Historie entfernt und gegebenenfalls rotiert werden; eine `.gitignore` verhindert nur neue Commits.
