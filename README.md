# Nijatva Appstore

A curated Android app store and ecosystem hub for Nijatva apps. Kotlin, Jetpack Compose, Material 3.
No accounts, no ads, no analytics, no Google Play Services.

- App name: **Nijatva Appstore**
- Package name: `com.nijatva.appstore`
- Version: 0.12.0 (versionCode 12)
- minSdk 26, targetSdk / compileSdk 35
- Language: Kotlin only
- UI: Jetpack Compose + Material 3

## What works in 0.1.0

| Area | State |
|---|---|
| Store tab: featured, Nijatva apps, community apps | Working: 10 Nijatva apps and 3 community apps |
| App details: description, changelog, permissions, screenshots | Working |
| Search: name / category / developer, recent searches | Working, history in DataStore |
| Updates tab: installed vs catalogue version, auto-update toggle | Working |
| Install pipeline: download, SHA-256 verify, PackageInstaller session | Active for Gallery, Browser, Camera, Dialer, Contacts, Messages and Music |
| Settings: theme, accent, notifications, Wi-Fi-only, about | Working |
| Appearance: System / Dark / Light / Black + Green / Saffron / Indigo | Working, applied instantly, restored before the first frame |
| Developer page | Working, reads `assets/developer.json` |

## Setup

1. Install Android Studio (Ladybug or newer) with JDK 17.
2. Open the project, let Gradle sync.
3. Build and install:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must point at your SDK (`sdk.dir=/path/to/Android/sdk`).

## Structure

```
app/src/main/java/com/nijatva/appstore/
├── core/theme/      design tokens, accent palettes, ColorScheme builder
├── data/            models, catalogue + settings repositories, StoreViewModel
├── install/         ApkDownloader, ApkInstaller (PackageInstaller), result receiver
├── navigation/      NavHost, routes, bottom navigation
├── ui/              shared components (badges, chips, cards, rows)
└── feature/         store, search, updates, settings, developer
```

## Design decisions

- **Accent colours carry their own label colour.** Green `#0B7A52` and Indigo `#1C1A7E` use white
  text (5.36:1 and 14.01:1). Saffron `#EE6E00` uses near-black text (6.17:1) because white on it is
  only 3.06:1, which fails WCAG AA. Filled chips therefore stay readable at every accent.
- **Theme is restored before the first frame.** The splash screen is held until the persisted
  `ThemeState` is read from DataStore, so there is no flash of the wrong theme.
- **Install pipeline verifies before it installs.** The APK is downloaded to cache, hashed with
  SHA-256 and compared with the catalogue value; a mismatch deletes the file. Only then is it
  streamed into a `PackageInstaller` session.
- **No image loader.** Every app icon ships inside the APK as a drawable: bitmaps pulled from the
  release APKs, vectors rebuilt from their resource tables, so there is no Coil/Glide dependency and
  no network image pipeline. Initials remain the fallback if a drawable ever fails to decode.
- **The catalogue is fetched, not baked.** `catalog/catalog.json` is the source of truth, hosted at
  `CatalogUrls.REMOTE`; `assets/sample_apps.json` is only the offline fallback. `tools/build_catalog.py`
  regenerates it from the GitHub releases.
- **The catalogue is real data.** `assets/sample_apps.json` lists nine Nijatva apps with the APK URL
  and SHA-256 of their latest GitHub release asset. Two entries (File, Editor) have no release yet,
  so their install button reports "Not published yet" instead of pretending to install something
  that does not exist.

## Third-party dependencies and why

| Dependency | Why |
|---|---|
| androidx.core:core-ktx | Kotlin extensions for platform APIs |
| androidx.activity:activity-compose | Compose entry point, edge-to-edge |
| androidx.lifecycle:lifecycle-runtime-compose / viewmodel-compose | `collectAsStateWithLifecycle`, `viewModel()` |
| androidx.compose:compose-bom + ui, material3, material-icons-core | The UI toolkit and design system |
| androidx.navigation:navigation-compose | Screen navigation |
| androidx.datastore:datastore-preferences | Persisting theme, toggles and search history |
| androidx.core:core-splashscreen | Holding the splash until the theme is read |
| androidx.browser:browser | Custom Tab for the GitHub profile link |
| libs/nijatva-signature-1.0.0.aar | The suite's Appearance and Developer screens, shared by every Nijatva app |

No Room, no Ktor, no Moshi, no Hilt, no Firebase. JSON is parsed with the platform `org.json` and
networking uses `HttpURLConnection`.

## Known gaps

- Catalogue refresh from `raw.githubusercontent.com` is not wired up yet; the app reads assets only.
- Install flow cannot be exercised until the first APK is published to a catalogue URL.
- No release signing config; debug builds only.
