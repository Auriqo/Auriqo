# CI and release review

This document records the safety boundary between public collaboration builds and maintainer-only releases.

## Current public CI

- `.github/workflows/gradle.yml` builds and tests `UniversalFossDebug`, runs the relevant module tests and lint, and runs the attribution Worker typecheck.
- The FOSS job uses JDK 21 and the runner's Android SDK. It does not read Firebase files, API keys, OAuth secrets, cookies, signing material or webhooks.
- `.github/workflows/codeql.yml` analyzes Actions, Kotlin/Java and TypeScript without passing provider credentials into the build.
- `.github/workflows/sync-player-configs.yml` validates upstream JSON and opens a draft pull request. It does not commit downloaded content directly to `main`.
- Actions are pinned to immutable commit SHAs with version comments. Build jobs have read-only contents permissions.
- No workflow creates a GitHub release, signs an official APK or sends a release notification.

## Removed unsafe behavior

The previous public workflow generated a predictable fallback keystore, built a GMS variant with provider secrets, granted `contents: write`, uploaded artifacts under release-like names and published releases/notifications from tags. That path has been removed.

The public FOSS build is deliberately separate from official release signing. A value placed in a client APK is recoverable by its recipient and is not a secret, even when it originated in GitHub Actions.

## Release boundary

Official releases remain a manual maintainer operation:

1. review the exact commit and release notes;
2. reconcile Android version metadata, tag and changelog;
3. resolve the provenance and dependency-license gates;
4. build the chosen release variant with the protected release keystore;
5. inspect the APK and calculate SHA-256;
6. test installation and upgrade behavior; and
7. create the tag and GitHub release manually after approval.

Do not add a tag-triggered publishing step to the public FOSS workflow. If release automation is introduced later, it needs a separate approval-gated workflow with explicit permissions, protected environments and no signing fallback.

## Acceptance criteria

- FOSS compile, assemble, tests and lint pass without private credentials.
- No API key, OAuth client secret, cookie, webhook or signing material is reachable in the build output.
- Release signing uses a protected external keystore and never the tracked persistent debug keystore.
- Artifact name, variant, commit and SHA-256 are recorded in manually reviewed release notes.
- Configuration sync is reviewed as a pull request.
- CI logs and artifacts are checked for accidental sensitive data before publication.
