# Play listing preparation

The English listing copy lives in `fastlane/metadata/android/en-US/`. It is versioned preparation material only; it does not claim submission, approval, or availability on Google Play.

## Current references

- Title: `fastlane/metadata/android/en-US/title.txt`
- Short description: `fastlane/metadata/android/en-US/short_description.txt`
- Full description: `fastlane/metadata/android/en-US/full_description.txt`
- Version-code change notes: `fastlane/metadata/android/en-US/changelogs/527.txt`
- Candidate screenshot source files for review: `Screenshots/sc_1.png` through `Screenshots/sc_6.png`
- Application ID: `com.auriqo.music`
- Prepared version: `1.0.0` (version code `527`)

## Submission prerequisites

- [ ] Select the intended Play distribution variant and verify its final signed artifact, application ID, version name/code, and signing identity. The current CI release flow prepares one FOSS universal APK; it does not publish a GMS artifact.
- [ ] Replace/change the version-code changelog with reviewed, user-facing release notes.
- [ ] Review every screenshot for current UI, locale, account data, media-library data, and third-party branding; export store-specific images only after review.
- [ ] Complete the Play Data safety form and permission declarations from the final artifact and actual runtime behavior.
- [ ] Publish a legally reviewed public privacy-policy URL and a user-facing support contact before submission.
- [ ] Confirm Firebase Analytics/Crashlytics initialization and data collection for the submitted GMS configuration.
- [ ] Obtain the account owner/maintainer's approval before using any publishing API or console.
