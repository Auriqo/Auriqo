# Release information

## Existing pre-release

The repository already contains the `v1.0.2-alpha` tag and its published release. It must not be replaced, retagged or overwritten without explicit maintainer approval.

The current Android module still declares `versionCode 1` and `versionName 1.0.0`. Before publishing a new release, reconcile the Gradle version metadata, tag name, changelog and release notes in one reviewed change.

The alpha release is a development preview. It should not be described as a stable compatibility guarantee or as evidence that every optional provider works.

## Build variants

- `UniversalFossDebug`: the reference credential-free debug build; no Google Play Services Cast integration.
- `UniversalGmsDebug`: debug build with Google Play Services integrations such as Cast.
- `UniversalFossRelease` and `UniversalGmsRelease`: maintainer-only signed builds. These require the protected release keystore and signing environment; they must never fall back to the tracked persistent debug keystore.

Architecture-specific variants are also available through the `abi` flavor dimension (`universal`, `arm64`, `armeabi`, `x86`, `x86_64`).

## Release checklist

Before creating a tag or publishing an APK, the maintainer must:

1. Confirm that `main` is clean, reviewable and at the intended commit.
2. Reconcile `versionCode`, `versionName`, tag, changelog and release notes.
3. Review the security and provenance items in [SECURITY.md](SECURITY.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [docs/PROVENANCE.md](docs/PROVENANCE.md).
4. Run the FOSS compile, unit tests, lint and secret scan without private credentials.
5. Build the release with a protected, non-repository signing key. Never use `app/persistent-debug.keystore`.
6. Calculate SHA-256 checksums for every published artifact and include them in the release notes.
7. Test installation, upgrade from the previous supported build, startup, playback, account logout and the affected optional providers.
8. Review the final APK contents for credentials, debug logging, obsolete branding and unexpected permissions.
9. Create the tag and release manually after approval. Do not let a tag-triggered workflow publish automatically until [docs/CI_RELEASE_REVIEW.md](docs/CI_RELEASE_REVIEW.md) is resolved.

## Artifact policy

Local debug APKs under `app/build/` are ignored build outputs, not releases. Do not commit or upload them automatically. A release artifact is publishable only when its exact variant, commit, signing identity and SHA-256 are recorded in the release notes.

## Checksums

Checksums belong to a specific artifact and commit. This file intentionally does not carry a moving checksum for a local build. Add SHA-256 values to the release notes of the approved release, alongside the APK filename and variant.

## Maintainer-only configuration

Release signing uses a protected `keystore/release.keystore` and the environment variables `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. CI must not generate a predictable release keystore, print secrets or embed client secrets in a distributed APK. The current workflow review and required remediation are documented separately.
