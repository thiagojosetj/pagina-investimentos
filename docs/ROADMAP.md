# Roadmap

O roadmap usa fatias verticais de aproximadamente uma a três horas. Itens futuros são propostas; decisões materiais continuam sujeitas à aprovação descrita em `AGENTS.md`.

## Fase 0 — Fundação e primeira fatia

### INC-001 — Estrutura e documentação inicial

- **Status:** concluído.
- **Objetivo:** criar uma raiz reproduzível, documentar produto, decisões e ambiente.
- **Dependências:** stack e escopo inicial aprovados.
- **Critérios de aceite:** arquivos obrigatórios presentes; exemplos sintéticos; comandos sem caminhos absolutos.
- **Verificações:** revisão de links, busca de secrets e validação dos XMLs/YAML.
- **Definition of Done:** documentação reflete somente o que existe e a raiz abre no IntelliJ.

### INC-002 — Regra pura de alocação

- **Status:** concluído.
- **Objetivo:** distribuir um aporte proporcionalmente aos déficits monetários projetados.
- **Dependências:** ADR-003 e ADR-004.
- **Critérios de aceite:** sem ponto flutuante; sem vendas; soma das sugestões igual ao aporte; desempate determinístico.
- **Verificações:** casos de R$ 2.000, R$ 500, carteira vazia, aporte zero, um centavo, entradas inválidas e invariantes.
- **Definition of Done:** domínio independente de HTTP com testes verdes e regra documentada.

### INC-003 — API do simulador

- **Status:** concluído.
- **Objetivo:** expor a regra por um contrato HTTP versionado.
- **Dependências:** INC-002.
- **Critérios de aceite:** DTOs explícitos, strings decimais, validação, Problem Details e OpenAPI.
- **Verificações:** MockMvc para sucesso, soma inválida, decimal inválido, item nulo, percentual fora do limite e JSON malformado.
- **Definition of Done:** endpoint empacotado pelo Maven e contrato navegável no Swagger UI.

### INC-004 — Interface funcional do simulador

- **Status:** concluído.
- **Objetivo:** entregar uma tela responsiva que consuma a API real.
- **Dependências:** INC-003.
- **Critérios de aceite:** editar/adicionar/remover classes, normalizar vírgula, mostrar loading/empty/error/success e comparar alocação atual/projetada.
- **Verificações:** Prettier, Oxlint, TypeScript, Vitest, Testing Library e build Vite.
- **Definition of Done:** fluxo crítico testado e nenhuma regra financeira duplicada no navegador.

### INC-005 — CI e primeira publicação

- **Status:** concluído.
- **Objetivo:** publicar o repositório e validar os dois módulos no GitHub Actions.
- **Dependências:** INC-001 a INC-004 e aprovação do estado exato do push.
- **Critérios de aceite:** repositório público vazio criado, `origin` correto, push normal de `main`, jobs backend/frontend verdes.
- **Verificações:** comparar `HEAD` local/remoto, inspecionar execução real e confirmar árvore limpa.
- **Definition of Done:** URL clonável e primeira CI concluída com sucesso; não basta o YAML existir.

## Fase 1 — Persistência de carteiras e metas

### INC-006 — Ratificar o primeiro modelo persistido

- **Status:** concluído.
- **Objetivo:** revisar usuários, carteiras, classes e metas antes da migration.
- **Dependências:** modelo proposto em `PROJECT_SPEC.md` e aprovação do usuário.
- **Critérios de aceite:** entidades, ownership, escalas, constraints, auditoria e dados derivados definidos.
- **Verificações:** revisão de ownership, exclusão restrita, concorrência otimista, escalas, índices e evolução do schema.
- **Definition of Done:** decisão registrada sem criar tabela antecipadamente.

### INC-007 — PostgreSQL, Flyway e teste de integração

