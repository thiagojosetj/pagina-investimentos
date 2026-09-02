# Especificação do projeto

## 1. Visão do produto

O Planejador de Carteira é uma aplicação web pessoal e educacional para registrar uma carteira fictícia, comparar a alocação atual com metas configuradas pelo usuário e planejar novos aportes. O diferencial do portfólio é uma regra financeira pequena, explicável e rigorosamente testada, apoiada por uma aplicação full stack bem documentada.

O produto não pretende substituir corretora, consolidador oficial, sistema contábil ou orientação profissional.

## 2. Objetivos

- Demonstrar domínio prático de Java/Spring e React/TypeScript.
- Evoluir em fatias verticais úteis, compatíveis com sessões de uma a três horas.
- Manter precisão monetária, isolamento entre usuários e rastreabilidade como requisitos arquiteturais.
- Produzir uma experiência clara com dados manuais e sintéticos antes de integrar provedores externos.

## 3. Escopo do MVP

### Entregue no primeiro corte vertical

- Simulação sem persistência com classes editáveis, valores atuais, metas e valor de aporte.
- Comparação visual da alocação atual e projetada.
- Estratégia proporcional ao déficit monetário e sem vendas.
- API versionada, validação, erros uniformes, OpenAPI e testes automatizados.

### Planejado para completar o MVP

- Cadastro e autenticação de usuário.
- Uma ou mais carteiras pertencentes ao usuário autenticado.
- Classes de ativos e metas por carteira.
- Cadastro manual de ativos e movimentações de compra e venda.
- Reconstrução de quantidade, custo e preço médio a partir das movimentações.
- Cotação manual identificada com data/hora e origem `MANUAL`.
- Dashboard com patrimônio, alocação, desvios e acesso ao simulador.
- Autorização no servidor e testes de isolamento entre usuários.

## 4. Fora do MVP

- Integração automática com B3, bancos, corretoras ou Open Finance.
- Scraping ou uso de endpoints internos/não documentados.
- Importação CSV/Excel e exportação.
- Proventos, eventos corporativos complexos e apuração fiscal.
- Resultado realizado com equivalência a regras contábeis ou fiscais oficiais.
- Watchlist, benchmarks, histórico de rentabilidade e rebalanceamento por venda.
- Seleção de ativos, análise de perfil, previsão de preço ou recomendação.
- Múltiplas moedas, consolidação cambial e cotações em tempo real.
- Aplicativo móvel, microserviços, filas, cache distribuído e Kubernetes.

Esses itens permanecem como candidatos pós-MVP; não são funcionalidades prometidas.

## 5. Regra do simulador de aportes

### Entradas

- moeda `BRL`;
- valor do novo aporte em escala monetária de duas casas;
- de 1 a 20 classes com identificador, nome, valor atual e percentual-alvo;
- metas entre `0.0000%` e `100.0000%`, cuja soma deve ser exatamente `100.0000%`.

### Representação e contrato

- O domínio Java converte valores monetários para centavos inteiros (`BigInteger`).
- Percentuais usam unidades de `0.0001` ponto percentual, também em `BigInteger`.
- O JSON recebe e devolve decimais como strings com ponto, evitando perda de precisão no transporte.
- Dinheiro de saída usa duas casas; percentuais usam quatro casas.
- Percentuais derivados apenas para exibição usam `HALF_EVEN` em quatro casas.
- O frontend pode converter valores para `number` somente para largura de gráficos e pistas visuais; o resultado financeiro sempre vem do backend.

### Fórmula

Considere:

- `Cᵢ`: valor atual da classe `i` em centavos;
- `pᵢ`: meta da classe `i`;
- `A`: novo aporte em centavos;
- `T = ΣCᵢ + A`: patrimônio projetado.

Primeiro, o total projetado é repartido pelas metas:

```text
Mᵢ = apportion(T, pᵢ)
Dᵢ = max(Mᵢ - Cᵢ, 0)
Sᵢ = apportion(A, Dᵢ)
```

`Mᵢ` é a meta monetária projetada, `Dᵢ` é o déficit monetário e `Sᵢ` é o aporte sugerido. Classes sem déficit recebem zero. Não são propostas vendas.

`apportion` usa quotas inteiras pelo método dos maiores restos:

