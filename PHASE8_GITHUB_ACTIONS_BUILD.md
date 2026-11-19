# Phase 8 — GitHub Actions APK Build

This project is prepared for building from GitHub Actions so the developer can use an Android phone without installing Android Studio or Gradle locally.

## Workflow
- Workflow file: `.github/workflows/android-apk.yml`
- Trigger: push to `main` or `master`, pull request to those branches, or manual `workflow_dispatch`.
- Environment: GitHub-hosted Ubuntu runner, Temurin JDK 17, Gradle 8.9, Android SDK Platform 35 / Build Tools 35.0.0.
- Output: debug APK `app/build/outputs/apk/debug/app-debug.apk` uploaded as artifact `NOVIA-HOME-debug-APK` for 30 days.
- Build reports uploaded (when present) for 7 days.

## Build from an Android phone
1. Upload/commit the project root contents into the GitHub repository. Ensure `.github/workflows/android-apk.yml` is included.
2. Open the repository's **Actions** tab in GitHub.
3. Select **NOVIA HOME Android APK** workflow. If no run has occurred, commit/push to `main` or `master` first.
4. Open the latest workflow run and wait until the job is green.
5. At the bottom of the run page, find **Artifacts** and tap `NOVIA-HOME-debug-APK` to download.
6. Extract the ZIP artifact and install the APK on Android. Android may ask permission to install apps from the browser/files app.

## Notes and limitations
- This is a debug APK and is intended for testing, not Play Store distribution.
- Workflow builds source only. It does not modify firmware or the ESP32.
- Actual compile and physical-device testing are pending the first GitHub Actions run.
- Keep relay testing initially on safe loads or relay indicator LEDs.