- **Status:** concluído.
- **Objetivo:** conectar a API ao banco e aplicar a primeira migration versionada.
- **Dependências:** INC-006.
- **Critérios de aceite:** configuração por ambiente, migration imutável, healthcheck e isolamento do Compose.
- **Verificações:** Testcontainers com PostgreSQL 18.6 real, startup do contexto, aplicação da V1, constraints principais e `docker compose config`.
- **Definition of Done:** clone limpo cria o schema pelo Flyway, sem `db push`, DDL do Hibernate ou alteração manual.

### INC-008 — Carteira, classes e metas na camada de aplicação

- **Status:** concluído localmente; ainda sem endpoint público por decisão de escopo.
- **Objetivo:** persistir e consultar uma carteira fictícia com suas metas na camada de aplicação, ainda sem expor operações anônimas.
- **Dependências:** INC-007.
- **Critérios de aceite:** validação de uma a vinte classes e soma exata `100.0000`, transação integral, compare-and-set pela versão da carteira e ownership obrigatório em toda operação; nenhum usuário fixo, controller ou endpoint provisório.
- **Verificações:** cinco testes puros da validação das metas e oito testes de serviço/repository com PostgreSQL real aprovados; suíte backend completa com 37 testes e zero falhas.
- **Definition of Done:** atendida — os dados são relidos em nova transação, ownership/concorrência/rollback foram validados e a camada de aplicação não expõe entidades JPA.
- **Limite conhecido:** a substituição integral atual recria os UUIDs das metas; revisar essa decisão antes de `portfolio_asset` ou de um contrato público depender desses IDs.

### INC-009 — Persistência na interface

- **Status:** proposto.
- **Objetivo:** carregar e editar as metas da carteira no fluxo existente.
- **Dependências:** INC-008 e INC-011.
- **Critérios de aceite:** estados de carregamento, conflito, erro e confirmação; nenhuma perda silenciosa.
- **Verificações:** testes de componente e integração com fake da API.
- **Definition of Done:** recarregar a página recupera a configuração persistida.

## Fase 2 — Identidade e isolamento

### INC-010 — Decidir autenticação e ameaças básicas

- **Status:** Google OpenID Connect e sessão backend aprovados; threat model e configuração ainda pendentes.
- **Objetivo:** detalhar Google OpenID Connect, criação da conta interna e sessão por cookie sem expor secrets ao frontend.
- **Dependências:** INC-007.
- **Critérios de aceite:** redirects permitidos, vínculo por `provider + subject`, CSRF, cookies, expiração, rate limit, logout e ownership documentados.
- **Verificações:** threat model pequeno e revisão dos contratos públicos afetados.
- **Definition of Done:** fluxo e configuração aprovados antes de implementar a identidade externa.

### INC-011 — Autenticação e autorização vertical

- **Status:** proposto.
- **Objetivo:** entrar com Google, criar ou localizar a conta interna, sair e proteger uma carteira do usuário.
- **Dependências:** INC-008 e INC-010.
- **Critérios de aceite:** identidade validada pelo backend, sessão segura e verificação de ownership no servidor; e-mail não é usado como identificador imutável.
- **Verificações:** testes de autenticação, CSRF, usuário anônimo e tentativa de acesso cruzado.
- **Definition of Done:** usuário A não consegue ler ou alterar recurso do usuário B.

## Fase 3 — Ativos e movimentações manuais

### INC-012 — Ratificar modelo de movimentações

- **Status:** proposto; aprovação obrigatória.
- **Objetivo:** definir ativos, compras, vendas, custos, ordenação, correções e auditoria.
- **Dependências:** INC-011.
- **Critérios de aceite:** escalas numéricas, invariantes e limites educacionais explícitos.
- **Verificações:** exemplos de compra parcial, múltiplas compras, venda e venda acima da posição.
- **Definition of Done:** regra de reconstrução aprovada antes da migration.

### INC-013 — Compra manual e posição derivada

- **Status:** proposto.
- **Objetivo:** cadastrar compra e mostrar quantidade, custo e preço médio derivados.
- **Dependências:** INC-012.
- **Critérios de aceite:** operação atômica, DTO validado e reconstrução determinística.
- **Verificações:** testes puros de preço médio, persistência e endpoint.
- **Definition of Done:** fatia backend/frontend funcional com dados fictícios.

