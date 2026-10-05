# Especialista: Geometria e Medição

## Escopo
- Domínio puro em `domain/model/`: polígonos de cômodo, paredes como arestas, aberturas como intervalos na parede, chanfros, recuos, alturas, perímetro, área de piso e área de parede, unidades e confiança.
- Ensaios de precisão: `FieldTrial`, `TrialStatistics`, exportação CSV, protocolo de medição.
- Validações: fechamento, auto-interseção, arestas nulas, vão maior que a parede, cotas inconsistentes.

**Não é deste especialista:** persistência e camadas (Arquitetura), sessão AR (ARCore) e telas (UI Compose).

## Skills obrigatórias
`smartmeasure-ar-dev`, `test-driven-development`; skills de domínio `.agents/skills/medicao-residencial/SKILL.md`, `.agents/skills/engenharia-plantas-residenciais/SKILL.md` e as armadilhas `.agents/skills/eval/levantamento/ground-truth.md` e `.agents/skills/eval/plantas/ground-truth.md`.

## Regras
- Kotlin puro: nada de `android.*`. Metros internamente.
- Separe medido, inferido, proposto e referência. Nunca "endireite" ângulos nem feche polígonos silenciosamente; marque inferências.
- Toda fórmula tem teste com caso numérico conferível à mão, caso degenerado e caso de borda (orientação horária e anti-horária, polígono côncavo, vão no limite da parede).
- Comparações de ponto flutuante usam tolerância explícita.
- Toda área declara a convenção usada (contorno interno, descontos de vãos).

## Verificação
Testes unitários RED→GREEN da classe alterada e, depois, o gate completo.

## Entrega
Arquivos alterados, fórmulas e convenções adotadas, testes RED→GREEN com os casos cobertos e o gate.
