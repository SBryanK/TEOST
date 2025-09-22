# CI/CD Notes (Commented Reference)

This file is a reference only. Steps are intentionally not wired into the active workflow until you're ready.

## GitHub Actions (build + optional Firebase App Distribution)

- Pre-req:
  - Add repository secret `FIREBASE_TOKEN` (`firebase login:ci` or Service Account with application-default login).
  - Ensure `google-services.json` exists in `app/`.
  - Keep `com.google.gms.google-services` plugin enabled in `app/build.gradle.kts`.

### Sample workflow snippet (commented)

```yaml
# .github/workflows/android-ci.yml
# jobs:
#   build:
#     runs-on: ubuntu-latest
#     steps:
#       - uses: actions/checkout@v4
#       - uses: actions/setup-java@v4
#         with:
#           distribution: temurin
#           java-version: '17'
#       - name: Gradle cache
#         uses: gradle/actions/setup-gradle@v3
#       - name: Build Debug
#         run: ./gradlew :app:assembleDebug
#       - name: Build Release (no signing)
#         run: ./gradlew :app:assembleRelease
#       - name: Upload APKs
#         uses: actions/upload-artifact@v4
#         with:
#           name: app-artifacts
#           path: |
#             app/build/outputs/apk/debug/*.apk
#             app/build/outputs/apk/release/*.apk
#
#       # Optional: Firebase App Distribution
#       - name: Upload to Firebase App Distribution (Release)
#         if: ${{ github.event_name == 'workflow_dispatch' }}
#         env:
#           FIREBASE_TOKEN: ${{ secrets.FIREBASE_TOKEN }}
#         run: |
#           ./gradlew appDistributionUploadRelease \
#             -PfirebaseAppDistribution.releaseNotes="Automated CI upload" \
#             -PfirebaseAppDistribution.groups="internal-testers"
```

## Local release to Firebase App Distribution

- One-time:
  - `./gradlew :app:assembleRelease`
  - `./gradlew appDistributionUploadRelease -PfirebaseAppDistribution.groups=internal-testers -PfirebaseAppDistribution.releaseNotes="Manual upload"`

## Notes

- Keep R8/shrinkResources disabled until you finish functional QA. When enabling, add minimal keep rules for Hilt/Room/WorkManager/Firebase.
- Use `workflow_dispatch` for manual promotion.
- Consider adding a simple script to verify i18n key parity between `values/` and `values-b+zh+Hans/`.
