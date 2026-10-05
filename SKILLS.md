# SmartMeasure AR — mapa de skills

Documento vivo. Atualize ao fim de cada sessão: skills usadas, skills que faltaram e skills criadas. Antes de qualquer tarefa, procure aqui a skill adequada; se a tarefa for recorrente e não houver skill, crie uma (Hermes: `skill_manage`; domínio do produto: `.agents/skills/`) e registre nesta página.

## Skills do domínio (no repositório, `.agents/skills/`)

| Skill | Quando usar |
|---|---|
| `medicao-residencial` | Levantamento de paredes, vãos, chanfros, perímetro e área; ensaios de precisão; confiança das medidas. |
| `engenharia-plantas-residenciais` | Modelo de planta, editor 2D, cotas, separação medido/inferido/proposto, limites técnicos. |
| `realidade-aumentada-residencial` | Captura ARCore, fallback, sinais de qualidade, Depth, validação em aparelho real. |
| `design-interiores-residenciais` | Fase 3 (remodelação, mobiliário, acabamentos). |
| `reconhecimento-moveis-itens` | Fase 4 (detecção/segmentação, dataset, catálogo). |
| `FONTES.md` + `eval/*/ground-truth.md` | Fontes primárias e armadilhas de cada domínio. Releia antes de afirmar compatibilidade ou precisão. |

## Especialistas

O desenvolvimento é dividido entre os especialistas definidos em `.agents/especialistas/` (veja o `README.md` de lá). Cada brief lista as skills obrigatórias daquele especialista; a tabela abaixo mostra as mesmas skills organizadas por tipo de tarefa.

## Skills do agente (Hermes) por tipo de tarefa

| Tarefa | Skills obrigatórias | Complementares |
|---|---|---|
| Qualquer sessão neste projeto | `smartmeasure-ar-dev`, `smartmeasure-especialistas` (despacho para especialistas) | `fable-method` (rotina de trabalho) |
| Funcionalidade nova ou correção de bug | `test-driven-development`, `smartmeasure-ar-dev` | `fable-loop` (tarefas com várias etapas), `lean-build` (risco de construir além do necessário), `surgical-patch` (correção pequena) |
| Arquitetura, camadas, repositórios, use cases | `android-clean-architecture` | `safe-refactor` (reestruturação sem mudar comportamento) |
| ViewModel, StateFlow, repositórios assíncronos e testes com coroutines | `kotlin-coroutines-flows` | — |
| ARCore: sessão, Depth, hit tests, âncoras, renderização | `arcore-android` | `realidade-aumentada-residencial` |
| Telas Compose / Material 3 | `mobile-android-design` | `impeccable` (crítica de UX, hierarquia, acessibilidade, textos de interface) |
| Falha sem causa conhecida | `systematic-debugging` ou `investigate-first` | — |
| Validar trabalho concluído | `fable-judge` (verificação adversarial), `verify-and-stop` | `requesting-code-review` |
| Pesquisa de domínio ou nova skill de domínio | `fable-domain`, `grounded-citations` | `hermes-agent-skill-authoring` |
| Economia de contexto e delegação | `caveman`, `cavecrew` | — |
| Emuladores, testes instrumentados e testes de uso | `android-emulator-testing` | `mobile-android-design` |
| Exportações futuras (PDF/planilha) | `pdf`, `xlsx` | — |

Não se aplica: `kotlin-springboot`, porque o projeto não tem backend Spring.

## Skills criadas para este projeto

| Skill | Motivo |
|---|---|
| `smartmeasure-ar-dev` | Comandos de build, teste e lint; ciclo TDD com stubs `TODO()`; regras de arquitetura; armadilhas do ambiente Windows/git-bash. |
| `arcore-android` | Não havia skill de ARCore. Registra ciclo de vida da sessão, Depth opcional, hit tests e a armadilha `getDisplay` (API 30) com `minSdk` 24. |
| `kotlin-coroutines-flows` | Já era citada por `android-clean-architecture`, mas não existia. Cobre StateFlow, repositório com `Mutex` e testes com `Dispatchers.setMain`. |
| `android-emulator-testing` | AVDs do projeto (API 24 e API 35 com Play/ARCore; API 37 instável nesta máquina), boot sem janela, instalação, captura de tela e `connectedDebugAndroidTest`. |
| `smartmeasure-especialistas` | Roteia toda tarefa ao especialista da área; inclui um modelo de contexto para `delegate_task`. |

## Lacunas conhecidas (criar quando a tarefa surgir)

- **Room/migrações**: quando o projeto/planta persistente substituir o arquivo de ensaios (Fase 1). Use também `migration`.
- **Exportação de planta (PDF/imagem cotada)**: Fase 1.
- **Protocolo de ensaio em campo**: a matriz de aparelhos e o roteiro de medição com trena laser podem virar skill de domínio em `.agents/skills/` depois da primeira rodada real.

## Histórico de uso

