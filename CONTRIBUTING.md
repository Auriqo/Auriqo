# Contributing to Auriqo

Thanks for helping improve Auriqo. Bug reports, ideas, documentation fixes and code contributions are all welcome. A good contribution is focused, buildable and compatible with existing installs.

## Before opening an issue or pull request

- Search existing issues and pull requests.
- For a security vulnerability, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
- For a provider outage or authentication breakage, include the provider, build variant, Android version and a short error description.
- Confirm that the proposed change is compatible with the `com.auriqa.music` application ID and existing preference/deep-link identifiers unless a migration is part of the proposal.

## Development setup

See [SETUP.md](SETUP.md) for the exact JDK, Android SDK, NDK and Gradle requirements. The reference contributor build is:

```bash
./gradlew :app:assembleUniversalFossDebug --no-daemon
```

Useful checks before a pull request:

```bash
./gradlew :app:compileUniversalFossDebugKotlin --no-daemon
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
./gradlew :innertube:testDebugUnitTest --no-daemon
./gradlew :letras:test --no-daemon
./gradlew :app:lintUniversalFossDebug --no-daemon
git diff --check
```

Changes under `workers/youtube-attribution` also need:

```bash
npm ci
npm run typecheck
```

Do not delete local Gradle caches to make a build pass. If a test cannot run, report the command and the failure in the pull request.

## Project map

- `app/`: Android application, UI, playback, settings and integrations.
- `innertube/`: YouTube/YouTube Music client models and requests.
- `betterlyrics/`, `lrclib/`, `paxsenixlyrics/`, `kugou/`, `simpmusic/`, `youlyplus/`, `letras/`: lyrics integrations.
- `workers/youtube-attribution/`: optional Cloudflare Worker used for playlist attribution.
- `wear/`: Wear OS module.
- `third_party/` and `app/src/main/res/font/`: bundled fonts and notices.

Keep provider-specific code isolated. When adding a service, document its endpoint, authentication, data sent, failure behavior and license/provenance in the relevant documentation.

## Branches and commits

Create a focused branch from `main`. Use Conventional Commit-style messages, for example:

```text
fix(lyrics): handle missing TTML timing
docs: clarify FOSS setup
chore(ci): pin action versions
```

Do not rewrite shared history, move tags, or force-push branches used by other contributors.

## Keep local files local

Do not commit local configuration (`local.properties`, Firebase or environment files), credentials, signing material, device logs or generated APKs. If a log or screenshot helps explain a problem, remove account tokens and personal details first. The tracked persistent debug keystore is for local debug builds only and must never sign a release.

## Pull requests

A good pull request should:

- explain the user-visible or maintenance reason for the change;
- identify affected build variants and modules;
- include tests or explain why no automated test is practical;
- update documentation and third-party notices when behavior, data flow or provenance changes;
- avoid unrelated formatting or mass renames;
- include screenshots only when they help explain the change and match the current app.

If a change affects an integration, stored setting or release behavior, call it out in the pull request so reviewers can check compatibility.

## Code review expectations

Changes involving integrations, permissions, dependencies or release behavior may need an extra review pass.

By participating, you agree to follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Questions and support

For setup help or user-facing questions, see [SUPPORT.md](SUPPORT.md) and use the Question issue template. The [roadmap](ROADMAP.md) lists areas where focused contributions are useful.
