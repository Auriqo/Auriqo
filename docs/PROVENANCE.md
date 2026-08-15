# Code and asset provenance

This document separates what can currently be attributed from what still needs maintainer evidence. It is deliberately conservative: a project name in a comment or commit message is not proof that a file was copied under a compatible license.

## Auriqo-owned work

The Auriqo-specific application package, branding, UI composition, build configuration, integration glue and documentation are maintained in this repository under the project license unless a file-level notice says otherwise. Contributors should add an SPDX/license header or an entry here when importing code or assets.

## Adapted or externally derived code requiring confirmation

### BetterLyrics

The `betterlyrics` module was integrated in commit `5721f005`. It contains a Kotlin client for `lyrics-api.boidu.dev` and a TTML parser. The current tree does not preserve a complete upstream file mapping or an author/license notice for that port. The upstream [Better Lyrics project](https://github.com/better-lyrics/better-lyrics) is GPL-3.0-licensed and requests attribution. Confirm the exact source, contributor permission and required notice before publishing an official artifact.

### KuGou lyrics client

`kugou/src/main/kotlin/com/music/kugou/KuGou.kt` contains a comment identifying the implementation as modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic). Preserve that attribution and verify the applicable license and modifications at file level.

### InnerTube/NewPipe-related code

The build comments identify a local NewPipe Extractor-related copy or adaptation in the InnerTube area. Map the relevant files to the exact upstream commit, retain the license/notice and verify that the resulting distribution obligations are compatible with GPLv3. Do not rely on the module name alone.

### Inspiration versus copied code

Project history names Metrolist, VIVI Music, SimpMusic, ArchiveTune, Music Recognizer and other projects as inspiration or sources of ideas. Unless a file has an explicit attribution or a documented mapping, this repository makes no claim that those projects' code is present. If code was copied or adapted, add the upstream commit, license, file paths, modifications and notice before merging.

## Fonts and binary assets

Font binaries and current repository SHA-256 values are listed in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). Cabinet Grotesk, BBH Bartle and Google Sans Flex still require different levels of upstream source/license verification. Do not infer a license from a font's appearance or a web specimen.

Unused historical or personal images are not acceptable repository assets. New images must have a documented source, license, author/attribution and a reason to ship in the APK.

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

The release is blocked until the open BetterLyrics, Google Sans Flex and complete dependency-inventory questions are resolved with verifiable evidence.
