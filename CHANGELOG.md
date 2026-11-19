# Changelog

## 0.12.0 — 19 November 2026

Moved to the `nijatva` organisation.

- `CatalogUrls.REMOTE` now points at
  `https://raw.githubusercontent.com/nijatva/nijatva-appstore-catalog/main/catalog.json`
- Repository and catalogue instructions updated for `nijatva/nijatva-appstore`

## 0.11.0 — 19 November 2026

The store now carries other people's apps too, with the Nijatva suite kept on top.

- Catalogue entries carry a `tier` ("official" or "community") and a `publisher`
- Store screen is sectioned: Featured and "Nijatva apps" first, then "Community apps" with a line
  saying the hash is checked but it is not a Nijatva build
- App details shows the same distinction: "Official Nijatva build" or "Community build by <publisher>"
- Search ranks official apps above community ones for the same query
- Three real community apps added, each with the SHA-256 of the file the store will download:
  NewPipe 0.29.3, F-Droid 1.18.0, KeePassDX 4.5.8 (libre)
- `tools/build_catalog.py` gained an `EXTERNAL` list, so third-party repos resolve the same way
  Nijatva repos do; `queries` covers the three new packages for installed detection

## 0.10.0 — 19 November 2026

Bug pass and a UI/UX pass.

Bugs
- The details screen read the catalogue once; it now follows the catalogue state, so a refresh
  while it is open lands instead of going stale
- Installed versions are read with `longVersionCode` on API 28+, so a high versionCode cannot wrap
- A failed background refresh is no longer swallowed: it reads "No network — showing the saved
  catalogue" or "Catalogue timed out"
- Installing with no connection now says so instead of surfacing a bare installer rejection
- The category filter resets itself when a refresh removes that category
- Resuming a pending install checks the cached file still exists
- "Not fetched yet" no longer sticks after the bundled catalogue is in use
- The Updates toggle claimed to auto-update; it now says what it does — check for updates on launch

UI/UX
- Skeleton rows while the catalogue loads, instead of an empty screen
- Every row carries an Install / Update / Installed / Soon chip with live percentage, so installing
  no longer needs a trip to the details screen
- Details: sticky bottom action bar, version/size/Android stat strip, the failure reason in an error
  container, "Installed 3 → 4 available", a Source on GitHub link and three screenshots
- Updates: summary header ("4 installed · 1 to update"), honest toggle text, explicit refresh
- Store header shows app and installed counts, section headers show counts, the category row hides
  itself when there is only one category
- Chips sit at a 48dp touch target

## 0.9.0 — 19 November 2026

The catalogue moved out of the APK.

- The store fetches `catalog.json` from a hosted URL, keeps the last good copy on disk and falls
  back to the bundled copy when there is no network. Adding an app or a release no longer needs a
  new store build
- Conditional GET with `If-None-Match`, so an unchanged catalogue costs one 304 and no bandwidth
- Store screen shows where the list came from (GitHub / saved on this device / bundled) with the
  fetch time, and a Refresh button
- The catalogue now carries the store's own row (`self: true`), so the Updates tab can offer a store
  update once one is published; the Store list hides it
- `tools/build_catalog.py` regenerates `catalog/catalog.json` from the GitHub releases: version,
  minSdk and SHA-256 are read from each APK, human text is preserved
- Catalogue refreshed from the repos: Dialer 1.6, Contacts 1.3, Messages 1.3, Editor 1.5.0 (its
  first published APK), Music rebuilt; eight of nine apps are now installable

## 0.8.0 — 17 November 2026

Catalogue refreshed to the newest camera release.

- Nijatva Camera now points at the v0.4.10 preview-match asset (11,139,527 bytes,
  SHA-256 `a6517ebd…`); the APK inside the tag still reports versionName 0.4.0 (versionCode 4)
- Everything else unchanged: nine apps, seven installable, three repositories still unreachable
  from this connection (photos, video-player, wtrlab, mini-x return 404)

## 0.7.0 — 17 November 2026

Install failures now say what actually went wrong.

