#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/smoke-evidence
trap 'adb pull /data/local/tmp/PhotoSweepSmoke app/build/smoke-evidence/ >/dev/null 2>&1 || true; adb logcat -d -v threadtime > app/build/smoke-evidence/logcat.txt || true' EXIT
adb shell wm size 720x1280
adb shell wm density 320
# Treat the fresh CI device as having seen Android's one-time fullscreen navigation tip.
# Google's own window-manager tests use this setting so the system overlay does not steal input.
adb shell settings put secure immersive_mode_confirmations confirmed
gradle :app:connectedDebugAndroidTest --no-daemon --stacktrace -Pandroid.testInstrumentationRunnerArguments.class="${PHOTO_SWEEP_TEST_CASE:-com.dominic.photosweep.TenSessionSmokeTest}"
