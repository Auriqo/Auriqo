# Auriqo v1.0.0

> Candidate release notes for the prepared, unpublished 1.0.0 source state. These notes are intentionally blocked from publication until every `PENDING:` gate below is resolved.

## Highlights

- Auriqo’s first prepared release candidate completes the application rebrand, visual refresh, settings cleanup, and removal of inherited legacy paths and obsolete canvas paths.
- Playback and library reliability work includes bounded lyrics caching/fallback, corrected download-state handling, clearer recognition outcomes, integration-boundary hardening, and widget policy coverage.
- Listen Together no longer has a source-default endpoint: authorized deployments may configure a `wss://` server and an `https://` invite base, while blank values keep it unavailable.

## Privacy, quality, and attribution

- GMS Firebase Analytics and Crashlytics collection defaults to disabled and is enabled only after an explicit recorded opt-in. FOSS does not expose Firebase telemetry code, prompt, or settings.
- The app includes an offline OSS attribution asset and screen, with validation for direct runtime dependency coverage.
- Focused unit tests cover privacy consent, OSS parsing, Listen Together configuration, lyrics, downloads, recognition, integrations, and widgets.
- Cold-start markers and bounded Coil memory/disk caches were added; image-loader initialization no longer blocks on a DataStore read, lyric-result caching is bounded, and widget artwork opts into disk caching.
- CI executes instrumented Compose smoke coverage for actual MainActivity home/search/settings destinations and the production player transport component through `:app:auriqoApi30UniversalFossDebugAndroidTest` on the Pixel 2/API 30/`aosp-atd` Gradle Managed Device. The job allows 30 minutes and always uploads instrumentation reports. A managed-device run recorded on 2026-08-05 passed all 4 smoke tests; it is not signed-release or universal-candidate evidence.
- The Auriqo Room `InternalDatabase` schema history is exported through version 44 (versions 1–44); this is static schema-history validation, not a migration-test result.
- Orphan capture material and the tracked dummy Flow Neuro API key were removed; optional configuration remains blank-safe.

## Distribution details

- Candidate release artifact: `universalFossRelease`
- Application ID: `com.auriqo.music`
- Version name/code: `1.0.0` / `527`
- Repository state: private; F-Droid and Play materials are prepared but disabled/unpublished.
- Artifact controls: CI stages exactly one universal APK, enforces a 200 MiB limit, and writes `SHA256SUMS`.

## Release gates

PENDING: Run and record Universal FOSS/GMS candidate validation in GitHub from the authorized candidate revision. Retain the recorded 4/4 managed-device smoke result as smoke-only evidence.
PENDING: Provide approved release signing material, build the FOSS universal APK, and record its signer certificate fingerprint.
PENDING: Record and verify the `SHA256SUMS` entries for the exact staged signed artifacts.
PENDING: Record the immutable source revision after explicit maintainer authorization to create `v1.0.0`.
PENDING: Obtain maintainer/legal approval for the final privacy notice, support contact, store declarations, and any external publication.

No tag, GitHub release, signed APK, F-Droid publication, or Play submission has been performed.