### INC-014 — Venda manual e validações

- **Status:** proposto.
- **Objetivo:** registrar venda sem permitir posição negativa.
- **Dependências:** INC-013.
- **Critérios de aceite:** quantidade disponível verificada no servidor; simplificação de resultado claramente rotulada.
- **Verificações:** venda total/parcial, ordem temporal e concorrência básica.
- **Definition of Done:** posição reconstruída corretamente e teste de regressão para overselling.

### INC-015 — Cotação manual e dashboard

- **Status:** proposto.
- **Objetivo:** informar cotação com timestamp/origem e alimentar valor de mercado das posições, desvios e simulador persistido.
- **Dependências:** INC-013 e INC-014.
- **Critérios de aceite:** fonte `MANUAL`, possível defasagem visível e estados sem cotação tratados.
- **Verificações:** regras de valuation, API, UI responsiva e isolamento entre usuários.
- **Definition of Done:** primeiro fluxo estável de cadastro até planejamento do aporte.

## Fase 4 — Acompanhamento da carteira

Os itens desta fase refletem a visão desejada, mas a divisão entre MVP e pós-MVP ainda precisa ser ratificada.

### INC-016 — Definir indicadores e fórmulas

- **Status:** proposto; aprovação obrigatória.
- **Objetivo:** definir patrimônio, saldo, custo, preço médio, resultados, rentabilidade e indicadores de proventos sem ambiguidade.
- **Dependências:** modelo de movimentações do INC-012.
- **Critérios de aceite:** período, fórmula, escala, arredondamento e tratamento de compras, vendas, custos e proventos documentados.
- **Verificações:** exemplos sintéticos normais e casos-limite calculados manualmente.
- **Definition of Done:** fórmulas aprovadas antes de aparecerem na API ou na interface.

### INC-017 — Dashboard por categoria e ativo

- **Status:** proposto; demonstração visual sintética implementada localmente, sem persistência ou cotações.
- **Objetivo:** consolidar valor das posições, resultados e alocação em uma superfície responsiva; caixa só compõe o saldo depois do INC-023.
- **Dependências:** INC-015 e INC-016.
- **Critérios de aceite:** visão por categoria e ativo, estados sem dados/cotação e origem dos valores visível.
- **Verificações:** testes de domínio, endpoint, componente e integração frontend/backend.
- **Definition of Done:** o dashboard reconcilia seus totais com as movimentações e cotações persistidas.

### INC-018 — Proventos manuais e indicadores

- **Status:** proposto.
- **Objetivo:** registrar proventos e exibir totais por ativo e período.
- **Dependências:** INC-013 e INC-016.
- **Critérios de aceite:** tipo, data de referência, data de pagamento, valor e moeda validados; fórmulas claramente rotuladas.
- **Verificações:** dividendos, JCP, rendimentos, estorno e períodos sem pagamento; amortizações exigem um incremento próprio.
- **Definition of Done:** os indicadores podem ser reproduzidos a partir dos fatos persistidos.

### INC-019 — Pesquisa e contrato do primeiro provedor de mercado

- **Status:** pesquisa inicial concluída; shortlist e contrato ainda exigem aprovação.
- **Objetivo:** selecionar uma fonte legalmente adequada para metadados e cotações com fallback manual.
- **Dependências:** universo inicial de ativos aprovado.
- **Critérios de aceite:** documentação oficial, termos, licença, custo, cobertura, defasagem, rate limit e atribuição registrados.
- **Verificações:** prova técnica isolada, timeout, indisponibilidade, símbolo ausente e fake determinístico.
- **Definition of Done:** provedor e contrato aprovados antes de integrar dados externos ao domínio.

### INC-020 — Gráficos de composição e evolução

