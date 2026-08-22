# Architecture

Auriqo is a multi-module Kotlin Android project. The Gradle wrapper and version catalog are the
source of truth for toolchain and dependency versions.

## Application layers

- `app/`: Android application, Compose UI, playback service, settings, library workflows and
  optional integrations.
- `innertube/`: YouTube and YouTube Music request models, parsers and page clients.
- `betterlyrics/`: Kotlin Better Lyrics client, models and TTML parser. The current checkout does
  not contain the historical browser-renderer source or generated web asset tree.
- `unison/`: lyrics identity and signed community actions.
- `lrclib/`, `paxsenixlyrics/`, `kugou/`, `simpmusic/`, `youlyplus/` and `letras/`: lyrics
  provider adapters.
- `canvas/`, `applecanvas/`, `artistvideo/` and related modules: artwork and media enrichment.
- `wear/`: Wear OS companion application and Tile.
- `workers/youtube-attribution/`: optional attribution Worker; it is not required by the FOSS
  playback build.

## Playback flow

1. A page client in `innertube` resolves media metadata and available formats.
2. The app selects a playable stream and creates a Media3 player item.
3. YouTube player JavaScript is fetched and evaluated by the native cipher runtime when signature
   or `n` transformations are needed.
4. A small WebView bridge is retained for PoToken acquisition only.
5. The playback service publishes state to Android system controls and, in the GMS variant, to the
   Wear data channel and Cast integration.

Provider changes should stay isolated in the relevant client, parser or runtime boundary. UI code
should consume stable models rather than parse provider response text directly.

## Variants

The `variant` dimension provides `foss` and `gms`; the `abi` dimension provides universal and
architecture-specific outputs. FOSS is the reference contributor build and must not require
Firebase files or private credentials. GMS-only code belongs behind the GMS source set or a clear
feature boundary.

## Data boundaries

- Account credentials and user-provided integration keys are configured at runtime, not committed.
- Provider requests and parsers live outside the UI layer.
- The current Better Lyrics boundary is Kotlin source under `betterlyrics/src/main/kotlin/`; there
  is no tracked web source, npm lockfile or generated renderer tree in this checkout.
- Official release signing is external to public CI and uses the maintainer-only keystore.

## Change guide

- Playback or provider behavior: start with `innertube/` and the relevant `app` runtime boundary.
- Lyrics source or rendering: inspect the provider module and `betterlyrics/` together.
- Wear behavior: change the phone publisher and `wear/` consumer as a protocol pair, then update
  [WEAR_OS.md](WEAR_OS.md).
- A new external service: document endpoint, authentication, payload, failure behavior and
  provenance before merging.
- UI-only changes: preserve the application ID, preferences, deep links and variant behavior.
