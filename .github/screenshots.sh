#!/bin/sh
# Drives the app with D-pad keys and saves a screenshot after each step.
name="$1"
mkdir -p shots
shot() { timeout 30 adb exec-out screencap -p > "shots/$name-$1.png" || true; }
key() { timeout 10 adb shell input keyevent "$1" || true; }

adb install app/build/outputs/apk/debug/*.apk
adb shell am start -W -n com.jamal2367.coreelec.debug/com.jamal2367.coreelec.ShapeGalleryActivity
sleep 6
shot 0-shapes

adb shell am start -W -n com.jamal2367.coreelec.debug/com.jamal2367.coreelec.MainActivity
sleep 8
shot 1-start

key DPAD_DOWN
sleep 2
shot 2-second-focused
key DPAD_UP
sleep 1

key DPAD_CENTER
sleep 1
shot 3-connecting
sleep 4
shot 4-after-5s
sleep 15
shot 5-after-20s

timeout 20 adb logcat -d -s AdbRebooter:V RebootViewModel:V AndroidRuntime:E > "shots/$name-logcat.txt" || true
exit 0
