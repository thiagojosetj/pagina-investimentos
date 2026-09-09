# Decisões técnicas

As decisões abaixo registram o contexto conhecido em 3 de setembro de 2026. Mudanças materiais exigem nova análise e aprovação.

## ADR-001 — Java/Spring no backend e React/Vite no frontend

**Status:** aceita e implementada.

### Alternativas comparadas

| Direção | Vantagens | Desvantagens | Aprendizagem, testes e deploy |
| --- | --- | --- | --- |
| Java 21 + Spring Boot + PostgreSQL + React/Vite/TypeScript | Aprofunda a stack mais alinhada a vagas Java Backend e Full Stack Júnior; domínio financeiro fica fortemente tipado; ecossistema maduro de validação, testes e migrations | Dois toolchains e maior consumo de memória que uma solução só em Node | Curva moderada e aderente ao conhecimento atual. JUnit/Testcontainers dão boa demonstração de backend. Deploy exige API, frontend estático e banco |
| NestJS + PostgreSQL + React ou Next.js | TypeScript ponta a ponta, alta velocidade de prototipação e oportunidade de ampliar Node.js | Menor profundidade Java no projeto de portfólio prioritário; cuidado adicional com decimais e separação de domínio | Curva inicial favorável, porém introduz mais novidade no backend. Jest ou Vitest precisaria ser escolhido por camada. Deploy semelhante com backend separado |
| Híbrida com dois backends | Poderia demonstrar integração entre linguagens | Duplica infraestrutura, fronteiras e contratos sem necessidade real no MVP | Maior tempo de desenvolvimento, testes e operação; pouco benefício para uma primeira versão pessoal |

### Decisão

Usar Java 21, Spring Boot 4.1 e Maven no backend; React 19, Vite 8 e TypeScript no frontend; PostgreSQL 18 na persistência. A escolha prioriza Java sem abandonar TypeScript e mantém o produto concluível em pequenos incrementos.

React com Vite é suficiente para um dashboard autenticado. SSR, React Server Components e BFF não oferecem benefício atual que justifique Next.js. Se essa necessidade surgir, deverá ser demonstrada antes de substituir a ferramenta.

## ADR-002 — Monólito modular e regras no backend

**Status:** aceita e implementada no primeiro módulo.

O backend é um único processo organizado por feature/domínio. A estrutura inicial separa:

- `api`: transporte HTTP, DTOs e mapeamento;
- `application`: coordenação do caso de uso;
- `domain`: regra financeira pura;
- `shared`: configuração e contrato uniforme de erros.

O frontend tem organização por feature. Ele normaliza o separador decimal e desenha gráficos, mas não calcula a sugestão financeira.

Microserviços, filas, cache distribuído, Kubernetes e contratos de providers inexistentes foram rejeitados por não resolverem um problema atual.

## ADR-003 — Distribuição proporcional ao déficit monetário

**Status:** aceita para o simulador versão `PROPORTIONAL_MONETARY_DEFICIT_V1`.

### Estratégias avaliadas quando o aporte é insuficiente

1. **Distribuição pelos percentuais-alvo:** simples, mas continua enviando dinheiro a uma classe já acima da meta e ignora os desvios atuais.
2. **Prioridade absoluta para o maior déficit:** reduz rapidamente o maior desvio, porém cria mudanças bruscas; pequenas alterações podem transferir todo o aporte para outra classe.
3. **Distribuição igual entre classes abaixo da meta:** explicável, mas trata déficits muito diferentes como equivalentes.
4. **Distribuição proporcional aos déficits monetários projetados:** considera patrimônio atual, novo total e tamanho relativo de cada falta; é contínua, explicável e não envia valor a uma classe sem déficit.

A quarta alternativa foi escolhida. A meta monetária é calculada sobre o patrimônio **após** o aporte. As partes inteiras são calculadas em centavos e os resíduos seguem o método dos maiores restos, com desempate lexicográfico por `classId`.

Consequências:

- todo o aporte é distribuído exatamente;
- nenhuma contribuição é negativa;
- classes acima da meta recebem zero;
- a solução aproxima as metas sem afirmar ser uma otimização universal;
- o método não considera lote mínimo, ativo específico, custos, impostos ou vendas.

Fórmula completa e exemplos estão em [`PROJECT_SPEC.md`](PROJECT_SPEC.md).

## ADR-004 — Precisão e serialização decimal

