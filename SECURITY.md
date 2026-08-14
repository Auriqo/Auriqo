# Security Policy

## Supported Versions

Security fixes are handled on the `main` branch and, when practical, on the
latest published prerelease.

| Version or channel | Support |
| --- | --- |
| `main` | :white_check_mark: |
| Latest `v1.0.2-alpha` prerelease | Best effort |
| Older builds | :x: |

## Reporting a Vulnerability

Please do not open a public issue for a security vulnerability.

1. Use GitHub's [private vulnerability reporting](https://github.com/Auriqo/Auriqo/security/advisories/new).
2. Include the affected version or commit, steps to reproduce, impact, and any
   suggested mitigation.
3. If private reporting is unavailable, contact the maintainers privately
   through the [Auriqo repository](https://github.com/Auriqo/Auriqo).

Reports are reviewed confidentially and acknowledged as soon as possible.

## Sensitive Information

Never commit or publish:

- `local.properties`
- `app/google-services.json`
- `gradle.properties` when it contains signing or service credentials
- `*.keystore`, `*.jks`, `*.pem`, or signing passwords
- `secrets.properties`
- `**/assets/po_token.html`
- API keys, OAuth tokens, cookies, or personal access tokens

The tracked `app/persistent-debug.keystore` is a deterministic debug-only
keystore. It must never be used to sign a release build.

Some internal package names and legacy service identifiers remain for
backward compatibility. They are not credentials; report any actual secret
or token instead of treating a historical identifier as sensitive data.

## Dependency and Build Hygiene

- Keep dependencies and GitHub Actions updated.
- Review generated diffs before committing.
- Run the relevant FOSS or GMS build before publishing an APK.
- Do not upload local build outputs, crash logs, or device data.

## Privacy

For information about app data handling, see
[PRIVACY_POLICY.md](PRIVACY_POLICY.md). For third-party licenses and bundled
font notices, see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Contact

Security contact: [private vulnerability reporting on GitHub](https://github.com/Auriqo/Auriqo/security/advisories/new).
