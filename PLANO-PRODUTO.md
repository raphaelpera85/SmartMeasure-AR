# SmartMeasure AR — estudo e plano de desenvolvimento

**Estado:** Fase 0 em desenvolvimento. O app Android já tem diagnóstico ARCore/Depth, medição AR ponto a ponto, fallback manual para cômodos retangulares, domínio geométrico puro (perímetro e área) e registro local de ensaios de precisão (AR × referência). Veja o progresso detalhado em "Progresso da Fase 0" e o mapa de skills em [SKILLS.md](SKILLS.md).

**Premissa confirmada:** a captura será feita com o celular que o próprio usuário já possui. Não se pode exigir sensor LiDAR/ToF, modelo premium, trena inteligente ou acessório externo para o fluxo principal.

## Objetivo

Permitir que uma pessoa faça levantamento guiado de cômodos com Android, revise uma planta cotada, corrija paredes e aberturas e crie versões de remodelação. A captura automática deve sugerir geometria com confiança visível; a pessoa confirma antes de virar dado do projeto.

## O que precisa funcionar

1. **Compatibilidade:** verificar instalação/atualização do Google Play Services for AR, suporte ARCore e suporte Depth por aparelho. Planejar modo manual para dispositivos ou sessões sem Depth.
2. **Captura espacial:** acompanhar posição do telefone, orientar percurso e cobertura, observar superfícies/cantos e reunir amostras ao longo do movimento. ARCore Depth usa movimento para estimar profundidade; nem todos os aparelhos suportam Depth e a função é desabilitada por padrão.
3. **Geometria do cômodo:** converter observações em segmentos de parede com comprimento, orientação e confiança; estimar fechamento do perímetro; detectar cantos fora de esquadro, chanfros, recuos e nichos sem “endireitar” dados automaticamente.
4. **Aberturas:** reconhecer ou inserir portas e janelas vinculadas à parede, com afastamento do canto, largura, altura e peitoril. Foto/caixa 2D não basta para garantir dimensões; permitir confirmação e ajuste manual.
5. **Cotas e áreas:** distinguir comprimento de parede, perímetro, área do piso e área de parede. Calcular área a partir de polígonos fechados com convenção indicada; área de parede deve descontar aberturas conforme a convenção selecionada.
6. **Elementos adicionais:** altura do pé-direito, rodapés, pilares, vigas aparentes, armários embutidos, bancadas, shafts, desníveis, tomadas/interruptores e sentido de abertura. São relevantes para orçamento e remodelação, mas podem entrar em fases posteriores.
7. **Editor de planta:** editar paredes, vértices, cotas, vãos e objetos; encaixar com cuidado; desfazer/refazer; duplicar pavimentos; exportar imagem e formato de dados revisável.
8. **Remodelação:** manter planta levantada como versão de referência e criar alternativas separadas. Comparar, nomear, desfazer e renderizar mobiliário/acabamentos em 2D/3D/AR. Sinalizar qualquer alteração de parede estrutural ou instalação para revisão profissional.
9. **Reconhecimento de itens:** tratar detecção, classe, segmentação e medição como tarefas diferentes. ML Kit fornece classes gerais e permite classificador customizado; reconhecimento específico de sofá/armário/mesa requer taxonomia, dados rotulados e validação próprios.
10. **Persistência e privacidade:** permitir uso local/offline, autosave, exportação/backup, consentimento explícito para imagens e política de remoção. Plantas e fotos de interiores são dados sensíveis do lar.

## Recomendação técnica

- **Android nativo:** Kotlin, Jetpack Compose para o produto; integrar componentes AR via Views/renderer quando necessário. ARCore gerencia sessão, câmera, tracking, planos, hit tests e profundidade. Não escolher Unity antes de provar que a necessidade de visualização 3D supera o custo de um stack adicional.
- **Arquitetura:** separar domínio geométrico puro do Android/AR; camadas de captura, domínio (planta, unidades, cálculo), persistência local, editor e apresentação. Persistir projetos estruturados localmente (Room é opção oficial para dados relacionais), com migrações e exportação versionada.
- **Modelo geométrico:** cômodo como polígono 2D; parede como aresta identificada; abertura como intervalo ao longo da aresta; objetos com pose, dimensões e origem; unidades internas em metros e conversão na apresentação. Guardar observações originais e correções como histórico.
- **Fusão:** associar amostras de câmera/depth a superfícies; consolidar arestas repetidas; detectar fechamento e inconsistências; deixar a edição final ao usuário. Não desenhar um scanner totalmente automático antes de medir cobertura e erro em aparelho real.
- **Visão computacional:** começar com detecção assistida e confirmação. Treinar/avaliar modelo customizado só depois de fechar taxonomia e dataset representativo. Para dimensões de móveis, priorizar cadastro manual/catálogo e usar visão como sugestão.
- **Distribuição:** suporte AR e Depth é diferente por aparelho. Consultar a lista ARCore e testar famílias de dispositivos reais antes de definir compatibilidade mínima; evitar tornar Depth requisito absoluto sem aceitar mercado menor.

