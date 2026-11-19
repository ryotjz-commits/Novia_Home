# NOVIA HOME APK Phase 4 — Unified Connection Manager

Implemented in this source package:
- ConnectionManager orchestrates existing LAN DeviceDiscovery.
- After LAN discovery misses, it probes the ESP32 AP default endpoint `192.168.4.1:80/api/device`.
- Valid identity response routes WebView to local dashboard.
- If AP probe fails, it selects online dashboard fallback when a network transport is available, otherwise offline recovery shell.
- MainActivity now delegates discovery/fallback orchestration to ConnectionManager.

Important limitations / test requirements:
- AP fallback does not cause Android to switch Wi-Fi. The user may need to join the ESP32 AP in Android Wi-Fi settings.
- Cloud fallback is the existing remote dashboard URL, not a cloud relay for relay commands.
- Firmware must implement `GET /api/device` with JSON identity and serve the dashboard at `/`.
- This package is source-only; compile/build was not verified here (Android SDK/Gradle unavailable).
- V5 firmware has not been edited.

Test checklist: build in Android Studio; LAN discovery against firmware responder; router DHCP IP change; phone on ESP32 AP; phone on internet with ESP32 absent; no internet; tap banner to retry; network change recovery.
