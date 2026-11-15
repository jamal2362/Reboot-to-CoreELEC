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

The interface is built with Jetpack Compose and Material 3 Expressive and is laid out
for 16:9 TV screens and remote controls.

If something goes wrong (for example ADB is not reachable), the reason is shown below the
app title.

## Building

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. Building requires JDK 17 or newer
and the Android SDK (platform 37).

## Releases

Releases are published by running the **Release** workflow manually (Actions → Release →
Run workflow). It builds the signed release APK and uploads it to a new GitHub release
tagged with the `versionName` from `app/build.gradle`, so raise `versionName` and
`versionCode` first.

The workflow needs these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Content |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | The signing keystore, encoded with `base64 -w0 keystore.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | Password of the keystore |
| `RELEASE_KEY_ALIAS` | Alias of the signing key |
| `RELEASE_KEY_PASSWORD` | Password of the signing key |

Use the same keystore as for earlier releases; otherwise the new APK cannot be installed
over an existing installation.

The app checks the latest GitHub release at start and shows a notice when a newer version
is available.

## Translations

English, German and Hungarian. Further translations are welcome: copy
`app/src/main/res/values/strings.xml` into a new `values-<language>` folder and add the
language to `localeFilters` in `app/build.gradle`.

## License

See [LICENSE](LICENSE).
