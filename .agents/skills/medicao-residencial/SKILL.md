---
name: medicao-residencial
description: Planejar e executar levantamento dimensional de ambientes, paredes, vãos, chanfros, perímetros e áreas, preservando incerteza e revisão humana.
---

# Medição residencial

Use este skill para levantar dimensões de um imóvel com trena, câmera, sensor de profundidade ou combinação de métodos. A câmera do telefone não é instrumento de levantamento certificado.

## Fluxo de trabalho

1. Registre aparelho, sensores disponíveis, versão do app, condições de iluminação, superfície, distância, unidade e método.
2. Oriente o usuário a capturar o cômodo completo, caminhar devagar, observar cantos e manter sobreposição visual. Avise que superfícies lisas, escuras, reflexivas, pouca luz e oclusões podem degradar rastreamento ou profundidade.
3. Calibre ou estabeleça escala com medidas conhecidas quando o método exigir; não assuma que todos os aparelhos têm o mesmo sensor ou suporte.
4. Levante cada parede como segmento com endpoints, comprimento, orientação e nível de confiança. Registre altura separadamente.
5. Levante portas e janelas como vãos associados à parede, incluindo posição ao longo do segmento, largura e altura; registrar peitoril e sentido de abertura quando observados.
6. Modele chanfros como segmentos próprios com comprimento e ângulos observados. Não converta um canto cortado em ângulo reto por conveniência.
7. Registre colunas, nichos, recuos, shafts, desníveis, rodapés, vigas aparentes e outros elementos que afetam área ou remodelação.
8. Feche o contorno, detecte deriva e lacunas, compare medidas independentes e peça ao usuário para confirmar ou corrigir pontos de baixa confiança.
9. Calcule perímetro e área em geometria 2D validada. Para polígonos simples, use o contorno interno declarado; inclua ressalva sobre paredes irregulares e convenção de medição.
10. Salve medidas brutas e versão corrigida; nunca silencie correção automática. Exporte cotas, área, perímetro, confiança e pendências.

## Critérios de qualidade

- Mostrar intervalo ou classe de confiança junto à medida automática.
- Não completar parede, canto ou vão oculto sem marcá-lo como inferido.
- Evitar somar área de projeções sobrepostas ou tratar portas como área útil sem convenção explícita.
- Verificar fechamento de polígono, paredes duplicadas, desvios angulares e consistência entre dimensões gerais e segmentos.
- Pedir nova captura quando rastreamento, profundidade ou textura não sustentarem uma estimativa estável.
- Validar desempenho em aparelhos reais contra trena a laser ou instrumento de referência; publicar erro observado por cenário, não só média global.

## Saída mínima

Planta em escala, cotas de paredes e vãos, áreas por cômodo, unidade, método, data, confiança por elemento, discrepâncias e lista de medidas manuais necessárias.

## Fontes

Consulte `../FONTES.md`; suporte e alcance de sensores devem ser rechecados por modelo de aparelho.