**Status:** aceita e implementada no simulador.

- Moeda inicial: BRL.
- Dinheiro no domínio do simulador: centavos em `BigInteger`.
- Metas: quatro casas decimais em unidades inteiras de `0.0001` ponto percentual.
- Tolerância da soma das metas: zero após normalização para quatro casas; soma obrigatória `100.0000%`.
- JSON: decimais como strings com ponto.
- Exibição de percentuais derivados: quatro casas com `RoundingMode.HALF_EVEN`.
- Alocação de centavos: maiores restos; desempate por `classId`.

O uso de `number` no frontend limita-se à formatação e ao tamanho visual de barras. Ele não é fonte de verdade financeira.

## ADR-005 — Primeiro corte sem persistência ou autenticação

**Status:** aceita e concluída como recorte histórico.

O primeiro corte entrega a regra principal de ponta a ponta sem banco. Isso reduz scaffolding e permite validar domínio, contrato e experiência antes de consolidar um schema difícil de reverter.

Esse recorte foi encerrado antes da primeira migration. A versão atual já abre conexão com PostgreSQL e aplica o schema inicial; autenticação e operações persistentes para o usuário continuam fora do produto entregue.

## ADR-006 — PostgreSQL local isolado por Docker Compose

**Status:** aceita e implementada.

O Compose usa imagem versionada do PostgreSQL 18, porta local `5433`, healthcheck e volume nomeado sob o projeto `pagina-investimentos`. Não usa `container_name`, volumes de outros projetos ou autenticação `trust`.

Docker serve apenas para padronizar serviços de infraestrutura; Java e Node continuam executando diretamente no host para facilitar depuração no IntelliJ. A API depende desse PostgreSQL no desenvolvimento local. Remover volumes é uma ação destrutiva e não faz parte do fluxo normal.

## ADR-007 — Bibliotecas principais de teste

**Status:** aceita e implementada.

- Backend: JUnit fornecido pelo Spring Boot Test, AssertJ e MockMvc.
- Frontend: Vitest e Testing Library.

Jest não é adicionado. E2E de navegador será considerado somente quando autenticação e persistência formarem um fluxo estável. Testcontainers usa a mesma imagem PostgreSQL do Compose e cria um banco efêmero para os testes de integração, sem H2 e sem depender do banco de desenvolvimento.

## ADR-008 — OpenAPI gerada no backend

**Status:** aceita e implementada.

Usar `springdoc-openapi` 3.1, compatível com Spring Boot 4, para gerar o contrato e a Swagger UI. DTOs Java continuam sendo o contrato executável nesta fase. Se clientes gerados ou versionamento formal entrarem, a estratégia API-first versus code-first deverá ser reavaliada.

## ADR-009 — Google para identidade, não para cotações

**Status:** direção aceita; ainda não implementada.

Usar Google Identity Services/OpenID Connect como primeiro mecanismo de entrada aprovado pelo usuário. O backend validará a identidade e será proprietário de uma sessão HTTP por cookie `HttpOnly`, `Secure` em produção e com proteção CSRF. Tokens do Google não serão armazenados em `localStorage`, e os primeiros escopos deverão se limitar a `openid`, e-mail e perfil.

O modelo separará `app_user` de `external_identity`, identificada pelo par imutável `provider + subject`. Não será criada coluna de senha enquanto login local estiver fora do escopo. Client ID e client secret existirão somente no ambiente; nenhuma credencial será versionada ou solicitada no chat.

Autenticação não autoriza acesso a outros serviços Google. Uma eventual integração com Sheets exigirá consentimento e decisão separada.

## ADR-010 — Flyway como proprietário do schema e JPA em validação

**Status:** aceita e implementada na fundação de persistência.

Spring Data JPA é o mecanismo de persistência do monólito. O Flyway é o único proprietário da evolução do schema; `spring.jpa.hibernate.ddl-auto=validate` impede que o Hibernate crie ou altere tabelas silenciosamente, e `spring.jpa.open-in-view=false` evita manter a sessão de persistência aberta durante a renderização HTTP. Os primeiros mapeamentos JPA de carteira e classes foram escritos no INC-008 e validados pelo Hibernate em PostgreSQL real.

