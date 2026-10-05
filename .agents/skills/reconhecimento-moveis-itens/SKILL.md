---
name: reconhecimento-moveis-itens
description: Reconhecer e cadastrar móveis, portas, janelas e itens domésticos a partir de imagens, separando detecção visual de identificação dimensional.
---

# Reconhecimento de móveis e itens

Use este skill para detectar objetos em imagens ou vídeo, classificá-los e oferecer cadastro assistido no projeto residencial.

## Fluxo de trabalho

1. Defina taxonomia útil: tipo, subtipo, estado, fixo/móvel, confiança e necessidade de dimensão.
2. Separe detecção (onde há objeto), segmentação (contorno), classificação (o que parece ser) e medição (dimensão em coordenadas do espaço). Uma caixa 2D não mede largura ou profundidade real.
3. Valide APIs e modelos atuais. Detector genérico pode retornar categorias amplas; uma taxonomia fina de sofá, armário, bancada, louça e item pequeno precisa de modelo e dados adequados.
4. Capture com enquadramento e luz suficientes. Evite processar todo frame; limite taxa e acompanhe IDs de tracking sem tratá-los como identidade persistente.
5. Apresente rótulo, confiança, recorte e alternativas. Permita confirmar, editar, rejeitar e criar item não reconhecido.
6. Para dimensões, solicite referência conhecida, profundidade confiável ou entrada manual e mostre incerteza. Não infira catálogo, fabricante ou tamanho exato só pela aparência.
7. Peça confirmação antes de transformar reconhecimento em elemento permanente da planta ou proposta.
8. Avalie por classe com precisão/recall, falsos positivos, falsos negativos e desempenho em imagens reais diversas. Separe teste por ambiente e não misture imagens quase duplicadas entre treino e validação.
9. Defina tratamento local ou remoto, consentimento, retenção e remoção de imagens antes de coletar fotos de interiores.

## Fontes

Consulte `../FONTES.md`. ML Kit Object Detection fornece categorias gerais e suporta classificação customizada; confirme documentação e limites antes de prometer reconhecimento detalhado.
