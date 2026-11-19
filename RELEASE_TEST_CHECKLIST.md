# NOVIA HOME APK RC — Device Test Checklist

## Build / install
- [ ] Gradle sync succeeds in Android Studio
- [ ] Debug APK builds without errors
- [ ] Install on Android phone; app launches and portrait layout fits

## Discovery
- [ ] Same router LAN: cached IP probe succeeds
- [ ] UDP discovery with firmware responder (requires firmware addon) succeeds
- [ ] V4.4-compatible fallback probe to `/api/state` succeeds
- [ ] IP changes: bounded /24 scan locates device (verify test; not guaranteed across subnet/VLAN)
- [ ] No device: cloud page opens only when internet is validated
- [ ] No internet/device: offline recovery page appears

## Relay API (V4.4, trusted LAN only)
- [ ] Read status shows 4 channels matching physical relay states
- [ ] Relay 1 ON/OFF; verify actual relay and refresh response
- [ ] Repeat for Relay 2–4
- [ ] Press physical button and verify app status updates within polling interval
- [ ] Change relay in dashboard/Blynk and verify app reflects state
- [ ] Disconnect local network: controls disabled/error is clear
- [ ] Verify no relay command is sent on startup or during refresh

## AP Direct
- [ ] Connect phone manually to ESP32 AP, choose “stay connected without internet” if Android prompts
- [ ] Open APK; AP probe/control works while phone remains on AP
- [ ] Test leaving AP and reconnecting; note Android version behavior

## Safety / security
- [ ] Do not port-forward ESP32 HTTP to internet
- [ ] Test with non-critical load or relay LEDs first
- [ ] Confirm relay active level and safe boot behavior on hardware
- [ ] Review API PIN/session enforcement in firmware before shared/untrusted LAN use

## Final release
- [ ] Confirm firmware/API version compatibility (V5 must be checked separately)
- [ ] Build signed release APK with user-managed signing key
- [ ] Record version, checksum, and test results
