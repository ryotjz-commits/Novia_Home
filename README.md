# NOVIA HOME Android (WebView wrapper)

A lightweight Android launcher for the existing NOVIA HOME V4.4 ESP32 web dashboard. The ESP32 firmware and its dashboard APIs remain on the ESP32; the Android app only hosts the existing web UI in a fullscreen WebView.

## Current behavior
- Opens `http://192.168.4.1/` by default (connect the phone to the NOVIA-HOME AP first).
- Can save a private LAN IPv4 address for the ESP32 STA interface when the phone is connected to the same home router.
- Keeps the chosen address between launches.
- Enables JavaScript and DOM storage required by the current dashboard.
- Restricts top-level WebView navigation to the configured ESP32 IP.
- Binds app traffic to the available Wi-Fi network when Android exposes one, useful when the NOVIA AP has no internet access.
- No relay/timer/schedule logic is duplicated in Android; the ESP32 remains authoritative.

## Build APK
1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open this folder (`NOVIA_HOME_Android`) in Android Studio.
3. Allow Gradle sync to download Android Gradle Plugin 8.7.3 and dependencies.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

This environment did not have Android SDK / Gradle installed, so an APK binary could not be compiled here. The project source is ready for an Android Studio build.

## Important connection notes
- AP mode: connect Android to SSID `NOVIA-HOME` (or the SSID configured in the ESP32) and open the app. Default address is `192.168.4.1`.
- Router/STA mode: keep the phone on the same router as the ESP32, tap **IP ESP32**, and enter the ESP32 STA IP shown by the firmware Serial Monitor.
- AP + STA means both ESP32 interfaces can remain active; it does not mean a phone can use both Wi-Fi networks simultaneously.
- Android may warn that the NOVIA-HOME Wi-Fi has no internet. Choose to stay connected to that network.

## Build directly from an Android phone (no PC)
This project includes a GitHub Actions workflow so GitHub's cloud runner can compile the APK.

1. On the phone, open github.com in Chrome and sign in (create a GitHub account if needed).
2. Create a **private** repository, for example `novia-home-android`.
3. Extract this ZIP on the phone using a file manager that supports ZIP extraction.
4. Upload the **contents** of the `NOVIA_HOME_Android` folder to the repository root. Make sure the `.github/workflows/build-apk.yml` file is included. GitHub's web upload may hide dot-folders in some file pickers; if so, use GitHub's web editor to create `.github/workflows/build-apk.yml` and paste the workflow from this project.
5. Open the repository's **Actions** tab. Select **Build NOVIA HOME APK**, tap **Run workflow**, then run it.
6. Wait until the workflow finishes with a green check. Open that run, scroll to **Artifacts**, and download `NOVIA-HOME-debug-apk`.
7. Extract the downloaded artifact ZIP. Install `app-debug.apk`. Android may ask permission to install apps from Chrome or your file manager; allow it only for the app you used to download the APK.

The build runs on GitHub's hosted computer; Android 13 on the phone is only used to upload the source and download/install the APK. Keep the repository private because it contains your project source.
