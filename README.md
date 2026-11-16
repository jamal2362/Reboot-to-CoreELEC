# Reboot to CoreELEC

<p align="center">
<img width="1200" alt="Preview" src="https://github.com/user-attachments/assets/d4627650-5c8b-4577-a1f8-fe82785a8496" />
</p>

A small Android / Android TV app that reboots an Amlogic box from Android into
[CoreELEC](https://coreelec.org/) with one click. It sends the reboot command to the
device's own ADB daemon, so no root is required.

## Requirements

- Android 7.0 (API 24) or newer
- CoreELEC installed on an SD card or USB drive
- **USB debugging** enabled in the developer options

## Usage

1. Start the app and choose a reboot mode:
   - **First reboot** – needed when CoreELEC is started for the first time or after an
     update of the Android system (`reboot update`).
   - **Normal reboot** – for every later boot into CoreELEC (`reboot`).
2. On first use, Android asks whether to allow debugging from this device. Tick
   *Always allow* and confirm.

## Translations

English, German, Hungarian and Simplified Chinese. Further translations are welcome: copy
`app/src/main/res/values/strings.xml` into a new `values-<language>` folder and add the
language to `localeFilters` in `app/build.gradle`.

## License

See [LICENSE](LICENSE).
