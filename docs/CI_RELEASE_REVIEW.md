# CI and release review

This is an audit record for the public workflows. The workflow files are intentionally not modified in this maintenance pass because the coordination handoff identifies an equivalent Windows checkout with an untracked workflow under active work. Apply the changes below only after reconciling that work with the maintainer.

## Findings in the current workflows

### Build variant and credentials

`.github/workflows/gradle.yml` currently builds `assembleUniversalGmsDebug`, not the credential-free FOSS reference build. It passes `LASTFM_API_KEY`, `LASTFM_SECRET`, `GH_CLIENT_ID` and `GH_CLIENT_SECRET` into Gradle. The Android build writes these values into `BuildConfig`; a value compiled into an APK is recoverable by anyone who receives the APK. In particular, the Last.fm secret is used by the client-side request signing code and cannot be treated as a distributed secret.

Required action: make the FOSS build and tests the default public CI path, and redesign any provider flow that needs a real secret so the secret stays server-side or uses a public/client-safe OAuth flow. Do not “fix” this by adding more GitHub secret masking.

### Signing and publication

The workflow can generate a predictable temporary release keystore when a release keystore secret is absent. It also has write permission and a tag-triggered path that creates a GitHub release and sends webhook notifications.

Required action: remove predictable signing fallbacks; keep release signing in a protected manual environment with a real maintainer-controlled key; use read-only `contents` permissions for build jobs; separate artifact creation from publication; and require explicit approval for a release job. Never publish a debug-signed APK as an official release.

### GMS/Firebase configuration

The public build should not require `google-services.json` or Firebase credentials. If a GMS/Firebase job is retained, it must be clearly separate, use an explicitly provisioned private configuration and prove that no credential is packaged into an artifact or log.

### CodeQL and configuration sync

The CodeQL workflow should analyze/build the FOSS path without secret environment variables. The player-config synchronization workflow currently has write access and commits downloaded content directly to `main`; review downloaded content, pin actions and prefer a reviewed pull request or a protected bot path before enabling it for a public repository.

### Actions and permissions

Review every third-party action at an immutable commit or an approved maintained version, set job-level least-privilege permissions, restrict triggers, and prevent untrusted pull-request code from accessing secrets. Caches must be disposable and must not contain credentials.

## Acceptance criteria before official publication

- `:app:compileUniversalFossDebugKotlin`, `:app:assembleUniversalFossDebug`, relevant tests and FOSS lint pass without private secrets.
- No API key, OAuth client secret, cookie, webhook or signing material is reachable in `BuildConfig`, APK resources, Gradle output or artifacts.
- Release signing uses a protected external keystore and an approval-gated job.
- Tag creation does not automatically create, overwrite or mutate a GitHub release without explicit maintainer approval.
- Artifact names, variant, commit and SHA-256 are recorded in manually reviewed release notes.
- CI logs and uploaded artifacts are scanned for secret markers and sensitive request data.
- The workflow change is reviewed together with the Windows checkout changes described in the coordination handoff.
