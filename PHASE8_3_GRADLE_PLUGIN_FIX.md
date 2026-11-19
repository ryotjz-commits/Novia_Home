# NOVIA HOME APK — Phase 8.3 Gradle Plugin Fix

## Diagnosis from the uploaded GitHub Actions log

The Phase 8.2 run now passes Android SDK installation and project-root detection.
The failure occurs during Gradle configuration:

`Plugin [id: 'com.android.application'] was not found ... plugin dependency must include a version number`

The repository's root `build.gradle` was applying `com.android.application` without a version, while `settings.gradle` includes the `:app` module. The Android source is under `app/src/main`, so the root build should act as a plugin-version declaration and the `:app` module should be the application module.

## Apply this fix

Replace the repository root `build.gradle` with the supplied `build.gradle`.

This declares Android Gradle Plugin `8.7.3` with `apply false`, compatible with the current Gradle 8.9 workflow, and lets `app/build.gradle` apply the plugin without repeating its version.

Do not modify firmware or application Java/HTML files for this error.

After committing, rerun Actions. This fixes the currently observed Gradle plugin resolution error; the next build may reveal further compilation/dependency issues that must be diagnosed from the new log.
