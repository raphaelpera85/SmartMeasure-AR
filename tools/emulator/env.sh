# shellcheck shell=bash
# Variáveis e funções comuns aos scripts de emulador do SmartMeasure AR.
# Uso: source "$(dirname "$0")/env.sh"  (git-bash no Windows ou bash no Linux/macOS)

EMU_TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$EMU_TOOLS_DIR/../.." && pwd)"

: "${ANDROID_SDK:=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-C:/Android/Sdk}}}"
: "${SM_AVD_DIR:=D:/Android/avd}"              # onde ficam os .avd deste projeto
: "${SM_ARCORE_DIR:=D:/Android/arcore}"        # cache do APK do Google Play Services for AR
: "${SM_ARCORE_VERSION:=1.56.0}"
: "${SM_PORT:=5580}"                           # porta do console => serial emulator-5580
: "${SM_GPU:=swiftshader_indirect}"            # ou: host
: "${SM_PACKAGE:=com.smartmeasure.ar}"
: "${SM_ACTIVITY:=com.smartmeasure.ar/.MainActivity}"
: "${SM_APK:=$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk}"
: "${SM_OUT_ROOT:=${TMPDIR:-/tmp}/smartmeasure-usage}"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) EXE=.exe; BAT=.bat ;;
  *) EXE=; BAT= ;;
esac
ADB="$ANDROID_SDK/platform-tools/adb$EXE"
EMULATOR="$ANDROID_SDK/emulator/emulator$EXE"
AVDMANAGER="$ANDROID_SDK/cmdline-tools/latest/bin/avdmanager$BAT"
SDKMANAGER="$ANDROID_SDK/cmdline-tools/latest/bin/sdkmanager$BAT"
PY=""
for _py in python3 python; do   # no Windows, python3 pode ser o atalho vazio da Microsoft Store
  if command -v "$_py" >/dev/null 2>&1 && "$_py" -c 'import sys' >/dev/null 2>&1; then PY="$(command -v "$_py")"; break; fi
done

SERIAL="${SERIAL:-emulator-$SM_PORT}"

log() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*" >&2; }
die() { log "ERRO: $*"; exit 1; }
a() { "$ADB" -s "$SERIAL" "$@"; }   # adb sempre com serial

# Converte caminho para o formato nativo aceito por executáveis Windows (C:/...).
native_path() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi
}

guard_avd_name() {
  case "$1" in
    SmartMeasure*) ;;
    *) die "AVD '$1' não pertence a este projeto (prefixo SmartMeasure obrigatório)." ;;
  esac
}
