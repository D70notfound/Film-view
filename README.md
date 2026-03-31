# NepuView

Native Android Streaming-App für nepu.to — mit Netflix-Style UI, HLS-Downloads und Offline-Playback.

## Features

- **Netflix-Style Home** — Hero-Banner + horizontale Karussell-Reihen pro Kategorie
- **WebView-Player** — Vollbild, PiP (Picture-in-Picture), automatisches Landscape-Fullscreen
- **HLS-Downloads** — M3U8-Streams herunterladen und offline abspielen (Media3)
- **Suche** — SearchBar mit Suchhistorie als Chips
- **Favoriten & Watchlist** — Swipe-Gesten (rechts = Watchlist, links = löschen)
- **Verlauf** — Watch-History mit Fortschrittsbalken
- **Continue Watching** — Position wird alle 5s gespeichert
- **Responsive Navigation** — BottomNav / NavigationRail / Drawer je nach Bildschirmgröße
- **Dark Theme** — Material 3, OLED-optimiert

## Anforderungen

| | Minimum |
|---|---|
| Android | 8.0 (API 26) |
| RAM | 2 GB |
| Speicher | 100 MB (App) + Download-Speicher |
| Internet | Ja (für Streaming; offline nach Download) |

## Installation

### Option A — APK direkt installieren

1. [Neuestes Release](https://github.com/D70notfound/Film-view/releases) herunterladen
2. Auf dem Gerät: **Einstellungen → Sicherheit → Unbekannte Quellen** erlauben
3. APK öffnen und installieren

### Option B — Selbst bauen

```bash
# 1. Repository klonen
git clone https://github.com/D70notfound/Film-view.git
cd Film-view

# 2. Debug-APK bauen
./gradlew assembleDebug

# APK liegt unter:
# app/build/outputs/apk/debug/app-debug.apk

# 3. Auf Gerät installieren (ADB)
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Release APK bauen

```bash
export KEYSTORE_FILE=/pfad/zum/keystore.jks
export KEYSTORE_PASSWORD=dein_passwort
export KEY_ALIAS=dein_alias
export KEY_PASSWORD=dein_key_passwort

./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Architektur

```
net.nepuview/
├── NepuApp.kt               # @HiltAndroidApp + globaler Exception-Handler
├── data/                    # Room Entities + DAO + AppDatabase
├── scraper/                 # ScraperEngine (WebView + JS-Injection)
├── repository/              # FilmRepository, DownloadRepository
├── viewmodel/               # HomeViewModel, PlayerViewModel, ...
├── ui/                      # Fragments, MainActivity, Dialoge
├── adapter/                 # RecyclerView Adapter
├── download/                # Media3 DownloadService + Notifications
└── util/                    # NetworkMonitor, PermissionHelper
```

**Pattern:** MVVM + Repository + Hilt DI + Kotlin Coroutines/Flow

### Datenfluss

```
nepu.to  →  ScraperEngine (JS-Injection)  →  FilmRepository  →  ViewModel  →  Fragment
```

## Scraper-Konfiguration

Der Scraper (`ScraperEngine.kt`) verwendet JS-Injection in einem versteckten WebView.
Falls nepu.to seine HTML-Struktur ändert:

1. `ScraperEngine.kt` → `FILM_LIST_JS` / `DETAIL_JS` suchen
2. CSS-Selektoren mit Browser-DevTools auf nepu.to aktualisieren
3. Neu bauen und testen

**Whitelist:** Nur `nepu.to` und `vr-m.net` laden im Player.
Falls weitere CDN-Domains nötig: `PlayerFragment.kt` → `allowedHosts` erweitern.

## CI/CD

| Workflow | Trigger | Aktion |
|---|---|---|
| `build.yml` | Push / PR auf main | Build + Unit Tests + Lint |
| `release.yml` | Git Tag `v*.*.*` | Release APK + GitHub Release |

## Häufige Fehler

| Problem | Lösung |
|---|---|
| Schwarzer Bildschirm im Player | CDN-Domain blockiert → `allowedHosts` in `PlayerFragment` prüfen |
| "Keine Inhalte gefunden" | Netzwerk prüfen oder CSS-Selektoren aktualisieren |
| Download startet nicht | `POST_NOTIFICATIONS` Permission erteilen (Android 13+) |
| App crasht beim Start | `adb logcat -s NepuApp` |

## Lizenz

Persönlicher, nicht-kommerzieller Gebrauch. Inhalte werden von nepu.to bereitgestellt.
