# Auriqo

Auriqo is an Android music application with streaming playback, local downloads, lyrics, music recognition, and optional integrations. The tracked release candidate is version `1.0.0` (version code `527`). The repository is currently private; this document does not represent an APK download page or a public source distribution.

## What is in this repository

- Kotlin/Jetpack Compose Android application in the `app` module
- FOSS and GMS product flavors, with universal and per-ABI variants
- Supporting playback, lyrics, recognition, canvas, privacy, and offline OSS-attribution behavior
- F-Droid and Play listing preparation under `distribution/` and `fastlane/`; they are metadata only and do not indicate publication or submission

See [BUILD.md](BUILD.md) for reproducible local and CI commands, [ARCHITECTURE.md](ARCHITECTURE.md) for the module and variant layout, and [SETUP.md](SETUP.md) for workstation setup.

## Build from an authorized checkout

```bash
cp local.properties.template local.properties
# Set sdk.dir in local.properties for this workstation.
./gradlew :app:assembleUniversalFossDebug
```

The FOSS debug variant is the no-signing, no-`google-services.json` validation path. It contains no Firebase telemetry SDK or telemetry prompt/control. CI validates both Universal FOSS and Universal GMS debug build/test/lint paths, and runs the production-surface Compose smoke suite through the `auriqoApi30UniversalFossDebugAndroidTest` Gradle Managed Device task (Pixel 2, API 30, `aosp-atd`); the signed release output is FOSS universal only. See [BUILD.md](BUILD.md) for the exact commands and requirements.

## Release state

Version `1.0.0` is prepared in source and metadata, but no signed APK, tag, GitHub release, F-Droid publication, or Play submission has been created. The managed-device production smoke suite has recorded a 4/4 pass; the remaining universal candidate validation is moving to GitHub. Signing material, finalized candidate evidence, and explicit maintainer release authority remain required.

## Contributing and security

Use the repository's issue and pull-request facilities if you have access. Read [CONTRIBUTING.md](CONTRIBUTING.md) before proposing a change. Report vulnerabilities using the private reporting route described in [SECURITY.md](SECURITY.md), not a public issue.

## Privacy and licensing

[PRIVACY_POLICY.md](PRIVACY_POLICY.md) describes source-evidenced local processing and optional third-party requests, along with controls available on the device. Auriqo is licensed under [GPL-3.0](LICENSE).
