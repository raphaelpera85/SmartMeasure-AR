#!/usr/bin/env bash
# Cria (ou reconfigura) os AVDs do SmartMeasure AR. Idempotente.
# Uso: tools/emulator/create-avds.sh [SmartMeasureApi35Play|SmartMeasureApi37|SmartMeasureApi24 ...]
set -euo pipefail
source "$(dirname "$0")/env.sh"

# nome | pacote da imagem | perfil | câmera traseira
AVDS=(
  "SmartMeasureApi35Play|system-images;android-35-ext14;google_apis_playstore;x86_64|pixel_7|virtualscene"
  "SmartMeasureApi37|system-images;android-37.0;google_apis;x86_64|pixel_7|emulated"
  "SmartMeasureApi24|system-images;android-24;google_apis;x86_64|pixel_2|emulated"
)

set_prop() { # arquivo chave valor
  local f="$1" k="$2" v="$3"
  if grep -q "^$k *=" "$f"; then
    sed -i "s|^$k *=.*|$k=$v|" "$f"
  else
    printf '%s=%s\n' "$k" "$v" >>"$f"
  fi
}

want=("$@")
mkdir -p "$SM_AVD_DIR"
for spec in "${AVDS[@]}"; do
  IFS='|' read -r name image device camera <<<"$spec"
  if ((${#want[@]})) && [[ ! " ${want[*]} " == *" $name "* ]]; then continue; fi
  guard_avd_name "$name"
  imgdir="$ANDROID_SDK/system-images/$(echo "$image" | cut -d';' -f2-4 | tr ';' '/')"
  if [[ ! -f "$imgdir/system.img" ]]; then
    log "$name: imagem ausente ($image); tentando sdkmanager..."
    if ! yes | "$SDKMANAGER" "$image" >/dev/null; then
      log "$name: download falhou; pulando."; continue
    fi
  fi
  avd_path="$SM_AVD_DIR/$name.avd"
  if [[ -f "$HOME/.android/avd/$name.ini" && -f "$avd_path/config.ini" ]]; then
    log "$name: já existe em $avd_path (apenas reaplicando configuração)."
  else
    log "$name: criando em $avd_path"
    echo no | "$AVDMANAGER" create avd -n "$name" -k "$image" -d "$device" -p "$(native_path "$avd_path")" --force >/dev/null
  fi
  cfg="$avd_path/config.ini"
  set_prop "$cfg" hw.camera.back "$camera"
  set_prop "$cfg" hw.camera.front emulated
  set_prop "$cfg" hw.ramSize 4096
  set_prop "$cfg" hw.keyboard yes
  set_prop "$cfg" hw.gpu.enabled yes
  set_prop "$cfg" disk.dataPartition.size 6G
  set_prop "$cfg" fastboot.forceColdBoot no
  # avdmanager grava PlayStore.enabled=no mesmo em imagem google_apis_playstore.
  if [[ "$image" == *google_apis_playstore* ]]; then set_prop "$cfg" PlayStore.enabled yes; fi
  log "$name: ok ($(grep -E '^(image.sysdir.1|PlayStore.enabled|hw.camera.back)=' "$cfg" | tr '\n' ' '))"
done
