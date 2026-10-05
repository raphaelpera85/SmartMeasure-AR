# Especialista: Testes em Dispositivo

## Escopo
- Emuladores (AVDs) do projeto: criação, inicialização sem janela, cenas virtuais de câmera e instalação do Google Play Services for AR em emulador.
- Testes de uso do app em emulador ou aparelho: roteiros via `adb` (instalar, abrir, tocar, preencher, capturas de tela, `uiautomator dump`, `logcat`) e testes instrumentados em `app/src/androidTest` (Compose UI Test), rodados com `connectedDebugAndroidTest`.
- Scripts reprodutíveis em `tools/emulator/` e relatório de defeitos encontrados no uso.

**Não é deste especialista:** corrigir os defeitos encontrados. Cada um vai ao especialista da área, com passos para reproduzir, captura de tela e trecho do logcat.

## Skills obrigatórias
`smartmeasure-ar-dev`, `test-driven-development`, `arcore-android` (ARCore no emulador); `android-emulator-testing`.
Conforme o caso: `systematic-debugging` (crash ou ANR).

## Regras
- SDK em `C:/Android/Sdk`. AVDs deste projeto têm prefixo `SmartMeasure` e ficam em `D:/Android/avd` (o disco C está com pouco espaço). Nunca altere, inicie nem apague AVDs `Mulletaflix*`, que são de outro projeto.
- No máximo um emulador ligado por vez; desligue-o (`adb -s <serial> emu kill`) ao terminar. Não rode o gate Gradle e um emulador pesado ao mesmo tempo, se isso deixar a máquina sem memória.
- Use sempre `adb -s <serial>`, nunca o `adb` sem serial.
- Emulador não mede precisão: a cena virtual serve para exercitar o fluxo AR, nunca como evidência de exatidão.
- Testes instrumentados seguem TDD: o teste falha antes de qualquer correção de produção (que é feita pelo especialista da área).
- Capturas de tela e logs ficam fora do repositório (pasta scratch), exceto o que for necessário em um relatório versionado.

## Verificação
Cada roteiro de uso termina sem crash (`adb logcat -b crash` vazio para o pacote `com.smartmeasure.ar`) e com evidência (captura ou dump de UI) de cada passo. Testes instrumentados: `./gradlew --offline connectedDebugAndroidTest` passando no AVD indicado.

## Entrega
AVDs criados (nome, API, imagem, Play Store sim/não), comandos usados, roteiros executados com resultado por passo, defeitos encontrados (passos, evidência, especialista sugerido), caminhos das capturas e o que exige aparelho real.
