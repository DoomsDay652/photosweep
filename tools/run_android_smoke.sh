#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke-evidence
trap 'adb pull /sdcard/Android/data/com.dominic.photosweep/files/smoke-evidence app/build/smoke-evidence/ >/dev/null 2>&1 || true; adb logcat -d -v threadtime > app/build/smoke-evidence/logcat.txt || true' EXIT
adb shell wm size 720x1280
gradle :app:connectedDebugAndroidTest --no-daemon --stacktrace
