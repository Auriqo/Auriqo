# Changelog

This changelog records the prepared `1.0.0` candidate. It does not indicate that an APK, tag, GitHub release, F-Droid publication, or Play submission exists.

## 1.0.0 — 2026-08-05 (prepared, unpublished)

### Product and maintenance

- Completed the Auriqo rebrand across application identity, launcher/notification/splash resources, themes, screens, strings, and distribution artwork.
- Removed inherited legacy paths and obsolete canvas paths; cleaned the related settings/navigation surface.
- Hardened lyrics fallback and bounded lyric-result caching, download-state updates, music-recognition status mapping, Spotify/Discord/Last.fm integration boundaries, and widget layout behavior.
- Made Listen Together deployment-configured: source defaults are blank, a valid `wss://` endpoint is required to enable it, and a valid `https://` base is required for share links.

### Privacy, quality, and performance

- Added GMS-only Firebase Analytics/Crashlytics consent: collection defaults disabled and turns on only after an explicit recorded opt-in. FOSS ships without Firebase telemetry UI or SDK references.
- Added an in-app offline OSS attribution screen backed by the version-controlled `oss-licenses.tsv` asset.
- Added focused unit tests for lyrics, downloads, recognition, Listen Together configuration, privacy consent, OSS parsing, integrations, and widget policy, plus CI execution of instrumented Compose smoke coverage for actual MainActivity home/search/settings surfaces and the production player transport component via the Pixel 2/API 30/`aosp-atd` Gradle Managed Device task. The job has a 30-minute timeout and always uploads instrumentation reports; a run recorded on 2026-08-05 passed all 4 smoke tests, which is smoke-only evidence and not signed-release evidence.
- Added Room schema-history validation for the Auriqo `InternalDatabase` namespace (versions 1–44); this is static schema validation, not a recorded migration-test run.
- Added cold-start Perfetto/Logcat markers, removed a blocking DataStore read from image-loader creation, and retained bounded Coil/lyric/widget cache behavior.

### Build, automation, and release preparation

- Set `versionName` to `1.0.0` and `versionCode` to `527`; added a release-signing verification task and blank-safe BuildConfig handling for optional configuration.
- Removed orphan capture material and the tracked dummy Flow Neuro API key; optional configuration remains blank-safe rather than embedding a placeholder credential.
- Updated CI to validate Universal FOSS and Universal GMS debug build/test/lint without release secrets, constrain CodeQL to relevant languages and supported code-scanning repositories, and require a guarded tag/manual publication path with signing checks.
- Prepared one unsigned FOSS universal candidate APK with a 200 MiB size budget and `SHA256SUMS` manifest. GitHub candidate validation remains pending. A separately signed universal publication requires approved signing material and the remaining external release gates.
- Changed player-configuration sync to validate upstream data and open a review pull request instead of pushing directly to `main`.
- Added private-repository-aware build, architecture, contribution, security, privacy, F-Droid, and Play preparation documentation.

### Not released

No signed artifact, immutable release tag, GitHub release, F-Droid publication, Play submission, or public release/support channel was created as part of this preparation.
