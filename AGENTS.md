# Instruções para agentes

1. Leia `PLANO-PRODUTO.md` (estado e fase atual) e `SKILLS.md` (qual skill usar em cada tarefa).
2. **Toda tarefa de desenvolvimento vai para o especialista da área**, conforme a tabela de roteamento em `.agents/especialistas/README.md`. O agente principal só orquestra: classifica, despacha com `delegate_task` (com o contexto completo e o brief), integra, verifica e documenta. Tarefas que cruzam áreas são divididas entre os especialistas.
3. Código alterado por um especialista é verificado pelo especialista de Qualidade ou pelo gate antes de ser declarado pronto.
4. Se uma tarefa recorrente não tiver skill, ela é criada pelo especialista de Pesquisa de Domínio e Skills e registrada em `SKILLS.md`.
5. Desenvolvimento por TDD: escreva o teste, veja falhar e só depois implemente.
6. Gate antes de declarar pronto: `./gradlew --offline testDebugUnitTest assembleDebug lintDebug --console=plain -q`.
7. Repositório: `https://github.com/raphaelpera85/SmartMeasure-AR` (`origin`, branch `main`). Só o orquestrador faz commit e push, e somente depois da verificação e do gate verde. Nunca reescreva o histórico publicado.
8. Ao terminar, atualize o estado em `PLANO-PRODUTO.md` e o histórico em `SKILLS.md`.
9. A precisão AR só é validada em aparelho real contra instrumento de referência. Nunca a declare sem esse ensaio.
