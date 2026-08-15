# Development setup

This document describes a reproducible local setup for Auriqo. It does not grant access to maintainer-only services, signing keys or private provider accounts.

## Toolchain

Install the following before opening the project:

- JDK 21 (the Android and Kotlin toolchains are configured for Java 21).
- Android SDK Platform 36 and matching Build-Tools.
- Android NDK `27.0.12077973` for the native modules.
- Git and a network connection for Gradle dependency resolution.
- Node.js/npm only when working on `workers/youtube-attribution`.

The wrapper supplies Gradle 9.3.1. The checked-in version catalog uses Android Gradle Plugin 9.0.0 and Kotlin 2.3.10. Android Studio may be used as an editor, but the wrapper commands below are the source of truth.

## Clone and configure the SDK

```bash
git clone https://github.com/Auriqo/Auriqo.git
cd Auriqo
cp local.properties.template local.properties
```

Edit `local.properties` and set an absolute `sdk.dir` path. Examples:

```properties
# Linux
sdk.dir=/home/example/Android/Sdk

# macOS
# sdk.dir=/Users/example/Library/Android/sdk

# Windows (use forward slashes or escaped backslashes)
# sdk.dir=C:/Users/example/AppData/Local/Android/Sdk
```

`local.properties` is ignored. Do not put API keys, tokens or signing passwords in it.

## FOSS build

The FOSS build is the recommended first build and must not need private credentials:

```bash
./gradlew :app:compileUniversalFossDebugKotlin --no-daemon
./gradlew :app:assembleUniversalFossDebug --no-daemon
```

The output is `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`. Install it only on a device or emulator you control:

```bash
adb install -r app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk
```

The debug build uses the tracked `app/persistent-debug.keystore` to keep local debug upgrades installable. It is not a release key and must not be reused for publication.

## GMS build

The GMS flavor enables Google Play Services integrations, including Cast:

```bash
./gradlew :app:assembleUniversalGmsDebug --no-daemon
```

`app/google-services.json` is optional in the current Gradle configuration. If a maintainer-provided file is present, the Google Services and Firebase plugins are enabled for that checkout. The file is ignored and must never be committed. A contributor does not need Firebase credentials to work on the FOSS variant.

## Tests and lint

Run the smallest relevant set while iterating, then the broader checks before requesting review:

```bash
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
./gradlew :innertube:testDebugUnitTest --no-daemon
./gradlew :letras:test --no-daemon
./gradlew :app:lintUniversalFossDebug --no-daemon
```

The repository also contains tests in `canvas` and `app`; Gradle task names can be inspected with `./gradlew tasks --all`. Do not hide a failing test by deleting it or changing global Gradle settings.

For the Worker:

```bash
cd workers/youtube-attribution
npm ci
npm run typecheck
```

Worker deployment requires a Cloudflare account and is maintainer-only. See [docs/WORKERS.md](docs/WORKERS.md).

## Optional app configuration

Optional providers are configured in the app, not by committing credentials:

- AI translation: enter an API key and, when needed, a base URL under the app's AI settings. The key is user-provided and should be treated as a secret.
- Spotify, Discord, YouTube and ListenBrainz: complete the relevant account flow or enter a token in the app. Do not paste tokens into source or issues.
- Listen Together: use the configured WSS service, or a `ws://` server only on localhost or the common Android emulator loopback addresses. See the session notes in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Release-only configuration

Release signing is not a contributor prerequisite. The current Android build expects a maintainer-provided `keystore/release.keystore` and the environment variables `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. The repository does not contain those files or values. Do not create a release key in CI as a fallback and do not add credentials to `gradle.properties`.

Before any official release, the CI/release workflow must be reviewed as described in [docs/CI_RELEASE_REVIEW.md](docs/CI_RELEASE_REVIEW.md).

## Troubleshooting

### SDK location not found

Verify that `local.properties` exists in the repository root and that `sdk.dir` points to a real SDK containing Platform 36.

### NDK or native build failure

Install NDK `27.0.12077973` through the SDK manager. Keep the local Gradle and Android caches; deleting them is not a supported fix.

### Firebase or GMS configuration failure

Try the FOSS command first. If GMS is required, check that `app/google-services.json` belongs to the intended package and is not an obsolete or private file copied from another project.

### Provider or lyrics failure

Check the provider-specific documentation, network connectivity and the redacted error. Upstream services can change independently of Auriqo; do not work around an outage by committing credentials or disabling certificate validation.

## Related documents

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [RELEASE_INFO.md](RELEASE_INFO.md)
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
- [docs/LYRICS_PROVIDERS.md](docs/LYRICS_PROVIDERS.md)
