#!/usr/bin/env bash
# Boots the 'ember' emulator as a window on your Windows desktop (via WSLg), then installs
# and launches the latest debug build. Usage: scripts/emulator.sh [--no-install]
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="$HOME/opt/jdk-17"; export ANDROID_HOME="$HOME/opt/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
if ! adb devices | grep -q "emulator-5554.*device"; then
  nohup emulator -avd ember -no-audio -gpu swiftshader_indirect -memory 2048 -cores 4 > /tmp/ember-emulator.log 2>&1 &
  echo "Booting emulator..."
  until [ "$(adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 3; done
fi
if [ "${1:-}" != "--no-install" ]; then
  ./gradlew :app:assembleDebug --console=plain -q
  adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
  adb -s emulator-5554 shell am start -n com.nhowe.ember.debug/com.nhowe.ember.MainActivity >/dev/null
fi
echo "Ready."
