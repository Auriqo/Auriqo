# AGENTS.md

This file is the repository-wide operating manual for human contributors and coding agents
working on Auriqo. It applies to every directory unless a more specific instruction file is
added later. It describes the actual project boundaries, not a generic Android checklist.

## 1. Mission And Scope

Auriqo is an open-source Android music player released under GPLv3. The product combines:

- YouTube Music and YouTube playback through a local InnerTube client.
- Local media playback, queues, library and playlists.
- Synchronized lyrics from several providers and the pinned Better Lyrics renderer.
- Optional account, scrobbling, recognition, Discord and Listen Together integrations.
- Standard Android Media3 controls and an optional GMS/Wear synchronization channel.
- A small optional YouTube attribution Worker under `workers/youtube-attribution/`.

Auriqo is independent from YouTube, Google, Spotify, Discord, Last.fm, ListenBrainz, Shazam and
all lyrics providers. Provider contracts can change without notice. Code that depends on a remote
service must fail clearly and locally; do not assume that a remote outage is an app regression.

The current supported release line is `v1.0.5`. Historical alpha, RC and debug artifacts are not
supported release baselines. The repository may still contain compatibility code for old installs;
do not remove it just because it looks unused without tracing its persisted-data or update impact.

## 2. Instruction Precedence

When instructions conflict, use this order:

1. System and platform safety rules.
2. The user's explicit request for the current task.
3. This file and any more specific repository instruction file.
4. Existing code and documentation conventions.

If an instruction is ambiguous and the choice can delete data, rewrite history, change a public
contract or publish an artifact, stop and ask. For ordinary implementation details, choose the
smallest compatible change and document the assumption in the final report.

## 3. Read Before Editing

Always read the relevant portions of these files before changing behavior:

- `README.md`: supported functionality, variants, commands and user-facing limitations.
- `SETUP.md`: toolchain, local configuration, tests and troubleshooting.
- `CONTRIBUTING.md`: branches, commits, review expectations and local-file rules.
- `docs/ARCHITECTURE.md`: module boundaries and playback flow.
- `docs/USER_GUIDE.md`: behavior that users are expected to see.
- `docs/TROUBLESHOOTING.md`: existing diagnosis paths and report requirements.
- The feature document under `docs/` when changing lyrics, Wear, Workers, provenance or release.

Before editing, establish the repository state:

```bash
git status --short --branch
git log --oneline -10
git diff --stat
```

Do not discard changes found in the worktree. They may belong to the user or another agent. If a
change in a file you need to edit conflicts with the current task, inspect it and preserve it when
possible; ask only when the conflict cannot be resolved safely.

## 4. Project Map

### Application

- `app/`: Android application, Compose UI, playback service, settings, updater, integrations and
  variant source sets.
- `app/src/main/`: behavior shared by FOSS and GMS unless overridden by a source set.
- `app/src/foss/`: code that must remain free of GMS/Firebase dependencies.
- `app/src/gms/`: Google Play Services, Cast and phone-side Wear Data Layer behavior.
- `app/src/debug/`: debug-only package/label and diagnostics. Never use it as the release baseline.
- `app/src/release/`: release-specific resources or behavior.
- `app/src/test/`: JVM unit tests for application logic, parsers, updater and runtime boundaries.
- `app/src/main/assets/`: shipped static assets. Generated assets must have a documented source.

### Provider And Feature Modules

- `innertube/`: YouTube and YouTube Music request models, parsers and page clients.
- `betterlyrics/`: Kotlin Better Lyrics client and TTML parser. The current checkout does not
  contain the historical browser-renderer source or generated web asset tree.
- `unison/`: lyrics identity and signed community actions.
- `lrclib/`, `paxsenixlyrics/`, `kugou/`, `simpmusic/`, `youlyplus/` and `letras/`: lyrics adapters.
- `canvas/`, `auriqocanvas/`, `applecanvas/` and `artistvideo/`: artwork and media enrichment.
- `shazamkit/`: optional recognition integration.
- `wear/`: Wear OS companion application and Tile.
- `workers/youtube-attribution/`: optional TypeScript Worker used for playlist attribution.

### Documentation And Operations

