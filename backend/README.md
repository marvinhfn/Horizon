# Horizon Backend

Dieses Backend ist die serverseitige Komponente fuer Horizon. Es ist dafuer gedacht, den Hypixel-API-Key ausschliesslich auf dem Server zu halten und spaeter die `/hv`-Profildaten fuer den Mod bereitzustellen.

## Warum ein Backend?

Hypixel schreibt aktuell, dass API-Keys nicht in Mods eingegeben, verteilt oder im Client gespeichert werden sollen. Fuer einen oeffentlich oder halb-oeffentlich genutzten Mod ist ein serverseitiges Backend deshalb der richtige Weg.

Relevante Quellen:

- https://developer.hypixel.net/policies/
- https://developer.hypixel.net/create/
- https://api.hypixel.net/

## Aktueller Stand

Dieses Verzeichnis ist ein Scaffold:

- `GET /health` prueft, ob der Dienst laeuft
- `POST /v1/auth/token` liefert vorerst ein kurzlebiges Dev-Token
- `GET /v1/skyblock/profile?player=<name>` ruft Mojang plus Hypixel serverseitig ab und liefert eine erste Profilsummary

Die produktive Minecraft-Signaturpruefung und der echte Storage-/Inventory-Parser folgen im naechsten Schritt.

## Benoetigte Umgebungsvariablen

- `HORIZON_PORT`
- `HORIZON_BASE_URL`
- `HORIZON_DEV_AUTH_SECRET`
- `HYPIXEL_API_KEY`
- `HYPIXEL_APP_NAME`

Das Backend liest zuerst echte Environment-Variablen und faellt lokal auf eine `backend/.env` zurueck.

## Starten

```powershell
cd backend
..\gradlew.bat run
```

## Was ich von deinem Hypixel Dashboard brauche

Du musst mir nicht den Key fuer den Client geben. Der Key gehoert nur auf den Server.

Fuer die naechsten Schritte brauche ich von dir:

1. Den registrierten App-Namen.
2. Ob es `Personal` oder `Production` ist.
3. Die URL, die du im Dashboard eintraegst.
4. Spaeter den Key nur fuer die Server-Konfiguration.

## Empfehlung fuer die URL im Dashboard

Wenn du noch keine echte Domain hast, entwickle zunaechst lokal mit `http://localhost:8787` und entscheide die spaetere Projekt- oder API-Domain erst vor dem echten Rollout.
