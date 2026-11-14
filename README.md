# Swift Cockpit

A native Android (Kotlin + Jetpack Compose) driver dashboard for a 10-inch, 1080p landscape
head unit in a Maruti Suzuki Swift. Live GPS speed, clock and quick hand-offs to maps, media
and settings. Principle: **never show a number that isn't real and current.**

## Status

| Phase | Scope | State |
|---|---|---|
| 1 | Project foundation: Gradle module layout, wrapper, version catalog, CI | ✅ |
| 2 | Reliable GPS speed: explicit states, stale-fix timeout, accuracy filter, tests | ✅ |
| 3 | Dashboard UI rework for 1080p and at-a-glance readability | ⏳ |
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
├── system/ExternalApps.kt   Crash-safe hand-offs to Maps, music, settings
└── ui/
    ├── CockpitViewModel.kt  StateFlows; GPS runs only while the dashboard is visible
    ├── CockpitScreen.kt     Compose UI
    └── theme/
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

## First-run test on the head unit

1. Install the APK and launch **Swift Cockpit**. Allow **precise** location when asked.
2. Make sure device location is on. Outdoors, the status should go from `GPS SEARCH` to `GPS LOCK`.
3. Compare speed with a trusted GPS speed app. Drive into a covered area: within ~3 s the speed should change to `--`.
4. Tap NAVIGATE, MEDIA, ALL APPS and SYSTEM SETTINGS; return to the app and check it stays full screen.

Do not interact with the screen while driving.

## Not in scope yet

- OBD2 (Bluetooth ELM327): planned as an optional module that only displays values the ECU actually returns.
- No vehicle data is fabricated.
