#!/usr/bin/env bash
# Roteiros de uso do SmartMeasure AR num emulador já ligado (SERIAL, padrão emulator-5580).
# Uso: tools/emulator/run-scenarios.sh --label <nome> [--lang en|pt] [--expect-arcore <recurso>] [R1 R2 R3 R4 R5 R6]
#   --label          nome da pasta de evidências (ex.: Api35Play-en)
#   --lang           idioma esperado na UI (en = values/, pt = values-pt-rBR/); o emulador deve estar nesse locale
#   --expect-arcore  nome do recurso de status esperado no Diagnóstico (ar_ready, ar_unsupported, ar_needs_install...)
#   --fresh          limpa os dados do app antes (pm clear)
# Evidências: $SM_OUT_ROOT/<AAAA-MM-DD>/<label>/ (NN-passo.png/.xml/.txt, logcat-*.txt, results.tsv)
set -uo pipefail
source "$(dirname "$0")/env.sh"
source "$(dirname "$0")/lib-ui.sh"

label=""; lang=en; expect_arcore=""; fresh=0; scenarios=()
while (($#)); do
  case "$1" in
    --label) label="$2"; shift 2 ;;
    --lang) lang="$2"; shift 2 ;;
    --expect-arcore) expect_arcore="$2"; shift 2 ;;
    --fresh) fresh=1; shift ;;
    R[1-6]) scenarios+=("$1"); shift ;;
    *) die "argumento desconhecido: $1" ;;
  esac
