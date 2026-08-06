# Contributing to Auriqo

The repository is private at present. Contributions require access granted by a maintainer; do not assume that a public fork, public issue tracker, or public release channel exists.

## Before making a change

1. Read [ARCHITECTURE.md](ARCHITECTURE.md) and [BUILD.md](BUILD.md).
2. Discuss significant work through the repository's available issue or pull-request process.
3. Create a focused branch from the current integration branch.
4. Keep unrelated formatting and generated files out of the change.

## Local checks

Run the relevant checks for the variant you changed. The baseline CI-equivalent check is:

```bash
./gradlew --no-daemon \
  :app:assembleUniversalFossDebug \
  :app:testUniversalFossDebugUnitTest \
  :app:lintUniversalFossDebug
```

Use `gradlew.bat` on Windows. The FOSS task is intentional: it can validate an authorized checkout without `google-services.json` or release-signing material. Changes specific to the GMS flavor need a maintainer-provided configuration and should state how they were checked.

The unit-test suite includes privacy-consent, offline OSS-attribution, Listen Together configuration, lyrics fallback/cache, download state, recognition, integration-boundary, and widget-policy coverage. Room schema history is checked statically; do not claim Room or DataStore migration execution coverage unless a test actually performs that migration. Add or update focused tests when changing those behaviors.

## Pull requests

Describe the problem, approach, tests, variant(s), and any user-visible or privacy impact. Include screenshots only when they do not expose accounts, media library data, tokens, device identifiers, or API keys.

Before requesting review:

- [ ] The relevant FOSS build/test/lint command completed, or the reason it could not run is stated.
- [ ] Documentation and distribution metadata changed when behavior or store disclosures changed.
- [ ] No generated APK/AAB, `local.properties`, `google-services.json`, keystore, token, or credential is included.
- [ ] The change does not silently modify versioning, signing, or release automation.
- [ ] Affected privacy, security, backup, permission, or third-party-service behavior is called out.
- [ ] Listen Together changes preserve blank-default deployment fields; no endpoint is added to source.
- [ ] Telemetry changes preserve the FOSS/GMS boundary and explicit recorded opt-in semantics.
- [ ] Direct runtime dependency changes update the offline OSS attribution asset and its validation as needed.

Use clear conventional-style commit subjects such as `fix:`, `feat:`, `docs:`, `test:`, or `chore:`.

## Sensitive information and security reports

Never commit credentials, OAuth material, signing keys, device backups, or YouTube authentication material. If you suspect a secret was exposed, stop sharing it, rotate/revoke it through the owning service, and notify a maintainer through a private channel.

Do not report security vulnerabilities in normal issues or pull requests. Follow [SECURITY.md](SECURITY.md).

## Release-related changes

Only a maintainer should prepare a release. Update [CHANGELOG.md](CHANGELOG.md), the tag-specific [RELEASE_INFO.md](RELEASE_INFO.md), and the checklist in [BUILD.md](BUILD.md) together. A tag or dispatch with publishing enabled fails rather than creating an unsigned or ambiguously documented release when required signing secrets, matching release notes, or `PENDING:`-marked candidate evidence remain. Version `1.0.0` is prepared but not released.
