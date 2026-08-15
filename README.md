# Auriqo

Auriqo is an open-source Android music player built around YouTube Music/YouTube playback, local media, playlists, synchronized lyrics and optional integrations. It is a community project released under the GNU General Public License v3.0.

Auriqo is an independent project. It is not affiliated with, endorsed by or operated by YouTube, Google, Spotify, Discord, Last.fm, ListenBrainz, Shazam, or any lyrics provider.

## Project status

Auriqo is in active development and is still an alpha release. Core playback, local media, playlists and lyrics are available today, while integrations continue to evolve with the services they use. Provider APIs and media availability can change, so occasional breakage is expected while the project grows.

The repository contains an existing `v1.0.2-alpha` tag. The Android module currently declares `versionCode 1` and `versionName 1.0.0`; this metadata must be reconciled before an official stable release. The existing alpha tag and release must not be replaced in place.

## Current functionality

The current codebase includes these user-facing areas, subject to provider availability and build variant:

- YouTube Music/YouTube playback through the local InnerTube client, plus local media playback.
- Queues, library and playlist workflows, including optional account-backed playlist access.
- Synchronized lyrics from multiple providers, including BetterLyrics, LRCLIB, Paxsenix, KuGou, SimpMusic, YouLyPlus and Letras.com.
- Optional lyrics translation through a user-selected AI endpoint.
- Artwork and canvas/video-related playback surfaces when a provider supplies the required data.
- Optional Spotify playlist import, Last.fm and ListenBrainz scrobbling, Discord Rich Presence, music recognition and Listen Together sessions.
- An optional Wear OS module and Google Cast support in the GMS variant.

The list above describes code present in this repository; it is not a guarantee that every remote service is available in every country or at every point in time.

## Screenshots

There is not a current screenshot gallery yet. Earlier images were removed because they no longer represented the app. Current screenshots and short demos are welcome when they match a released build.

## Requirements

- JDK 21.
- Android SDK Platform 36 and Build-Tools provided by the Android SDK installation.
- Android NDK `27.0.12077973` for native components.
- Git. Android Studio is optional; the Gradle wrapper is the canonical build entry point.
- Linux, macOS or Windows with a working Android SDK path. Windows users should use `gradlew.bat`.

The repository pins Gradle 9.3.1, Android Gradle Plugin 9.0.0 and Kotlin 2.3.10 in the checked-in build configuration. Do not commit `local.properties`, Firebase configuration, API keys or signing material.

## Build

Clone the repository and configure the SDK path locally:

```bash
git clone https://github.com/Auriqo/Auriqo.git
cd Auriqo

# Linux/macOS
cp local.properties.template local.properties
# Edit local.properties and set sdk.dir to your Android SDK directory.
```

On Windows PowerShell, use `Copy-Item local.properties.template local.properties` instead.

The FOSS debug variant does not require private credentials or a Firebase file:

```bash
./gradlew :app:assembleUniversalFossDebug --no-daemon
```

The APK is written to:

```text
app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk
```

Install a locally built debug APK on an authorized device or emulator with:

```bash
adb install -r app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk
```

The GMS debug variant enables Google Play Services integrations such as Cast:

```bash
./gradlew :app:assembleUniversalGmsDebug --no-daemon
```

`app/google-services.json` is optional and ignored by Git. When present, the current Gradle configuration also enables the Firebase plugins for that local configuration. It is only needed for the maintainer's Firebase setup; the FOSS build does not need it.

Release builds require maintainer-controlled signing material and are not part of the contributor setup. See [SETUP.md](SETUP.md) and [RELEASE_INFO.md](RELEASE_INFO.md).

## Tests and checks

Useful local checks include:

```bash
./gradlew :app:compileUniversalFossDebugKotlin --no-daemon
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
./gradlew :innertube:testDebugUnitTest --no-daemon
./gradlew :letras:test --no-daemon
./gradlew :app:lintUniversalFossDebug --no-daemon
```

Run the worker type check separately when changing `workers/youtube-attribution`:

```bash
cd workers/youtube-attribution
npm ci
npm run typecheck
```

## Variants

The `variant` dimension provides `foss` and `gms` builds. The `abi` dimension provides `universal`, `arm64`, `armeabi`, `x86` and `x86_64` builds. The `UniversalFossDebug` build is the least dependent on external credentials and is the reference build for pull requests.

The application identifier remains `com.auriqa.music` for compatibility with existing installs, preferences and deep links. Some URI hosts and package names inherited from earlier development also remain in technical code; do not rename them as a cosmetic cleanup.

## Optional integrations

Auriqo can connect to external services for lyrics, playlist access, scrobbling, recognition, Discord Rich Presence and Listen Together. These integrations are optional and are used when you choose the corresponding feature. The current data flows are summarized in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

- Lyrics providers and BetterLyrics: [docs/LYRICS_PROVIDERS.md](docs/LYRICS_PROVIDERS.md).
- The playlist-attribution Worker: [docs/WORKERS.md](docs/WORKERS.md).
- Provenance and open license questions: [docs/PROVENANCE.md](docs/PROVENANCE.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Known limitations

- YouTube/YouTube Music and lyrics providers can change protocols, rate limits, authentication requirements or content availability.
- Some account features require a sign-in flow, cookie or OAuth token. These settings are kept in the app and should only be configured on a device you trust.
- Listen Together uses WSS for remote servers; `ws://` is limited to localhost and common Android emulator loopback addresses.
- Official release signing is maintainer-only; contributors can build and install the FOSS debug APK.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md), follow the [Code of Conduct](CODE_OF_CONDUCT.md), and open a focused pull request. Keep local configuration, credentials and generated build output out of commits. Security reports must follow [SECURITY.md](SECURITY.md), not a public issue.

## License

Auriqo is licensed under the [GNU General Public License v3.0](LICENSE). Third-party code, fonts and services have additional notices and open provenance questions documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [docs/PROVENANCE.md](docs/PROVENANCE.md).
