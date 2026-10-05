# Emuladores e roteiros de uso (SmartMeasure AR)

Scripts bash (git-bash no Windows) para criar os AVDs do projeto, ligar um emulador sem janela, instalar o app e
rodar roteiros de uso com evidências (captura, dump de UI, logcat). Um emulador por vez.

## Pré-requisitos
- Android SDK em `C:/Android/Sdk` (ou `ANDROID_SDK=...`): `emulator`, `platform-tools`, `cmdline-tools/latest`.
- Python 3 no PATH (lê os dumps do `uiautomator` e os `strings.xml`).
- Opcional, para mover a câmera da cena virtual: Python com `grpcio` e `grpcio-tools`
  (`uv venv grpcenv && uv pip install --python grpcenv/Scripts/python.exe grpcio grpcio-tools`).

## AVDs (em `D:/Android/avd`, prefixo `SmartMeasure`)
| AVD | Imagem | Play Store | Uso |
|---|---|---|---|
| `SmartMeasureApi35Play` | android-35-ext14 google_apis_playstore x86_64, pixel_7, câmera traseira `virtualscene` | sim | ARCore no emulador |
| `SmartMeasureApi37` | android-37.0 google_apis x86_64, pixel_7 | não | fallback sem ARCore, targetSdk |
| `SmartMeasureApi24` | android-24 google_apis x86_64, pixel_2 | não | minSdk |

Nunca use, altere ou apague AVDs `Mulletaflix*` (outro projeto).

## Uso
```bash
export SM_OUT_ROOT="$LOCALAPPDATA/hermes/cache/scratch/smartmeasure-usage"   # evidências fora do repositório
tools/emulator/create-avds.sh                       # idempotente; baixa a imagem se faltar
SM_GRPC_PORT=8554 tools/emulator/start-avd.sh SmartMeasureApi35Play -- -change-locale en-US
tools/emulator/install-app.sh --arcore              # --arcore só no AVD com Play Store; --build regera o APK
SM_GRPC_PORT=8554 SM_GRPC_PY=.../grpcenv/Scripts/python.exe \
  tools/emulator/run-scenarios.sh --label Api35Play-en --fresh --expect-arcore ar_ready R1 R2 R3 R4 R6
tools/emulator/stop-avd.sh                          # desliga e mostra adb devices
# idioma: religue com -change-locale pt-BR e rode: run-scenarios.sh --label Api35Play-pt --lang pt R5
```
Roteiros: R1 diagnóstico, R2 modo manual, R3 ensaios (salvar, excluir, exportar, persistência), R4 fluxo AR,
R5 idioma, R6 rotação e morte de processo. Resultado por passo em `<label>/results-*.tsv`; cada passo gera
`NNN-nome.png`, `.xml` (dump) e `.txt` (textos visíveis); logcat por roteiro em `logcat-*.txt`.

Os toques são feitos pelo texto do recurso (`strings.xml` do idioma pedido), nunca por coordenada fixa.
`vscene.py` move a câmera virtual via gRPC (`get`, `pos x y z`, `rot x y z`, `sweep`).

## Armadilhas
- Executáveis Windows não entendem `/d/...`: os scripts convertem caminhos com `cygpath -m`.
- `python3` pode ser o atalho vazio da Microsoft Store; `env.sh` testa antes de usar.
- Com `set -o pipefail`, `cmd | grep -q` pode falhar por SIGPIPE mesmo achando o texto: guarde a saída numa variável.
- Rolar a tela com o teclado aberto digita teclas (ex.: `.`): `hide_keyboard` fecha o IME antes.
- Emulador não mede precisão; a cena virtual só exercita o fluxo.
