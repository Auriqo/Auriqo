# F-Droid preparation

`metadata/com.auriqo.music.yml` is a deliberately disabled preparation record for package ID `com.auriqo.music`. It is not F-Droid submission metadata and must not be copied into an F-Droid data repository as-is.

Before publication, a maintainer must:

1. make a reviewable source location available and add its verified URL to the final recipe;
2. replace `PENDING_RELEASE_REVISION` with an immutable, reviewed 1.0.0 release revision;
3. reproduce the F-Droid-targeted `:app:assembleUniversalFossRelease` in the F-Droid build environment; the CI release flow is universal-only;
4. review all dependencies, non-free-service implications, permissions, anti-features, and reproducibility;
5. verify the package ID/version/output path against the actual generated artifact; and
6. provide required project contact, licensing, and listing details.

The existing Gradle configuration comments out the Foojay resolver specifically for F-Droid compatibility, but that comment is not evidence that the complete F-Droid build has been reproduced.
