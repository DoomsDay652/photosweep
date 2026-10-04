#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke-evidence
trap 'adb pull /sdcard/Download/PhotoSweepSmoke app/build/smoke-evidence/ >/dev/null 2>&1 || true; adb logcat -d -v threadtime > app/build/smoke-evidence/logcat.txt || true' EXIT
adb shell wm size 720x1280
gradle :app:connectedDebugAndroidTest --no-daemon --stacktrace -Pandroid.testInstrumentationRunnerArguments.class="${PHOTO_SWEEP_TEST_CASE:-com.dominic.photosweep.TenSessionSmokeTest}"