### Implicações do celular do usuário

- Detectar modelo, sistema, câmera, disponibilidade de ARCore e suporte Depth durante a configuração; explicar o resultado em linguagem simples.
- Não depender de LiDAR nem ToF. ARCore Depth pode estimar profundidade com movimento da câmera, mas processamento, cobertura e suporte variam por aparelho.
- Manter modo de planta manual 2D para aparelho incompatível, sessão degradada ou usuário que não consiga concluir a captura AR. Permitir inserir medidas lidas de qualquer instrumento, sem exigir instrumento adicional.
- Adaptar instruções e qualidade da captura ao aparelho; conservar medidas de origem diferente como tal e não compará-las como se tivessem igual precisão.
- Planejar a prova de conceito com celulares reais de faixas e fabricantes distintos, incluindo modelos comuns no mercado brasileiro. O modelo exato mínimo depende de teste e não foi presumido neste plano.

## Progresso da Fase 0

| Item | Estado | Evidência |
|---|---|---|
| Diagnóstico ARCore, instalação/atualização e Depth | Feito | `data/ar/*`, `presentation/diagnostics/*` |
| Medição AR ponto a ponto (planos, pontos e Depth) | Feito; não validada em aparelho real | `presentation/ar/ArMeasureView.kt` |
| Fallback manual retangular | Feito | `presentation/manual/*` |
| Ensaios de precisão: medida AR × referência, por tipo de medida, Depth e condição; estatística (média, mediana, P90, máximo, viés, erro relativo); armazenamento local; exportação CSV | Feito, coberto por testes unitários; fluxo ainda não exercitado em aparelho | `domain/model/FieldTrial.kt`, `TrialStatistics.kt`, `FieldTrialCsv.kt`, `data/trial/*`, `presentation/trials/*` |
| Resumo da sessão AR gravado em cada ensaio (formato v2 com migração automática da v1; arquivos de versão futura protegidos contra sobrescrita) e exportado no CSV | Feito (Geometria e Arquitetura; verificado pela Qualidade, com ressalvas) | `data/trial/*`, `domain/model/FieldTrial*.kt` |
| Aviso e recuperação (backup sem apagar) de arquivo de ensaios ilegível; resumo da sessão AR em cada ensaio da lista | Feito (Arquitetura e UI; verificado pela Qualidade, com ressalvas); não verificado visualmente | `presentation/trials/*`, `data/trial/*` |
| Revisão de UX/acessibilidade da tela de ensaios (especialista de UI; verificada pela Qualidade, com ressalvas menores) | Feito; não verificada visualmente em aparelho | `presentation/trials/FieldTrialsScreen.kt` |
| Correção de crash em Android 7–10 (`Activity#getDisplay`, API 30) | Feito; confirmado pelo lint `NewApi` | `ArMeasureView.kt` |
| Registro de percurso e qualidade de rastreamento (distância percorrida com filtro de jitter de 2 cm, perdas de rastreamento, tempo rastreando, planos horizontais e verticais), exibido na tela AR | Feito (especialista de ARCore; verificado pela Qualidade, com ressalvas menores); não validado em aparelho | `domain/model/ArSessionPathRecorder.kt`, `presentation/ar/*` |
| Emuladores e roteiros de uso (`tools/emulator/`, R1–R6) nos AVDs `SmartMeasureApi24` e `SmartMeasureApi35Play` (API 37 instável nesta máquina) | Feito; a primeira rodada achou defeitos (abaixo) | `tools/emulator/README.md` |
| Defeitos de uso no emulador: voltar do sistema na tela manual fecha o app (R2.11); girar a tela volta ao diagnóstico e perde o rascunho (R6.1/6.2); morte de processo perde a tela (R6.3); erro do modo manual fixo em inglês (R5.3); "Preparar AR" falha no emulador com erro genérico, sem causa registrada (R4.2) | Em correção pelos especialistas | evidências na pasta scratch `smartmeasure-usage/2026-10-05` |
| Rodada real de ensaios: matriz de aparelhos, cômodos claros/escuros/lisos/reflexivos/pequenos | Pendente; depende de aparelhos físicos e trena laser | Exportar o CSV de cada aparelho |
| Tabela de decisão (requisitos mínimos/fallback) | Pendente; depende da rodada real | — |

**Gate atual:** 105 testes unitários passando, `assembleDebug` e `lintDebug` sem erros.

## Fases sugeridas

### Fase 0 — prova de risco

Protótipo Android que verifica ARCore/Depth, detecta planos horizontal e vertical, registra percurso e apresenta medidas manuais sobre hit tests/depth. Ensaiar cômodos claros, escuros, lisos, reflexivos, pequenos e com obstáculos. Comparar cada dimensão com trena laser e medir falha de tracking, cobertura e erro por modelo.