1. calcula a parte inteira de cada quota;
2. soma as partes inteiras;
3. entrega os centavos restantes aos maiores restos;
4. em empate exato, usa `classId` em ordem lexicográfica.

Assim, `ΣSᵢ = A` para toda entrada válida e o resultado não depende da ordem de iteração de mapas.

### Exemplos

Com patrimônio atual de R$ 10.000,00, metas `40/25/15/20` e posições `48/18/14/20`:

- aporte de R$ 2.000,00: Ações R$ 0,00; FIIs R$ 1.200,00; ETFs R$ 400,00; Renda fixa R$ 400,00;
- aporte de R$ 500,00: Ações R$ 0,00; FIIs R$ 375,00; ETFs R$ 79,55; Renda fixa R$ 45,45.

O segundo caso não corrige todos os desvios; distribui todo o valor proporcionalmente aos déficits calculados sobre o total projetado.

## 6. Modelo de dados proposto

Nenhuma tabela está implementada. O desenho abaixo deve ser revisado antes da primeira migration.

### Primeira etapa de persistência

- `app_user`: identidade local, e-mail normalizado, credencial derivada e timestamps.
- `portfolio`: nome, moeda-base, proprietário e timestamps.
- `allocation_class`: classe configurada dentro da carteira; nome, ordem visual e vínculo com a carteira.
- `allocation_target`: percentual-alvo vigente por classe e carteira.

Relacionamentos:

```text
app_user 1 ── N portfolio 1 ── N allocation_class 1 ── 1 allocation_target
```

### Etapa de movimentações

- `asset`: ativo cadastrado manualmente, código, nome, tipo e moeda.
- `portfolio_transaction`: lançamento de compra ou venda com ativo, data/hora, quantidade, preço unitário, custos e chave de origem opcional.
- `manual_quote`: cotação informada manualmente, data/hora de referência, moeda e origem explícita.

Relacionamentos esperados:

```text
allocation_class 1 ── N asset
portfolio 1 ── N portfolio_transaction N ── 1 asset
asset 1 ── N manual_quote
```

### Persistido versus derivado

Persistir fatos de entrada: usuários, carteiras, classes, metas, ativos, movimentações e cotações identificadas. Derivar quantidade, custo da posição, preço médio, valor de mercado, patrimônio, desvios e sugestões de aporte.

As posições serão reconstruídas ordenando movimentações por data/hora e identificador estável. A venda não poderá exceder a quantidade disponível. Alterar um lançamento exigirá nova reconstrução; caches só serão considerados quando a medição justificar.

### Precisão proposta no PostgreSQL

- dinheiro consolidado: `NUMERIC(19,2)`;
- preço unitário: `NUMERIC(19,8)`;
- quantidade: `NUMERIC(28,10)`;
- percentual: `NUMERIC(7,4)` com `CHECK` entre 0 e 100;
- moeda: código ISO 4217, inicialmente limitado a `BRL`.

O mapeamento Java usará `BigDecimal` com escala validada nas bordas e tipos de valor no domínio. Nenhuma coluna monetária usará `REAL` ou `DOUBLE PRECISION`.

### Auditoria e idempotência

- `created_at`, `updated_at` e identificadores imutáveis nas entidades mutáveis.
- Registros de movimentação devem preservar autoria e momento de criação.
- Quando importação entrar no escopo, criar `import_batch` com hash do arquivo normalizado e restrição única por carteira; cada linha terá número e chave idempotente.
- Não criar tabelas de importação antes desse incremento.

### Limites conhecidos

- O modelo inicial não cobre desdobramentos, grupamentos, amortizações, subscrições, transferências de custódia ou tributação.
- A metodologia de preço médio será uma simplificação educacional documentada, não uma apuração fiscal oficial.
- A estratégia de correção/remoção de movimentações precisa ser aprovada antes da migration correspondente.

## 7. Requisitos não funcionais

- Responsividade a partir de 320 px e navegação por teclado nos fluxos críticos.
- API sem stack trace ou detalhes internos em respostas.
- Segredos somente em ambiente local/CI; exemplos sempre fictícios.
- Verificações reproduzíveis por wrappers e lockfiles.
- Migrations versionadas e testes com PostgreSQL real quando a persistência entrar.
- `main` estável e publicação somente após aprovação do estado exato.