### 2026-10-05 — ensaios de precisão da Fase 0
- **Usadas:** `fable-loop`, `android-clean-architecture`, `test-driven-development`; skills de domínio `medicao-residencial`, `engenharia-plantas-residenciais` e `realidade-aumentada-residencial` (lidas no início).
- **Invocadas, mas não carregadas em detalhe nesta sessão:** `fable-method`, `fable-judge`, `fable-domain`, `mobile-android-design`, `impeccable`, `caveman`, `cavecrew`. 
- **Criadas:** `smartmeasure-ar-dev`, `arcore-android`, `kotlin-coroutines-flows`.
- **Especialistas criados:** Arquitetura Android, ARCore e Captura, Geometria e Medição, UI Compose e UX, Qualidade e Verificação, Pesquisa de Domínio e Skills.
- **Primeiro ciclo com especialistas:** UI Compose e UX revisou a tela de ensaios (`mobile-android-design`, `impeccable`); Qualidade e Verificação conferiu (`fable-judge`, `verify-and-stop`) e deu o veredito VERIFICADO COM RESSALVAS. O gate passou: 34 testes e lint sem erros. As ressalvas foram encaminhadas a Geometria e Medição e a UI.
- Geometria e Medição levou o critério de amostra representativa para o domínio (`TrialSummary.isRepresentative`, por TDD), e o orquestrador verificou com o gate: 38 testes, lint sem erros. O repositório git foi ligado a `github.com/raphaelpera85/SmartMeasure-AR`.
- UI Compose e UX corrigiu as 7 ressalvas da tela de ensaios: critério de amostra no domínio, `TrialFormatting` feito por TDD (sem "-0,0"), textos sem concatenação no código, data acompanhando o idioma, descrição de excluir diferenciada, chips com semântica de escolha única e "profundidade" em pt-BR. Qualidade: VERIFICADO COM RESSALVAS, com 45 testes. A prova por mutação mostrou que os testes pegam a formatação ingênua.
- ARCore e Captura implementou o registro de percurso e qualidade de rastreamento (`arcore-android`, `lean-build`, TDD com 10 testes). Qualidade: VERIFICADO COM RESSALVAS, com 55 testes; a prova por mutação mostrou que os testes deixaram passar 2 de 8 mutações, e as lacunas foram encaminhadas.
- ARCore e Captura corrigiu as ressalvas do registro de percurso: borda de 2 cm com tolerância de float, testes que pegam as 2 mutações que passavam, pausa curta via `markInterrupted` com flag atômica no GL thread. O orquestrador verificou pelo diff e pelo gate: 61 testes.
- Geometria e Medição colocou o resumo da sessão AR no ensaio (`FieldTrial.session`), com as invariantes em `ArSessionSummary`, e acrescentou 6 colunas no fim do CSV, por TDD. O orquestrador verificou pelo diff e pelo gate: 75 testes.
- Arquitetura Android criou o formato v2 dos ensaios, com migração da v1, a regra de manter o ensaio quando só a sessão é inválida e a proteção contra sobrescrever arquivo de versão futura (`migration`, TDD com 12 testes). Qualidade: VERIFICADO COM RESSALVAS, com 87 testes; 7 de 8 mutações detectadas.
- Arquitetura Android implementou a recuperação de arquivo de ensaios ilegível: estado `UNREADABLE` exposto e backup que nunca apaga nem sobrescreve; também cobriu as lacunas de arquivo vazio e de falha ao excluir, com prova por mutação. O orquestrador verificou pelo diff e pelo gate: 102 testes.
- UI Compose e UX criou o aviso e o diálogo de recuperação do arquivo de ensaios e passou a mostrar o resumo da sessão em cada ensaio; os formatadores `pathMeters` e `wholePercent` foram feitos por TDD. Qualidade: VERIFICADO COM RESSALVAS, com 105 testes; a mutação de arredondamento comum no lugar do arredondamento para baixo foi detectada.
- O orquestrador criou o AVD `SmartMeasureApi24` e validou os AVDs: o app abre na API 24 e na API 35 (Play, ARCore instalado); a API 37 está instável nesta máquina (loop de crash do surfaceflinger). Foram criados a skill `android-emulator-testing` ; o especialista Testes em Dispositivo e os roteiros `tools/emulator/` (de uma sessão anterior) foram mantidos.
- Testes em Dispositivo configurou os testes instrumentados (Compose UI Test) com 5 testes de caminho feliz, verdes em 2 execuções, e 4 testes de defeito em RED (R6.1, R6.2, R2.11, R5.3), conferidos pelo orquestrador no emulador da API 35.
- Arquitetura Android corrigiu a navegação e o estado (R2.11, R6.1, R6.2, R6.3) com `SavedStateHandle`, por TDD. Qualidade: VERIFICADO COM RESSALVAS; reproduziu a morte de processo e mostrou por mutação que falta um teste para o "voltar" no diagnóstico.
- UI Compose e UX corrigiu o R5.3 com um erro tipado no ViewModel e o texto vindo de `stringResource`, por TDD, e removeu um BackHandler redundante. O orquestrador verificou pelo diff, pelo gate (119 testes) e pelos instrumentados (9/9).
