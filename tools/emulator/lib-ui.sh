# shellcheck shell=bash
# Funções de interação com a UI via adb (toque por texto do dump, capturas, logs).
# Requer env.sh carregado e OUT (pasta de evidências da execução atual).

UI_PY="$(native_path "$EMU_TOOLS_DIR/ui.py")"
pyui() { "$PY" "$UI_PY" "$@"; }
STEP_N=0

ui_dump() { # [arquivo] -> caminho do dump
  local f="${1:-$OUT/.last.xml}"
  local i
  for i in 1 2 3; do
    if a shell uiautomator dump /sdcard/sm_dump.xml >/dev/null 2>&1; then
      a exec-out cat /sdcard/sm_dump.xml >"$f" && [[ -s "$f" ]] && { echo "$f"; return 0; }
    fi
    sleep 1
  done
  return 1
}

shot() { # nome -> salva captura PNG
  a exec-out screencap -p >"$OUT/$1.png"
}

# Registra evidência de um passo: captura + dump + textos visíveis.
step() { # descrição-curta-sem-espaços
  STEP_N=$((STEP_N + 1))
  local name; name=$(printf '%02d-%s' "$STEP_N" "$1")
  sleep "${STEP_SETTLE:-1}"
  shot "$name"
  ui_dump "$OUT/$name.xml" >/dev/null || true
  "$PY" "$UI_PY" texts "$OUT/$name.xml" >"$OUT/$name.txt" 2>/dev/null || true
  log "passo $name"
  LAST_STEP="$name"
}

has_text() { # texto [--contains] -> 0 se visível agora
  local f; f=$(ui_dump) || return 1
  "$PY" "$UI_PY" find "$f" "$@" >/dev/null
}

wait_text() { # segundos texto [--contains]
  local t="$1"; shift
  local end=$((SECONDS + t))
  while ((SECONDS < end)); do
    has_text "$@" && return 0
    sleep 1
  done
  return 1
}

# Rola para baixo até o texto aparecer (sem tocar). Útil antes de registrar evidência.
seek_text() { # texto [--contains]
  local f i
  for i in 0 1 2 3 4 5 6 7; do
    f=$(ui_dump) || return 1
    pyui find "$f" "$@" >/dev/null && return 0
    scroll_down
  done
  return 1
}

# Toca no nó cujo texto/descrição casa. Rola a tela para baixo até achar (máx. 6 rolagens).
tap_text() { # texto [--contains] [--index N]
  local f xy i
  for i in 0 1 2 3 4 5 6; do
    f=$(ui_dump) || return 1
    if xy=$("$PY" "$UI_PY" find "$f" "$@"); then
      a shell input tap $xy
      sleep 0.7
      return 0
    fi
    scroll_down
  done
  log "texto não encontrado: $*"
  return 1
}

scroll_down() { local s; s=$(screen_size); a shell input swipe $((${s%x*} / 2)) $((${s#*x} * 3 / 4)) $((${s%x*} / 2)) $((${s#*x} / 3)) 300; sleep 0.6; }
scroll_up()   { local s; s=$(screen_size); a shell input swipe $((${s%x*} / 2)) $((${s#*x} / 3)) $((${s%x*} / 2)) $((${s#*x} * 3 / 4)) 300; sleep 0.6; }
scroll_top()  { local i; for i in 1 2 3 4 5; do scroll_up; done; }
screen_size() { a shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o?o:p)}'; }

# Digita texto no N-ésimo campo editável visível (0 = primeiro), limpando antes.
type_in_edit() { # índice texto
  local f line
  f=$(ui_dump) || return 1
  line=$("$PY" "$UI_PY" edits "$f" | awk -v i="$1" '$1==i{print $2, $3}')
  [[ -n "$line" ]] || { log "campo $1 não encontrado"; return 1; }
  a shell input tap $line
  sleep 0.4
  clear_focused
  a shell input text "$2"
  sleep 0.4
}

# Digita no campo cujo rótulo (texto filho/irmão) é dado: toca no rótulo, que foca o campo.
type_in_label() { # rótulo texto
  tap_text "$1" || return 1
  clear_focused
  a shell input text "$2"
  sleep 0.4
}

clear_focused() {
  a shell input keyevent KEYCODE_MOVE_END
  a shell input keyevent KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL
}

hide_keyboard() { # BACK só quando o teclado está visível (senão navegaria)
  local ims; ims=$(a shell dumpsys input_method | tr -d '\r')   # sem grep -q no pipe (pipefail + SIGPIPE)
  if grep -qE '^ *mInputShown=true' <<<"$ims"; then
    a shell input keyevent KEYCODE_BACK; sleep 0.5
  fi
}
press_back() { a shell input keyevent KEYCODE_BACK; sleep 0.8; }

app_start() { a shell am start -W -n "$SM_ACTIVITY" >/dev/null; sleep "${APP_SETTLE:-2}"; }
app_stop() { a shell am force-stop "$SM_PACKAGE"; }
app_clear() { a shell pm clear "$SM_PACKAGE" >/dev/null; }

logcat_clear() { a logcat -c; a logcat -b crash -c 2>/dev/null || true; }
# Salva logcat do app e buffer de crash. Retorna 1 se houver crash/ANR do pacote.
collect_logs() { # sufixo
  a logcat -b crash -d >"$OUT/logcat-crash-$1.txt" 2>&1 || true
  a logcat -d -v time >"$OUT/logcat-all-$1.txt" 2>&1 || true
  # Só conta crash/ANR do app (crashes do GMS etc. no emulador não são do app)
  if grep -qE "Process: $SM_PACKAGE(,|$| )" "$OUT/logcat-crash-$1.txt" ||
     grep -qE "ANR in $SM_PACKAGE|Process: $SM_PACKAGE(,| |$)" "$OUT/logcat-all-$1.txt"; then
    log "CRASH/ANR detectado (ver $OUT/logcat-crash-$1.txt)"; return 1
  fi
  return 0
}

# Registra resultado de verificação numa tabela TSV.
RESULTS_FILE=""
check() { # id descrição condição(comando)
  local id="$1" desc="$2"; shift 2
  local r=FALHOU
  if "$@"; then r=OK; fi
  printf '%s\t%s\t%s\t%s\n' "$id" "$r" "$desc" "${LAST_STEP:-}" >>"$RESULTS_FILE"
  log "$id [$r] $desc"
}
note() { printf '%s\tNOTA\t%s\t%s\n' "$1" "$2" "${LAST_STEP:-}" >>"$RESULTS_FILE"; log "$1 NOTA $2"; }
last_has() { "$PY" "$UI_PY" find "$OUT/$LAST_STEP.xml" "$@" >/dev/null; }
