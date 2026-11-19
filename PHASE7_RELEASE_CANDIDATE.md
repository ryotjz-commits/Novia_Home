# NOVIA HOME APK — Phase 7 Release Candidate (source)

## Included
- Phase 3 discovery/AP/offline shell, Phase 4 connection manager, Phase 5 WebView/recovery, Phase 6 local relay UI/API adapter.
- LAN discovery now probes `/api/device` first, then recognizes V4.4-compatible `/api/state` (four `channels` plus `wifi` and `mode`).
- Bounded subnet scan: only the local /24, maximum 254 hosts, avoiding unbounded task submission.
- AP probe uses V4.4 `/api/state` signature.
- Cloud fallback is the hosted dashboard only; it is **not** a cloud relay API.
- Relay status page polls actual `/api/state` and controls through `/api/set`.

## Important firmware facts from the uploaded V4.4 source
- `/api/state` GET and `/api/set` POST exist.
- `setupWebServer()` route wrappers call `requireLocalClient()`, which limits callers to AP/LAN subnet.
- `requireAuthenticatedClient()` exists, and README describes session authentication, but these two route wrappers do not call it in the inspected source. Thus API PIN/session enforcement is not demonstrated on relay routes. Treat LAN/AP as trusted-only and do not port-forward/expose the ESP32.
- `/api/device` was not found in this V4.4 source. Discovery fallback to `/api/state` is added in this APK candidate.
- Firmware source is V4.4, not the user's latest stable V5. Do not flash/modify V5 from this package.

## Android AP Direct limitation
Android generally requires user consent/system UI to join another Wi-Fi network. This candidate can probe `192.168.4.1` only if the phone is already connected/routed to the ESP32 AP. It does not silently change Wi-Fi networks.

## Build
Open the `novia_apk_phase1` directory in Android Studio (recent version), allow Gradle sync, then Build > Build APK(s). Current config: compileSdk 35, minSdk 23, targetSdk default Android Gradle Plugin behavior (not explicitly set). No Gradle wrapper is included.

## Release readiness
This is a source release candidate, **not a verified APK build**. Android SDK/Gradle build and device tests remain required. Run the checklist in `RELEASE_TEST_CHECKLIST.md` before using it to control mains-powered loads.