- `docs/USER_GUIDE.md`: install and use the released application.
- `docs/ARCHITECTURE.md`: module map, playback flow and data boundaries.
- `docs/TROUBLESHOOTING.md`: user and contributor diagnosis.
- `docs/LYRICS_PROVIDERS.md`: provider behavior and failure boundaries.
- Better Lyrics details: `docs/LYRICS_PROVIDERS.md` and `docs/PROVENANCE.md`.
- `docs/WEAR_OS.md`: phone-to-Wear protocol and compatibility.
- `docs/WORKERS.md`: attribution Worker configuration and deployment.
- `docs/PROVENANCE.md` and `THIRD_PARTY_NOTICES.md`: source, license and asset provenance.
- `docs/CI_RELEASE_REVIEW.md`: public CI versus maintainer release boundary.
- `RELEASE_INFO.md`: official artifact checklist and signing policy.

## 5. Compatibility Invariants

Treat these as public APIs even when they are represented by constants or file names:

- Preserve application ID `com.auriqa.music`.
- Preserve existing preferences, DataStore keys, deep links and URI hosts.
- Preserve resource identifiers used by persisted state, notifications, widgets and integrations.
- Preserve updater package/version/signature checks and Android package-installer handoff.
- Preserve provider models and failure semantics unless the provider contract is intentionally
  migrated and the migration is documented.
- Preserve the Wear Data Layer paths, payload versioning and ordering rules in `docs/WEAR_OS.md`.
- Keep production and debug package/signing behavior distinct.
- Keep FOSS and GMS builds independently buildable.

Do not perform mass renames, package changes, preference migrations or resource cleanups as part of
an unrelated feature. If a technical identifier is inherited from an upstream project, assume it
may be part of compatibility until proven otherwise.

## 6. Build Variants And Artifacts

The `variant` dimension provides `foss` and `gms`. The `abi` dimension provides `universal`,
`arm64`, `armeabi`, `x86` and `x86_64`.

The normal contributor targets are:

```text
:app:compileUniversalFossDebugKotlin
:app:testUniversalFossDebugUnitTest
:app:assembleUniversalFossDebug
:app:lintUniversalFossDebug
```

The corresponding phone release tasks are:

```text
:app:assembleUniversalFossRelease
:app:assembleUniversalGmsRelease
```

The Wear tasks are:

```text
:wear:testDebugUnitTest
:wear:assembleDebug
:wear:assembleRelease
```

Expected APK locations include:

- `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`.
- `app/build/outputs/apk/universalGms/debug/app-universal-gms-debug.apk`.
- `app/build/outputs/apk/universalFoss/release/app-universal-foss-release.apk`.
- `app/build/outputs/apk/universalGms/release/app-universal-gms-release.apk`.
- `wear/build/outputs/apk/release/wear-release.apk`.

Build outputs, reports, logs, mappings and APKs are ignored artifacts. Do not commit them.

`UniversalFossDebug` is the reference build for pull requests. It must not require Firebase files,
private provider credentials, cookies, a release keystore or a maintainer account. GMS behavior must
remain behind GMS dependencies and source boundaries. FOSS should still expose standard Android
media controls even when GMS-only Wear synchronization or Cast is unavailable.

## 7. Toolchain And Local Configuration

Use the versions documented in the repository:

- JDK 21.
- Android SDK Platform 36 and matching Build-Tools.
- Android NDK `27.0.12077973`.
- Gradle wrapper 9.3.1.
- Android Gradle Plugin 9.0.0.
- Kotlin 2.3.10, unless a dependency update intentionally changes the compatible toolchain.

Use `./gradlew` on Linux/macOS and `gradlew.bat` on Windows. Android Studio is an editor, not a
separate source of truth. Configure an ignored root `local.properties` with `sdk.dir`; never put
tokens, passwords or signing data there.

The following files must remain local or secret:

- `local.properties`.
- `app/google-services.json` and Firebase configuration.
- `.env*`, `*.env`, provider cookies and account exports.
- Release keystores and signing credentials.
- Device logs, screenshots with personal data and generated APKs.

The tracked `app/persistent-debug.keystore` is intentionally public for installable debug builds.
It is not an official identity and must never sign a stable release.

## 8. Memory-Constrained Builds

The development environment may have approximately 6 GB total RAM shared with other programs.
Avoid parallel Gradle builds and avoid running Android Studio, emulators and multiple Gradle daemons
at the same time.

Use these constraints when a build is expensive:

```bash
./gradlew <task> --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
  -Dorg.gradle.workers.max=1 \
  -Dkotlin.compiler.execution.strategy=in-process
```

