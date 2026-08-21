# Changelog

This file records user-visible and release-process changes. It does not replace GitHub release notes, and the existing `v1.0.2-alpha` release remains unchanged.

## [Unreleased]

### Build

- Debug builds now install as a separate app: package `com.auriqo.music.debug`, version suffix
  `-debug` and label "Auriqo Debug", so a debug APK can coexist with the production-signed release
  on the same device instead of replacing it.

## [1.0.5] - 2026-08-21

### Playback

- Moved YouTube cipher and n-transform execution to native Rhino over the complete bundled player,
  keeping WebView limited to PoToken acquisition.
- Updated NewPipe Extractor to `0.26.2` and retained serialized extractor/cache state across restarts.

### Interface

- Reworked view and like counts into compact, accessible metric rows in the player and media details.
- Surface view counts consistently in YouTube song lists and grids, including responses where YouTube
  moves the count to a different metadata column.
- About debug information now exposes the release identifier and complete source SHA.

## [1.0.4] - 2026-08-20

### Playback

- Restored runtime cipher-config sync: the player now reads the live player-hash table from the
  `ZemerTeam/zemer-cipher` upstream instead of the retired Echo Music repository, covering the
  current YouTube player (`3891b194`, STS 20681) so playback resumes on rotated player scripts.
- Show play counts (views) and like counts for songs in search, artists, albums and playlists,
  and display views/likes for the current track on the player screen.

### Brand and release preparation

- Changed the fixed Compose wordmark from `Auriqo` to `auriqo`, using the verified lowercase
  Cabinet Grotesk outline and metrics while keeping the accessible app name unchanged.
- Aligned phone and Wear metadata on the `1.0.4` stable release with `versionCode 3`.
- Required phone and Wear release builds to use the same protected production signing
  configuration; the public persistent debug key remains test-only.

### Wear OS

- Added a background Data Layer listener and a local Wear `MediaSession` proxy, so Auriqo's
  metadata and custom actions are available from the system media surface without opening the
  companion Activity.
- Added a Wear media notification/session activity, artwork loading for system controls and
  forwarding for seek, play/pause, previous/next, like, shuffle and repeat.
- Reworked the complete Wear companion surface toward the TIDAL reference: near-black Now Playing,
  left-aligned track typography, thin progress rail, plain transport glyphs and the Auriqo mark as
  the secondary-controls affordance.
- Wired the physical rotary bezel to media volume (including Cast output); progress seeking is now
  an explicit drag on the progress rail instead of a rotary action.
- Added Home, Tracks, Albums, Artists, Playlists and Queue screens backed by a bounded GMS Data
  Layer browse protocol; tapping a library item can start it on the phone.
- Added periodic GMS Data Layer heartbeats while playing and aligned the Tile palette with the
  Auriqo Wear surface.
- Aligned the Wear companion and Tile with Auriqo's Material You direction: Android 12+ uses the
  system dynamic Material 3 palette, with a branded fallback on older Wear OS versions while
  preserving the circular-display layout.
- Expanded the Wear companion Material You treatment with tonal containers, adaptive round/square
  screen metrics, dynamic progress thumb/track colors and responsive control sizes.
- Tightened Wear UI/UX: Now Playing has an explicit Library affordance, visible like/shuffle/repeat
  actions, icon-based navigation, progress semantics and contextual rotary behavior for scrolling
  Home/browse screens while keeping volume on Now Playing.

## [Unreleased]

### Brand and release preparation

- Changed the fixed Compose wordmark from `Auriqo` to `auriqo`, using the verified lowercase
  Cabinet Grotesk outline and metrics while keeping the accessible app name unchanged.
- Aligned phone and Wear metadata on the `1.0.3` stable candidate with `versionCode 2`.
- Required phone and Wear release builds to use the same protected production signing
  configuration; the public persistent debug key remains test-only.

### Wear OS

- Added a background Data Layer listener and a local Wear `MediaSession` proxy, so Auriqo's
  metadata and custom actions are available from the system media surface without opening the
  companion Activity.
- Added a Wear media notification/session activity, artwork loading for system controls and
  forwarding for seek, play/pause, previous/next, like, shuffle and repeat.
