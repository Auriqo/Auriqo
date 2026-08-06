# Development setup

This setup applies to an authorized checkout of the private Auriqo repository. It intentionally contains no personal paths, tokens, or release credentials.

## Requirements

- JDK 21 (the Android module compiles Java and Kotlin for JVM 21)
- Android SDK with API 36 platform installed
- Android NDK `27.0.12077973` when native dependencies require it
- Android Studio or the command-line SDK tools
- Git

The prepared release is version `1.0.0` (version code `527`), with `minSdk 26`, `targetSdk 36`, and namespace/application ID `com.auriqo.music`. See [BUILD.md](BUILD.md) for variants and exact tasks.

## Local SDK configuration

Copy the tracked template and replace only its placeholder:

```bash
cp local.properties.template local.properties
```

Set `sdk.dir` to the Android SDK directory for the current workstation. `local.properties` is ignored and must not be committed.

On Windows, run `gradlew.bat` (for example, `gradlew.bat :app:assembleUniversalFossDebug`) instead of `./gradlew`.

## Optional service configuration

The FOSS flavor neither needs nor uses `app/google-services.json`. The GMS flavor has Firebase Analytics and Crashlytics dependencies, and the Google Services/Crashlytics Gradle plugins are applied only when `app/google-services.json` exists. If a maintainer supplies that file for an authorized GMS build, keep it untracked; never copy it from another project or commit it.

Some optional features accept credentials at runtime or build time (for example, Last.fm and GitHub OAuth). Leave them unset for normal FOSS validation. Do not put credentials in source, screenshots, documentation, issues, or commits.

### Listen Together deployment values

Listen Together has no built-in server or share URL. `LISTEN_TOGETHER_SERVER_URL` and `LISTEN_TOGETHER_SHARE_BASE_URL` default to blank and may be supplied through `local.properties`, a Gradle `-P` property, or the build environment. The server value must be a valid `wss://` URL; the share value must be a valid `https://` URL. Leave both blank unless an authorized deployment supplies them—blank values leave the feature unavailable rather than connecting to a placeholder service.

### Telemetry variants

The FOSS source set has no Firebase telemetry SDK and exposes no telemetry prompt or control. In a GMS build with Firebase configuration, Analytics and Crashlytics collection default to disabled. The app records an explicit accept/decline choice before enabling collection, and the setting can later be changed in Privacy settings.

## Release signing

The release build configuration reads its keystore from `app/keystore/release.keystore` and its passwords/alias from the `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` environment variables. The keystore directory and common keystore extensions are ignored. Do not add signing values to `gradle.properties` or version control.

For CI, the separately configured `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` secrets are required before a release job can sign anything. Missing values deliberately fail the release job.

`1.0.0` remains an unpublished candidate. Do not create a tag, upload an artifact, or use a store console unless an authorized maintainer has completed the release gates in [BUILD.md](BUILD.md).

## Next steps

1. Run the FOSS debug validation in [BUILD.md](BUILD.md).
2. Consult [ARCHITECTURE.md](ARCHITECTURE.md) before changing modules or flavors.
3. Follow [CONTRIBUTING.md](CONTRIBUTING.md) for review and secret-handling expectations.