Build one variant at a time. Prefer the smallest task that proves the change. Keep Gradle caches;
deleting caches is not a valid memory fix and often makes the next build more expensive.

For release R8 builds, a maintainer may use a release-only 3-4 GB heap override when enough memory
is available. Never raise the global heap requirement for all contributors or CI without evidence.

## 9. Coding Rules

- Keep changes local and composable; avoid speculative abstractions.
- Follow existing Kotlin, Compose, Gradle and TypeScript style in the surrounding file.
- Prefer existing models, helpers and dependency versions over new libraries.
- Do not add a dependency for a small utility or duplicate an existing provider abstraction.
- Keep UI code separate from provider response parsing and network authentication.
- Preserve accessible labels, semantics, loading states, empty states and error states.
- Use explicit variant boundaries instead of runtime checks when the dependency itself is variant-only.
- Avoid global mutable state and long-lived references to Android `Context` or `Activity`.
- Do not log cookies, OAuth tokens, account IDs, PoToken material, signed requests or full remote
  error bodies.
- Add a focused test when changing a parser, updater, crypto/signature boundary, persisted model,
  protocol payload or provider failure path.
- Add comments only when they explain a non-obvious invariant or compatibility constraint.

## 10. Provider And Playback Changes

The playback path crosses several boundaries. Keep the responsibilities separate:

1. `innertube` resolves page data, media metadata and stream information.
2. The app selects a playable format and creates a Media3 item.
3. The native player runtime evaluates YouTube signature and `n` transformations.
4. The WebView bridge acquires PoToken only; do not reintroduce cipher execution into WebView
   without a documented compatibility reason.
5. The playback service publishes state to system controls and optional GMS surfaces.

When YouTube changes:

- Reproduce with a public media item first.
- Capture the player hash, response boundary and failure stage without logging sensitive material.
- Inspect `PlayerJsFetcher`, URL parsing, native runtime and PoToken code before changing UI.
- Add or update focused parser/runtime tests.
- Keep fallback behavior bounded and observable; do not silently accept an unverified transform.
- Document the provider change in the changelog when it affects users.

Do not solve a provider outage by disabling TLS verification, committing cookies, adding a private
API key or making the app accept arbitrary player code.

## 11. Lyrics And Better Lyrics

Provider adapters should return the common lyrics model and keep endpoint-specific parsing inside
their module. When adding or changing an adapter, document:

- Endpoint and request method.
- Authentication or user configuration.
- Data sent and data retained.
- Timing, translation and romanization behavior.
- Empty, rate-limit and outage behavior.
- License and source provenance.

The current checkout contains only the Kotlin client, models and TTML parser under
`betterlyrics/src/main/kotlin/`. It does not contain `betterlyrics/web/package.json`, a web lockfile,
renderer source or generated web assets. Do not invent a regeneration command or claim that the
historical browser renderer is shipped. If the renderer source is restored in a future change,
add its source boundary, lockfile, verification command and provenance record before using it.

For the code that is present:

- Keep provider parsing inside the Better Lyrics module.
- Run `./gradlew :betterlyrics:testDebugUnitTest --no-daemon` for Kotlin changes.
- Preserve any origin, sequence, generation or reduced-motion invariant only when it exists in the
  restored implementation; verify the actual source before describing it as current behavior.

Unison identity and signed community actions are a security boundary. Do not weaken signature
verification, export encryption, origin checks or replay protection to make a UI test pass.

## 12. Wear OS And Protocol Changes

The phone publisher and `wear/` consumer form one protocol. Change them together when changing:

- Data Layer paths or payload fields.
- Session/boot/sequence ordering.
- Like, shuffle, repeat, seek or transport commands.
- Package identity or signing assumptions.
- Background heartbeat and reconnect behavior.

For every protocol change:

- Update `docs/WEAR_OS.md`.
- Test a fresh install and an upgrade from the previous supported build.
- Test phone restart, watch restart, disconnect/reconnect and stale message ordering.
- Verify that FOSS still builds without GMS classes.
- Verify that standard Media3 controls remain usable when custom sync is unavailable.

Do not rename `com.auriqa.music` or the Wear technical namespace as cosmetic cleanup.

## 13. Optional Integrations And Workers

