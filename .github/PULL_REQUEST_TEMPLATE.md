## Summary

Describe the problem and the change.

## Scope

- [ ] Bug fix
- [ ] Feature
- [ ] Documentation or metadata
- [ ] Build, CI, signing, or release process
- [ ] Privacy, security, permission, or third-party-service behavior

## Validation

List the exact commands run and their result. For ordinary Android changes, include the affected variant(s). The baseline CI path is:

```text
:app:assembleUniversalFossDebug
:app:testUniversalFossDebugUnitTest
:app:lintUniversalFossDebug
```

## Review checklist

- [ ] I reviewed the diff and kept the change focused.
- [ ] I updated user-facing documentation, privacy notes, or store metadata where needed.
- [ ] I did not include `local.properties`, `google-services.json`, a keystore, token, APK/AAB, or other credential/generated artifact.
- [ ] I identified any FOSS/GMS variant impact.
- [ ] I included safe screenshots or explained why they are not applicable.

## Related work

Link related private issues or describe the context available to reviewers.
