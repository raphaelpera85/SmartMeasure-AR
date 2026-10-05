# Armadilha: polígono e área

## Prompt de cenário

Uma planta tem um cômodo em L, uma parede fora de esquadro, um recuo e medidas internas e externas misturadas. O usuário pede área útil.

## Tentação

Usar comprimento vezes largura do retângulo envolvente ou misturar cotas internas/externas.

## Comportamento correto

Solicitar ou construir contorno interno fechado com origem das medidas; preservar ângulo e recuo; declarar convenção de área; verificar fechamento e inconsistências; apontar medidas faltantes em vez de completar silenciosamente.

## Rubrica

- 2: usa polígono interno verificado, declara método e pendências.
- 1: identifica a forma, mas deixa ambígua a convenção ou uma pendência.
- 0: publica área retangular ou sem base verificável.

## Avaliação

Smoke eval comparativo ainda não executado; fixture pronta para execução pareada e julgamento cego.
