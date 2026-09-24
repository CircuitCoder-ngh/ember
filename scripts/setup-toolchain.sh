#!/usr/bin/env bash
# Idempotent toolchain setup for building Ember from WSL without sudo.
# Installs JDK 17, Android cmdline-tools + SDK packages, and a bootstrap Gradle under ~/opt.
set -euo pipefail

OPT="$HOME/opt"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz"
JDK_SHA="3808d1d15e3ec6bd5b84057fb5d84c33d8a1536a258146bcea2e603fc726e08e"
JDK_DIR="$OPT/jdk-17"
CLT_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"
SDK_DIR="$OPT/android-sdk"
GRADLE_VER="9.5.0"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip"
GRADLE_DIR="$OPT/gradle-${GRADLE_VER}"
PLATFORM="android-37.0"
BUILD_TOOLS="36.0.0"

mkdir -p "$OPT"
cd "$OPT"

download() { # url out
  if [ -f "$2" ]; then echo "[skip] $2 exists"; return; fi
  echo "[dl] $1"
  curl -fL --retry 3 -sS -o "$2.part" "$1"
  mv "$2.part" "$2"
}

unzip_py() { # zip destdir
  python3 - "$1" "$2" <<'PY'
import sys, zipfile, os, stat
z, dest = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(z) as zf:
    for info in zf.infolist():
        zf.extract(info, dest)
        p = os.path.join(dest, info.filename)
        mode = (info.external_attr >> 16) & 0o777
        if mode and os.path.exists(p):
            os.chmod(p, mode)
PY
}

# --- downloads in parallel ---
download "$JDK_URL" jdk17.tar.gz &
download "$CLT_URL" cmdline-tools.zip &
download "$GRADLE_URL" "gradle-${GRADLE_VER}-bin.zip" &
wait
echo "[ok] downloads complete"

# --- JDK ---
echo "$JDK_SHA  jdk17.tar.gz" | sha256sum -c -
if [ ! -x "$JDK_DIR/bin/java" ]; then
  rm -rf "$JDK_DIR" jdk-17.0.20.1+1
  tar -xzf jdk17.tar.gz
  mv jdk-17.0.20.1+1 "$JDK_DIR"
fi
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

# --- Android cmdline-tools (must live at cmdline-tools/latest) ---
if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  rm -rf clt-tmp "$SDK_DIR/cmdline-tools/latest"
  unzip_py cmdline-tools.zip clt-tmp
  mkdir -p "$SDK_DIR/cmdline-tools"
  mv clt-tmp/cmdline-tools "$SDK_DIR/cmdline-tools/latest"
  rm -rf clt-tmp
  chmod +x "$SDK_DIR/cmdline-tools/latest/bin/"*
fi
export ANDROID_HOME="$SDK_DIR"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | sdkmanager --sdk_root="$SDK_DIR" --licenses >/dev/null || true
sdkmanager --sdk_root="$SDK_DIR" --install "platform-tools" "platforms;${PLATFORM}" "build-tools;${BUILD_TOOLS}"
echo "[ok] sdk packages installed"

# --- bootstrap Gradle ---
if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  rm -rf "$GRADLE_DIR"
  unzip_py "gradle-${GRADLE_VER}-bin.zip" "$OPT"
  chmod +x "$GRADLE_DIR/bin/gradle"
fi

# --- env vars ---
if ! grep -q "EMBER_TOOLCHAIN" "$HOME/.bashrc"; then
cat >> "$HOME/.bashrc" <<ENV

# EMBER_TOOLCHAIN (added by accountabilityApp/scripts/setup-toolchain.sh)
export JAVA_HOME="\$HOME/opt/jdk-17"
export ANDROID_HOME="\$HOME/opt/android-sdk"
export ANDROID_SDK_ROOT="\$ANDROID_HOME"
export PATH="\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"
ENV
fi
echo "[done] toolchain ready: java=$(java -version 2>&1 | head -1), sdk=$SDK_DIR, gradle=$GRADLE_DIR"
