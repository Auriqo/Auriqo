# User guide

Auriqo is an Android music player with YouTube Music playback, local media, playlists,
synchronized lyrics and optional integrations. This guide describes the stable `v1.0.5`
release; provider behavior can change independently of the app.

## Install

Download the APK that matches the phone and service set you use from the
[latest release](https://github.com/Auriqo/Auriqo/releases/latest):

- `app-universal-foss-release.apk`: phone build without Google Play Services integrations.
- `app-universal-gms-release.apk`: phone build with Cast and GMS phone-to-Wear features.
- `wear-release.apk`: Wear OS companion; pair it with the GMS phone build for Auriqo sync.

Android may ask you to allow installation from the browser or file manager used to open the APK.
Only install release files from the official repository or builds you produced yourself.

## First launch

1. Open Auriqo and allow media notifications when Android asks.
2. Choose the phone variant that matches your device and optional integrations.
3. Search for a song, album, artist or playlist from the home screen.
4. Start playback and use the queue or mini-player to continue browsing.

Account-backed features are optional. The app can play public media without signing in, while
private playlists and some library actions require the relevant account flow.

## Playback and library

- Tap a result to play it immediately; use the item menu to add it to the queue or a playlist.
- The player exposes queue, repeat, shuffle, like and output controls where the provider supports them.
- Song lists and media details show view and like counts when YouTube supplies those values.
- Local files are available through the local-media/library surfaces supported by the device.
- External system controls use Android Media3. Cast and the custom Wear data channel require GMS.

## Lyrics

When lyrics are available, open the lyrics surface from the player. Depending on the provider,
you can choose a source, adjust the offset, switch translation or romanization, and select a
renderer theme. Missing or delayed lyrics usually indicate provider availability rather than a
playback failure. See [lyrics providers](LYRICS_PROVIDERS.md) for provider-specific behavior.

## Optional integrations

Optional features are configured from their respective settings areas:

- Spotify playlist import can create an Auriqo playlist from a Spotify playlist.
- Last.fm and ListenBrainz can scrobble playback when configured.
- Discord Rich Presence publishes the current activity when enabled.
- Listen Together connects to a compatible server; remote connections require WSS.
- AI translation uses an endpoint and key chosen by the user.

Each integration can fail independently. Disable the affected integration to confirm whether it
is the source of a problem before reporting a general playback issue.

## Wear OS

The Wear companion provides standard media controls in both variants. For library browsing,
artwork, synchronized progress and custom actions, install `wear-release.apk` and use the GMS
phone variant. Pairing, permissions and Android background limits can affect synchronization;
see [Wear OS](WEAR_OS.md) for the protocol and known boundaries.

## Updates and debug builds

Stable APKs are signed with the official production certificate and update normally over the same
package. Contributor debug builds use a separate debug package and signing identity; they are
intended for testing and may require uninstalling a debug build before installing a stable build.

Do not report a problem from a debug build without including its version, variant and commit SHA.