A migration V1 cria apenas `app_user`, `external_identity`, `portfolio` e `allocation_class`. IDs são UUIDs gerados pela aplicação, timestamps são `TIMESTAMPTZ`, registros mutáveis possuem coluna `version` e a moeda-base está limitada a `BRL`. Exclusões por chave estrangeira permanecem restritas até uma decisão explícita de retenção e auditoria. Ativos, movimentações, caixa, renda fixa e cotações não foram antecipados.

O ambiente local usa configuração `PORTFOLIO_POSTGRES_*`. Testes usam uma configuração compartilhada `@ServiceConnection`, com PostgreSQL real descartável e ciclo de vida gerenciado pelo contexto Spring. A primeira camada JPA, a transação de substituição das metas e as consultas sempre filtradas por proprietário pertencem ao INC-008.

Referências oficiais:

- [Spring Boot: SQL Databases e Spring Data JPA](https://docs.spring.io/spring-boot/reference/data/sql.html)
- [Spring Boot: inicialização com Flyway](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Spring Boot: Testcontainers e service connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Flyway: suporte PostgreSQL em módulo separado](https://documentation.red-gate.com/fd/postgresql-database-277579325.html)
- [Testcontainers: módulo PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)

## ADR-011 — Serviço interno de carteiras e substituição integral das metas

**Status:** aceita, implementada e validada localmente.

O INC-008 mantém carteira e metas em uma camada interna de aplicação, sem controller ou endpoint provisório. Toda criação, leitura e alteração recebe o identificador do proprietário por uma fronteira confiável e consulta os dados com ownership; quando a autenticação entrar, esse identificador deverá vir da sessão, nunca do corpo livre de uma requisição.

O conjunto de uma a vinte metas é validado por inteiro, com `BigDecimal` em escala quatro e soma obrigatória de `100.0000`. A substituição ocorre em uma única transação. Um compare-and-set filtra `portfolio`, proprietário e versão esperada, incrementando a versão somente quando o estado lido ainda é atual; ausência ou concorrência não pode produzir atualização parcial.

Para manter este primeiro incremento pequeno, a substituição remove todas as linhas anteriores e insere novas metas com novos UUIDs. Isso evita tratar IDs internos como contrato antes de existir consumidor, mas não é a estratégia definitiva: IDs estáveis devem ser decididos antes de criar `portfolio_asset`, adicionar outra chave estrangeira para `allocation_class` ou publicar esses identificadores na API.

Cinco testes puros da validação das metas e oito testes PostgreSQL/Testcontainers do serviço passaram. A suíte backend completa executou 37 testes sem falhas, incluindo migration, mapeamentos JPA, ownership, concorrência e rollback.

## Decisões pendentes

- Estratégia de IDs estáveis e atualização das metas antes de `portfolio_asset` ou de contrato público.
- Detalhes de implantação da autenticação: domínios, redirects, expiração de sessão, CSRF, logout e configuração do Google Cloud.
- Regra de correção/exclusão de movimentações e nível de auditoria.
- Hospedagem e ambientes públicos.
- Primeiro provedor de cotações, seus termos e licença.

## Pesquisa preliminar — Google e dados de mercado

**Consulta inicial:** 2 de setembro de 2026. **Provedores revisados:** 3 de setembro de 2026. **Status:** nenhum provedor selecionado.

A documentação oficial localizada oferece `GOOGLEFINANCE` como uma função do Google Sheets, não como uma API de cotações destinada ao backend desta aplicação. O próprio Google informa que as cotações podem atrasar até 20 minutos, não cobrem todos os mercados, podem faltar para alguns símbolos e que dados históricos da função não podem ser acessados pela Sheets API ou por Apps Script. O aviso legal também restringe copiar, armazenar e redistribuir esses dados sem consentimento. Portanto, usar uma planilha como ponte automática para o backend seria tecnicamente frágil e juridicamente inadequado sem licença específica.

Uma integração Google separada continua possível para autenticação por OpenID Connect ou, futuramente, importação/exportação consentida no Google Sheets. Isso não resolve a origem das cotações e deverá ser decidido junto da estratégia de autenticação.

O portal B3 for Developers informa que suas APIs são B2B e não oferecem acesso direto para pessoas físicas. A política de consumo de Market Data também prevê análise e contrato para desenvolvimento de produtos. Assim, dados B3 não serão copiados ou redistribuídos sem fonte e licença compatíveis.

Antes de implementar cotações, serão comparados provedores documentados com cobertura real dos ativos escolhidos, custo sustentável, permissão de exibição, histórico, limites e fallback manual. Scraping do Google Finance, de corretoras ou de áreas autenticadas permanece rejeitado.

### Comparação de provedores com cobertura ampla

A pesquisa atual separa duas portas, mesmo que um fornecedor implemente ambas:

- `InstrumentCatalogProvider`: busca por símbolo/nome, tipo, bolsa/MIC, moeda e identificadores;
- `MarketPriceProvider`: cotação, histórico, moeda, fonte, instante de referência e defasagem.

Essa separação é necessária porque direitos de catálogo, cache e exibição de preços podem ser diferentes. A chave nunca irá ao frontend, e nenhuma consulta enviará carteira, posição ou movimentação pessoal ao fornecedor.

**Shortlist condicional:**

- **Twelve Data Business:** melhor piloto técnico self-service encontrado para busca, catálogo, REST/WebSocket e cobertura global, incluindo BVMF. O plano gratuito é somente para uso interno; exibição externa depende de plano empresarial, aprovação para dados fora dos EUA e possíveis licenças de bolsa. Só poderá ser adotado após confirmação escrita sobre B3/FIIs, exibição pública, cache, histórico e valores derivados.
- **dxFeed:** alternativa enterprise com catálogo pesquisável e presença na lista de distribuidores da B3. É mais forte em compliance, mas preço, entitlement, redistribuição e cobertura específica de FIIs dependem de proposta contratual.
- **Cedro:** alternativa licenciada para B3, adequada caso a prioridade seja Brasil. Catálogo global e autocomplete amplo não foram comprovados, portanto pode exigir uma segunda fonte.

Alpha Vantage, Finnhub, EODHD, Marketstack e Massive/Polygon não comprovaram simultaneamente cobertura global, B3/FIIs, busca filtrável e direito de exibição pública nos planos acessíveis. FMP permanece apenas como alternativa enterprise a validar.

**Decisão temporária:** continuar com dados manuais e fakes determinísticos. Nenhum provedor pago foi selecionado, nenhuma chave é necessária e nenhuma cotação externa será apresentada como pronta.

Referências oficiais consultadas:

- [Função GOOGLEFINANCE e limitações](https://support.google.com/docs/answer/3093281)
- [Aviso legal do Google Finance](https://www.google.com/intl/pt-BR/googlefinance/disclaimer/)
- [Google Sheets API](https://developers.google.com/workspace/sheets/api/reference/rest)
- [Google Identity Services](https://developers.google.com/identity)
- [B3 for Developers](https://developers.b3.com.br/)
- [Política de consumo de Market Data B3](https://www.b3.com.br/data/files/7F/D0/F8/0C/1541B9105B12E5A9AC094EA8/Market%20Data%20B3%20Consumption%20Policy.pdf)
- [Distribuidores licenciados pela B3](https://www.b3.com.br/pt_br/market-data-e-indices/servicos-de-dados/market-data/distribuidores/distribuidores-licenciados/)
- [Twelve Data: descoberta e catálogo](https://twelvedata.com/docs/discovery)
- [Twelve Data: uso comercial e pessoal](https://support.twelvedata.com/en/articles/5332349-commercial-and-personal-usage)
- [Twelve Data: termos](https://twelvedata.com/terms)
- [dxFeed: cobertura de mercado](https://dxfeed.com/market-data/)
- [dxFeed: cobertura do Brasil](https://dxfeed.com/coverage/brazil/)
- [Cedro: API de cotações](https://cedrotech.com/apis/api-cotacao-de-ativos/)

## Referências consultadas

Consulta realizada em 2 de setembro de 2026:

- [Requisitos do Spring Boot 4.1.1](https://docs.spring.io/spring-boot/system-requirements.html)
- [Versões suportadas do React](https://react.dev/versions)
- [Guia oficial do Vite](https://vite.dev/guide/)
- [Ciclo de releases do Node.js](https://nodejs.org/en/about/previous-releases)
- [Política de versões do PostgreSQL](https://www.postgresql.org/support/versioning/)
- [Compatibilidade do springdoc-openapi](https://springdoc.org/)
- [Configurações compartilháveis do IntelliJ IDEA](https://www.jetbrains.com/help/idea/run-debug-configuration.html)

Nenhuma fonte de dados financeiros foi integrada. A pesquisa preliminar acima não constitui aprovação de provedor.
