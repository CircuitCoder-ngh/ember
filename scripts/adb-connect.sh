#!/usr/bin/env bash
# Usage: scripts/adb-connect.sh <phone-ip>:<port>   (port from Settings > Developer options > Wireless debugging)
# First time only: scripts/adb-connect.sh pair <phone-ip>:<pairing-port>
set -euo pipefail
export PATH="$HOME/opt/android-sdk/platform-tools:$PATH"
if [ "${1:-}" = "pair" ]; then adb pair "$2"; exit; fi
adb connect "${1:?need ip:port}"
adb devices
