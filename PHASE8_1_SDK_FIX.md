# NOVIA HOME APK — Phase 8.1 SDK Setup Fix

## Root cause confirmed from Actions logs
The previous workflow invoked `android-actions/setup-android@v3`, which ran `sdkmanager tools`. The modern SDK repository no longer provides that legacy `tools` package; setup failed before Gradle compilation.

## Fix
Replace the repository file `.github/workflows/android-apk.yml` with the file in this patch. It removes the legacy setup action/package request, installs only current SDK packages (`platform-tools`, `platforms;android-35`, `build-tools;35.0.0`), then runs `assembleDebug`.

## Apply from Android phone
1. Open repository `ryotjz-commits/Novia_Home` in GitHub.
2. Navigate to `.github/workflows/android-apk.yml`.
3. Edit the file (pencil icon), select all, and paste the replacement content from this package's workflow file.
4. Commit the change to the default branch.
5. Open **Actions** and select **NOVIA HOME Android APK**. The push should trigger the workflow; use **Run workflow** if needed.
6. If green, download artifact `NOVIA-HOME-debug-APK`. If it fails, download `NOVIA-HOME-build-reports` and send the ZIP/log.

## Important
This patch fixes the Android SDK setup failure seen in the provided logs. It does not yet prove that source compilation or APK installation succeeds. The workflow assumes project root folder `novia_apk_phase1`, matching the Phase 7/8 project layout. If the repository layout differs, the workflow will stop with a clear directory check.
