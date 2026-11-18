# Kotoba

![License](https://img.shields.io/badge/license-GPL--3.0--only-blue)
![Min SDK](https://img.shields.io/badge/minSdk-31-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF)
![Material 3](https://img.shields.io/badge/Material%20Design-3-6750A4)

On-screen keyboard (input method) for Android.

## Dependencies

- JDK 21
- Android SDK with platform 36 and build-tools 36
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
