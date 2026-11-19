# NOVIA HOME APK Phase 9.1 — WebView API Diagnostics

Cache-clearing did not fix the online dashboard API status. This build is **diagnostic, not yet a functional API fix**.

## What is added
- Captures WebView JavaScript console warning/error messages and displays them in a diagnostic strip above the webview.
- Captures page start/finish, WebView network error, HTTP status error, and SSL error events.
- SSL errors are canceled; the app does not bypass certificate validation.
- Keeps the dashboard URL, discovery, LAN/AP logic, relay bridge, and existing WebView settings unchanged.

## Apply
Replace only:
`app/src/main/java/com/novia/home/MainActivity.java`
with the Java file included in this package, then commit to GitHub and build a fresh APK via the existing Actions workflow. Install the newly built APK (uninstall the previous debug APK first if Android refuses to replace it due signing/version mismatch).

## Test
1. Open NOVIA HOME APK and choose Dashboard (online) if needed.
2. Wait 10–20 seconds for API polling.
3. Screenshot the diagnostic strip if it shows errors.
4. If it stays hidden, screenshot the API status and the app's top connection banner.

Do not modify ESP32 firmware or the hosted dashboard while diagnosing.
