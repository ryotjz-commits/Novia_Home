# Phase 8 — APK Build Preflight

## Build environment required
- Android Studio with Android SDK Platform 35 and compatible Gradle runtime.
- Android Gradle Plugin 8.7.3 is declared at root. Gradle wrapper is not included in this ZIP.

## Build debug APK
1. Extract ZIP to a writable folder.
2. Android Studio > Open > select `novia_apk_phase1` (project root containing `settings.gradle`).
3. Install SDK Platform 35 if prompted and allow Gradle sync.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. If successful, debug APK appears under `app/build/outputs/apk/debug/app-debug.apk`.
6. Install on phone, test first with relay LEDs/non-critical loads.

## Signed release APK
Use Build > Generate Signed Bundle / APK > APK. Create and securely back up a signing key. Do not send keystore or passwords in chat. Build release only after device test checklist passes.

## Current blocker
Android SDK/Gradle is not installed in this environment, so APK compile/install cannot be verified here. This is a source release candidate, not a compiled APK.
