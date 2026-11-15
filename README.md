# Reboot to CoreELEC

A small Android / Android TV app that reboots an Amlogic box from Android into
[CoreELEC](https://coreelec.org/) with one click. It sends the reboot command to the
device's own ADB daemon, so no root is required.

## Requirements

- Android 7.0 (API 24) or newer
- CoreELEC installed on an SD card or USB drive
- **USB debugging** enabled in the developer options
- **Network debugging (ADB over network)** enabled, so that ADB listens on port 5555

## Usage

1. Start the app and choose a reboot mode:
   - **First reboot** – needed when CoreELEC is started for the first time or after an
     update of the Android system (`reboot update`).
   - **Normal reboot** – for every later boot into CoreELEC (`reboot`).
2. On first use, Android asks whether to allow debugging from this device. Tick
   *Always allow* and confirm.

The interface is built with Jetpack Compose and Material 3 Expressive and can be used
with a remote control as well as by touch.

If something goes wrong (for example ADB is not reachable), the reason is shown below the
app title.

## Building

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. Building requires JDK 17 or newer
and the Android SDK (platform 37).

## Translations

English, German and Hungarian. Further translations are welcome: copy
`app/src/main/res/values/strings.xml` into a new `values-<language>` folder and add the
language to `localeFilters` in `app/build.gradle`.

## License

See [LICENSE](LICENSE).
