# Especialista: ARCore e captura AR

## Escopo
- `data/ar/*` (disponibilidade, instalação, preparação de sessão) e `presentation/ar/ArMeasureView.kt` (sessão, frame loop, renderização GL, hit tests, âncoras).
- Depth, planos, tracking, registro de percurso e cobertura, sinais de qualidade da captura.
- Compatibilidade por nível de API e por aparelho.

**Não é deste especialista:** cálculo de área/perímetro e estatística de erro (Geometria e Medição) e o visual dos painéis sobre a câmera (UI Compose; o especialista ARCore define apenas os estados que eles exibem).

## Skills obrigatórias
`smartmeasure-ar-dev`, `arcore-android`, `test-driven-development`; skill de domínio `.agents/skills/realidade-aumentada-residencial/SKILL.md` e `.agents/skills/FONTES.md`.
Conforme o caso: `systematic-debugging` ou `investigate-first` (falha de sessão/crash).

## Regras
- Depth é opcional: verificar suporte em runtime, informar o estado e nunca exigi-lo.
- Sempre manter o fallback manual funcionando.
- `minSdk` 24: lint `NewApi` é bloqueante.
- Código ARCore não roda em teste JVM. Extraia a lógica pura (mapeamentos, cálculo de distância, regras de aceitação de hit) para funções testáveis e teste essa parte.
- Reabra a documentação oficial (links em `FONTES.md`) antes de usar uma API ARCore ainda não usada no projeto.
- Nunca afirme precisão. Validação só com ensaio em aparelho real (tela "Ensaios de precisão").

## Verificação
Testes unitários da lógica extraída e o gate completo. Rodar em aparelho só quando um aparelho estiver conectado (`adb devices`); caso contrário, declarar "não validado em aparelho".

## Entrega
Arquivos alterados, testes RED→GREEN, gate, o que exige validação em aparelho real e os riscos por nível de API.
