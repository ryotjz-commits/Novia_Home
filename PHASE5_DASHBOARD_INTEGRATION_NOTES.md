# NOVIA HOME APK Phase 5 — Dashboard Integration & Offline Recovery UI

## Implemented in this source
- Bundled offline recovery UI at `app/src/main/assets/phase5_offline.html`.
- Limited JavaScript bridge: `scan()`, `openLocal()`, `openOnline()`.
- MainActivity routes main-frame load failures to the bundled recovery page.
- Recovery page provides retry scan, open local dashboard (last detected local URL or AP default), and online dashboard options.
- Recovery UI clearly states that relay controls are not yet available in this page.

## Not yet implemented / requires source firmware API verification
- No relay toggle buttons or assumed command routes are added.
- No relay status is fabricated or cached.
- The tested V5 INO must be reviewed to identify exact state and command endpoints, request formats, authentication, and synchronization behavior before local control is implemented.
- AP auto Wi-Fi switching remains subject to Android OS permission and user approval.

## Verification
- Source/archive structure validation performed.
- Android compile/build and runtime test are not claimed; Android SDK/Gradle build environment is not available here.

## Suggested test
1. Install/build APK and open with internet: verify dashboard online.
2. Disconnect internet with ESP32 absent: verify bundled recovery page.
3. Tap retry scan: verify discovery begins and status banner updates.
4. Connect phone to ESP32 AP, tap retry, then open local: verify local firmware dashboard.
5. Restore router connection and test automatic discovery recovery.
