# Changelog

This file records user-visible and release-process changes. It does not replace GitHub release notes, and the existing `v1.0.2-alpha` release remains unchanged.

## [Unreleased]

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
