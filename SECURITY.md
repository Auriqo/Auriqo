# Security policy

## Supported builds

No release line is currently designated as receiving security updates. Version `1.0.0` (version code `527`) is prepared in the tracked source, but no signed APK, tag, GitHub release, or store publication has been created. It is therefore not a published or supported release. A maintainer must define supported versions when a release channel is established.

## Reporting a vulnerability

Do not open a public issue or pull request for a suspected vulnerability. Use the private vulnerability-reporting/advisory facility available to repository members, or contact a repository maintainer through an already established private channel. Include:

- affected version, variant, and device/Android version when known;
- a minimal reproduction or proof of concept;
- impact and any constraints; and
- whether credentials, account data, or other sensitive material may have been exposed.

Do not attach secrets, keystores, account tokens, or private media data. If a report requires sensitive details, first ask the maintainer for a safe exchange method.

## Release and build integrity

CI validates the FOSS debug variant without Firebase configuration or release secrets. Signing and publishing run only for a `v*` tag or a manual dispatch with publishing enabled, and fail if the required signing inputs are absent, the tag/release-note heading differs, or candidate notes contain an unresolved `PENDING:` gate. This is an automation control, not a guarantee that every artifact is safe or suitable for distribution.

Before trusting a release artifact, maintainers should verify its tag, source revision, variant, package ID, signer certificate fingerprint, and checksum using their approved release process. Do not distribute developer keystores or locally built artifacts as official releases.

## Sensitive files

Keep the following untracked and out of reports:

- `local.properties`
- `app/google-services.json`
- release or debug keystores and key material
- OAuth/API credentials and tokens
- local media, downloads, backups, and diagnostic logs containing user data

See [SETUP.md](SETUP.md) and [BUILD.md](BUILD.md) for the expected local and CI configuration.
