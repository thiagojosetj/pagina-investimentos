# Simulação de equalização por classe

O simulador permite optar por **Incluir vendas para equalizar classes**. A opção começa desmarcada. Com ela desmarcada, todo o aporte continua distribuído proporcionalmente aos déficits monetários projetados, conforme a ADR-003.

Com a opção marcada, o cálculo representa transferências hipotéticas entre classes para atingir as metas monetárias do próprio usuário. Nenhuma ordem é executada; o resultado não escolhe ativos, quantidades ou preços e não constitui recomendação de investimento.

## Regra financeira

Para cada classe `i`, em centavos inteiros:

```text
Cᵢ = valor atual
A = novo aporte externo, não negativo
T = ΣCᵢ + A
Mᵢ = apportion(T, pᵢ)
Dᵢ = max(Mᵢ - Cᵢ, 0)
compraᵢ = Dᵢ
vendaᵢ = max(Cᵢ - Mᵢ, 0)
projeçãoᵢ = Cᵢ + compraᵢ - vendaᵢ = Mᵢ
```

As vendas simuladas financiam parte das compras, além do aporte externo. O resultado conserva o patrimônio:

```text
Σcompraᵢ - Σvendaᵢ = A
Σprojeçãoᵢ = T
```

Uma classe nunca tem compra e venda positivas simultaneamente; a venda não excede seu valor atual. Aporte zero é válido e permite simular somente transferências. Uma carteira completamente vazia e sem aporte produz todos os valores iguais a zero. Uma classe com meta zero pode ter venda integral simulada.

## Precisão e desempate

BRL, duas casas monetárias e metas com quatro casas decimais de ponto percentual. A soma das metas deve ser exatamente `100.0000%`, sem tolerância. O domínio calcula em `BigInteger`; dinheiro e percentuais trafegam como strings decimais no JSON.

`apportion` usa a parte inteira de cada quota e distribui os centavos restantes pelos maiores restos, com empate resolvido pelo `classId` em ordem lexicográfica. Assim, as metas monetárias somam exatamente `T` e são independentes da ordem de entrada. Percentuais derivados exibidos usam quatro casas e `HALF_EVEN`; podem diferir minimamente das metas, pois um centavo é indivisível.

## Exemplo sintético

Carteira de R$ 10.000,00, aporte de R$ 500,00 e metas `40/25/15/20`:

| Classe | Valor atual | Meta monetária | Compra simulada | Venda simulada |
| --- | --- | --- | --- | --- |
| Ações | R$ 4.800,00 | R$ 4.200,00 | R$ 0,00 | R$ 600,00 |
| FIIs | R$ 1.800,00 | R$ 2.625,00 | R$ 825,00 | R$ 0,00 |
| ETFs | R$ 1.400,00 | R$ 1.575,00 | R$ 175,00 | R$ 0,00 |
| Renda fixa | R$ 2.000,00 | R$ 2.100,00 | R$ 100,00 | R$ 0,00 |

Compras de R$ 1.100,00 menos vendas de R$ 600,00 correspondem ao aporte externo de R$ 500,00. Desmarcando a opção, a mesma entrada mantém o resultado anterior: aportes de `0 / 375,00 / 79,55 / 45,45`, sem vendas.

## Contrato da API

O endpoint continua sendo `POST /api/v1/allocation-simulations/contributions`.

- `includeSales` é booleano opcional; omitido equivale a `false`.
- A resposta informa `includeSales` e o método: `PROPORTIONAL_MONETARY_DEFICIT_V1` no padrão ou `TARGET_CLASS_REBALANCING_WITH_SIMULATED_SALES_V1` com vendas.
- Cada classe contém `suggestedPurchase` e `suggestedSale`, como strings monetárias não negativas.
- O campo existente `suggestedContribution` mantém a divisão somente do aporte externo, proporcional aos déficits, e sua soma continua igual a `contribution`. No modo com vendas, ele não representa a compra total: a interface usa `suggestedPurchase` e `suggestedSale`. Não se deve somar `suggestedContribution` novamente às compras.
- `projectedAmount` considera compras menos vendas. Sem vendas, `suggestedPurchase = suggestedContribution` e `suggestedSale = "0.00"`.

## Limites

A equalização é matemática por classe. Ela ignora impostos, taxas, spread, liquidez, restrições de resgate, vencimentos e lotes/quantidades de ativos. Não fornece valor líquido de venda nem garante que as operações sejam executáveis. Não registra movimentações ou altera uma carteira salva. Um fluxo futuro de execução ou registro de vendas exige regras próprias; essa opção é apenas uma simulação educacional.
