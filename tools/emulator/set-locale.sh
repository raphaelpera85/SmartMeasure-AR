#!/usr/bin/env bash
# Troca o idioma visto pelo app. Uso: tools/emulator/set-locale.sh pt-BR|en-US
# API 33+: idioma por app (cmd locale set-app-locales), funciona em imagem Play sem root.
# API < 33: exige imagem google_apis (adb root): persist.sys.locale + reinício do framework.
# Obs.: a opção -change-locale do emulator não teve efeito na imagem Play API 35 (testado).
set -euo pipefail
source "$(dirname "$0")/env.sh"
loc="${1:?locale, ex.: pt-BR}"
sdk=$(a shell getprop ro.build.version.sdk | tr -d '\r')
if ((sdk >= 33)); then
  a shell cmd locale set-app-locales "$SM_PACKAGE" --locales "$loc"
  a shell am force-stop "$SM_PACKAGE"
  log "Idioma do app: $(a shell cmd locale get-app-locales "$SM_PACKAGE" | tr -d '\r')"
else
  a root >/dev/null; sleep 2; a wait-for-device
  a shell setprop persist.sys.locale "$loc"
  a shell 'stop; sleep 1; start'
  sleep 5
  until [[ "$(a shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do sleep 2; done
  sleep 5
  a shell input keyevent KEYCODE_WAKEUP || true
  a shell wm dismiss-keyguard 2>/dev/null || true
  log "Locale do sistema: $(a shell getprop persist.sys.locale | tr -d '\r')"
fi
