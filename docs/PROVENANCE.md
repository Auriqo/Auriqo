# Code and asset provenance

This document separates Auriqo-owned work, adapted code and bundled assets. A project name in a comment or commit message is not by itself proof that a file was copied under a compatible license.

## Auriqo-owned work

The Auriqo-specific application package, branding, UI composition, build configuration, integration glue and documentation are maintained in this repository under the project license unless a file-level notice says otherwise. Contributors should add an SPDX/license header or an entry here when importing code or assets.

## Adapted or externally derived code

### BetterLyrics

- Introduced in commit `5721f005` as a Kotlin client and TTML parser.
- Current paths: `betterlyrics/src/main/kotlin/com/auriqa/music/betterlyrics/BetterLyrics.kt`, `TTMLParser.kt` and `models/Track.kt`.
- The upstream [Better Lyrics project](https://github.com/better-lyrics/better-lyrics) is GPLv3-licensed and requests attribution.
- The upstream project is a TypeScript browser extension; the current Auriqo files are Kotlin and the repository history has no exact file-level copy mapping. Keep the attribution and have the maintainer confirm the contributor's authorship record before a stable release.
- The faithful renderer port is pinned to upstream commit `931f25829f6cfd81d0042ca36b4308a0cd38d467`
  and `@braccato/core@1.1.0`. Its exact boundary, hashes and regeneration procedure are recorded in
  [BETTER_LYRICS_SNAPSHOT.md](BETTER_LYRICS_SNAPSHOT.md).
- Generated web assets are derived from the checked-in `betterlyrics/web` source and the exact npm
  lockfile. Browser background scripts, request sniffing and YouTube Music DOM adapters are not
  bundled.

### KuGou lyrics client

`kugou/src/main/kotlin/com/music/kugou/KuGou.kt` contains a comment identifying the implementation as modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic). Preserve that attribution and verify the applicable license and modifications at file level.

### InnerTube/NewPipe-related code

The build comments identify a local NewPipe Extractor-related copy or adaptation in the InnerTube area. Map relevant files to the exact upstream commit, retain the license/notice and verify that distribution obligations remain compatible with GPLv3. Do not rely on a module name alone.

### Inspiration versus copied code

Project history names Metrolist, VIVI Music, SimpMusic, ArchiveTune, Music Recognizer and other projects as inspiration or sources of ideas. Unless a file has an explicit attribution or documented mapping, this repository makes no claim that those projects' code is present. If code was copied or adapted, add the upstream commit, license, file paths, modifications and notice before merging.

## Fonts and binary assets

- BBH Bartle is documented as an OFL 1.1 asset with a local license copy and current blob hash.
- Cabinet Grotesk is governed by Fontshare's ITF Free Font License. Because that license restricts redistribution of Font Software, Auriqo removed the TTF files and retains only the fixed Q/lowercase-a mark and `Auriqo` wordmark outlines permitted as logo/vector artwork. The source Cabinet Bold blob hash and paths are recorded in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
- The unused Google Sans Flex binaries were removed from the current tree because their redistributable provenance was not established.
- Historical or personal images are not acceptable repository assets. New images need a documented source, license, author/attribution and a reason to ship in the APK.

## Dependency and service boundaries

Gradle/npm packages and remote APIs are not Auriqo-owned code. Their exact versions and licenses must be resolved from the lockfiles/build graph for each release. Remote services are documented for data-flow purposes in [PRIVACY_POLICY.md](../PRIVACY_POLICY.md) and [docs/LYRICS_PROVIDERS.md](LYRICS_PROVIDERS.md); service availability or terms are not granted by the GPL license of this repository.

## Required provenance record for new imports

For every imported or adapted file, record:

1. upstream project and canonical URL;
2. exact commit/tag or download version;
3. original file path and current Auriqo path;
4. license and required notice;
5. modifications made by Auriqo; and
6. whether the source is bundled, generated or fetched at runtime.

Before an official artifact, complete the dependency inventory. Keep provenance questions visible in pull requests instead of silently assuming a compatible license.
