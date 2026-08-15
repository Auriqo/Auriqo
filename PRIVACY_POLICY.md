# Auriqo privacy and data-flow notes

Last reviewed: 2026-08-14

This document describes what the current open-source Android code can send or store. It is technical documentation, not a claim that a third-party provider has a particular retention policy or that a legal regime applies. Review the terms and privacy policy of every provider you enable.

## Data stored on the device

Auriqo stores ordinary application state such as playback position, queues, library metadata, downloaded media references, appearance settings and provider preferences in app-private storage. The current settings DataStore also contains sensitive values when the user enables an integration, including some of the following:

- YouTube/InnerTube cookies and account metadata;
- Spotify access data;
- Discord access and refresh tokens;
- ListenBrainz and Last.fm session/token values;
- YouTube playlist-attribution access tokens;
- AI provider keys and custom endpoint settings;
- Listen Together session values and proxy credentials.

These values are not committed to the repository. The current implementation does not encrypt every DataStore/shared-preference value with Android Keystore. Android backup is enabled for parts of the app data and the settings file is not currently excluded, so a device backup or transfer may contain settings and credentials. Treat a backup as sensitive. This is an open hardening item documented in [SECURITY.md](SECURITY.md).

Uninstalling the app or clearing its app data is the practical way to remove app-private storage. Use each provider's logout/revocation controls as well; deleting local state does not revoke a token that a provider has already issued.

## Network data flows

Requests are feature-driven. Auriqo does not send every category below on every launch.

| Feature | Data that may leave the device | Destination in the current code |
| --- | --- | --- |
| Playback, search, home and account playlists | Search terms, media and playlist identifiers, playback requests, and user-provided YouTube cookies or OAuth data | YouTube/YouTube Music and Google endpoints through the InnerTube/Data API clients |
| Synchronized lyrics | Song title, artist, album, duration and, for some providers, a media/video identifier | BetterLyrics, LRCLIB, Paxsenix, KuGou, SimpMusic, YouLyPlus and Letras.com; see [docs/LYRICS_PROVIDERS.md](docs/LYRICS_PROVIDERS.md) |
| AI translation, generation or recommendations | User-entered API key in the request authorization, lyrics and song metadata, and the configured model/base URL | The AI provider selected in the app, with OpenRouter as the default endpoint in the current UI |
| Playlist attribution | Playlist ID and, when the user supplies it, an OAuth bearer token | The optional Auriqo Worker, which forwards the request to YouTube/Google; see [docs/WORKERS.md](docs/WORKERS.md) |
| Spotify import | OAuth authorization data and playlist identifiers/metadata | Spotify authorization and Web API endpoints |
| Discord Rich Presence | OAuth data and playback/activity metadata | Discord endpoints |
| Scrobbling | Track, artist and album metadata plus the relevant token/session | Last.fm and/or ListenBrainz when enabled |
| Music recognition | A generated audio signature and recognition request data | Shazam-compatible endpoints used by the recognition module |
| Listen Together | Room/session code, playback state and track metadata | The configured Listen Together server; the default code contains both remote and local-session compatibility paths |
| Artwork, canvas and provider status | Media identifiers and ordinary HTTP metadata | The selected canvas/artwork and provider endpoints |
| Optional Firebase integration | Data defined by the Firebase project configuration, if `app/google-services.json` is supplied in a GMS checkout | The maintainer's Firebase project; no Firebase configuration is stored in this repository |

Network operators and service providers can also receive ordinary connection data such as an IP address, timestamps, user agent and request metadata. Auriqo does not control their collection or retention.

## Android permissions and components

The manifest requests permissions for the features that are currently present:

- Internet and network state for playback, search, lyrics, account and optional integrations.
- Notifications, wake locks and foreground media/data-sync services for playback, downloads and visible task status.
- Microphone access only for music recognition, when the user starts that feature; the recognizer services are not exported.
- Audio-library access for local media, and Bluetooth access for connected audio devices.
- `WRITE_SETTINGS` for the user-invoked ringtone/system-audio workflow.
- `REQUEST_INSTALL_PACKAGES` for the updater in the GMS manifest path; the FOSS manifest removes this permission.
- Boot completion for playback/task restoration behavior.

The main activity, media session, media-button receiver, widgets, quick-settings tile and OAuth/deep-link callback are exported because Android or other apps need to invoke those contracts. The FileProvider and internal workers/services are not exported. Deep-link handling retains historical hosts and URI schemes for install compatibility; those identifiers are not a claim that the old web properties are maintained.

## Cookies, WebViews and tokens

The PoToken WebView loads local bundled JavaScript with a YouTube base URL and makes HTTPS requests to YouTube's Botguard endpoints. The current WebView does not need a user's YouTube cookie for that flow. The app's account flow can separately store a user-provided YouTube cookie for InnerTube requests.

The app currently uses debug logging in development builds. Sensitive values must not be logged; recent hardening removed cookie, token, PoToken, Botguard response and full provider-response logging from the audited paths. Users should still avoid sharing logs from authenticated devices.

## Listen Together and cleartext traffic

The Android network security configuration permits cleartext traffic to preserve compatibility with local Listen Together servers. This means an `http://` endpoint configured or reached by a feature is not protected by TLS. Use the production WSS endpoint or a trusted local network only. Narrowing the policy is a pending compatibility decision; it should not be interpreted as a security guarantee.

## Third-party retention and deletion

The repository's Worker implementation does not write playlist requests to a database. That does not eliminate transient platform, access, error or infrastructure logs. The retention behavior of YouTube/Google, lyrics providers, AI services, Firebase, Spotify, Discord, Last.fm, ListenBrainz, Shazam and hosting providers is governed by those services, not by this repository.

To reduce future data sharing:

1. Disable the relevant provider or integration in Auriqo.
2. Log out or remove the token in the relevant settings screen.
3. Revoke third-party OAuth grants at the provider.
4. Clear Auriqo app data or uninstall it when local removal is required.
5. Request deletion from the provider when its own policy provides that mechanism.

## Children and sensitive content

Auriqo is a general-purpose application. Do not use it to submit another person's credentials, private media metadata or sensitive personal information to a third-party service without authorization.

## Changes and contact

Changes to data flows should update this document and the provider/Worker documentation in the same pull request. For security issues, use [SECURITY.md](SECURITY.md); do not publish tokens or private logs in an issue.
