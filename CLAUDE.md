# Horizon Mod Notes

## Project

Horizon is a Fabric client mod for Hypixel SkyBlock.

Supported Minecraft targets:
- `1.21.10`
- `1.21.11`

The project uses Java 21, Fabric Loom and Yarn mappings. The Gradle property `mcVersion` selects the target Minecraft version.

## Build And Test

Compile:

```powershell
.\gradlew.bat compileJava -PmcVersion=12110
.\gradlew.bat compileJava -PmcVersion=12111
```

Build jars:

```powershell
.\gradlew.bat build -PmcVersion=12110 --rerun-tasks
.\gradlew.bat build -PmcVersion=12111 --rerun-tasks
```

For the local `1.21.10` PrismLauncher setup, `build` also deploys the remapped Horizon jar directly into:

```text
C:/Users/marvi/AppData/Roaming/PrismLauncher/instances/1.21.10(1)/minecraft/mods
```

It removes older `horizon-mod-*.jar` files there before copying the new one.

You can override the deploy target with:

```powershell
.\gradlew.bat build -PmcVersion=12110 -PprismModsDir="C:/path/to/instance/minecraft/mods" --rerun-tasks
```

Output jars:

```text
build/libs/horizon-mod-1.0.0-1.21.10.jar
build/libs/horizon-mod-1.0.0-1.21.11.jar
```

Run client from Gradle:

```powershell
.\gradlew.bat runClient -PmcVersion=12110
.\gradlew.bat runClient -PmcVersion=12111
```

Avoid parallel Gradle builds for both MC versions in the same workspace. Loom writes into shared build directories and can temporarily produce invalid or tiny jars.

## Important Entry Points

- `de.horizon.HorizonClient`: main client initializer, commands, tick hooks, render hooks, HUD registry.
- `de.horizon.config.HorizonConfig`: persisted config values.
- `de.horizon.screen.HorizonConfigScreen`: custom config UI.
- `de.horizon.feature.dungeon.DungeonSolverOverlay`: terminal overlays and world puzzle overlays.
- `de.horizon.feature.dungeon.room.DungeonRoomDetector`: room detection foundation for dungeon puzzle solvers.
- `de.horizon.hypixel.HypixelProfileService`: profile and dungeon stats loading, currently SkyCrypt first, Hypixel API fallback.
- `de.horizon.hypixel.PartyFinderOverlay`: party finder overlay for S+ times.
- `de.horizon.spotify.SpotifyService`: Spotify auth/playback integration.

## Commands

- `/horizon`: opens Horizon config.
- `/hv`: opens the ingame Horizon profile screen for the current player.
- `/hv <player>`: opens the ingame Horizon profile screen for another player.

There is a command fallback in `HorizonClient.handleClientCommandFallback`. Keep it compatible with both command formats, with and without a leading slash.

For Minecraft `1.21.10`, do not rely only on Fabric client command registration. `HorizonClient.registerScreenHooks` also intercepts Enter in `ChatScreen` and handles `/horizon` and `/hv` before the message is sent to the server. Keep both paths working.

## HUD Rules

HUD elements are registered in `HorizonClient`.

Each HUD element decides:
- if it is enabled via config
- if it is movable
- default position
- render size

Only HUDs with `isMovable() == true` should be editable in the HUD layout screen.

Ping is handled by `de.horizon.feature.misc.PingService`. It is ticked from `HorizonClient.onClientTick` and should follow the forced vanilla ping sample flow: force vanilla ping sampling in `ClientPlayNetworkHandler.tick`, then read recent samples from the debug HUD ping log. Do not move ping updates into HUD render only.

The dungeon solver HUD panel is a debug overlay. Keep it controlled by `HorizonConfig.solverDebugHudEnabled` from the Config UI under `Misc`, while solver highlights and world-space overlays remain controlled by their existing dungeon solver settings.

The room scanner should be signature-based and dungeon-only:
- use the room core database from `assets/horizon/dungeons/rooms.json`
- scan only while `DungeonStateService.isInDungeon()` is true
- do not render world outlines for room scanning
- prefer room-relative puzzle logic over loose nearby-block heuristics

Spotify inventory controls support a collapsed arrow-only state and in-overlay device switching. Keep those interactions inside `SpotifyInventoryOverlay` and `SpotifyService`, without adding browser-only flows.

Hypixel SkyBlock sidebar replacement is rendered as a slim bottom status bar:
- hide the vanilla right-side sidebar only while the replacement is active
- keep the overlay compact across the full bottom edge
- move the hotbar and lower vanilla HUD bars up enough so the custom status bar stays readable
- move related text anchors such as chat input and XP level with the raised lower HUD
- allow the vanilla armor/defense bar to be hidden through `Misc` when desired
- when compressing Hypixel overflow health into one row, prefer vanilla HUD atlas heart sprites over custom external GUI textures

Current Skyblocker-style reimplementation is iterative:
- start with the `Dungeons` area first, because Horizon already has the strongest dungeon foundation
- the first Dungeons package adds config-managed dungeon utilities instead of trying to land the whole Skyblocker dungeon surface at once
- keep rare room alerts based on `DungeonRoomDetector` room names and origins so alerts only trigger once per discovered room

## Config UI

`HorizonConfigScreen` is intentionally non-graphical right now. Keep it as a simple text-first config screen instead of rebuilding decorative cards, themed chrome or custom background art.

Current screen rules:
- `HORIZON` stays top-left and the search field stays top-right
- main tabs live in a left text sidebar
- dungeon subtabs stay in a top text row under the header
- only feature rows get descriptions; tabs and subtabs stay label-only
- the content area must stay resolution-aware and compact so many settings fit on screen
- scrollable content should stay clipped to the viewport, but avoid reintroducing heavyweight graphical mask systems unless they are strictly necessary
- avoid screen-blur background calls in the config screen on `1.21.10`; use a simple flat dim background instead
- the `HUD` tab owns the shared Horizon accent color as a free hex value like `#FF66CC`; other Horizon HUDs and overlay panels should read from the same accent source instead of hardcoded turquoise values

## Dungeon Solver Direction

Do not keep adding one-off puzzle heuristics directly into `DungeonSolverOverlay`.

Preferred structure:
- detect current room in `DungeonRoomDetector`
- expose room name, confidence, origin and rotation
- solve puzzles using room-relative positions where possible
- render overlays through stable world-space boxes, not camera-relative screen guesses

The current room detector should stay database-driven with known room signatures and room-relative puzzle positions.

Solver color semantics should stay consistent across world overlays and terminal overlays:
- current action or current target: purple
- next likely steps or previews: pink
- already used, visited or consumed options: red

Teleport Maze should highlight the most likely next pad in purple, other likely candidates in pink, and visited pads in red.

Boulder solver progress should stay tied to the loaded room solution after the first correct click. Do not drop the overlay just because the live room layout hash changed during puzzle progress.

Ice Fill should render as a thin pre-drawn path above the floor, not as full block overlays.

## System Stats Limits

CPU load is provided by OSHI.

CPU temperature depends on available Windows sensors. Many systems do not expose CPU package temperature through standard WMI. LibreHardwareMonitor or OpenHardwareMonitor can expose better sensor data under:

```text
root\LibreHardwareMonitor
root\OpenHardwareMonitor
```

GPU usage/temperature uses `nvidia-smi` when available. Non-NVIDIA GPUs may show `n/a`.

## Style

- Keep code modular and client-only.
- Prefer explicit services over large feature logic inside screens.
- Avoid external browser flows where an ingame UI is practical.
- Keep Fabric 1.21.10 and 1.21.11 compatibility in mind when using mapped Minecraft methods.
