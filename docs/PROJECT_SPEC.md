# Especificação do projeto

## 1. Visão do produto

O Planejador de Carteira é uma aplicação web pessoal e educacional para acompanhar uma carteira por categorias e ativos, registrar movimentações, visualizar indicadores e planejar novos aportes. O simulador de alocação é um dos módulos da plataforma, não o produto inteiro. O diferencial do portfólio combina regras financeiras pequenas, explicáveis e rigorosamente testadas com uma aplicação full stack bem documentada.

O produto não pretende substituir corretora, consolidador oficial, sistema contábil ou orientação profissional.

## 2. Objetivos

- Demonstrar domínio prático de Java/Spring e React/TypeScript.
- Evoluir em fatias verticais úteis, compatíveis com sessões de uma a três horas.
- Manter precisão monetária, isolamento entre usuários e rastreabilidade como requisitos arquiteturais.
- Produzir uma experiência clara com dados manuais e sintéticos antes de integrar provedores externos.

## 2.1. Visão funcional em evolução

A experiência principal desejada é um dashboard capaz de consolidar, com origem e data de referência explícitas:

- patrimônio e eventual saldo em caixa;
- posições separadas por categoria e por ativo;
- quantidade, preço médio, custo, cotação e valor de mercado de cada posição;
- resultado realizado e não realizado segundo metodologia educacional documentada;
- proventos recebidos e indicadores de dividendos com período e fórmula identificados;
- alocação atual, meta e desvio por classe e por ativo;
- gráficos de composição e evolução;
- acesso ao simulador de novos aportes por preenchimento manual ou por uma fotografia da carteira persistida do próprio usuário.

Rentabilidade, saldo, dividend yield e resultado possuem mais de uma definição válida. As fórmulas e o tratamento de compras, vendas, custos, proventos e períodos precisam ser aprovados antes da implementação. Alertas de desvio e simulações podem explicar a distância até as metas do usuário, mas não selecionar ativos nem se apresentar como recomendação financeira.

Quando houver carteira persistida, o simulador deverá oferecer “usar minha carteira” e carregar posições, avaliações e metas sem exigir nova digitação. A primeira evolução continuará distribuindo o aporte entre classes; distribuir entre ativos só será permitido quando o próprio usuário definir metas ou outra regra explícita para cada ativo, evitando transformar a simulação em seleção implícita de investimentos.

Caixa aparece na demonstração apenas como hipótese de interface. O modelo persistido de conta e movimentações de caixa ainda não foi aprovado; enquanto ele não existir, telas conectadas deverão usar o termo “valor de mercado das posições” e não inferir saldo de caixa.

## 3. Escopo do MVP

### Entregue no primeiro corte vertical

- Simulação sem persistência com classes editáveis, valores atuais, metas e valor de aporte.
- Comparação visual da alocação atual e projetada.
- Estratégia proporcional ao déficit monetário e sem vendas.
- API versionada, validação, erros uniformes, OpenAPI e testes automatizados.

### Incremento demonstrativo subsequente

- Visão geral sem persistência, com valores sintéticos por categoria e item.
- Total do cenário sintético separado entre posições e uma hipótese visual de caixa.
- Filtro por Ações, FIIs, ETFs, Renda fixa e Caixa.
- Navegação entre a visão geral e o simulador.
- Modos claro e escuro com preferência local e respeito à configuração inicial do sistema.

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

## 6. Modelo de dados inicial e evolução proposta

A migration `V1__create_identity_and_portfolio_tables.sql` implementa somente a fundação aprovada abaixo. O incremento implementado e validado localmente acrescenta mapeamentos JPA, repositories e um serviço transacional interno para carteira e classes; nenhuma operação pública foi criada para esses registros.

### Primeira etapa de persistência implementada

- `app_user`: UUID interno, nome de exibição, versão otimista e timestamps; nenhuma senha local foi criada antecipadamente.
- `external_identity`: vínculo com usuário, `provider`, `subject`, e-mail recebido do provedor, estado de verificação, versão e timestamps. O par `provider + subject` é único; e-mail não identifica a conta e pode se repetir.
- `portfolio`: UUID, nome, moeda-base `BRL`, proprietário, versão e timestamps. O nome é único por proprietário sem diferenciar maiúsculas e minúsculas.
- `allocation_class`: classe configurada dentro da carteira, nome, ordem visual, percentual-alvo vigente, versão e timestamps. Nome e ordem são únicos dentro da carteira; o percentual usa `NUMERIC(7,4)` entre `0.0000` e `100.0000`.

Os UUIDs são gerados pela aplicação, sem extensão específica no banco. Timestamps usam `TIMESTAMPTZ` e o backend opera em UTC. As chaves estrangeiras usam deleção restrita até que retenção, auditoria e exclusão de conta/carteira sejam decididas; não existe hard delete exposto. A camada de persistência mantém `updated_at` explicitamente.

A soma de metas e o limite de uma a vinte classes são invariantes entre várias linhas. O serviço interno substitui o conjunto completo em uma transação, valida a soma exata `100.0000` com escala de quatro casas e exige ownership em todas as operações. A escrita usa compare-and-set sobre a versão da carteira para rejeitar edição concorrente; um `CHECK` isolado não consegue garantir essas regras.

Nesta primeira implementação, a substituição integral remove as metas anteriores e cria novas linhas com novos UUIDs. Essa semântica é interna e temporária: a estabilidade dos identificadores deverá ser decidida e implementada antes de `portfolio_asset` possuir uma chave estrangeira para `allocation_class` ou de IDs de metas integrarem um contrato público.