Integrations must be optional and independently diagnosable. A failure in Spotify import, Last.fm,
ListenBrainz, Discord, Shazam, AI translation or Listen Together must not prevent basic local or
public playback.

For each integration change, update the relevant documentation and tests. Record whether it needs:

- A login, cookie, OAuth token or user-entered API key.
- A public or private endpoint.
- FOSS, GMS, Wear or Worker support.
- Network access, background work or a new permission.
- A migration for stored settings.

The attribution Worker is separate from the Android build. Work under `workers/youtube-attribution/`
uses Node/npm and TypeScript checks. Never put Worker credentials or deployment configuration in the
Android repository. Worker deployment is maintainer-only.

## 14. Tests And Validation Matrix

Start with the narrowest relevant check and expand based on the change.

### Ordinary app change

```bash
./gradlew :app:compileUniversalFossDebugKotlin --no-daemon
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
./gradlew :app:assembleUniversalFossDebug --no-daemon
./gradlew :app:lintUniversalFossDebug --no-daemon
git diff --check
```

### Provider or parser change

```bash
./gradlew :innertube:testDebugUnitTest --no-daemon
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
```

### Lyrics or Better Lyrics Kotlin change

```bash
./gradlew :betterlyrics:testDebugUnitTest :unison:test --no-daemon
```

There is no web renderer source or npm project in the current checkout. Do not run `npm ci` under
`betterlyrics/web`; that directory is not a tracked source tree. A future renderer restoration must
add its own documented verification command and checked-in provenance first.

### Wear change

```bash
./gradlew :wear:testDebugUnitTest :wear:assembleDebug --no-daemon
./gradlew :app:assembleUniversalGmsDebug --no-daemon
```

### Worker change

```bash
cd workers/youtube-attribution
npm ci
npm run typecheck
```

### Release change

Run the focused checks above, then the release checklist in `RELEASE_INFO.md`. Official artifacts
must be built with the protected production keystore, verified with `apksigner`, hashed with
SHA-256 and recorded in the release notes. Public CI does not sign or publish official artifacts.

If a command cannot run, report the exact command, environment limitation and first meaningful
failure. Never weaken a test, remove a failing test or hide a failure to make CI green.

## 15. CI Workflows

The repository currently has three workflow families:

- `.github/workflows/gradle.yml`: FOSS Android build/tests/lint and Worker typecheck.
- `.github/workflows/codeql.yml`: Actions, Java/Kotlin and JavaScript/TypeScript analysis.
- `.github/workflows/sync-player-configs.yml`: validates upstream player config and opens a review
  pull request; it must not write downloaded data directly to `main`.

Actions must stay pinned to immutable commit SHAs with a version comment. Keep permissions minimal.
Public CI must not receive provider secrets, cookies, Firebase credentials or release keystores.
Do not add a tag-triggered release publisher or predictable signing fallback to public CI.

When a dependency PR has failed CodeQL but no build/test result, do not call it safe based on the
version number alone. Inspect the workflow run, run the relevant local task or wait for a fresh CI
result on the current `main` before merging.

## 16. Git Workflow

Use a focused branch based on current `main`:

```bash
git switch main
git pull --ff-only origin main
git switch -c fix/short-description
```

Use Conventional Commit subjects such as:

```text
fix(playback): handle rotated player transforms
feat(lyrics): expose provider offset controls
docs: clarify FOSS setup
test(wear): cover stale session ordering
chore(ci): pin action to immutable commit
```

Before committing:

```bash
git status --short
git diff --check
git diff --stat
git log --oneline -10
```

Stage only intended paths. Do not amend a commit unless explicitly requested. Do not use
`git reset --hard`, `git checkout --`, force-push, interactive history rewrites or tag movement.
Do not delete another contributor's branch, PR, tag or release without explicit authorization.

The user must explicitly authorize push, merge, release publication and destructive cleanup. A
request to "finish the release" is authorization to perform the documented release steps, but not
permission to expose secrets or publish a debug artifact as stable.

## 17. Pull Request Requirements

A pull request should state:

- User-visible or maintenance reason.
- Affected modules and FOSS/GMS/Wear variants.
- Compatibility impact on application ID, settings, deep links or protocols.
- Tests run and tests not run with a reason.
- Provider, permission, data-flow and provenance changes.
- Screenshots or recordings for meaningful UI changes when available.
- Migration or rollback considerations for persisted data and releases.

