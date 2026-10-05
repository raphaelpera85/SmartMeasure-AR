---
name: engenharia-plantas-residenciais
description: Produzir, revisar e organizar plantas residenciais existentes e propostas, com geometria rastreável e limites claros entre levantamento e projeto técnico.
---

# Engenharia de plantas residenciais

Use este skill para transformar medidas, croquis, fotos ou uma planta existente em uma representação arquitetônica residencial editável. A saída é documentação de levantamento e layout. Ela não substitui projeto ou responsabilidade técnica de arquiteto ou engenheiro.

## Limites de segurança e autoridade

- Não dimensione, remova, corte ou declare seguro qualquer parede, viga, pilar, laje, fundação ou instalação. Marque dúvidas como **revisão profissional necessária**.
- Não declare conformidade com código, acessibilidade, incêndio ou norma sem abrir a edição aplicável e confirmar jurisdição, uso e data.
- Separe sempre **medido**, **inferido** e **proposto**. Nunca converta uma inferência em medida observada.
- Preserve origem, unidade, precisão estimada, confiança e data de cada medida.
- Se a pessoa pedir autorização para obra estrutural ou avaliação de segurança, encaminhe a um profissional habilitado local.

## Fluxo de trabalho

1. Defina imóvel, pavimento, cômodo, finalidade, usuário da planta, jurisdição e formatos de entrega.
2. Reúna croqui, fotos, medidas de referência, planta anterior e restrições. Registre o que falta.
3. Escolha datum e convenção de coordenadas; registre unidade, escala, norte quando conhecido e tolerância observada.
4. Modele cômodos como contornos fechados, paredes como segmentos com espessura quando conhecida e vãos como elementos associados a uma parede.
5. Verifique fechamento, interseções, duplicatas, continuidade dos encontros, cotas e consistência de áreas. Não force ângulos retos se não foram observados.
6. Identifique portas, janelas, vãos, pilares, recuos, chanfros, mudanças de nível e elementos fixos. Registre largura, altura, peitoril, sentido de abertura e posição quando medidos.
7. Calcule áreas a partir do polígono interno declarado e anote a convenção usada; não misture área bruta e útil.
8. Faça revisão visual e revisão de medidas. Liste divergências, trechos inacessíveis e confiança baixa.
9. Entregue planta cotada, tabela de cômodos e lista de pendências. Mantenha o levantamento original separado de qualquer proposta.

## Verificações

- Cada cota tem unidade e origem.
- Cada contorno é fechado, sem aresta nula ou cruzamento não intencional.
- A soma de segmentos e as dimensões gerais são compatíveis dentro da tolerância declarada.
- Áreas têm método e contorno explícitos.
- Vãos pertencem a paredes identificadas e não excedem o trecho disponível.
- Itens inferidos e conflitos estão visíveis na entrega.

## Fontes

Consulte `../FONTES.md`. Para documentação técnica, abra a fonte primária atual antes de citar regras ou requisitos locais.
