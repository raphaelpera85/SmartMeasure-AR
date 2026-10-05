# Especialistas do SmartMeasure AR

Toda tarefa de desenvolvimento é despachada ao especialista da área. O agente principal atua como **orquestrador**: classifica a tarefa, despacha, integra, roda o gate final, atualiza a documentação e reporta. Ele não implementa o trabalho de uma especialidade.

## Roteamento

| Tarefa envolve… | Especialista | Brief |
|---|---|---|
| Camadas, repositórios, persistência, ViewModel/StateFlow, Gradle, formatos de dados | Arquitetura Android | `arquitetura-android.md` |
| Sessão ARCore, Depth, hit tests, âncoras, renderização, percurso/cobertura, compatibilidade por API | ARCore e Captura | `arcore-captura.md` |
| Polígonos, paredes, vãos, áreas, perímetro, unidades, confiança, estatística de ensaios | Geometria e Medição | `geometria-medicao.md` |
| Telas Compose, Material 3, UX, acessibilidade, textos e traduções | UI Compose e UX | `ui-compose.md` |
| Conferir trabalho "pronto", revisar diff, testes enfraquecidos, regressões | Qualidade e Verificação | `qualidade-verificacao.md` |
| Fontes, aparelhos compatíveis, protocolos de ensaio, criar ou atualizar skills | Pesquisa de Domínio e Skills | `pesquisa-dominio.md` |
| Emuladores, testes de uso em emulador/aparelho, testes instrumentados (`androidTest`) | Testes em Dispositivo | `testes-dispositivo.md` |

Tarefas que cruzam áreas são divididas em subtarefas por especialista, em ordem de dependência. Exemplo: Geometria define o modelo, Arquitetura persiste, UI exibe e Qualidade verifica.

## Protocolo de despacho (Hermes `delegate_task`)

1. **Contexto completo**, porque o subagente não vê a conversa: raiz do projeto, caminho do brief, a tarefa com critério de pronto, arquivos relevantes, restrições e o idioma da resposta (português).
2. **Instrução fixa:** "Leia `.agents/especialistas/<brief>.md` e `AGENTS.md`, carregue com `skill_view` cada skill obrigatória do brief e siga o TDD."
3. **Paralelismo** só entre especialistas com arquivos disjuntos. Tarefas que tocam os mesmos arquivos rodam em sequência, e dois builds Gradle simultâneos no mesmo diretório disputam o daemon e a pasta `build`.
4. **Verificação:** o relatório do especialista é uma autoavaliação. Toda mudança de código passa pelo especialista de Qualidade (ou pelo gate rodado pelo orquestrador) antes de ser declarada pronta.
5. **Commit:** depois do veredito da Qualidade e do gate verde, o orquestrador faz um commit por tarefa (Conventional Commits, em português ou inglês, com o especialista no corpo) e faz o push para `origin main`. Especialistas não fazem commit nem push.
6. **Registro:** ao final, o orquestrador atualiza `PLANO-PRODUTO.md` e o histórico de `SKILLS.md`, indicando qual especialista fez cada item.

## Especialistas futuros (criar quando a fase chegar)

- **Remodelação e Interiores** (Fase 3): `.agents/skills/design-interiores-residenciais`.
- **Visão Computacional** (Fase 4): `.agents/skills/reconhecimento-moveis-itens`, ML Kit e dataset.
- **Exportação e Documentos** (Fase 1): PDF e imagem cotada, com as skills `pdf` e `xlsx`.
