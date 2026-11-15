#!/bin/sh
# Drives the app with D-pad keys and saves a screenshot after each step.
name="$1"
mkdir -p shots
shot() { timeout 30 adb exec-out screencap -p > "shots/$name-$1.png" || true; }
key() { timeout 10 adb shell input keyevent "$1" || true; }

adb install app/build/outputs/apk/debug/*.apk
adb shell am start -W -n com.jamal2367.coreelec.debug/com.jamal2367.coreelec.MainActivity
sleep 8
shot 1-start

key DPAD_RIGHT
sleep 2
shot 2-info-focused
key DPAD_CENTER
sleep 3
shot 3-info-dialog
key BACK
sleep 2

key DPAD_LEFT
sleep 1
key DPAD_CENTER
sleep 1
shot 4-connecting
sleep 4
shot 5-after-5s
sleep 15
shot 6-after-20s

timeout 20 adb logcat -d -s AdbRebooter:V RebootViewModel:V AndroidRuntime:E > "shots/$name-logcat.txt" || true
exit 0
