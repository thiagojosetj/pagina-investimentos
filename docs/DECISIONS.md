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

O frontend utiliza TypeScript 7 e Vitest 5 conforme o lockfile atualizado; a regra financeira permanece no backend.

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

Essa política inicial de recriação de IDs foi substituída pela ADR-012; o restante da decisão permanece válido.

Cinco testes puros da validação das metas e oito testes PostgreSQL/Testcontainers do serviço passaram. A suíte backend completa executou 37 testes sem falhas, incluindo migration, mapeamentos JPA, ownership, concorrência e rollback.

## ADR-012 — Identidade estável das classes na substituição de metas

**Status:** implementada e validada na CI do PR #7, incluindo testes de integração PostgreSQL.

O comando interno de substituição recebe um UUID para cada classe existente. `id = null` cria uma classe; omitir um ID existente remove essa classe do conjunto. Renomear, reordenar ou mudar o percentual mantém o UUID e `created_at`; a versão da carteira continua protegida pelo compare-and-set e avança uma vez por substituição.

O serviço valida nomes, soma e IDs repetidos antes de gravar. Após reivindicar a versão da carteira, carrega as classes pertencentes àquela carteira e rejeita qualquer ID desconhecido ou de outra carteira sem revelar sua origem. A transação reverte a reivindicação se a validação falhar.

Os índices únicos existentes verificam nome e ordem imediatamente. Para permitir trocas simultâneas sem apagar as linhas preservadas, o serviço exclui somente classes omitidas, move temporariamente as mantidas para nomes e ordens livres, descarrega essas alterações no banco e então grava os valores finais e as novas classes. Os nomes temporários evitam os nomes antigos e finais, inclusive os informados para classes novas. Nenhum valor temporário deve sobreviver ao commit; uma falha provoca rollback de toda a substituição. Não há nova migration nem endpoint de carteira neste incremento.

Alternativas descartadas neste momento: atualização direta, que pode violar os índices únicos durante swaps; exclusão e recriação, que destrói a identidade; alteração do schema apenas para facilitar a troca, que ampliaria o incremento. A futura exclusão de uma classe já referenciada por ativos exigirá regra própria. Leitura consistente e compatibilidade dos nomes são tratadas pela ADR-018.

## Decisões pendentes

- Detalhes de implantação da autenticação: domínios, redirects, expiração de sessão, CSRF, logout e configuração do Google Cloud.
- Regra de correção/exclusão de movimentações e nível de auditoria.
- Hospedagem da futura versão com contas e dados persistidos; a demonstração pública sem banco usa a ADR-013.
- Primeiro provedor de cotações, seus termos e licença.
- Escopo de lançamento web e celular: proposta web responsiva → PWA opcional → cliente nativo apenas com necessidade confirmada, em [`WEB_MOBILE_PLAN.md`](WEB_MOBILE_PLAN.md). A stack atual permanece aprovada; não há decisão de framework móvel.

## ADR-013 — Demonstração pública sem persistência no Render Free

**Status:** aprovada pelo autor em 19 de setembro de 2026; publicada e verificada em 21 de setembro de 2026.

O primeiro endereço público será uma demonstração educacional, não um ambiente de carteira pessoal. Um único serviço Docker no Render Free constrói React/Vite, incorpora o resultado ao JAR Spring Boot e serve interface e API na mesma origem HTTPS. O perfil `demo` desliga DataSource, Flyway, JPA e o serviço interno de carteiras; não há banco, login, cotações externas ou dados persistidos. A API continua calculando aportes em Java. Somente o tema visual é guardado pelo navegador.

Alternativas: separar frontend estático e API simplificaria a entrega dos arquivos web, mas exigiria dois serviços, CORS e mais configuração; usar PostgreSQL gratuito para esta vitrine criaria retenção ilusória, pois o banco gratuito expira. O serviço único reduz pontos de falha para este recorte. Não substitui a arquitetura futura de dados reais: autenticação, privacidade, banco durável, backups e custos exigirão decisão separada.

`render.yaml` declara `plan: free`, região Virginia, `main` como fonte, healthcheck e deploy após os checks da CI. É referência para um Blueprint: o serviço existente foi criado manualmente e sua configuração efetiva de Auto-Deploy deve ser conferida no painel; editar o YAML não a altera automaticamente. A imagem executa como usuário sem privilégios e limita o heap da JVM. O aviso na interface pede somente valores fictícios; entradas digitadas transitam à API para cálculo, sem serem salvas. Swagger e o endpoint de informação ficam desativados no perfil público. O limite de 65.536 bytes para JSON permanece, mas não substitui proteção contra abuso de muitas requisições.

