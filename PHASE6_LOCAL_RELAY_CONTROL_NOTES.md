# NOVIA HOME APK — Phase 6 Local Relay Control (API Verification Pending)

## Source work
- Added a local relay-control dashboard asset with 4 relay tiles.
- Added `RelayApiAdapter` as a separated API adapter layer.
- Added `Phase6Bridge` and registered `NoviaRelay` bridge in MainActivity.
- UI explicitly shows relay status as unknown and disables relay buttons.
- Adapter deliberately rejects command requests until the tested V5 firmware API is confirmed.

## Safety / integrity
No endpoint, command route, request method, JSON schema, authentication rule, or relay state was guessed. No relay command is sent by this phase. The V5 firmware baseline has not been modified.

## Needed to activate control
Upload the exact tested NOVIA HOME V5 `.ino` (or source bundle). Review its web-server routes, relay status representation, command parsing, and sync behavior (physical buttons, dashboard, Blynk, automation). Then configure and implement the verified adapter, connect UI, and test on hardware.

## Verification
- ZIP structure validated after packaging.
- HTML asset and manifest/source files included.
- Android SDK/Gradle build and hardware API tests are not verified in this environment.
