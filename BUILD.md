# Building and releasing Auriqo

## Build facts

The root Gradle project is `auriqo`. The Android application module is `:app`, with namespace and release application ID `com.auriqo.music`. The prepared candidate is version `1.0.0` (version code `527`). It uses compile/target SDK 36, min SDK 26, JDK 21, and NDK `27.0.12077973`.

The two flavor dimensions are ABI (`universal`, `arm64`, `armeabi`, `x86`, `x86_64`) and service variant (`foss`, `gms`). Build types are `debug` and `release`. Product-flavor task names put ABI before service variant, for example `UniversalFossDebug`.

## Local validation

Create an ignored `local.properties` from `local.properties.template` and set its `sdk.dir`. Then run:

```bash
./gradlew --no-daemon \
  :app:assembleUniversalFossDebug \
  :app:testUniversalFossDebugUnitTest \
  :app:lintUniversalFossDebug \
  :app:assembleUniversalGmsDebug \
  :app:testUniversalGmsDebugUnitTest \
  :app:lintUniversalGmsDebug
```

This is the CI debug validation path. It intentionally needs no release keystore; the GMS task also remains configuration-safe when `app/google-services.json` is absent. On Windows, replace `./gradlew` with `gradlew.bat`. This documentation records the configured commands, not a local Android build result.

CI additionally executes the production-surface Compose smoke suite with the Gradle Managed Device task below. The device is `auriqoApi30` (Pixel 2, API 30, `aosp-atd`), the job allows 30 minutes, and instrumentation reports are uploaded even when the task fails. A recorded managed-device run on 2026-08-05 passed all four smoke tests (Home→Search navigation, Search text entry, Settings→Player Settings navigation, and player transport semantics). That evidence is smoke coverage only; it does not verify a signed release or the universal release candidate.

```bash
./gradlew --no-daemon :app:auriqoApi30UniversalFossDebugAndroidTest
```

Other useful verified task shapes are:

```bash
./gradlew :app:assembleUniversalGmsDebug
./gradlew :app:assembleUniversalFossRelease
./gradlew :app:assembleArm64FossDebug
```

The GMS flavor includes Google Cast, Google Drive, Firebase Analytics, and Crashlytics dependencies. Gradle applies the Google Services and Crashlytics plugins only when `app/google-services.json` exists. GMS manifest defaults disable Analytics and Crashlytics collection; the app enables them only after an explicit recorded opt-in. FOSS includes neither Firebase telemetry SDK nor telemetry UI. `app/google-services.json` is an untracked maintainer-provided file; do not create, copy, or commit it merely to make a build pass.

Listen Together endpoint fields are blank by default. `LISTEN_TOGETHER_SERVER_URL` accepts a configured `wss://` server and `LISTEN_TOGETHER_SHARE_BASE_URL` an `https://` invite base; provide them only through ignored local properties, `-P` properties, or CI environment configuration. Blank or invalid values leave the feature unavailable.

## Signing

Release builds use `app/keystore/release.keystore` plus the `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` environment variables. The release task must not be represented as a signed release unless all four inputs are present and the produced signer is independently verified. `app/keystore/` is ignored.

CI accepts signing material only in its protected secret configuration:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The publication job materializes the keystore only in its runner and fails before the build if any required secret is empty. It prepares exactly one signed FOSS artifact, `universalFossRelease`, from `app/build/outputs/apk/universalFoss/release/`, stages it as `auriqo-foss-universal.apk`, rejects it above 200 MiB, and writes its `SHA256SUMS` entry. It does not build or publish a GMS artifact.

After supplying approved signing inputs locally, use the tracked guard before treating a release build as signed:

```bash
./gradlew :app:checkReleaseSigning
```

## CI and release gates

Pushes to `main`, pull requests targeting `main`, tags matching `v*`, and manual dispatches run Universal FOSS and Universal GMS debug build/test/lint validation without release secrets, then execute the Universal FOSS production-surface Compose smoke suite on the managed Pixel 2/API 30/`aosp-atd` device with a 30-minute timeout and always-uploaded reports. A manual dispatch also builds and uploads one **unsigned** FOSS universal release-candidate APK; it may use the selected branch or an optional existing tag. This candidate artifact is for validation only and must not be represented as an installable signed release. Publishing is a separate job: it runs only for a `v*` tag or manual dispatch with `publish` set to true, and it requires an existing version tag plus all signing secrets. That job rejects a non-version tag, requires the first line of `RELEASE_INFO.md` to be exactly `# Auriqo <tag>`, and rejects any line beginning `PENDING:`.

CodeQL targets GitHub Actions and Java/Kotlin only when GitHub code scanning is available: on a public repository or a private repository with Advanced Security enabled. This private repository does not claim a CodeQL result while that capability is unavailable. When analysis is supported, its Java/Kotlin job compiles the FOSS debug variant without Firebase or release secrets.

## Reproducible v1.0.0 preparation checklist

This checklist prepares a release; it does not authorize creating a tag, publishing an artifact, or submitting to a store.

- [x] Confirm that `app/build.gradle.kts` declares `versionName 1.0.0` and `versionCode 527`.
- [x] Update `CHANGELOG.md` and the `v1.0.0` candidate notes with source-evidenced changes.
- [ ] Resolve every `PENDING:` gate in `RELEASE_INFO.md`; its heading is already `# Auriqo v1.0.0` and must remain exact for the intended tag.
- [x] Record the managed-device Compose smoke task: `auriqoApi30` completed 4/4 production-surface tests on 2026-08-05.
- [ ] Run and record the Universal FOSS/GMS debug candidate validation in GitHub from the authorized candidate revision. The universal release candidate is not release evidence.
- [ ] Build the signed FOSS universal APK with approved signing material; inspect its package ID/version, signer certificate fingerprint, size budget, and `SHA256SUMS` entry.
- [ ] Verify the FOSS/GMS distinction, permissions, third-party disclosures, backup behavior, and [PRIVACY_POLICY.md](PRIVACY_POLICY.md) against the final source.
- [ ] Complete the unpublished F-Droid and Play metadata prerequisites in `distribution/` and `fastlane/`.
- [ ] Obtain maintainer/legal confirmation for public privacy contact, policy, store disclosures, and release authorization.
- [ ] Only then have an authorized maintainer create the tag or run a manual dispatch with publication enabled.