- The installer callback carries `EXTRA_STATUS` and `EXTRA_STATUS_MESSAGE` through to the UI, so a
  rejection reads as "The APK is not a valid package", "This build conflicts with a copy already on
  the device", "Not enough storage for the install" and so on, with the system's own message appended
- Before a session starts, the store compares the signing certificate of the downloaded APK with the
  certificate of the installed copy. A key mismatch is reported as such instead of as a bare rejection
- `PackageInstaller` sessions now set the target package name
- The certificate reader falls back to `GET_SIGNATURES` below API 28, so the check is safe on Android 8

## 0.6.0 — 17 November 2026

The signature library replaces the store's own Appearance and Developer screens.

- `nijatva-signature-1.0.0.aar` added as a local dependency; the two activities come from the
  library's manifest, so no per-app manifest editing is needed
- Settings > "Theme and accent" opens `SignatureAppearanceActivity`; "About the developer" and the
  developer name on an app's page open `SignatureDeveloperActivity`
- Theme and accent now live in the library's `nijatva_signature` SharedPreferences file, the same
  store every other Nijatva app uses; MainActivity reads it synchronously and re-reads it on resume
- Accents switched to the library's three: Green `#1E7A46`, Saffron `#D98431`, Indigo `#3F45A8`,
  with the label colour chosen by the 0.179 relative-luminance cut-off
- The Compose `AppearanceScreen` and `DeveloperScreen` are deleted; the store no longer keeps a
  second theme engine
- Manifest gains the `developer.*` and `nijatva.*` meta-data stamps

## 0.5.0 — 17 November 2026

New launcher icon: the navy shopping-bag-with-arrow artwork.

- Adaptive icon: gradient background layer plus the white glyph as a separate foreground layer,
  so the launcher can mask it to a circle, squircle or rounded square
- Legacy 192px `ic_launcher` and `ic_launcher_round` PNGs generated from the same artwork
- The splash screen now shows the same glyph instead of the old white "N"

## 0.4.0 — 17 November 2026

Real launcher icons for every app in the catalogue.

- Six bitmap icons taken from the release APKs themselves (Gallery, Camera, Music, Browser) and from
  the repository source of the two apps without a release (File, Editor)
- Three vector icons (Contacts, Dialer, Messages) rebuilt from each APK's own resource table
- Initials badge stays as the fallback if an icon ever fails to load
- Developer page "Maintains" row now shows three real icons instead of initials

## 0.3.0 — 17 November 2026

Nine apps in the catalogue, seven of them installable.

- Added Dialer 1.3, Contacts 1.0 and Messages 1.0
- Camera moved to the v0.4.8 portrait-blur asset (APK still reports 0.4.0 / versionCode 4)
- Every installable entry carries the SHA-256 of the release asset it points at
- Permission lists on the details screen now match the permissions declared inside each APK
- `queries` covers all nine package names
- Three repositories linked by the owner return 404 and are not listed: nijatva-photos,
  nijatva-video-player, nijatva-notes

## 0.2.0 — 17 November 2026

Real catalogue instead of sample data.

- Six apps from the vasudev-work GitHub repositories: Gallery, Browser, Music, Camera, File, Editor
- Four of them install from their GitHub release assets, with the SHA-256 of each APK verified on device
- File and Editor are listed but marked not published, because those repositories have no release yet
- `queries` in the manifest now names the real packages, so installed-app and update detection works
- Developer stats refreshed: 33 public repositories, 6 apps with code
- App size shows an em dash instead of "0 B" when a build is not published yet

## 0.1.0 — 16 November 2026

First build.

- Store, Search, Updates and Settings tabs with bottom navigation
- App details screen with description, changelog, permissions and screenshot placeholders
- Developer page with avatar, handle, bio, stats, maintains row, location, website and email
- Appearance screen: System / Dark / Light / Black themes and Green / Saffron / Indigo accents,
  applied instantly and persisted in DataStore, restored before the first frame via the splash screen
- APK download with SHA-256 verification and PackageInstaller session install flow
- Bundled placeholder catalogue (`assets/sample_apps.json`) and developer profile (`assets/developer.json`)
