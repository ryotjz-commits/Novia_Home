# NOVIA HOME APK — Phase 8.5 (Gradle module correction)

## Root cause from Phase 8.4 run

The log shows the Gradle normalization step set the root `build.gradle` to the Android plugin with `apply false`, **but also wrote the same `apply false` plugin-only declaration into `app/build.gradle`**. That means the `:app` module never applied the Android application plugin and no `assembleDebug` task was registered. The failure:

`Task 'assembleDebug' not found in root project 'NOVIA_HOME' and its subprojects.`

## Apply from Android / GitHub mobile

1. Download and extract this ZIP.
2. In the repository, open `.github/workflows/android-apk.yml`.
3. Replace its complete content with the workflow file included in this ZIP.
4. Commit to the main branch and open Actions.

## What Phase 8.5 fixes

- Root `build.gradle`: declares AGP 8.7.3 with `apply false` only.
- `app/build.gradle`: applies `com.android.application` (no `apply false`) and defines the Android application module configuration.
- Calls `:app:tasks --all` and checks that Android build tasks are registered before compile.
- Builds explicitly using `:app:assembleDebug`.
- Checks that `app/src/main/AndroidManifest.xml` exists; if absent, emits a directory listing and stops with a clear message.
- Keeps Java 17, Gradle 8.9, SDK API 35 and Build Tools 35.0.0.

## Note

This workflow normalization is CI-only. It does not edit ESP32 firmware. The current Android source layout is not independently validated by a successful APK compile yet. If the manifest check fails, the next run's file listing will tell us where the source tree actually lives so we can correct paths without guessing.
