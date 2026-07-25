# IP Cam Viewer

An Android app (Kotlin) for viewing IP camera live feeds. Add cameras manually
by RTSP address, or auto-scan your Wi-Fi network for ONVIF cameras and add
them with one tap.

## Features

- **Camera grid dashboard** — dark-themed grid of saved cameras, tap to watch, long-press to remove (same layout pattern as Reolink / Tinycam / Hik-Connect).
- **Manual add** — enter host/IP, RTSP port, stream path, and optional username/password. Works with any standard RTSP camera.
- **Auto network scan** — sends a WS-Discovery UDP multicast probe to find ONVIF cameras on the local Wi-Fi network, lists them, and lets you tap "Add" (prompting for credentials if the camera needs them). The app then calls the camera's ONVIF Media service (GetProfiles → GetStreamUri) to resolve the real RTSP stream URL automatically.
- **Live view** — fullscreen RTSP playback via libVLC (handles H.264/H.265, TCP or UDP transport), with a retry button if the stream drops.

## Project structure

```
app/src/main/java/com/example/ipcamviewer/
  data/         Room database (CameraEntity, DAO, repository)
  discovery/    WS-Discovery (UDP multicast) + ONVIF SOAP client
  player/       libVLC wrapper for RTSP playback
  ui/           Fragments/Activities/ViewModels (camera list, add camera, live view)
```

## How to build and install

1. Install **Android Studio** (Hedgehog 2023.1.1 or newer — it bundles the JDK 17 this project needs).
2. Open this folder (`IPCamViewer/`) in Android Studio: **File → Open**.
3. Let Gradle sync finish (Android Studio will download the Gradle wrapper, SDK platform 34, and dependencies automatically — needs an internet connection the first time).
4. Connect an Android phone (USB debugging on) or start an emulator, then click **Run ▶**.
5. To install without a cable: **Build → Generate Signed Bundle / APK**, then transfer the resulting `.apk` to your phone and open it (allow "install unknown apps" if prompted).

No native/NDK setup is required — the RTSP player library (`libvlc-all`) ships prebuilt native code for all device architectures.

## Using the app

- **Add a camera manually**: tap **+** → **Manual** tab → fill in the camera's IP, RTSP port (default 554), and stream path (check your camera's manual — common examples: `/stream1`, `/live/ch0`, `/cam/realmonitor?channel=1&subtype=0`).
- **Auto-discover ONVIF cameras**: tap **+** → **Discover** tab. The app scans automatically; found cameras appear in a list. Tap **Add**, enter the camera's ONVIF username/password if it has one, and the app resolves the live stream for you.
- Your phone and the cameras must be on the **same Wi-Fi network** (or same subnet) for both discovery and playback to work — this app does not do internet/cloud relay, only local network streaming.

## Notes and limitations

- **ONVIF discovery** relies on UDP multicast (239.255.255.250:3702), which some routers/APs block by default (especially "AP/client isolation" or guest networks). If no devices are found, try a different Wi-Fi network or check your router's multicast settings.
- **ONVIF SOAP parsing** in `OnvifClient.kt` is intentionally lightweight (regex-based) rather than a full XML/WSDL client, since responses vary a lot between camera vendors. It covers the common `GetCapabilities → GetProfiles → GetStreamUri` flow that virtually all ONVIF Profile S cameras support. If a specific camera's response format isn't picked up, it can be extended there.
- The **manual add** path is the reliable fallback for any RTSP camera (ONVIF or not) — just enter its known RTSP URL parts directly.
- Cleartext (non-HTTPS/non-TLS) traffic is allowed, since IP cameras almost always serve plain RTSP/HTTP on the local network.

## Tech stack

Kotlin, Jetpack Navigation, ViewModel + LiveData, Room, OkHttp (ONVIF SOAP calls), libVLC (RTSP playback), Material 3 (dark theme).

## Building an APK in the cloud (GitHub Actions, no Android Studio needed)

This project includes `.github/workflows/build-apk.yml`, which builds a debug
APK on GitHub's servers every time you push. To use it:

1. Go to [github.com/new](https://github.com/new) and create a new repository (any name, e.g. `ip-cam-viewer`). Public or private both work.
2. On the new repo's page, click **uploading an existing file** (or **Add file → Upload files**).
3. Unzip the project if you haven't already, then drag the **entire `IPCamViewer` folder** (not just individual files) onto the upload page — GitHub recreates the folder structure, including the `.github` folder that holds the build workflow.
4. Commit the upload (commit directly to `main`).
5. Go to the repo's **Actions** tab. A "Build APK" run should already be in progress (or click **Run workflow** if it didn't start automatically).
6. When the run finishes (green check, a few minutes), open it and scroll to **Artifacts** — download `IPCamViewer-debug-apk`. It's a zip containing `app-debug.apk`.
7. Transfer `app-debug.apk` to your Android phone (email, cloud drive, USB) and tap it to install (allow "install unknown apps" for that source if prompted).

This produces a debug-signed APK — fine for installing on your own device. If you later want to publish it (e.g. to the Play Store), it needs to be signed with a release key instead.

Note: EAS Build (Expo's cloud build service) does not apply here since this is a native Kotlin/Gradle Android project, not an Expo/React Native project — GitHub Actions is the cloud-build equivalent for this kind of project.