Reviewers should be especially cautious with dependencies, parsers, authentication, WebView
bridges, signing, updater logic, permissions and protocol changes. A small diff can still have a
large compatibility surface.

## 18. Security And Privacy

Security-sensitive reports belong in `SECURITY.md`, never in a public issue or PR. Never print or
commit:

- Passwords, PATs, OAuth tokens, cookies, PoTokens or Firebase secrets.
- Release keystore contents, aliases or signing passwords.
- Private playlist URLs or unredacted provider responses.
- Device identifiers, personal account data or full crash dumps.

When showing logs, redact before sharing. Treat user-entered integration keys as secrets even when
the app stores them locally. Do not broaden permissions, cleartext networking or WebView origins
without a documented threat model and focused review.

Privacy documentation describes data flows; it does not replace user-facing documentation. Any new
integration must also update the user guide, troubleshooting path and architecture boundary where
appropriate.

## 19. Documentation Rules

Update documentation in the same change when behavior changes. Choose the document by audience:

- User-visible workflow: `docs/USER_GUIDE.md`.
- Build/setup: `SETUP.md` and `docs/TROUBLESHOOTING.md`.
- Code/module boundary: `docs/ARCHITECTURE.md`.
- Provider behavior: `docs/LYRICS_PROVIDERS.md` or the relevant feature document.
- Wear protocol: `docs/WEAR_OS.md`.
- Release artifact: `RELEASE_INFO.md` and `docs/releases/`.
- Source/license: `docs/PROVENANCE.md` and `THIRD_PARTY_NOTICES.md`.
- Security or data flow: `SECURITY.md` and `PRIVACY_POLICY.md`.

Do not leave links to deleted releases or testing branches. Do not describe a feature as supported
unless the current code and a supported variant actually provide it. Prefer concrete commands,
paths, variant names and failure symptoms over vague prose.

## 20. Release And Signing Boundary

Official stable releases require all of the following:

- Intended commit on `main` with a clean, reviewable worktree.
- Matching `versionCode`, `versionName`, tag, changelog and release notes.
- Protected `app/keystore/release.keystore` and maintainer-only signing environment.
- FOSS, GMS and Wear artifacts built from the same source commit where applicable.
- APK signature verification, SHA-256 checksums and exact artifact names recorded.
- Installation, upgrade, startup, playback, logout and affected integration checks.
- Review for credentials, debug logging, permissions, obsolete branding and licenses.

Release environment variables are `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Never print
their values or place them in tracked files. The public persistent debug key is never a signing
fallback. Never publish a debug APK under a stable release name.

The updater must continue to use HTTPS, select the artifact matching installed package/variant,
verify digest, package, version and signing history, and hand installation to Android's package
installer. Never add an arbitrary-APK fallback or bypass platform installer safeguards.

## 21. Completion Checklist

Before reporting a code task complete, confirm:

- The requested behavior is implemented in the correct module and variant.
- Existing compatibility identifiers and contracts were preserved.
- Relevant tests, compile, lint or web checks were run.
- `git diff --check` passes.
- No secrets, local files, generated outputs or APKs are staged.
- User-facing, architecture, provider, release or provenance docs were updated when needed.
- The final diff contains only task-related changes.
- The worktree and branch state are reported accurately.

Before reporting a review or merge task complete, also confirm:

- Every requested PR was inspected, including failed or stale checks.
- Only PRs with sufficient evidence were merged.
- Merges did not silently delete branches, tags or releases outside the request.
- CI status was checked on the resulting `main` commit.
- Remaining blockers are named with their exact PRs and reason.

## 22. Useful Commands

```bash
# Repository state
git status --short --branch
git log --oneline -10
git diff --check
git diff --stat

# Gradle discovery
./gradlew tasks --all --no-daemon
./gradlew :app:tasks --all --no-daemon

# Low-memory reference build
./gradlew :app:assembleUniversalFossDebug --no-daemon \
  -Dorg.gradle.workers.max=1 \
  -Dkotlin.compiler.execution.strategy=in-process

# Inspect release APK without exposing secrets
apksigner verify --verbose --print-certs path/to/app.apk
sha256sum path/to/app.apk

# GitHub review
gh pr list --state open
gh pr checks <number>
gh pr diff <number>
gh run list --branch main
```

When in doubt, prefer a smaller patch, a narrower build and a clear report over speculative cleanup.
