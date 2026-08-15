# Release information

## Current status

Auriqo is in active alpha development. The repository already contains the `v1.0.2-alpha` tag and release; do not replace, retag or overwrite it. The current Android module declares `versionCode 1` and `versionName 1.0.0`, so those values must be reconciled with a future release tag and changelog in one reviewed change.

The public CI validates the credential-free FOSS debug variant. It does not sign or publish official artifacts.

## Build variants

- `UniversalFossDebug`: reference contributor build; no Google Play Services Cast integration.
- `UniversalGmsDebug`: debug build with Google Play Services integrations such as Cast.
- `UniversalFossRelease` and `UniversalGmsRelease`: maintainer-only signed builds using protected release material.
- Architecture-specific variants are available through the `abi` dimension: `universal`, `arm64`, `armeabi`, `x86` and `x86_64`.

## Release checklist

Before creating a tag or publishing an APK:

1. Confirm that `main` is clean, reviewable and at the intended commit.
2. Reconcile `versionCode`, `versionName`, tag, changelog and release notes.
3. Review [SECURITY.md](SECURITY.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [docs/PROVENANCE.md](docs/PROVENANCE.md) and [docs/CI_RELEASE_REVIEW.md](docs/CI_RELEASE_REVIEW.md).
4. Run FOSS compile, unit tests, lint and the final secret scan without private credentials.
5. Build the release with a protected, non-repository signing key. Never use `app/persistent-debug.keystore`.
6. Calculate SHA-256 for every published artifact and include the values in the release notes.
7. Test installation, upgrade from the previous supported build, startup, playback, account logout and affected optional providers.
8. Review the final APK for credentials, debug logging, obsolete branding and unexpected permissions.
9. Create the tag and release manually after approval. Do not mutate `v1.0.2-alpha`.

## Artifact policy

Debug APKs under `app/build/` are ignored build outputs, not releases. Do not commit them. A release artifact is publishable only when its exact variant, commit, signing identity and SHA-256 are recorded in the release notes.

## Checksums

Checksums belong to a specific artifact and commit. This file intentionally does not carry a moving checksum for a local build. Add SHA-256 values to the approved release notes alongside the APK filename and variant.

## Maintainer-only configuration

Release signing uses a protected `keystore/release.keystore` and the environment variables `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. The repository does not contain those files or values. Do not create a release key in CI as a fallback or add credentials to `gradle.properties`.
