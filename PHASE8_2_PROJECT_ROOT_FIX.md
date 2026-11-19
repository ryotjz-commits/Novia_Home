# NOVIA HOME APK — Phase 8.2 Project Root Fix

## Root cause from Phase 8.1 logs
Android SDK installation and package verification **passed**. Build failed before Gradle compile because the workflow hard-coded `PROJECT_DIR="novia_apk_phase1"`, but repository checkout has `build.gradle`, `settings.gradle`, and `app/` directly at repository root.

## Fix
Replace `.github/workflows/android-apk.yml` with the included workflow. It detects the Android project root at either:
- repository root (`build.gradle` and `app/`), or
- `novia_apk_phase1/` (legacy nested layout).
It then builds from the detected folder and uploads the APK from either matching output location.

## Apply from Android phone
1. Open repository `Novia_Home` → `.github/workflows` → `android-apk.yml`.
2. Edit the file, replace its full contents with the included workflow file, and commit.
3. Open Actions and inspect the latest run.

No ESP32 firmware or app source code is changed in this fix.
