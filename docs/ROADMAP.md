# Roadmap

O roadmap usa fatias verticais de aproximadamente uma a três horas. Itens futuros são propostas; decisões materiais continuam sujeitas à aprovação descrita em `AGENTS.md`.

## Fase 0 — Fundação e primeira fatia

### INC-001 — Estrutura e documentação inicial

- **Status:** concluído localmente.
- **Objetivo:** criar uma raiz reproduzível, documentar produto, decisões e ambiente.
- **Dependências:** stack e escopo inicial aprovados.
- **Critérios de aceite:** arquivos obrigatórios presentes; exemplos sintéticos; comandos sem caminhos absolutos.
- **Verificações:** revisão de links, busca de secrets e validação dos XMLs/YAML.
- **Definition of Done:** documentação reflete somente o que existe e a raiz abre no IntelliJ.

### INC-002 — Regra pura de alocação

- **Status:** concluído localmente.
- **Objetivo:** distribuir um aporte proporcionalmente aos déficits monetários projetados.
- **Dependências:** ADR-003 e ADR-004.
- **Critérios de aceite:** sem ponto flutuante; sem vendas; soma das sugestões igual ao aporte; desempate determinístico.
- **Verificações:** casos de R$ 2.000, R$ 500, carteira vazia, aporte zero, um centavo, entradas inválidas e invariantes.
- **Definition of Done:** domínio independente de HTTP com testes verdes e regra documentada.

### INC-003 — API do simulador

- **Status:** concluído localmente.
- **Objetivo:** expor a regra por um contrato HTTP versionado.
- **Dependências:** INC-002.
- **Critérios de aceite:** DTOs explícitos, strings decimais, validação, Problem Details e OpenAPI.
- **Verificações:** MockMvc para sucesso, soma inválida, decimal inválido, item nulo, percentual fora do limite e JSON malformado.
- **Definition of Done:** endpoint empacotado pelo Maven e contrato navegável no Swagger UI.

### INC-004 — Interface funcional do simulador

- **Status:** concluído localmente.
- **Objetivo:** entregar uma tela responsiva que consuma a API real.
- **Dependências:** INC-003.
- **Critérios de aceite:** editar/adicionar/remover classes, normalizar vírgula, mostrar loading/empty/error/success e comparar alocação atual/projetada.
- **Verificações:** Prettier, Oxlint, TypeScript, Vitest, Testing Library e build Vite.
- **Definition of Done:** fluxo crítico testado e nenhuma regra financeira duplicada no navegador.

### INC-005 — CI e primeira publicação

- **Status:** configuração local concluída; execução real e publicação pendentes.
- **Objetivo:** publicar o repositório e validar os dois módulos no GitHub Actions.
- **Dependências:** INC-001 a INC-004 e aprovação do estado exato do push.
- **Critérios de aceite:** repositório público vazio criado, `origin` correto, push normal de `main`, jobs backend/frontend verdes.
- **Verificações:** comparar `HEAD` local/remoto, inspecionar execução real e confirmar árvore limpa.
- **Definition of Done:** URL clonável e primeira CI concluída com sucesso; não basta o YAML existir.

## Fase 1 — Persistência de carteiras e metas

### INC-006 — Ratificar o primeiro modelo persistido

- **Status:** proposto.
- **Objetivo:** revisar usuários, carteiras, classes e metas antes da migration.
- **Dependências:** modelo proposto em `PROJECT_SPEC.md` e aprovação do usuário.
- **Critérios de aceite:** entidades, ownership, escalas, constraints, auditoria e dados derivados definidos.
- **Verificações:** revisão de cenários de exclusão, concorrência e evolução do schema.
- **Definition of Done:** decisão registrada sem criar tabela antecipadamente.

### INC-007 — PostgreSQL, Flyway e teste de integração

- **Status:** proposto.
- **Objetivo:** conectar a API ao banco e aplicar a primeira migration versionada.
- **Dependências:** INC-006.
- **Critérios de aceite:** configuração por ambiente, migration imutável, healthcheck e isolamento do Compose.
- **Verificações:** Testcontainers com PostgreSQL real, teste de startup/migration e `docker compose config`.
- **Definition of Done:** clone limpo consegue criar o schema sem `db push` ou alteração manual.

### INC-008 — Carteira, classes e metas na API

- **Status:** proposto.
- **Objetivo:** criar e consultar uma carteira fictícia com suas metas.
- **Dependências:** INC-007.
- **Critérios de aceite:** validação de soma, DTOs, erros, ownership provisório explícito e transações no backend.
- **Verificações:** testes de serviço, repository e endpoint com PostgreSQL real.
- **Definition of Done:** dados sobrevivem ao reinício e o endpoint não expõe entidades JPA.

### INC-009 — Persistência na interface

- **Status:** proposto.
- **Objetivo:** carregar e editar as metas da carteira no fluxo existente.
- **Dependências:** INC-008.
- **Critérios de aceite:** estados de carregamento, conflito, erro e confirmação; nenhuma perda silenciosa.
- **Verificações:** testes de componente e integração com fake da API.
- **Definition of Done:** recarregar a página recupera a configuração persistida.

## Fase 2 — Identidade e isolamento

### INC-010 — Decidir autenticação e ameaças básicas

- **Status:** proposto; aprovação obrigatória.
- **Objetivo:** escolher sessão/cookies, fluxo de registro e recuperação sem expor secrets ao frontend.
- **Dependências:** INC-008.
- **Critérios de aceite:** CSRF, cookies, hash de senha, rate limit, logout e ownership documentados.
- **Verificações:** threat model pequeno e revisão dos contratos públicos afetados.
- **Definition of Done:** decisão aprovada antes de implementar credenciais.

### INC-011 — Autenticação e autorização vertical

- **Status:** proposto.
- **Objetivo:** registrar, entrar, sair e proteger uma carteira do usuário.
- **Dependências:** INC-010.
- **Critérios de aceite:** senha derivada com algoritmo adequado, sessão segura e verificação de ownership no servidor.
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
- **Objetivo:** informar cotação com timestamp/origem e alimentar patrimônio, desvios e simulador persistido.
- **Dependências:** INC-013 e INC-014.
- **Critérios de aceite:** fonte `MANUAL`, possível defasagem visível e estados sem cotação tratados.
- **Verificações:** regras de valuation, API, UI responsiva e isolamento entre usuários.
- **Definition of Done:** primeiro fluxo estável de cadastro até planejamento do aporte.

## Pós-MVP candidato

Prioridade futura, sujeita a pesquisa e aprovação: proventos, filtros históricos, watchlist, benchmarks, importação CSV/Excel idempotente, exportação segura, primeiro provedor substituível de cotações, deploy público e E2E no navegador.

Integrações financeiras só podem entrar após consulta a fontes oficiais, termos/licenças, custos, limites, atribuição, data da pesquisa e riscos. B3 autenticada, credenciais bancárias e endpoints internos permanecem fora de escopo.
