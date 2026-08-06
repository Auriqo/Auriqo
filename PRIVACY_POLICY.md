# Privacy notes for Auriqo

Last reviewed: 2026-08-05

This document describes behavior evidenced by the tracked source and build configuration. It is not a legal compliance certification, does not create a company relationship, and must be reviewed by a maintainer before any public distribution or store submission.

## Local processing and storage

The app stores preferences, library/playlist data, playback and search history, recognition history, downloaded media, and caches on the device as features are used. Local storage supports playback, offline downloads, settings, and library functions. The app requests access to audio media for local-library use and microphone access for music recognition; microphone use requires the applicable Android permission and is feature-dependent.

The Android manifest currently enables backup. Its backup rules exclude ExoPlayer download/cache files and an internal ExoPlayer database, but other app data may be included in Android backup or device-transfer behavior depending on the device, OS, and user settings. This is not a claim that all local data is excluded from backups.

To remove local data, use the app's available clear/delete controls for histories, downloads, caches, playlists, or profiles where applicable, revoke permissions in Android settings, or clear the app's storage/uninstall it in Android settings. Clearing storage or uninstalling removes local app data from that device but does not control data already held by a third-party service or device backup.

## Network requests and optional services

Using streaming, search, artwork, lyrics, playlist import, music recognition, translation, or social integrations can cause the app to send requests to the relevant content or service provider. Depending on the feature, requests can include a search term, media identifier/metadata, playlist link, selected settings, an authentication exchange, or audio needed for recognition. Network providers can receive technical information normally associated with a request, such as IP address and user-agent/device characteristics.

Source modules include YouTube playback/metadata handling and lyric-provider integrations. Spotify import is optional and can involve Spotify authorization and playlist data. Optional Last.fm and Discord integrations can send listening or presence information to those services. AI lyric translation is user-configured: text submitted to an enabled provider and any API credential are handled according to that provider and the selected configuration. Do not enable an optional service unless you accept its own terms and privacy practices.

The GMS flavor includes Firebase Analytics and Crashlytics dependencies and only applies the associated Gradle plugins when `app/google-services.json` is present. Its manifest defaults Analytics and Crashlytics collection to disabled. When Firebase is configured, the app presents a telemetry choice, records an explicit accept or decline, and enables collection only after acceptance; the choice can later be changed in Privacy settings. The FOSS flavor does not include those flavor-specific dependencies and never exposes a telemetry prompt or control.

## Data control and deletion limits

Auriqo has no documented operator-operated account server in this repository. The app cannot delete data retained by YouTube, Spotify, an AI provider, Last.fm, Discord, a recognition service, an Android backup provider, or any other service selected by the user. Use the account, privacy, and deletion controls supplied by the relevant provider for those requests.

No email address, physical address, or public support endpoint is asserted here because none is established in this repository. Questions from authorized collaborators should use the repository's private maintainer channel. A public privacy contact and a release-specific, legally reviewed privacy notice are prerequisites for public distribution.

## Scope and required follow-up

This policy does not promise GDPR, CCPA, PIPEDA, or other legal compliance. Before release, maintainers/legal reviewers need to confirm the actual runtime destinations and data categories for each enabled feature, the microphone-recognition processing path, Firebase behavior in GMS builds, the implemented consent controls, child-directed use, retention, international transfers, and an actionable public contact method.
