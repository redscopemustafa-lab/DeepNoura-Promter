# DeepNoura Prompter

Tablet‑friendly teleprompter/karaoke app built with **Kotlin**, **Jetpack Compose**, and **Material3**.

## Requirements
* Android Studio Hedgehog or newer (tested with Gradle 8.7, AGP 8.5.1)
* Android SDK 26+

## Build & Run
1. Open the project in Android Studio.
2. Let Gradle sync (Kotlin DSL build scripts are provided).
3. Run on a device or emulator (tablet recommended, landscape friendly).
4. Build a debug APK via **Build > Build APK(s)**.

## Features
* Builder screen for adding, editing, deleting, and reordering script segments.
* Automatic JSON persistence with import/export (SAF + FileProvider sharing).
* Player screen with Prompter and Karaoke modes, adjustable speed, font size, and line spacing.
* Mirror flip and dark mode toggles; large controls optimized for tablets.
