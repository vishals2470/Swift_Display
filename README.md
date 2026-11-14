# Swift Cockpit

A native Android (Kotlin + Jetpack Compose) driver dashboard for a 10-inch, 1080p landscape
head unit in a Maruti Suzuki Swift. Live GPS speed, clock and quick hand-offs to maps, media
and settings. Principle: **never show a number that isn't real and current.**

## Status

| Phase | Scope | State |
|---|---|---|
| 1 | Project foundation: Gradle module layout, wrapper, version catalog, CI | ✅ |
| 2 | Reliable GPS speed: explicit states, stale-fix timeout, accuracy filter, tests | ✅ |
| 3 | Dashboard UI rework: readable type, scaling to any screen, now-playing media | ✅ |
| 4 | Home-screen (launcher) role with app drawer and escape hatch | ⏳ |
| 5 | Embedded live map (MapLibre + MapTiler), offline-tolerant | ⏳ |
| 6 | Signed release build, install guide, road-test checklist | ⏳ |

## Architecture

```
app/src/main/java/com/swiftcockpit/
├── MainActivity.kt          Immersive full-screen host, keeps the screen on
├── location/
│   ├── SpeedState.kt        Live / Acquiring / GpsDisabled / PermissionRequired / PreciseLocationRequired
│   ├── SpeedEvaluator.kt    Pure logic: freshness (3 s), accuracy filter, parking jitter → unit-tested
│   └── GpsSpeedSource.kt    LocationManager GPS_PROVIDER → Flow<SpeedState>
├── media/
│   ├── NowPlayingSource.kt  Active media session → title/artist/art + play/pause/skip
│   └── MediaNotificationListener.kt  Only to obtain notification access (required by Android)
├── system/ExternalApps.kt   Crash-safe hand-offs to Maps, music, settings
└── ui/
    ├── CockpitViewModel.kt  StateFlows; GPS runs only while the dashboard is visible
    ├── CockpitScreen.kt     Screen wiring + stateless CockpitContent
    ├── components/          Speed, navigation and media panels, shared widgets
    └── theme/               Colours; ReferenceScale (1280×720 dp canvas scaled to any screen)
```

**Why GPS_PROVIDER rather than the Play Services fused provider:** fused blends Wi-Fi/cell
positions and smooths output, which suits "where am I" but not real-time ground speed, and it
would make the app depend on Play Services.

**Speed rules** (`SpeedEvaluator`):
- A reading older than 3 seconds is discarded and the display shows `--` (tunnel, garage, lost signal).
- Fixes with no speed, or speed accuracy worse than ±5 m/s, are ignored.
- Below 2 km/h reads as 0 to hide GPS jitter when parked.

## Build

Requirements: JDK 17, Android SDK Platform 35 (Android Studio Ladybug or newer handles both).

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Every push also builds on GitHub Actions; the debug APK is attached to each run as the
`swift-cockpit-debug-apk` artifact.

## Screen sizes

The layout is designed on a 1280×720 dp canvas (a 1080p screen at "smallest width 720") and
scaled uniformly to the actual display, so it looks the same on 1024×600, 1080p or 1200p units.

## Now playing (media panel)

Android only lets an app read other apps' media sessions if it has **notification access**.
The media panel has an *ALLOW ACCESS* button that opens the right settings page. On Android 13+
a sideloaded app may show that switch greyed out: open **App info → ⋮ → Allow restricted
settings**, then grant access. No notification content is read or stored.

## First-run test on the head unit

1. Install the APK and launch **Swift Cockpit**. Allow **precise** location when asked.
2. Make sure device location is on. Outdoors, the status should go from `GPS SEARCH` to `GPS LOCK`.
3. Compare speed with a trusted GPS speed app. Drive into a covered area: within ~3 s the speed should change to `--`.
4. Grant media access, play music in any app: title, artist and controls should appear and work.
5. Tap NAVIGATE, APPS and SETTINGS; return to the app and check it stays full screen.

Do not interact with the screen while driving.

## Not in scope yet

- OBD2 (Bluetooth ELM327): planned as an optional module that only displays values the ECU actually returns.
- No vehicle data is fabricated.