Limitações do plano: 0,1 CPU e 512 MB de RAM; o serviço dorme após 15 minutos sem acesso e pode levar cerca de um minuto para acordar. O sistema de arquivos é efêmero, e a disponibilidade não tem SLA. O serviço não deve receber carteira real nem ser tratado como produção financeira. A publicação foi verificada em [pagina-investimentos-demo.onrender.com](https://pagina-investimentos-demo.onrender.com), a partir do commit `270fa663bf1136992b8893f3ca18183163587e98`: CI aprovada, página HTTP 200, health `UP` e resposta sintética `40.00/60.00`. Procedimento em [`render-deployment.md`](render-deployment.md).

**Fontes oficiais consultadas em 19 de setembro de 2026:** [Render Free](https://render.com/docs/free), [planos de computação](https://render.com/docs/compute-plans), [Blueprint YAML](https://render.com/docs/blueprint-spec), [deploy após CI](https://render.com/docs/deploys) e [Docker no Render](https://render.com/docs/docker).

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

## ADR-014 — Equalização opcional por classe com vendas simuladas

**Status:** aprovada pelo autor em 27 de setembro de 2026, implementada e validada na CI do PR #10.

Manter a ADR-003 como comportamento padrão e oferecer uma opção explícita para simular vendas entre classes. Calcular a meta monetária sobre patrimônio atual mais aporte, usando os mesmos centavos inteiros, maiores restos e desempate por `classId`. Uma classe acima da meta fornece a diferença como venda hipotética; uma classe abaixo recebe a diferença como compra. Compras menos vendas conservam exatamente o aporte externo.

O contrato evolui de forma aditiva: `includeSales` omitido ou nulo significa `false`; a resposta acrescenta `includeSales`, `suggestedPurchase` e `suggestedSale`. `suggestedContribution` continua sendo somente a divisão do dinheiro novo, preservando consumidores existentes. A modalidade usa o identificador `TARGET_CLASS_REBALANCING_WITH_SIMULATED_SALES_V1`; a padrão mantém `PROPORTIONAL_MONETARY_DEFICIT_V1`.

Não foram adicionados ativos, ordens, persistência, impostos ou custos. A tela distingue compras e vendas simuladas e explica as limitações. A alternativa de distribuir vendas por ativo foi adiada, pois exigiria quantidade, lote, liquidez e regras próprias. Detalhes e exemplo em [`class-rebalancing.md`](class-rebalancing.md).

## ADR-015 — Verificação parcial explícita sem Docker

**Status:** implementada em 27 de setembro de 2026.

O perfil Maven `without-docker` exclui somente a tag `postgres` das duas classes dependentes de Testcontainers. O script `check.ps1 -SkipDocker` e a configuração portátil do IntelliJ usam esse perfil sem abrir Docker Desktop. O comando padrão e a CI continuam executando PostgreSQL real. Não reutilizar relatórios antigos como evidência da execução atual; validação parcial não autoriza afirmar que migrations e persistência passaram.

## ADR-016 — Limite do corpo JSON antes do MVC

**Status:** implementada; comportamento anterior preservado.

Limitar a 65.536 bytes o corpo real de comandos JSON síncronos, inclusive sem `Content-Length`, lendo no máximo limite mais um byte e devolvendo 413 antes da conversão MVC. Não confiar apenas no tamanho declarado. Essa proteção não cobre upload assíncrono, clientes lentos ou taxa de requisições. Os cabeçalhos públicos são aplicados antes desse filtro para também proteger sua resposta 413.

## ADR-017 — Demonstração pública com limites e cache específicos

**Status:** implementada em 27 de setembro de 2026; verificação da imagem pelo job Public demo image.

Somente o perfil `demo` aplica CSP, bloqueio de frames, `nosniff`, política sem Referer e isolamento de opener. CSP permite estilos inline necessários aos gráficos, mas não scripts inline. Swagger e o servidor Vite local não recebem essa política. `/assets/**` recebe cache público imutável por um ano para arquivos gerados com hash; HTML e demais recursos devem revalidar. Compressão começa em 1.024 bytes. Tomcat limita threads a 25, conexões a 200, fila a 50 e espera de conexão a dez segundos; isso não é rate limiting nem uma garantia contra ataques.

A imagem define bind `0.0.0.0`, porta padrão `10000` e heap máximo de 50% com Serial GC; o profile permanece selecionado externamente. O bind local da aplicação continua `127.0.0.1`. A CI inicia a imagem sem sobrescrever esses defaults e verifica saúde, simulação, headers, cache, compressão e rotas de documentação desabilitadas. Cancelamento de CI automático se restringe a PRs para não interromper verificações de `main`.

O cliente espera até 120 segundos e oferece cancelamento explícito. Trinta segundos seriam insuficientes para inicialização observada do Render. A validação visual das metas usa inteiros em unidades de 0,0001 ponto percentual; a API continua sendo a autoridade e entradas inválidas seguem para sua validação. Região de estado permanece montada para anúncios assistivos. Limiter por IP foi adiado até definir confiança no proxy, expiração e limite de armazenamento; não confiar livremente em `X-Forwarded-For`.

Fontes oficiais consultadas em 27 de setembro de 2026: [Render Free](https://render.com/docs/free), [deploy após CI](https://render.com/docs/deploys) e [Content-Security-Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy).

## ADR-018 — Snapshot escalar de carteira e compatibilidade dos nomes

**Status:** implementada localmente no INC-008B em 27 de setembro de 2026; execução PostgreSQL ainda pendente.

Substituir a leitura de cabeçalho e classes em duas consultas por uma projeção escalar imutável, com `LEFT JOIN`, filtro por carteira/proprietário e ordenação pela ordem da classe. PostgreSQL `READ COMMITTED` usa um snapshot por instrução; duas consultas podem observar commits diferentes. Projetar valores, não entidades, evita também combinar um cabeçalho antigo mantido no contexto JPA com metas novas.

O repositório retorna uma linha por classe. Carteira existente sem classes retorna uma linha com campos da classe nulos; ausência de linhas significa não encontrada ou não pertencente ao usuário. O serviço mantém `PortfolioView` e lista imutável. Criação/substituição continuam usando os flushes existentes e o CAS; não foram acrescentados relacionamentos JPA, bloqueios ou isolamento global. Os índices atuais de carteira e `(portfolio_id, display_order)` atendem o acesso; não há índice/migration novo nem alegação de benchmark.

Alternativas: `REPEATABLE READ` protege consultas sucessivas numa transação própria, mas exige tratar participação em transação externa e muda o alcance do snapshot; locks de leitura ampliariam contenção; associação com fetch join acrescentaria mapeamento desnecessário. A consulta com projeção é suficiente para este agregado limitado a vinte metas. O join de entidades sem associação usa HQL suportado pelo Hibernate já adotado, sem promessa de portabilidade entre todos os provedores JPA.

Novas definições/substituições usam limite de 60 unidades UTF-16, consistente com a API atual. `VARCHAR(80)` e migration V1 permanecem intactos; dados antigos não são truncados ou removidos. Um nome histórico maior que 60 exige renomeação explícita ao editar. Ampliar silenciosamente o contrato público ou reduzir a coluna com possível perda de dados foi rejeitado.

Testes sem banco validam mapeamento, lista imutável, ownership dos parâmetros, carteira sem classes, limites dos nomes e construção da HQL com os mapeamentos reais. Testes PostgreSQL acrescentados verificam uma instrução/zero entidades carregadas, carteira sem classes, nome histórico/renomeação e edição concorrente enquanto o leitor mantém um cabeçalho antigo no contexto. Compilar esses testes não significa executá-los; a CI completa permanece necessária.

Fontes oficiais consultadas em 27 de setembro de 2026: [PostgreSQL 18: isolamento](https://www.postgresql.org/docs/18/transaction-iso.html) e [Hibernate: joins e projeções](https://docs.hibernate.org/orm/7.1/querylanguage/html_single/).

## INC-010A — Proposta de autenticação, sem configuração ativa

O detalhamento da ADR-009 está em [`AUTH_SECURITY_PLAN.md`](AUTH_SECURITY_PLAN.md). Fluxo OIDC, cookies, expiração, CSRF, contrato e operação privada são propostas para aprovação. Nenhum login, endpoint privado, dependência de segurança, credencial ou novo provedor de hospedagem foi adicionado. Autenticação somente após ratificar esses detalhes e seu ambiente.
