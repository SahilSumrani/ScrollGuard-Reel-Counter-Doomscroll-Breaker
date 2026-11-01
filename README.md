# ScrollGuard Reel Counter & Doomscroll Breaker

ScrollGuard is an Android app that helps you notice short-form video scrolling and set daily focus boundaries. It is built with Kotlin, Jetpack Compose, Android Accessibility Services, overlay windows, and Room.

> The project is currently an early Android app. Scroll detection depends on accessibility events exposed by each supported app and can vary across app versions and devices.

## Features

- Daily dashboard with total and per-app reel counts, hourly activity, and focus status.
- Scroll monitoring for Instagram, YouTube, Facebook, Snapchat, and TikTok.
- Optional floating counter while a tracked app is open.
- Configurable daily and per-app limits, with a focus boundary screen when a limit is reached.
- A mindful pause screen, social arena UI, and home-screen widget.
- Local Room database storage for daily and hourly statistics.

## Requirements

- Android Studio with Android SDK Platform 34 installed.
- JDK 17.
- Android device or emulator running Android 8.0 (API 26) or later.
- Internet access on first Gradle sync to download dependencies.

The project uses the Gradle wrapper, so you do not need to install Gradle separately.

## Open and run

1. Clone this repository and open its root folder in Android Studio.
2. Allow Gradle sync to finish.
3. Select the `app` run configuration and a connected Android device or emulator.
4. Press **Run**.

To build a debug APK from a terminal:

```bash
# macOS / Linux
./gradlew :app:assembleDebug

# Windows PowerShell
.\gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To install it on a USB-connected Android device with USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Required phone setup

Counting and floating overlays will not work until their Android permissions are enabled:

1. Open ScrollGuard and choose **Reel Detection Service**. In Android Accessibility settings, enable the ScrollGuard service and accept the system confirmation.
2. Return to the app and enable **Display over other apps** for ScrollGuard to show the floating counter and focus screen.
3. On OPPO/ColorOS and some other Android builds, allow background activity or auto-launch for ScrollGuard and set its battery use to **Unrestricted** (wording varies by OS version).
4. Open a supported app and scroll its short-form feed. The dashboard and overlay update when the service receives a supported scroll event.

Accessibility access is used to identify the foreground package and receive scroll events. The service configuration restricts events to the supported package names listed below. No analytics or network backend is configured in this project; usage statistics are stored locally in the app database.

## Supported apps

| App | Android package |
| --- | --- |
| Instagram | `com.instagram.android` |
| YouTube | `com.google.android.youtube` |
| Facebook | `com.facebook.katana` |
| Snapchat | `com.snapchat.android` |
| TikTok | `com.zhiliaoapp.musically` |

Detection is based on accessibility scroll events; an app update or device-specific UI may change whether those events are available. The counter is not a guaranteed measurement of every individual video viewed.

## Troubleshooting

- **Count stays at zero:** confirm the Accessibility service is enabled in Android settings, then test by scrolling inside a supported app. Some app screens do not expose scroll events in a format the current detector recognizes.
- **Floating pill or boundary screen does not appear:** grant Display over other apps permission.
- **Stops after leaving the app or locking the screen:** allow background activity/auto-start and remove ScrollGuard from battery optimization if your device provides those controls.
- **Build fails during sync:** confirm JDK 17 and Android SDK Platform 34 are installed, then sync again.

## Tech stack

- Kotlin and Android Gradle Plugin
- Jetpack Compose and Material 3
- Android Accessibility Service and `WindowManager` overlays
- Room persistence with Kotlin coroutines
- Gradle wrapper

## Repository layout

```text
app/src/main/java/com/example/braindrain/  App source
app/src/main/res/                         Android resources and service config
gradle/                                   Gradle wrapper and version catalog
```

## License

No license file is currently included. All rights remain with the repository owner unless a license is added.
