#!/usr/bin/env bash
# Instala o APK de debug do app (e, com --arcore, o Google Play Services for AR para emulador).
# Uso: tools/emulator/install-app.sh [--arcore] [--build]
set -euo pipefail
source "$(dirname "$0")/env.sh"
want_arcore=0; build=0
for arg in "$@"; do
  case "$arg" in
    --arcore) want_arcore=1 ;;
    --build) build=1 ;;
    *) die "opção desconhecida: $arg" ;;
  esac
done

if ((build)) || [[ ! -f "$SM_APK" ]]; then
  log "Gerando APK: ./gradlew --offline assembleDebug"
  (cd "$REPO_ROOT" && ./gradlew --offline assembleDebug --console=plain -q)
fi

if ((want_arcore)); then
  apk="$SM_ARCORE_DIR/Google_Play_Services_for_AR_${SM_ARCORE_VERSION}_x86_for_emulator.apk"
  if [[ ! -f "$apk" ]]; then
    mkdir -p "$SM_ARCORE_DIR"
    url="https://github.com/google-ar/arcore-android-sdk/releases/download/$SM_ARCORE_VERSION/$(basename "$apk")"
    log "Baixando $url"
    curl -fsSL -o "$apk" "$url"
  fi
  log "Instalando $(basename "$apk")"
  a install -r "$(native_path "$apk")"
  a shell dumpsys package com.google.ar.core | grep -m1 versionName || true
fi

log "Instalando $(native_path "$SM_APK")"
a install -r "$(native_path "$SM_APK")"
