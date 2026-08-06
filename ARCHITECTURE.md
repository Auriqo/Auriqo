# Auriqo architecture

## Project layout

Auriqo is a Gradle multi-module Android project. The application module is `:app`; primary application code is under `app/src/main/kotlin/com/auriqo/music/`, and its manifest declares `com.auriqo.music` as the namespace/application ID.

`settings.gradle.kts` includes these supporting modules:

| Area | Modules |
| --- | --- |
| Playback and service access | `:innertube`, `:simpmusic`, `:youlyplus`, `:unison` |
| Lyrics and music metadata | `:paxsenixlyrics`, `:betterlyrics`, `:lrclib`, `:kugou` |
| Recognition and artist media | `:shazamkit`, `:artistvideo` |
| Visual/canvas features | `:canvas`, `:applecanvas` |

The app uses Jetpack Compose, Hilt, Room, DataStore, Media3, Retrofit/Ktor, and Protobuf according to the tracked Gradle dependencies. This is a source-layout description, not a claim that every dependency is active in every build.

## Variants and distribution boundary

The `abi` dimension selects `universal`, `arm64`, `armeabi`, `x86`, or `x86_64`; the `variant` dimension selects `foss` or `gms`. FOSS disables Cast and excludes GMS-only Firebase, Google Drive, and Cast dependencies. GMS enables Cast and carries those flavor-specific dependencies. Both have debug and release build types.

The release application ID is `com.auriqo.music`; debug adds `.debug`. The prepared source version is `1.0.0`/`527`. Store metadata and any release verification must use the release ID, not an inherited package name.

Room exports `InternalDatabase` schemas at version 44. The current Auriqo namespace preserves the complete `com.auriqo.music.db.InternalDatabase` schema history from versions 1 through 44; legacy copies remain for compatibility verification. This is schema-history validation, not a claim that a database migration test was executed.

## Data and network boundaries

The app persists library/playback-related information and feature caches locally. The manifest permits network access, audio-media access, optional microphone use, Bluetooth connectivity, foreground playback/data-sync services, notifications, boot reception, settings writes, and package-install requests. Permissions describe capabilities requested by the package, not proof that every capability runs in every session.

Cipher player configuration has a bundled offline default at `app/src/main/assets/player_configs.json`. Playback code loads that asset and may overlay a validated remote configuration cache. `player_dates.json` is a separately fetched informational mapping rather than a bundled playback input. The scheduled workflow validates the upstream player-configuration JSON before proposing changes to the consumed configuration asset; it does not write directly to `main`.

Listen Together is deployment-configured rather than tied to a source default: `LISTEN_TOGETHER_SERVER_URL` and `LISTEN_TOGETHER_SHARE_BASE_URL` are blank-safe BuildConfig fields. A valid `wss://` server is required before the feature is available; a valid `https://` share base is required to create invite links.

## Privacy, attribution, and performance boundaries

The GMS source set bridges Firebase Analytics and Crashlytics; its manifest defaults both collection flags to false. The common consent store records an accept/decline choice and enables collection only for an accepted choice. The FOSS source set implements that bridge as unavailable, so it neither packages Firebase telemetry code nor exposes a telemetry prompt/control.

Open-source notices are bundled as the version-controlled `app/src/main/assets/oss-licenses.tsv` asset and displayed from local data; the attribution screen does not fetch them at runtime. Cold-start work adds Perfetto/Logcat startup markers, removes a blocking DataStore read from image-loader creation, uses bounded Coil memory/disk caches, and keeps lyric-result caching bounded and access-ordered. Widget artwork requests reuse the application image loader with disk caching.

The tracked test sources include focused unit coverage for privacy, OSS parsing, Listen Together configuration, lyrics fallback/cache, download state, recognition status, integration boundaries, and widget policies. `FeatureSurfaceSmokeTest` is instrumented Compose smoke coverage against actual `MainActivity` home/search/settings destinations plus the production player transport component. CI executes it through `:app:auriqoApi30UniversalFossDebugAndroidTest` on the `auriqoApi30` Gradle Managed Device (Pixel 2, API 30, `aosp-atd`), with a 30-minute timeout and always-uploaded instrumentation reports. A managed-device run recorded on 2026-08-05 passed its four smoke tests. The universal release candidate remains a GitHub validation gate; this smoke result is not signed-release evidence.

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for user-facing handling and control information, and [BUILD.md](BUILD.md) for build/release boundaries.