**Saída para decisão:** tabela de aparelhos testados, erro por tipo de medida, taxa de captura concluída, condições de falha e decisão sobre requisitos mínimos/fallback.

### Fase 1 — planta confiável com revisão manual

Criar projeto/cômodo; captura guiada; segmentos e vãos sugeridos; edição 2D; cálculo de perímetro e áreas; histórico; salvamento local; exportação PDF/imagem e dados do projeto. Medidas automáticas aparecem como estimativas até confirmação.

### Fase 2 — levantamento completo e organização

Adicionar múltiplos cômodos/pavimentos, alinhamento de paredes adjacentes, chanfros e recuos, alturas, colunas, tabelas e objetos fixos. Criar validações para fechamento, sobreposição e cotas inconsistentes.

### Fase 3 — remodelação

Duplicar planta em cenário, mover/remover/adicionar elementos não estruturais conceituais, comparar versões, testar mobiliário e acabamentos em 2D/3D/AR. Avisos encaminham alterações técnicas a profissionais.

### Fase 4 — reconhecimento especializado

Dataset consentido e rotulado; detector/segmentador por categorias priorizadas; cadastro de catálogo; avaliação por classe; estimativa dimensional apenas com profundidade ou referência conhecida; fluxo de correção humana.

## Critérios de validação propostos

Estabelecer limiares depois da Fase 0, com o usuário e com aparelho/uso alvo definidos. Medir erro absoluto e relativo separadamente para parede, abertura e área; cobertura do contorno; taxa de conclusão; edição necessária; precisão/recall por classe; latência, bateria e falhas por condição. Não publicar um valor único de “precisão AR” sem contexto, percentis e aparelhos.

## Decisões que ainda exigem produto

- Público principal: morador, designer, corretor, instalador ou profissional de medição.
- Precisão aceitável e finalidade (estimativa para layout versus orçamento/execução).
- Aparelhos Android mínimos, tratamento de ausência de Google Play Services for AR e suporte offline.
- Entrega inicial e exportação prioritária; necessidade de cadastro de catálogo.
- Se colaboração/cloud é necessária; política para imagens e plantas do imóvel.
- Quais itens devem ser automáticos na primeira versão: parede, porta, janela, chanfro, altura e móveis.

## Riscos e respostas

| Risco | Resposta de produto/engenharia |
|---|---|
| Profundidade indisponível ou instável | Detectar em runtime, fallback e medidas manuais; comunicar estado da sessão. |
| Deriva ao percorrer o cômodo | Fechamento assistido, observações repetidas, referência de escala, alerta e correção manual. |
| Parede lisa, pouca luz, reflexo ou móvel ocluindo canto | Guiar recaptura, marcar trecho inferido e pedir medida de referência. |
| Abertura ou chanfro confundido com parede | Classificação assistida e edição; manter segmento bruto para auditoria. |
| Reconhecimento de móvel não mede dimensões | Pedir catálogo/entrada manual ou referência métrica; não converter caixa visual em medida real. |
| Uso do plano como projeto de obra | Rotular como levantamento/layout conceitual e encaminhar estrutura, instalações e conformidade a profissional. |
| Exposição de imagens de interiores | Processamento local sempre que viável, consentimento e controles de retenção/exportação. |

## Plano de execução após aprovação

1. Entrevistar usuário-alvo e definir casos de uso e limiares de aceitação.
2. Definir matriz de aparelhos e protocolo de medição de referência.
3. Criar prova de conceito ARCore, medir limites e aprovar fallback.
4. Fechar esquema geométrico e formatos de exportação.
5. Implementar MVP de planta editável e persistência local.
6. Adicionar sugestão automática de paredes e vãos com confirmação.
7. Testar em aparelhos/cômodos reais e ajustar limiares.
8. Implementar cenários de remodelação; depois, catálogo e reconhecimento customizado.

## Fontes

Fontes primárias e limitações estão em [.agents/skills/FONTES.md](.agents/skills/FONTES.md). Em particular, a documentação ARCore informa suporte de Depth variável, Depth desligada por padrão e estimativa baseada em movimento. ML Kit requer modelo customizado para categorias mais específicas que as classes gerais. As fontes não estabelecem precisão residencial universal; essa é uma hipótese a medir.

## Próxima etapa

1. Instalar o APK de debug em aparelhos reais e fazer a primeira rodada de ensaios: paredes, vãos e alturas, com e sem Depth, nas condições listadas. Exportar o CSV de cada aparelho.
2. Validar em aparelho: leitura pelo TalkBack (tela de ensaios, chips, resumo da sessão), duplo toque na recuperação, registro de percurso após pausa curta.
3. Usar espaço não separável também nos resultados da medição manual e da tela AR (`area_result`, `perimeter_result`, `ar_distance_result`).
4. Com os dados reais, definir requisitos mínimos de aparelho, política de fallback e limites de aceitação.
