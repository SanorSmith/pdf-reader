# PDF Reader

A lightweight PDF reader for Android phones (including Samsung Galaxy devices). Native Kotlin + Jetpack Compose, using Android's built-in `PdfRenderer` - no third-party PDF libraries.

## Features
- Open any PDF from device storage, Samsung My Files, Google Drive, etc. (system file picker)
- Open PDFs directly from other apps ("Open with PDF Reader")
- Smooth vertical scrolling, page counter
- Pinch-to-zoom and double-tap zoom
- Remembers recent files and the last page you read
- Light/dark theme follows the system

## Build
Requires Android Studio (or JDK 17 + Android SDK 35).

    ./gradlew assembleDebug

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Min Android version: 8.0 (API 26). Target: Android 15 (API 35).
