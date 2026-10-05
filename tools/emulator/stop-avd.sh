#!/usr/bin/env bash
# Desliga o emulador do projeto (SERIAL, padrão emulator-5580) e confirma com adb devices.
set -euo pipefail
source "$(dirname "$0")/env.sh"
if grep -q "^$SERIAL[[:space:]]" <<<"$("$ADB" devices)"; then
  log "Desligando $SERIAL"
  a emu kill >/dev/null 2>&1 || true
  for _ in $(seq 1 30); do
    grep -q "^$SERIAL[[:space:]]" <<<"$("$ADB" devices)" || break
    sleep 2
  done
fi
"$ADB" devices