done
((${#scenarios[@]})) || scenarios=(R1 R2 R3 R4 R6)
[[ -n "$label" ]] || label="$(a shell getprop ro.build.version.sdk | tr -d '\r')-$lang"

OUT="$SM_OUT_ROOT/$(date +%F)/$label"
mkdir -p "$OUT"
RESULTS_FILE="$OUT/results-$(IFS=-; echo "${scenarios[*]}").tsv"
: >"$RESULTS_FILE"

case "$lang" in
  pt) RES="$(native_path "$REPO_ROOT/app/src/main/res/values-pt-rBR/strings.xml")"; DEC=, ;;
  *)  RES="$(native_path "$REPO_ROOT/app/src/main/res/values/strings.xml")"; DEC=. ;;
esac
S() { pyui str "$RES" "$@"; }                 # texto de um recurso no idioma esperado
num() { printf '%s' "${1//./$DEC}"; }          # 12.00 -> 12,00 em pt
focus() { a shell dumpsys window | tr -d '\r' | awk '/mCurrentFocus=/{print; exit}'; }
app_on_top() { local f; f=$(focus); [[ "$f" == *"$SM_PACKAGE"* ]]; }   # sem 'cmd | grep -q' (pipefail + SIGPIPE)

log "Evidências em $OUT"
a shell getprop ro.build.version.sdk >"$OUT/device.txt"
{ a shell getprop ro.product.model; a shell getprop persist.sys.locale; a shell dumpsys package com.google.ar.core | grep -m1 versionName; } >>"$OUT/device.txt" 2>&1
((fresh)) && app_clear
logcat_clear

go_home_screen() { # volta ao Diagnóstico reiniciando o app
  app_stop; app_start
  wait_text 10 "$(S diagnostics_title)" || true
}

# ---------------------------------------------------------------- R1
R1() {
  go_home_screen
  step R1-diagnostico
  check R1.1 "Diagnóstico abre (título no idioma $lang)" last_has "$(S diagnostics_title)"
  local st found=""
  for st in ar_ready ar_needs_install ar_unsupported ar_unknown ar_checking; do
    if last_has "$(S $st)"; then found=$st; break; fi
  done
  note R1.2 "Status ARCore exibido: ${found:-nenhum} ('$( [[ -n $found ]] && S $found)')"
  if [[ -n "$expect_arcore" ]]; then
    check R1.3 "Status ARCore coerente com o AVD (esperado $expect_arcore)" test "$found" == "$expect_arcore"
  fi
  tap_text "$(S refresh)" && step R1-verificar-novamente
  seek_text "$(S open_field_trials)"; step R1-rodape
  check R1.4 "Sem crash" collect_logs R1
}

# ---------------------------------------------------------------- R2
manual_calc() { # largura comprimento
  type_in_edit 0 "$1"; type_in_edit 1 "$2"; hide_keyboard
  tap_text "$(S calculate)"
}
R2() {
  go_home_screen
  tap_text "$(S open_manual_mode)"; step R2-manual
  check R2.1 "Tela de medição manual abre" last_has "$(S manual_measurement_title)"
  manual_calc 4 3; step R2-4x3
  check R2.2 "4 x 3 -> área $(num 12.00)" last_has "$(S area_result "$(num 12.00)")"
  check R2.3 "4 x 3 -> perímetro $(num 14.00)" last_has "$(S perimeter_result "$(num 14.00)")"
  manual_calc 3,5 3; step R2-3virgula5x3
  check R2.4 "3,5 x 3 -> área $(num 10.50)" last_has "$(S area_result "$(num 10.50)")"
  check R2.5 "3,5 x 3 -> perímetro $(num 13.00)" last_has "$(S perimeter_result "$(num 13.00)")"
  manual_calc 3.5 3; step R2-3ponto5x3
  check R2.6 "3.5 (ponto) também aceito -> área $(num 10.50)" last_has "$(S area_result "$(num 10.50)")"
  manual_calc abc 3; step R2-invalido
  check R2.7 "Valor inválido não mostra resultado" bash -c "! grep -q 'm²' '$OUT/$LAST_STEP.txt'"
  local err; err=$(sed -n '/m)$/,$p' "$OUT/$LAST_STEP.txt" | grep -vE '\(m\)$|^abc$|^3$' | grep -v "$(S calculate)" | head -1)
  note R2.8 "Mensagem de erro exibida: '$err'"
  manual_calc 0 3; step R2-zero
  check R2.9 "Zero rejeitado (sem resultado)" bash -c "! grep -q 'm²' '$OUT/$LAST_STEP.txt'"
  tap_text "$(S back)"; step R2-voltar
  check R2.10 "Botão Voltar retorna ao Diagnóstico" last_has "$(S diagnostics_title)"
  tap_text "$(S open_manual_mode)"; press_back; step R2-voltar-sistema
  check R2.11 "Voltar do sistema na tela manual volta ao Diagnóstico (app continua aberto)" app_on_top
  check R2.12 "Sem crash" collect_logs R2
}

# ---------------------------------------------------------------- R3
save_trial() { # ar ref kind_resource
  scroll_top
  type_in_edit 0 "$1"; type_in_edit 1 "$2"; hide_keyboard
  tap_text "$(S "$3")"
  tap_text "$(S trials_save)"
}
open_trials() {
  go_home_screen
  tap_text "$(S open_field_trials)"
  wait_text 10 "$(S trials_title)" || true
}
R3() {
  open_trials; step R3-ensaios
  check R3.1 "Tela de ensaios abre" last_has "$(S trials_title)"
  save_trial 2,52 2,50 kind_wall; step R3-salvo-parede
  note R3.2 "Mensagem pós-salvar visível: $(last_has "$(S trials_saved)" && echo sim || echo não)"
  save_trial 0,81 0,80 kind_opening; step R3-salvo-vao
  seek_text "$(S trials_list_title 2)"; step R3-resumo-lista
  check R3.3 "Lista mostra 2 ensaios" last_has "$(S trials_list_title 2)"
  seek_text "$(num 2.520)" --contains; step R3-linha-parede
  check R3.4 "Linha da parede: AR $(num 2.520) / ref $(num 2.500)" last_has "$(S trials_row_values "$(num 2.520)" "$(num 2.500)")"
  seek_text "$(num 0.810)" --contains; step R3-linha-vao
  check R3.5 "Linha do vão: AR $(num 0.810) / ref $(num 0.800)" last_has "$(S trials_row_values "$(num 0.810)" "$(num 0.800)")"
  # Resumo por tipo (cartões acima da lista)
  scroll_top; seek_text "$(S trials_summary_title)"; scroll_down; step R3-resumo
  check R3.6 "Resumo por tipo exibe estatísticas" last_has "$(S trials_stat_mean)"
  # Excluir o primeiro ensaio da lista, com confirmação
  local del_prefix; del_prefix="$(S trials_delete_a11y X X X X | sed 's/ *(.*//')"
  tap_text "$del_prefix" --contains || tap_text "$(S trials_delete)"
  step R3-dialogo-excluir
  check R3.7 "Diálogo de confirmação aparece" last_has "$(S trials_delete_title)"
  tap_text "$(S trials_delete_confirm)"; step R3-excluido
  scroll_top; seek_text "$(S trials_list_title 1)"; step R3-lista-1
  check R3.8 "Lista passa a 1 ensaio" last_has "$(S trials_list_title 1)"
  tap_text "$(S trials_export)"; sleep 2; step R3-exportar
  focus >"$OUT/R3-export-focus.txt"
  check R3.9 "Share sheet (chooser) abre" bash -c "grep -qiE 'chooser|resolver' '$OUT/R3-export-focus.txt'"
  press_back; sleep 1; scroll_top; step R3-volta-do-share
  check R3.10 "Volta do share sheet para os ensaios" last_has "$(S trials_title)"
  app_stop; app_start; tap_text "$(S open_field_trials)"; sleep 1
  seek_text "$(S trials_list_title 1)"; step R3-apos-reabrir
  check R3.11 "Após force-stop + reabrir, 1 ensaio persiste" last_has "$(S trials_list_title 1)"
  scroll_top; tap_text "$(S back)"; step R3-voltar
  check R3.12 "Voltar sai dos ensaios para o Diagnóstico" last_has "$(S diagnostics_title)"
  check R3.13 "Sem crash" collect_logs R3
}

# ---------------------------------------------------------------- R4
allow_permission_dialog() {
  local id f xy
  f=$(ui_dump) || return 1
  for id in permission_allow_foreground_only_button permission_allow_button; do
    if xy=$(pyui find "$f" "$id" --contains); then
      a shell input tap $xy; sleep 1; return 0
    fi
  done
  return 1
}
R4() {
  a shell pm revoke "$SM_PACKAGE" android.permission.CAMERA >/dev/null 2>&1 || true
  go_home_screen
  tap_text "$(S prepare_ar)"; sleep 2; step R4-pedido-permissao
  focus >"$OUT/R4-focus-permission.txt"
  if allow_permission_dialog; then
    note R4.1 "Permissão de câmera concedida pelo diálogo"
  else
    a shell pm grant "$SM_PACKAGE" android.permission.CAMERA
    note R4.1 "Diálogo de permissão não encontrado; usado pm grant"
    app_on_top || app_start
    tap_text "$(S prepare_ar)"
  fi
  sleep 6; step R4-apos-preparar
  focus >"$OUT/R4-focus-after-prepare.txt"
  if last_has "$(S ar_measurement_title)"; then
    check R4.2 "Tela AR abre sem crash" collect_logs R4-abertura
    sleep 8; step R4-ar-estavel
    if [[ -n "${SM_GRPC_PORT:-}" && -n "${SM_GRPC_PY:-}" ]]; then
      "$SM_GRPC_PY" "$(native_path "$EMU_TOOLS_DIR/vscene.py")" get >>"$OUT/R4-vscene.txt" 2>&1
      "$SM_GRPC_PY" "$(native_path "$EMU_TOOLS_DIR/vscene.py")" sweep >>"$OUT/R4-vscene.txt" 2>&1
      sleep 3; step R4-apos-mover-camera
    fi
    tap_text "$(S capture_first_point)"; sleep 2; step R4-ponto1
    if [[ -n "${SM_GRPC_PORT:-}" && -n "${SM_GRPC_PY:-}" ]]; then
      "$SM_GRPC_PY" "$(native_path "$EMU_TOOLS_DIR/vscene.py")" rot 0 -20 0 >>"$OUT/R4-vscene.txt" 2>&1 || true
      sleep 3
    fi
    tap_text "$(S capture_second_point)"; sleep 2; step R4-ponto2
    if last_has "$(S ar_distance_result X | sed 's/ X.*//')" --contains; then
      note R4.3 "Distância exibida: $(grep -m1 -F "$(S ar_distance_result X | sed 's/ X.*//')" "$OUT/$LAST_STEP.txt")"
      tap_text "$(S record_trial)"; sleep 1; step R4-comparar
      check R4.4 "'Comparar com referência' abre o formulário" last_has "$(S trials_title)"
      pyui edits "$OUT/$LAST_STEP.xml" >"$OUT/R4-form-fields.txt"
      check R4.5 "Campo AR vem preenchido" bash -c "awk 'NR==1 && NF>=4' '$OUT/R4-form-fields.txt' | grep -q ."
      press_back
    else
      note R4.3 "Sem distância (sem superfície/rastreamento na cena virtual): $(grep -m1 -E "$(S ar_msg_no_surface | cut -c1-20)|$(S ar_msg_move_phone | cut -c1-20)" "$OUT/$LAST_STEP.txt")"
    fi
    grep -E "$(S ar_session_path X X X | cut -c1-6)" "$OUT/$LAST_STEP.txt" >"$OUT/R4-session-summary.txt" || true
    note R4.6 "Resumo de sessão no card: $(head -2 "$OUT/R4-session-summary.txt" | tr '\n' ' ')"
    press_back; step R4-voltar-sistema
    check R4.7 "Voltar do sistema na tela AR volta ao Diagnóstico (app continua aberto)" app_on_top
  else
    local msg="" r
    for r in ar_failed_incompatible ar_failed_not_installed ar_failed_apk_too_old ar_failed_sdk_too_old \
             ar_failed_install_declined ar_failed_camera ar_failed_configuration ar_failed_unknown \
             install_requested permission_denied; do
      last_has "$(S $r)" && { msg="$r: $(S $r)"; break; }
    done
    note R4.2 "Tela AR não abriu; mensagem: ${msg:-nenhuma conhecida}; foco: $(cat "$OUT/R4-focus-after-prepare.txt")"
    app_on_top || { press_back; sleep 1; }
    app_on_top || app_start
    seek_text "$(S open_manual_mode)"; step R4-retorno
    check R4.3 "Fallback: Diagnóstico segue utilizável (modo manual visível)" last_has "$(S open_manual_mode)"
  fi
  check R4.9 "Sem crash" collect_logs R4
}

# ---------------------------------------------------------------- R5 (idioma: roda R1+R2 rápidos e confere textos)
R5() {
  go_home_screen; step R5-diagnostico-$lang
  check R5.1 "Diagnóstico no idioma $lang" last_has "$(S prepare_ar)"
  tap_text "$(S open_manual_mode)"; manual_calc abc 3; step R5-manual-erro-$lang
  check R5.2 "Tela manual no idioma $lang" last_has "$(S manual_measurement_title)"
  local en_err="Enter width and length greater than zero."
  if [[ $lang == pt ]] && grep -qF "$en_err" "$OUT/$LAST_STEP.txt"; then
    check R5.3 "Mensagem de erro do modo manual traduzida" false
  else
    note R5.3 "Mensagem de erro do modo manual: $(grep -m1 -iE 'zero' "$OUT/$LAST_STEP.txt")"
  fi
  manual_calc 3,5 3; step R5-manual-ok-$lang
  check R5.4 "Número formatado no idioma ($(num 10.50))" last_has "$(S area_result "$(num 10.50)")"
  open_trials; step R5-ensaios-$lang
  check R5.5 "Ensaios no idioma $lang" last_has "$(S trials_new_title)"
  seek_text "$(S trials_summary_title)"; step R5-ensaios-resumo-$lang
  check R5.6 "Sem crash" collect_logs R5
}

# ---------------------------------------------------------------- R6 (rotação e morte de processo)
R6() {
  open_trials
  type_in_edit 0 1,23; type_in_edit 1 1,20; hide_keyboard; step R6-rascunho-retrato
  a shell settings put system accelerometer_rotation 0
  a shell settings put system user_rotation 1; sleep 3; step R6-paisagem
  check R6.1 "Após girar continua na tela de ensaios" last_has "$(S trials_title)"
  pyui edits "$OUT/$LAST_STEP.xml" >"$OUT/R6-fields-landscape.txt" || true
  check R6.2 "Após girar o rascunho (1,23 / 1,20) se mantém" bash -c "grep -q '1,23' '$OUT/R6-fields-landscape.txt' && grep -q '1,20' '$OUT/R6-fields-landscape.txt'"
  a shell settings put system user_rotation 0; sleep 3; step R6-retrato-de-novo
  # Morte de processo em segundo plano (simula o sistema liberando memória)
  open_trials
  type_in_edit 0 1,23; type_in_edit 1 1,20; hide_keyboard
  a shell input keyevent KEYCODE_HOME; sleep 2
  local pid1 pid2
  pid1=$(a shell pidof "$SM_PACKAGE" | tr -d '\r')
  # APK de debug: run-as permite SIGKILL com o mesmo uid (funciona também em imagem Play, sem root)
  a shell run-as "$SM_PACKAGE" kill -9 "$pid1" 2>/dev/null || a shell am kill "$SM_PACKAGE"
  sleep 2
  pid2=$(a shell pidof "$SM_PACKAGE" | tr -d '\r')
  note R6.3a "Processo antes=$pid1 depois=${pid2:-morto}"
  a shell am start -n "$SM_ACTIVITY" >/dev/null; sleep 3; step R6-apos-morte-processo
  check R6.3 "Após morte de processo volta à tela de ensaios" last_has "$(S trials_title)"
  pyui edits "$OUT/$LAST_STEP.xml" >"$OUT/R6-fields-after-kill.txt" || true
  note R6.4 "Campos após morte de processo: $(tr '\n' ';' <"$OUT/R6-fields-after-kill.txt")"
  check R6.5 "Sem crash" collect_logs R6
}

for s in "${scenarios[@]}"; do
  STEP_N=$(( (${s#R} * 100) ))
  log "=== $s ==="
  "$s"
done
app_stop
a shell settings put system user_rotation 0 >/dev/null 2>&1
log "Resumo:"; column -t -s $'\t' "$RESULTS_FILE" 2>/dev/null || cat "$RESULTS_FILE"