- **Status:** proposto.
- **Objetivo:** visualizar alocação por classe/ativo e evolução dos indicadores aprovados.
- **Dependências:** INC-017 e histórico suficiente.
- **Critérios de aceite:** unidades, períodos, legendas, tooltips e estados vazios acessíveis; nenhum gráfico implica previsão.
- **Verificações:** testes de transformação dos dados, componentes e responsividade.
- **Definition of Done:** cada gráfico usa dados reconciliados e informa período e fonte.

### INC-021 — Busca e cadastro assistido de ativos listados

- **Status:** proposto; depende do primeiro provedor aprovado.
- **Objetivo:** filtrar por tipo e pesquisar símbolo ou nome, como FIIs contendo `MX`, antes de registrar um ativo na carteira.
- **Dependências:** INC-013 e INC-019.
- **Critérios de aceite:** debounce, cancelamento de resposta obsoleta, filtro por tipo/mercado, identidade de integração por provedor + ID externo, listagem identificada por símbolo + MIC, metadados normalizados e formulário editável de compra.
- **Verificações:** busca vazia, nome parcial, símbolo parcial, homônimos em bolsas diferentes, tipo sem resultado, rate limit, indisponibilidade e fake determinístico.
- **Definition of Done:** selecionar um resultado preenche somente metadados públicos; quantidade, preço de compra, data, custos e classe continuam sob controle do usuário.

### INC-022 — Renda fixa nacional manual

- **Status:** proposto; universo inicial aprovado, regras de cálculo pendentes.
- **Objetivo:** cadastrar Tesouro, CDB, LCI, LCA, CRI, CRA, debênture e outros contratos sem depender de ticker universal.
- **Dependências:** INC-013 e modelo `fixed_income_terms` aprovado.
- **Critérios de aceite:** emissor, tipo, vencimento, indexador, taxa e modo unitário/nocional validados; avaliação manual com data e fonte.
- **Verificações:** produtos com taxas/vencimentos diferentes, prefixado, percentual de índice, índice + spread, ausência de avaliação e dados inválidos.
- **Definition of Done:** a posição aparece separadamente e nenhuma rentabilidade contratada ou marcação a mercado é inventada.

### INC-023 — Conta e movimentações de caixa

- **Status:** proposto; não pertence à primeira migration.
- **Objetivo:** incluir caixa real no saldo por meio de fatos reconciliáveis.
- **Dependências:** movimentações da carteira estáveis e modelo de caixa aprovado.
- **Critérios de aceite:** depósitos, retiradas e eventos vinculados são idempotentes; saldo é sempre derivado e não fica negativo sem regra explícita.
- **Verificações:** concorrência, estorno, compra/venda, provento, custo e isolamento entre usuários.
- **Definition of Done:** saldo total soma posições avaliadas e caixa reconciliado sem dupla contagem.

### INC-024 — Simulador preenchido pela carteira salva

- **Status:** proposto; comportamento geral solicitado pelo usuário, regra por ativo ainda pendente.
- **Objetivo:** oferecer “usar minha carteira” para carregar posições avaliadas e metas do usuário sem redigitação.
- **Dependências:** INC-009, INC-015 e autorização do INC-011.
- **Critérios de aceite:** somente carteiras pertencentes ao usuário podem ser selecionadas; valores, fontes e instante da fotografia ficam visíveis; a simulação não altera a carteira.
- **Verificações:** carteira vazia, posição sem avaliação, cotação defasada, troca de carteira, acesso cruzado e atualização da fotografia.
- **Definition of Done:** o simulador reproduz a soma por classe da fotografia selecionada e permite voltar ao preenchimento manual. Distribuição por ativo só entra após metas ou regra por ativo aprovadas.

## Pós-MVP candidato

Prioridade futura, sujeita a pesquisa e aprovação: filtros históricos avançados, watchlist, benchmarks, importação CSV/Excel idempotente, exportação segura, notificações, múltiplas moedas, deploy da aplicação e E2E no navegador.

Integrações financeiras só podem entrar após consulta a fontes oficiais, termos/licenças, custos, limites, atribuição, data da pesquisa e riscos. B3 autenticada, credenciais bancárias e endpoints internos permanecem fora de escopo.
