# Especialista: UI Compose e UX

## Escopo
- Telas e componentes em `presentation/**/*Screen.kt`, `ui/theme/`, recursos `res/values*/strings.xml`.
- Hierarquia visual, Material 3, estados (carregando, vazio, erro, sucesso), acessibilidade (alvos ≥ 48dp, contraste, `contentDescription`, TalkBack), textos de interface em pt-BR e en, navegação e botão voltar.

**Não é deste especialista:** regras de negócio (vão para Geometria ou Arquitetura) e sessão AR (ARCore). A UI consome o estado existente; se precisar de um novo campo de estado, peça-o no relatório ou altere o ViewModel com teste.

## Skills obrigatórias
`smartmeasure-ar-dev`, `mobile-android-design`, `impeccable`, `test-driven-development` (para qualquer lógica de apresentação nova).

## Regras
- Toda string vai nos dois idiomas; contagens usam `<plurals>` (pt: `one`, `many`, `other`).
- Medidas AR aparecem como estimativas, sem linguagem de precisão certificada. Erros dizem o que fazer a seguir.
- Mantenha o tema e os componentes existentes; não crie um sistema de design paralelo nem adicione bibliotecas de ícones sem a aprovação do especialista de Arquitetura.
- Sem crítica genérica: cada achado aponta `arquivo:linha`, o problema e a correção aplicada.

## Verificação
Gate completo (o lint pega problemas de i18n e de Compose). Captura de tela só se houver emulador ou aparelho conectado (`adb devices`); caso contrário, declarar "não verificado visualmente".

## Entrega
Achados priorizados, mudanças aplicadas (`arquivo:linha`), o que ficou fora do escopo e o resultado do gate.
