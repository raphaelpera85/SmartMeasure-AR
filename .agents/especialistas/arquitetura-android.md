# Especialista: Arquitetura Android

## Escopo
- Camadas, pacotes e dependências (`domain/`, `data/`, `presentation/`, `MainActivity` como raiz de composição).
- Interfaces de repositório, persistência local (arquivo hoje; Room com migrações na Fase 1), formatos versionados de dados e exportação estruturada.
- ViewModels, StateFlow, fábricas e injeção manual de dependências.
- Dependências Gradle (`gradle/libs.versions.toml`, `app/build.gradle.kts`).

**Não é deste especialista:** fórmulas geométricas ou estatísticas (Geometria e Medição), código ARCore/GL (ARCore) e layout visual (UI Compose).

## Skills obrigatórias
`smartmeasure-ar-dev`, `android-clean-architecture`, `kotlin-coroutines-flows`, `test-driven-development`.
Conforme o caso: `migration` (mudança de esquema/formato), `safe-refactor` (reestruturação), `lean-build` (funcionalidade nova com risco de excesso).

## Regras
- `domain/` não importa `android.*`; `presentation/` não importa `data/`.
- Todo formato persistido tem versão no cabeçalho e leitura tolerante a linhas corrompidas. Mudança de formato exige migração e teste com o formato antigo.
- Antes de adicionar uma dependência, confira o cache Gradle offline e alinhe com as versões já resolvidas.
- Escritas assíncronas reportam falha no estado da UI; nunca declare sucesso antes do retorno.

## Verificação
Teste unitário da classe alterada e, depois, o gate completo (`AGENTS.md`).

## Entrega
Arquivos alterados (`caminho:linha`), testes RED→GREEN com contagem, resultado do gate e decisões de arquitetura com o motivo de cada uma.
