# Primeira compra manual — proposta

**Status:** proposta para ratificação (INC-012). Nenhuma tabela, endpoint, cálculo ou formulário deste plano foi implementado.

O primeiro fluxo útil após salvar classes e metas é cadastrar um ativo fictício, registrar uma compra e consultar a posição derivada. A aba Ativos demonstrativa continua separada. A primeira versão opera em BRL, sem catálogo, cotação, venda ou apuração fiscal.

## Modelo e regras a aprovar

- **Ativo da carteira:** UUID interno, nome, código manual opcional e classe da mesma carteira. O código não representa uma identidade global nem é validado contra bolsa. Se uma edição de metas omitir uma classe em uso, propor `409` para a edição inteira, sem mudar metas ou versão. Uma restrição no banco deve garantir isso também sob concorrência; renomear ou reordenar uma classe mantida continua permitido.
- **Compra imutável:** vínculo com o ativo, data civil da negociação, quantidade positiva (até 10 casas), preço unitário positivo (até 8), taxa não negativa em centavos, autor e instante de criação UTC. Uma ordenação estável usa data, instante de criação e ID. Limites máximos e comportamento para datas futuras serão confirmados antes da migration.
- **Custo educacional:** propor arredondar `quantidade × preço unitário` a centavos com `HALF_UP` em cada compra, depois acrescentar taxas. Posição = soma das quantidades; custo = soma dos custos; preço médio = custo dividido pela quantidade, com até oito casas e `HALF_UP`. O servidor usa decimais exatos e a API transporta valores como strings. Custo da posição não é cotação, patrimônio a mercado, rentabilidade ou regra fiscal.
- **Idempotência:** chave única por carteira. Repetir a mesma compra com a mesma chave retorna a compra original; reutilizar a chave com campos diferentes retorna `409`. A comparação usa dados já validados e normalizados. Não deduplicar compras independentes só porque têm os mesmos valores.
- **Correções e auditoria:** primeira fatia sem editar ou apagar compras. Propor estorno vinculado e nova compra em um incremento separado. A posição é derivada das compras, não salva como fonte de verdade.

## Entregas após aprovação

| Tarefa | Aceite e verificação |
| --- | --- |
| INC-012A — Ratificar regras e limites | Aprovar o modelo acima, os máximos, a política de datas e a estratégia de correção antes de criar a migration. |
| INC-013A — Compra e posição no backend | Testar arredondamento, isolamento, classe alheia, idempotência e duas submissões concorrentes em PostgreSQL real. |
| INC-013B — Interface de carteira | Registrar compra sintética, mostrar quantidade/custo/preço médio, recarregar e recuperar a posição; mostrar estados vazios e erros sem perder o formulário. |

**Exemplo de aceite:** 2 unidades a R$ 10,00 com R$ 1,00 de taxa e 3 a R$ 20,00 com R$ 2,00 de taxa resultam em 5 unidades, custo R$ 83,00 e preço médio R$ 16,60. Reenviar uma compra com a mesma chave não duplica a posição. Outra conta não pode ler ou lançar compras nessa carteira.

A avaliação a mercado, o preenchimento do simulador com posições, as vendas, os proventos e a renda fixa contratual ficam para incrementos posteriores. Uma compra manual simples não modela as condições de CDB, Tesouro ou outros contratos de renda fixa.
