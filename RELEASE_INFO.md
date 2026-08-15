# Release information

## Current status

Auriqo is in active alpha development. The current test release is `v1.0.3-alpha`; its immutable artifact record is [docs/releases/v1.0.3-alpha.md](docs/releases/v1.0.3-alpha.md). The existing `v1.0.2-alpha` tag and release must not be replaced, retagged or overwritten. The Android modules still declare `versionCode 1` and `versionName 1.0.0`; `v1.0.3-alpha` discloses that mismatch and is debug-signed, while an official stable release must reconcile the package metadata with its tag and changelog.

The public CI validates the credential-free FOSS debug variant. It does not sign or publish official artifacts.

## Build variants

- `UniversalFossDebug`: reference contributor build; no Google Play Services Cast integration.
- `UniversalGmsDebug`: debug build with Google Play Services integrations such as Cast.
- `UniversalFossRelease` and `UniversalGmsRelease`: maintainer-only signed builds using protected release material.
- `wear:Debug`: Wear OS companion/Tile test build; rich synchronization requires `UniversalGmsDebug` on the paired phone.
- Architecture-specific variants are available through the `abi` dimension: `universal`, `arm64`, `armeabi`, `x86` and `x86_64`.

## Release checklist

Before creating a tag or publishing an APK:

1. Confirm that `main` is clean, reviewable and at the intended commit.
2. Reconcile `versionCode`, `versionName`, tag, changelog and release notes for an official release. An explicitly labelled debug alpha may instead disclose the mismatch in its immutable release record.
3. Review [SECURITY.md](SECURITY.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [docs/PROVENANCE.md](docs/PROVENANCE.md) and [docs/CI_RELEASE_REVIEW.md](docs/CI_RELEASE_REVIEW.md).
4. Run FOSS compile, Better Lyrics web verification, module/unit tests, Wear tests, lint and the final secret scan without private credentials.
5. Build official artifacts with a protected, non-repository signing key. A debug alpha may use `app/persistent-debug.keystore` only under the artifact policy below.
6. Calculate SHA-256 for every published artifact and include the values in the release notes.
7. Test installation, upgrade from the previous supported build, startup, playback, account logout and affected optional providers.
8. Review the final APK for credentials, debug logging, obsolete branding, renderer assets/licenses and unexpected permissions.
9. Create the tag and release manually after approval. Do not mutate `v1.0.2-alpha`.

## Artifact policy

Debug APKs under `app/build/` are ignored build outputs and must not be committed. A deliberately
labelled alpha/test pre-release may attach a debug-signed APK only when the release notes say that
it is signed by the public persistent debug key and is not an official/stable signing identity.
Stable or official artifacts require the protected release key. Every published artifact must
record its exact variant, commit, signing class and SHA-256.

## Checksums

Checksums belong to a specific artifact and commit. This file intentionally does not carry a moving checksum for a local build. Add SHA-256 values to the approved release notes alongside the APK filename and variant.

## Maintainer-only configuration

Release signing uses a protected `keystore/release.keystore` and the environment variables `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. The repository does not contain those files or values. Do not create a release key in CI as a fallback or add credentials to `gradle.properties`.
