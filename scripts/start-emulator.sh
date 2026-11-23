#!/usr/bin/env bash
# Starts the AVD, waits until Android is booted, then installs the app.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
EMULATOR="$SDK/emulator/emulator"
ADB="$SDK/platform-tools/adb"
AVD="${AVD:-Medium_Phone_API_37.0}"

if [[ ! -x "$EMULATOR" ]]; then
  echo "emulator not found: $EMULATOR" >&2
  exit 1
fi

if ! "$EMULATOR" -list-avds | grep -qx "$AVD"; then
  echo "AVD '$AVD' not found. Available AVDs:" >&2
  "$EMULATOR" -list-avds >&2
  exit 1
fi

if [[ ! -e /dev/kvm ]]; then
  cat >&2 <<'EOF'
WARNING: /dev/kvm is missing, so the x86_64 emulator cannot start.

Enable virtualization in BIOS/UEFI:
  - Intel: VT-x / Intel Virtualization Technology
  - AMD: SVM / AMD-V

Then reboot and make sure /dev/kvm exists:
  ls -l /dev/kvm

If virtualization cannot be enabled, use a real phone with install-apk.sh.
EOF
  exit 1
fi

if ! "$ADB" devices | awk 'NR > 1 && $2 == "device" {print $1}' | grep -q '^emulator-'; then
  echo "Starting emulator '$AVD' ..."
  nohup "$EMULATOR" -avd "$AVD" -no-audio -gpu swiftshader >/tmp/music-player-emulator.log 2>&1 &
fi

echo "Waiting for emulator to boot..."
"$ADB" wait-for-device

BOOTED="0"
for _ in $(seq 1 180); do
  BOOTED="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [[ "$BOOTED" == "1" ]] && break
  sleep 2
done

if [[ "$BOOTED" != "1" ]]; then
  echo "Emulator did not finish booting. Log: /tmp/music-player-emulator.log" >&2
  exit 1
fi

"$ROOT/scripts/install-apk.sh"
