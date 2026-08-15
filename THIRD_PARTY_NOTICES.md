# Third-party notices

This file records third-party assets and source integrations that have been identified in the repository. It is not yet a machine-generated exhaustive license report for every Gradle dependency. Before an official release, generate and review a dependency license inventory for every application and Worker artifact.

## Fonts

The following font binaries are tracked under `app/src/main/res/font/`. The SHA-256 values below identify the current repository blobs; they are not a substitute for verifying the upstream download and license.

### BBH Bartle

- Files: `bbh_bartle_regular.ttf` and `bbh_bartle.xml`.
- Upstream project: [Studio-DRAMA/BBH](https://github.com/Studio-DRAMA/BBH).
- Declared license: SIL Open Font License 1.1, with a copy at [third_party/fonts/OFL-1.1.txt](third_party/fonts/OFL-1.1.txt).
- Current TTF SHA-256: `bbh_bartle_regular.ttf` — `6240252862fa9dadea44af7e9eb119320c89c667c9ef176bbff1d77241965f3a`.
- Verify the copyright notice and the exact binary source against the upstream repository before the next release.

### Cabinet Grotesk

- Files: `cabinet_grotesk_regular.ttf`, `cabinet_grotesk_bold.ttf` and `cabinet_grotesk.xml`.
- Source/licensing reference: [Cabinet Grotesk on Fontshare](https://www.fontshare.com/fonts/cabinet-grotesk) and [Fontshare's license information](https://fontshare.com/licenses/itf-ffl).
- Current TTF SHA-256: regular — `982a97b68034bf65b53518aba720823c7cc501660c8c8085cfc66dfb5d168a13`; bold — `f2e2f7b99f1c17715567a84046e6ae2c13bbb24bb76847644df903f4b361f38d`.
- The project currently treats these files as distributed under the Fontshare free-font terms. Preserve the source/receipt or an upstream checksum in maintainer records; the repository does not currently contain that provenance record.

### Google Sans Flex and Sans Flex

- Files: `google_sans_flex.ttf` and `sans_flex.ttf`.
- Catalog reference: [Google Fonts](https://fonts.google.com/).
- Current TTF SHA-256: `google_sans_flex.ttf` — `9db5a1555133dad228ed52dc619ab62018e85d2aaf39f8acae11b2ed388d34cf`; `sans_flex.ttf` — `2510a8b7a24beb1fe8163e9a49813ccfe96b5453444b9443d42665ca4fa320c9`.
- License/provenance status: pending exact verification for the bundled binaries. Google Fonts metadata and a public discussion identify an OFL label for Google Sans Flex, but the source and binary history are not complete in this repository. Do not add a Google trademark or designer attribution as a substitute for a license record.

## Source integrations

### BetterLyrics

The `betterlyrics` module contains the Kotlin client and TTML parser used by Auriqo. Project history records its integration in commit `5721f005` ("Better Lyrics integrated"). The exact file-level origin and author permission for the current Kotlin code are not fully recorded in the repository, so maintainer confirmation is required before an official publication.
- Current paths: `betterlyrics/src/main/kotlin/iad1tya/echo/music/betterlyrics/BetterLyrics.kt`, `TTMLParser.kt` and `models/Track.kt`.

The upstream [Better Lyrics repository](https://github.com/better-lyrics/better-lyrics) is GPL-3.0-licensed and requests attribution. The upstream browser extension and this Android module are not assumed to be byte-for-byte identical. Keep the upstream license and a clear adaptation notice with the module once provenance is confirmed.

### Other adapted or referenced code

The repository contains comments and history referring to projects such as ViMusic, Metrolist, VIVI Music, SimpMusic and NewPipe Extractor. These references may represent inspiration, adapted code or a local copy depending on the file. The file-level attribution and license matrix is maintained separately in [docs/PROVENANCE.md](docs/PROVENANCE.md) and remains a release gate where the source is not explicit.

## Gradle and npm dependencies

The Android build uses dependencies from Google Maven, Maven Central, JitPack and an additional mirror configured in Gradle. The Worker uses npm packages recorded in `workers/youtube-attribution/package-lock.json`. A complete release audit must:

1. resolve the exact dependency graph for each published variant;
2. collect each component's license and required notices from authoritative metadata;
3. check transitive native binaries and generated resources;
4. verify that packaging exclusions in the Android build do not remove a required notice; and
5. attach the resulting report to the release review.

This repository does not claim that a dependency is permissively licensed merely because its package is available from a public registry.

## Services are not bundled dependencies

YouTube/Google, Spotify, Discord, Last.fm, ListenBrainz, Shazam-compatible endpoints, lyrics providers, AI providers, Cloudflare and Firebase are remote services, not licenses granted by this repository. Their terms, trademarks, availability and retention policies remain the responsibility of the user and the service operator. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and [docs/LYRICS_PROVIDERS.md](docs/LYRICS_PROVIDERS.md).

## Maintainer release gate

Do not publish an official artifact while the BetterLyrics file-level attribution, Google Sans Flex binary provenance or complete dependency license inventory is still marked pending. Update this file and [docs/PROVENANCE.md](docs/PROVENANCE.md) with verifiable evidence, not assumptions.
