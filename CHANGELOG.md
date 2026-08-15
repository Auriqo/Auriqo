# Changelog

This file records user-visible and release-process changes. It does not replace GitHub release notes, and the existing `v1.0.2-alpha` release remains unchanged.

## [Unreleased]

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
