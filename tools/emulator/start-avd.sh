#!/usr/bin/env bash
# Inicia um AVD do projeto sem janela e espera o boot completo.
# Uso: tools/emulator/start-avd.sh <NomeAVD> [-- opções extras do emulator, ex.: -change-locale pt-BR]
# Variáveis: SM_PORT (padrão 5580 => emulator-5580), SM_GPU (swiftshader_indirect|host), SM_WINDOW=1 para ver a janela.
set -euo pipefail
source "$(dirname "$0")/env.sh"
name="${1:?nome do AVD}"; shift || true
[[ "${1:-}" == "--" ]] && shift
guard_avd_name "$name"

if grep -q "^$SERIAL[[:space:]]" <<<"$("$ADB" devices)"; then
  die "$SERIAL já está em uso. Desligue com tools/emulator/stop-avd.sh antes (um emulador por vez)."
fi

mkdir -p "$SM_OUT_ROOT"
emu_log="$SM_OUT_ROOT/emulator-$name.log"
win=(-no-window)
[[ "${SM_WINDOW:-0}" == 1 ]] && win=()
grpc=()
[[ -n "${SM_GRPC_PORT:-}" ]] && grpc=(-grpc "$SM_GRPC_PORT")   # para mover a cena virtual (vscene.py)
log "Iniciando $name em $SERIAL (gpu=$SM_GPU); log: $emu_log"
"$EMULATOR" -avd "$name" -port "$SM_PORT" "${win[@]}" -no-audio -no-boot-anim -no-snapshot \
  -gpu "$SM_GPU" "${grpc[@]}" "$@" >"$emu_log" 2>&1 &
echo $! >"$SM_OUT_ROOT/emulator-$name.pid"

"$ADB" -s "$SERIAL" wait-for-device
deadline=$((SECONDS + ${SM_BOOT_TIMEOUT:-600}))
until [[ "$(a shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do
  ((SECONDS < deadline)) || die "boot não completou em ${SM_BOOT_TIMEOUT:-600}s (veja $emu_log)"
  sleep 3
done
# Desliga animações para toques determinísticos; mantém a tela ligada.
a shell settings put global window_animation_scale 0
a shell settings put global transition_animation_scale 0
a shell settings put global animator_duration_scale 0
a shell svc power stayon true || true
a shell input keyevent KEYCODE_WAKEUP || true
a shell wm dismiss-keyguard 2>/dev/null || a shell input keyevent 82 || true
log "$name pronto: API $(a shell getprop ro.build.version.sdk | tr -d '\r'), locale $(a shell getprop persist.sys.locale | tr -d '\r')"
echo "$SERIAL"
