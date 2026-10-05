# Fontes primárias consultadas

Pesquisa realizada em 2026-10-05. Reabra os documentos antes de fixar versões, compatibilidade ou requisitos em código e material público.

## Android, AR e visão computacional

- [ARCore Depth API](https://developers.google.com/ar/develop/depth): profundidade estimada por movimento, possível uso de sensor de hardware, suporte varia por dispositivo; documentação indica melhor precisão aproximadamente entre 0,5 m e 5 m.
- [Usar Depth no Android](https://developers.google.com/ar/develop/java/depth/developer-guide): Depth é desabilitada por padrão; verificar suporte antes de habilitar. A própria documentação alerta que nem todos os aparelhos ARCore suportam Depth.
- [Raw Depth API](https://developers.google.com/ar/develop/java/depth/raw-depth): profundidade bruta pode ter maior precisão em pixels disponíveis, mas cobertura incompleta e imagem de confiança; requer interpretação apropriada.
- [Referência ARCore PlaneFindingMode](https://developers.google.com/ar/reference/java/com/google/ar/core/Config.PlaneFindingMode): ARCore pode procurar planos horizontais, verticais ou ambos; deteção de plano não equivale à planta completa.
- [Guia de hit tests ARCore](https://developers.google.com/ar/develop/java/hit-test/developer-guide): modalidades de hit test e limitações de posicionamento.
- [Dispositivos compatíveis com ARCore](https://developers.google.com/ar/devices): lista oficial dinâmica; consultar para o aparelho exato.
- [ML Kit Object Detection](https://developers.google.com/ml-kit/vision/object-detection): detecção e tracking local, categorias gerais e opção de classificação com modelo customizado.
- [ML Kit custom models para Android](https://developers.google.com/ml-kit/vision/object-detection/custom-models/android): modelo pode ser empacotado ou hospedado; atualização e disponibilidade variam pelo método.
- [Jetpack Compose e Views](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose): integrar Views via `AndroidView` quando um componente não tiver equivalente Compose.
- [Room](https://developer.android.com/training/data-storage/room): persistência local estruturada, consultas verificadas em compilação e migrações.
- [Quickstart ARCore Android](https://developers.google.com/ar/develop/java/quickstart): execução em dispositivo suportado/emulador e Google Play Services for AR.

## Limites de aplicação

As fontes acima sustentam capacidades das APIs, não uma garantia de erro dimensional para cômodos nem conformidade arquitetônica brasileira. Não foi possível usar fonte primária aberta que estabeleça limites universais de precisão para levantamento residencial por telefone. Não publicar precisão prometida sem ensaio próprio com instrumento de referência e variedade de aparelhos.
