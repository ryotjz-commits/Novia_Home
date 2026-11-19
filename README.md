# NOVIA HOME Android APK — Phase 7 RC Source

Android WebView companion app source for NOVIA HOME. This package consolidates the previous Phase 3–6.1 work and adds V4.4 API compatibility discovery and release test notes.

## Project
- `app/src/main/java/com/novia/home/MainActivity.java` — app shell, WebView and network lifecycle
- `network/DeviceDiscovery.java` — last-IP probe, UDP discovery, bounded local /24 scan
- `network/ConnectionManager.java` — LAN/AP/cloud/offline flow
- `control/RelayApiAdapter.java` — GET `/api/state`, POST `/api/set`
- `web/Phase6Bridge.java` — JavaScript bridge
- `assets/local_relay_control.html` — local 4-relay dashboard
- `assets/phase5_offline.html` — recovery shell

## V4.4 compatibility
V4.4 source in the upload does not include `/api/device`; the app therefore probes `/api/state` and checks the four-channel signature. The relay API uses HTTP on the local AP/LAN. The source's route wrappers only enforce local-subnet access; API PIN/session enforcement is not demonstrated for these routes. Do not expose this firmware to the internet.

## AP connection
The APK cannot silently force a Wi-Fi network switch on modern Android. User may need to join NOVIA AP in Android Wi-Fi settings and select “stay connected without internet.” AP probe works only once the phone is actually connected/routed to `192.168.4.1`.

## Cloud
Cloud fallback opens the hosted dashboard URL. This is not a cloud relay-control integration; local relay control is enabled only after local ESP32 detection.

## Build
Open this folder (`novia_apk_phase1`) in Android Studio and sync/build. See `PHASE7_RELEASE_CANDIDATE.md`, `PHASE8_BUILD_PREFLIGHT.md`, and `RELEASE_TEST_CHECKLIST.md`. Android SDK build has not been verified in this environment; no APK is included.


## Build APK via GitHub Actions (Android phone workflow)

See `PHASE8_GITHUB_ACTIONS_BUILD.md`. The `.github/workflows/android-apk.yml` workflow builds a debug APK on GitHub runners and uploads it as an artifact. No Android Studio or local Gradle installation is required on the phone.
