# Kotoba

![License](https://img.shields.io/badge/license-GPL--3.0--only-blue)
![Min SDK](https://img.shields.io/badge/minSdk-31-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF)
![Material 3](https://img.shields.io/badge/Material%20Design-3-6750A4)

On-screen keyboard (input method) for Android.

## Screenshots

<p>
  <img src="screenshots/setup-enable.png" width="190" alt="setup-enable">
  <img src="screenshots/setup-select.png" width="190" alt="setup-select">
  <img src="screenshots/setup-ready.png" width="190" alt="setup-ready">
  <img src="screenshots/settings.png" width="190" alt="settings">
</p>
<p>
  <img src="screenshots/keyboard-letters.png" width="190" alt="keyboard-letters">
  <img src="screenshots/keyboard-symbols.png" width="190" alt="keyboard-symbols">
  <img src="screenshots/keyboard-emoji.png" width="190" alt="keyboard-emoji">
  <img src="screenshots/keyboard-tools.png" width="190" alt="keyboard-tools">
</p>
<p>
  <img src="screenshots/layouts.png" width="190" alt="layouts">
  <img src="screenshots/keyboard-ru.png" width="190" alt="keyboard-ru">
  <img src="screenshots/keyboard-kk.png" width="190" alt="keyboard-kk">
  <img src="screenshots/keyboard-de.png" width="190" alt="keyboard-de">
</p>

## Dependencies

- JDK 21
- Android SDK with platform 37 and build-tools 37
- Android 12 (API 31) or newer on the target device

## Build

Create `local.properties` in the project root and point it at the Android SDK:

```
sdk.dir=/path/to/Android/Sdk
```

Build the debug APK:

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Releases

| Version | Codename  |
|---------|-----------|
| 0.1.0   | Yakushima |

## License

GPL-3.0-only. See [LICENSE](LICENSE).
