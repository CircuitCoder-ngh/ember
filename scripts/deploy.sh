#!/usr/bin/env bash
# Usage: scripts/deploy.sh [debug|release]  — builds, installs on the connected Pixel, and copies the APK to /mnt/c.
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="$HOME/opt/jdk-17"; export ANDROID_HOME="$HOME/opt/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
TYPE="${1:-release}"
./gradlew "assemble${TYPE^}" --console=plain
APK="app/build/outputs/apk/${TYPE}/app-${TYPE}.apk"
DROP="/mnt/c/Users/nghho/accountabilityApp/apks"
mkdir -p "$DROP" && cp "$APK" "$DROP/ember-${TYPE}-$(date +%Y%m%d-%H%M).apk"
echo "[ok] APK copied to $DROP"
if adb devices | grep -q -E "device$"; then
  adb install -r "$APK" && echo "[ok] installed on phone"
else
  echo "[warn] no adb device connected; sideload from $DROP or run scripts/adb-connect.sh ip:port"
fi
