# AGENTS.md

These instructions apply to every human contributor and coding agent working
in the Auriqo repository. They are repository-wide guidance, not instructions
for a particular person, machine, branch or task.

## Project context

Auriqo is an open-source Android music player. The main application is in
app/, with provider and feature modules for InnerTube, lyrics, Better Lyrics,
Unison, Wear OS and the optional YouTube attribution Worker.

Before changing code, read the relevant sections of:

- README.md for the user-facing project status and supported workflows.
- SETUP.md for toolchain and local configuration.
- CONTRIBUTING.md for contribution and review expectations.
- ARCHITECTURE.md and the feature documentation under docs/ when changing
  module boundaries or integrations.

## Compatibility rules

- Preserve the application ID com.auriqa.music, existing preferences, deep
  links, resource identifiers and provider contracts unless a migration is
  part of the change.
- Keep FOSS and GMS variants working. UniversalFossDebug is the reference
  build and must not require Firebase files, private credentials or release
  signing material.
- Do not rename inherited technical identifiers or perform mass cosmetic
  renames without a concrete compatibility or maintenance benefit.
- Keep provider-specific behavior documented when an endpoint, authentication
  flow, data payload or failure mode changes.
- Treat user-facing behavior as important: preserve accessible flows, sensible
  error states and backwards-compatible upgrades.

## Safe repository workflow

- Start by checking git status --short, the current branch and recent history.
  Read this file and the relevant project documentation before editing.
- Preserve changes you did not create. Keep the patch focused, reviewable and
  free of unrelated formatting or generated files.
- Use small, reviewable patches and Conventional Commit subjects such as
  fix(lyrics): ... or docs: ....
- Do not commit secrets or local configuration such as local.properties,
  Firebase files, environment files or signing material. Never print secret
  values. Generated APKs, logs, dumps and build directories stay out of
  commits.
- Do not use git reset --hard, git checkout --, history rewrites or force
  pushes. Do not move or replace existing tags or releases.
- Follow the task's stated authorization for pushes and releases. If the
  request is not explicit, leave publication to a maintainer.

## Validation

Use JDK 21, Android SDK Platform 36 and NDK 27.0.12077973 as documented in
SETUP.md. Prefer the Gradle wrapper and keep the local Gradle/Android caches.

For normal application changes, run the smallest relevant checks and, when
practical, the reference checks:

~~~bash
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
./gradlew :app:compileUniversalFossDebugKotlin --no-daemon
./gradlew :app:assembleUniversalFossDebug --no-daemon
./gradlew :app:lintUniversalFossDebug --no-daemon
git diff --check
~~~

Changes that affect GMS, Better Lyrics, Worker or Wear code should also run
the focused commands listed in README.md and SETUP.md. Report commands that
cannot run instead of hiding or weakening their failures.

## Updater and release behavior

The in-app updater must continue to use HTTPS, select the artifact matching the
installed FOSS/GMS and debug/release variant, verify the downloaded digest,
package, version and signing history, and hand installation to Android's
package installer. Do not add a fallback that installs an arbitrary APK or
bypasses Android's installer safeguards.

Release assets use the documented variant names such as
app-universal-foss-debug.apk and app-universal-gms-debug.apk. A release change
should also update the relevant changelog, release notes, checksums and
documentation. Official signing and publication remain maintainer-only.

## Documentation and provenance

Update user-facing documentation when behavior, setup, supported services,
permissions or release steps change. Keep third-party notices and provenance
accurate; do not guess a license or copy upstream code without recording its
source and required attribution.

For security issues, follow SECURITY.md rather than opening a public issue.
For ordinary bugs, feature requests and setup questions, use the public issue
templates and keep the report friendly, reproducible and free of credentials.