Relacionamentos:

```text
app_user 1 ── N external_identity
app_user 1 ── N portfolio 1 ── N allocation_class
```

### Catálogo, ativos da carteira e movimentações

- `instrument`: identidade pública e opcional de um ativo listado selecionado em provedor aprovado; usa símbolo mais MIC/bolsa, nome, moeda, tipo normalizado e referência externa. Não será criado antes da integração real.
- `portfolio_asset`: vínculo privado e obrigatório entre carteira, classe e item acompanhado. Pode referenciar um `instrument` listado ou representar renda fixa cadastrada manualmente.
- `fixed_income_terms`: detalhes 1:1 adicionados somente na fatia de renda fixa, como tipo, emissor, vencimento, indexador, taxa e modo unitário ou nocional.
- `position_movement`: fato imutável de aquisição ou alienação com data/hora, valor bruto, custos, quantidade e preço quando aplicáveis, origem e chave idempotente.
- `manual_valuation`: avaliação privada com tipo `UNIT_PRICE` ou `TOTAL_VALUE`, fonte e instante de referência.
- `income_event`: provento privado introduzido em incremento próprio; não altera quantidade nem custo médio.

Relacionamentos esperados:

```text
instrument 0..1 ── N portfolio_asset N ── 1 portfolio
allocation_class 1 ── N portfolio_asset
portfolio_asset 1 ── N position_movement
portfolio_asset 1 ── N manual_valuation
portfolio_asset 1 ── N income_event
portfolio_asset 1 ── 0..1 fixed_income_terms
```

Ticker isolado não será chave global, pois pode colidir entre bolsas. Resultados externos serão identificados ao menos por símbolo e MIC; a classificação escolhida pelo usuário permanece no `portfolio_asset`, não no catálogo do provedor.

Renda fixa nacional deverá admitir inicialmente `TREASURY`, `CDB`, `LCI`, `LCA`, `CRI`, `CRA`, `DEBENTURE` e `OTHER`. Aplicações do mesmo produto com taxas ou vencimentos diferentes serão itens separados. Esses termos começam descritivos: rentabilidade contratada, accrual, dias úteis, cupons, impostos e marcação a mercado exigem regras próprias antes de qualquer cálculo automático.

Caixa não fará parte da primeira migration. Quando entrar, será modelado por `cash_account` e fatos em `cash_movement`; o saldo será derivado. Até lá, o dashboard conectado mostrará somente o valor das posições, conforme aprovado pelo usuário.

### Persistido versus derivado

Na fundação atual, o schema admite identidades, carteiras, classes e metas. As próximas migrations persistirão apenas fatos de entrada aprovados: vínculos privados de ativos, termos contratados, movimentações, avaliações e proventos identificados. Quantidade ou principal, custo da posição, preço médio, valor atual, resultados, saldo de caixa, totais por classe, desvios e sugestões de aporte continuarão derivados.

As posições serão reconstruídas ordenando movimentações por data/hora e identificador estável; nenhuma tabela `position` será criada inicialmente. A venda não poderá exceder a quantidade disponível. Correções deverão preservar auditoria e provocar nova reconstrução; caches ou snapshots só serão considerados quando a medição justificar.

### Precisão proposta no PostgreSQL

- dinheiro consolidado: `NUMERIC(19,2)`;
- preço unitário: `NUMERIC(19,8)`;
- quantidade: `NUMERIC(28,10)`;
- taxa contratada: `NUMERIC(13,8)` com unidade explícita;
- percentual: `NUMERIC(7,4)` com `CHECK` entre 0 e 100;
- moeda: código ISO 4217, inicialmente limitado a `BRL`.

O mapeamento Java das metas usa `BigDecimal` com escala validada nas bordas; os futuros valores monetários continuarão exigindo tipos de valor no domínio. Nenhuma coluna monetária usará `REAL` ou `DOUBLE PRECISION`.

### Auditoria e idempotência

- `created_at`, `updated_at`, `version` e identificadores imutáveis nas entidades mutáveis da primeira migration.
- Registros de movimentação devem preservar autoria e momento de criação.
- Quando importação entrar no escopo, criar `import_batch` com hash do arquivo normalizado e restrição única por carteira; cada linha terá número e chave idempotente.
- Não criar tabelas de importação antes desse incremento.

### Limites conhecidos

- O modelo inicial não cobre desdobramentos, grupamentos, amortizações, subscrições, transferências de custódia ou tributação.
- Instrumento global, renda fixa, movimentações, caixa, avaliações e proventos não pertencem à primeira migration; o desenho apenas reserva um caminho de evolução.
- A substituição atual recria os IDs das metas e precisa ser revisada antes da associação com ativos ou da exposição desses IDs em contrato público.
- A metodologia de preço médio será uma simplificação educacional documentada, não uma apuração fiscal oficial.
- A estratégia de correção/remoção de movimentações precisa ser aprovada antes da migration correspondente.

## 7. Requisitos não funcionais

- Responsividade a partir de 320 px e navegação por teclado nos fluxos críticos.
- API sem stack trace ou detalhes internos em respostas.
- Segredos somente em ambiente local/CI; exemplos sempre fictícios.
- Verificações reproduzíveis por wrappers e lockfiles.
- Migrations Flyway versionadas e imutáveis; estrutura e mapeamentos JPA devem ser validados pelo Hibernate em PostgreSQL real antes de concluir o INC-008.
- `main` estável e publicação somente após aprovação do estado exato.
