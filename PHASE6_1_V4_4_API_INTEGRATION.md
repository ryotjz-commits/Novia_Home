# NOVIA HOME APK Phase 6.1 — V4.4 API Integration Patch

## Firmware API extracted from uploaded NOVIA_HOME_V4_4.zip
- GET `/api/state` returns relay `channels` as an array of 0/1 plus `wifi`, `blynk`, `ip`, `mode`, `active`, timer and schedule metadata.
- POST `/api/set` accepts form fields `ch` (1..4) and `state` (0/1), then calls `applyChannel(ch-1, state==1, true)`.
- Local/AP subnet restrictions are checked via `requireLocalClient()`.

## Integration in this patch
- Configured RelayApiAdapter for GET `/api/state` and POST `/api/set`.
- Added local relay-control HTML page with state polling and ON/OFF commands.
- MainActivity adds Dashboard and Kontrol Relay Lokal actions. Relay controls enable only after a local connection callback supplies the host.
- API adapter uses the current local base URL and does not hard-code router IP.

## Important source discrepancy/security caveat
`NOVIA_Auth.h` contains PIN/session functions, and README says all API endpoints require PIN/session. However, the uploaded `setupWebServer()` route registrations for `/api/state` and `/api/set` call `requireLocalClient()` and do not call `requireAuthenticatedClient()`. Auth endpoints are not registered in the route excerpt/source scan. Therefore this APK patch must be used only on a trusted AP/LAN; don't expose ESP32 web server to the public internet. Confirm/repair auth route enforcement in firmware separately before broader deployment.

## Compatibility limitation
This API was verified against the uploaded V4.4 source only. It is not proof that the user's tested V5 firmware uses the same routes/format. Discovery still requires `/api/device` and UDP responder described by prior APK phases; the uploaded V4.4 source does not contain those discovery endpoints.

## Verification
ZIP source files packaged and inspected. No Android SDK/Gradle compile, device install, or live ESP32 HTTP test performed. Do not treat as a tested APK.
