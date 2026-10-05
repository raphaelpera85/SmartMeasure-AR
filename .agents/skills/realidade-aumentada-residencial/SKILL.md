---
name: realidade-aumentada-residencial
description: Projetar captura e visualização espacial residencial em Android com ARCore, fallback, sinais de qualidade e medidas que não escondem incerteza.
---

# Realidade aumentada residencial

Use este skill para levantar ou visualizar espaços internos com câmera Android e ARCore. AR tracking e profundidade estimada dão suporte a uma experiência interativa; não garantem precisão de instrumento profissional.

## Fluxo de trabalho

1. Defina tarefas espaciais e aparelhos mínimos suportados; consulte a lista atual de dispositivos ARCore e suporte de Depth API.
2. Faça uma prova de conceito em aparelhos representativos antes de desenhar o fluxo final. Registre disponibilidade de ARCore, câmera, profundidade e sensores.
3. Escolha plano horizontal/vertical, hit tests, anchors e Depth API conforme tarefa. Depth API é opcional por aparelho e deve ser verificada e habilitada em runtime.
4. Crie fallback de captura guiada e medição manual para aparelhos sem Depth ou com tracking insuficiente.
5. Oriente movimento para cobrir superfícies, cantos e vãos; exponha estado de tracking e qualidade em vez de aceitar silenciosamente uma sessão ruim.
6. Detecte perda de tracking, relocalização, movimento rápido, baixa luz, pouca textura, superfícies reflexivas e oclusão. Pause medidas e solicite nova observação quando necessário.
7. Converta posições do AR para uma planta com origem, escala e unidade persistentes. Não confunda coordenadas de câmera com coordenadas do cômodo.
8. Mescle observações repetidas com regra rastreável, mostre confiança e permita corrigir manualmente.
9. Mantenha renderização AR isolada da tela declarativa; trate ciclo de vida da sessão/câmera e liberação de recursos segundo as instruções atuais do SDK.
10. Compare captura com instrumento de referência em diferentes aparelhos e condições. Informe erro, cobertura, falhas e fallback por cenário.

## Decisões para Android

- UI em Kotlin e Jetpack Compose é uma opção adequada para telas do produto; use interoperabilidade com View apenas onde a superfície do SDK ou renderer exigir.
- ARCore precisa de experiência de sessão, tratamento de permissão/câmera, estados de instalação/atualização e ciclo de vida.
- Não bloqueie a proposta em Depth API: disponibilidade varia, e a documentação descreve depth-from-motion que melhora conforme o aparelho observa a cena em movimento.
- Faça protótipo para paredes verticais; detecção de planos é uma pista geométrica, não uma reconstrução completa de cômodo.

## Fontes

Consulte `../FONTES.md`. Abra docs ARCore atuais e a lista de dispositivos antes de codificar ou divulgar compatibilidade.
