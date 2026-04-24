# Horizon

Fabric Client Mod fuer Hypixel SkyBlock QoL.

## Status

Dieses Projekt ist auf Minecraft `1.21.1` ausgelegt. Die von dir genannte Version `1.21.11` existiert fuer Java Edition so nicht, deshalb ist das Projekt auf `1.21.1` aufgebaut.

## Vorhandene Funktionen

- Client-Command `/horizon` oeffnet ein Ingame-Optionsmenue
- Verschiebbares HUD-System mit explizit freigegebenen HUD-Elementen
- Erstes HUD fuer:
  - Spirit Mask
  - Bonzo Mask
  - Phoenix Pet
- Gruener Ready-Status oder Cooldown-Timer pro Eintrag
- Persistente Konfiguration in `config/horizon.json`

## Projektstruktur

- `de.horizon.HorizonClient`: Client-Einstiegspunkt
- `de.horizon.config`: Laden/Speichern der Konfiguration
- `de.horizon.hud`: HUD-Registry und HUD-Elemente
- `de.horizon.feature.revive`: Cooldown-Tracking fuer Death-Save-Abilities
- `de.horizon.screen`: Config-Screen und HUD-Layout-Screen

## Hinweise

- Die Erkennung der Cooldowns laeuft aktuell ueber Hypixel-Chatnachrichten.
- Bonzo Mask verwendet einen konfigurierbaren Cooldown, weil der echte Wert von deinem Dungeoneering-Level abhaengt.
- Neue HUD-Elemente koennen ueber `HudRegistry` registriert werden. Verschiebbar sind nur Elemente, bei denen `isMovable()` `true` liefert.
