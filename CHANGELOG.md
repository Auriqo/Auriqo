# Changelog

This file records user-visible and release-process changes. It does not replace GitHub release notes, and the existing `v1.0.2-alpha` release remains unchanged.

## [Unreleased]

### Security

- Removed debug logging of YouTube cookies, account identifiers, Botguard responses, integrity/PoToken material and full Discord asset API error bodies.
- Documented the remaining Android backup, credential-storage, cleartext-network and Worker authentication decisions.

### Maintenance

- Added public setup, contribution, security, privacy, provenance, lyrics-provider, Worker and CI/release documentation.
- Added repository line-ending/editor conventions and contributor issue/PR guidance.
- Removed unused legacy-brand and personal image assets that had no code references.

### Release engineering

- Added a manual release checklist and artifact checksum policy.
- Marked the current CI/release workflow as requiring maintainer remediation before it can publish official artifacts.

## [1.0.2-alpha]

The existing alpha tag includes the Auriqo branding work, YouTube Music home/playlist changes, video playback improvements, lyrics-provider work and related UI updates. See [RELEASE_INFO.md](RELEASE_INFO.md) and the immutable Git tag for the exact release contents.

The alpha is not replaced or rewritten by this changelog update.
