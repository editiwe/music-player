#!/usr/bin/env bash
# Builds (if needed) and installs the debug APK on every connected device/emulator.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

if ! command -v "$ADB" >/dev/null 2>&1 && [[ ! -x "$ADB" ]]; then
  echo "adb not found: $ADB" >&2
  exit 1
fi

if [[ ! -f "$APK" ]]; then
  echo "APK not found, building it first..."
  (cd "$ROOT" && ./gradlew assembleDebug)
fi

DEVICES="$("$ADB" devices | awk 'NR > 1 && $2 == "device" {print $1}')"

if [[ -z "$DEVICES" ]]; then
  cat >&2 <<'EOF'
No connected devices.

For a phone:
  1. Settings -> About phone -> tap "Build number" 7 times.
  2. Settings -> Developer options -> enable "USB debugging".
  3. Connect the phone via USB and accept the RSA prompt on the phone.
  4. Run: adb devices

For an emulator:
  ./scripts/start-emulator.sh
EOF
  exit 1
fi

for device in $DEVICES; do
  echo "Installing on $device ..."
  "$ADB" -s "$device" install -r "$APK"
done

echo "Done. Launch 'Музыкальный плеер' on the device."