- Reworked the complete Wear companion surface toward the TIDAL reference: near-black Now Playing,
  left-aligned track typography, thin progress rail, plain transport glyphs and the Auriqo mark as
  the secondary-controls affordance.
- Wired the physical rotary bezel to media volume (including Cast output); progress seeking is now
  an explicit drag on the progress rail instead of a rotary action.
- Added Home, Tracks, Albums, Artists, Playlists and Queue screens backed by a bounded GMS Data
  Layer browse protocol; tapping a library item can start it on the phone.
- Added periodic GMS Data Layer heartbeats while playing and aligned the Tile palette with the
  Auriqo Wear surface.
- Aligned the Wear companion and Tile with Auriqo's Material You direction: Android 12+ uses the
  system dynamic Material 3 palette, with a branded fallback on older Wear OS versions while
  preserving the circular-display layout.
- Expanded the Wear companion Material You treatment with tonal containers, adaptive round/square
  screen metrics, dynamic progress thumb/track colors and responsive control sizes.
- Tightened Wear UI/UX: Now Playing has an explicit Library affordance, visible like/shuffle/repeat
  actions, icon-based navigation, progress semantics and contextual rotary behavior for scrolling
  Home/browse screens while keeping volume on Now Playing.

## [1.0.3-alpha.1] - 2026-08-15

### Wear OS

- Fixed the Wear `applicationId` to `com.auriqo.music`, matching the phone package and persistent
  debug signature required by Google Play Services Data Layer. The previous `v1.0.3-alpha` Wear
  APK used a different package and could not connect to the phone.

## [1.0.3-alpha] - 2026-08-15

### Lyrics

- Replaced the reconstructed lyrics animation with a faithful renderer port pinned to Better Lyrics `931f2582` and `@braccato/core` 1.1.0.
- Added stable multi-provider candidates, in-screen source/offset controls, custom lyrics fonts, translation/romanization and same-activity PiP.
- Added the verified Better Lyrics theme marketplace, bounded declarative shader runtime and reduced-motion behavior.
- Added Unison metadata plus signed nickname, vote, report and submission actions with an encrypted, exportable device identity.
- Hardened the local renderer bridge with origin-scoped messaging, monotonic playback snapshots, generation/sequence gates and crash fallback.

### Playback and Wear OS

- Updated Media3 to 1.10.1 and published repeat/shuffle as standard player commands while retaining like as an Auriqo custom command.
- Reworked the Wear companion and Tile with Auriqo artwork, progress and transport/like/shuffle/repeat controls.
- Added versioned GMS Data Layer state, startup snapshot loading, monotonic watch-side position and one-cycle compatibility for historical paths.

### Brand and distribution

- Replaced redistributable Cabinet Grotesk font binaries with fixed Cabinet-derived vector logo and wordmark outlines.
- Added renderer, marketplace, Unison, Wear, privacy, security and provenance documentation for the public source/release boundary.

### Maintenance

- Added a public FOSS CI path for compile, tests, lint, Worker typecheck and CodeQL.
- Reworked player-config synchronization to open a draft pull request for review instead of writing downloaded content directly to `main`.
- Removed a tracked machine-specific SDK path and a test API value from `gradle.properties`.
- Removed unused Google Sans Flex binaries whose redistributable provenance was not established.
- Added support, roadmap, documentation-index and release-review guidance.

### Security and reliability

- Redacted sensitive debug logging for cookies, account identifiers, Botguard responses, integrity/PoToken material and full Discord API error bodies.
- Excluded the settings DataStore from Android cloud backup and device transfer; the explicit in-app backup remains available.
- Restricted Listen Together cleartext WebSockets to local development hosts and made remote server URLs require WSS.
- Made the attribution Worker fail closed for browser origins and anonymous playlist requests by default.
- Removed public CI fallbacks that generated predictable signing material or passed provider secrets to a distributed build.

## [1.0.2-alpha]

The existing alpha tag includes the Auriqo branding work, YouTube Music home/playlist changes, video playback improvements, lyrics-provider work and related UI updates. See [RELEASE_INFO.md](RELEASE_INFO.md) and the immutable Git tag for the exact release contents.

The alpha is not replaced or rewritten by this changelog update.
