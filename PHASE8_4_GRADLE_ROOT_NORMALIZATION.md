# NOVIA HOME APK — Phase 8.4 Gradle Root Normalization

The previous build log still showed the root plugin error:

`Plugin [id: 'com.android.application'] ... plugin dependency must include a version number`

The Phase 8.3 edit apparently did not make it into the committed root `build.gradle` used by Actions. This workflow now prints the existing root files and then normalizes `settings.gradle` and root `build.gradle` in the runner **before the build**. It sets plugin repositories (Google, Maven Central, Gradle Plugin Portal), dependency repositories, `:app` inclusion, and AGP 8.7.3.

## Install from Android phone

Replace the repository file `.github/workflows/android-apk.yml` with the workflow in this ZIP and commit. Do not edit firmware or app source in this step.

The root Gradle normalization is CI-only and does not modify the Git repository contents. It will expose the next genuine app/module error if any. If the build succeeds, the APK artifact will be attached to the run.
