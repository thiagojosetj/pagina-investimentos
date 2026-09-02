# Decisões técnicas

As decisões abaixo registram o contexto conhecido em 2 de setembro de 2026. Mudanças materiais exigem nova análise e aprovação.

## ADR-001 — Java/Spring no backend e React/Vite no frontend

**Status:** aceita e implementada.

### Alternativas comparadas

| Direção | Vantagens | Desvantagens | Aprendizagem, testes e deploy |
| --- | --- | --- | --- |
| Java 21 + Spring Boot + PostgreSQL + React/Vite/TypeScript | Aprofunda a stack mais alinhada a vagas Java Backend e Full Stack Júnior; domínio financeiro fica fortemente tipado; ecossistema maduro de validação, testes e migrations | Dois toolchains e maior consumo de memória que uma solução só em Node | Curva moderada e aderente ao conhecimento atual. JUnit/Testcontainers dão boa demonstração de backend. Deploy exige API, frontend estático e banco |
| NestJS + PostgreSQL + React ou Next.js | TypeScript ponta a ponta, alta velocidade de prototipação e oportunidade de ampliar Node.js | Menor profundidade Java no projeto de portfólio prioritário; cuidado adicional com decimais e separação de domínio | Curva inicial favorável, porém introduz mais novidade no backend. Jest ou Vitest precisaria ser escolhido por camada. Deploy semelhante com backend separado |
| Híbrida com dois backends | Poderia demonstrar integração entre linguagens | Duplica infraestrutura, fronteiras e contratos sem necessidade real no MVP | Maior tempo de desenvolvimento, testes e operação; pouco benefício para uma primeira versão pessoal |

### Decisão

Usar Java 21, Spring Boot 4.1 e Maven no backend; React 19, Vite 8 e TypeScript no frontend; PostgreSQL 18 quando a persistência entrar. A escolha prioriza Java sem abandonar TypeScript e mantém o produto concluível em pequenos incrementos.

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

**Status:** aceita e implementada.

O primeiro corte entrega a regra principal de ponta a ponta sem banco. Isso reduz scaffolding e permite validar domínio, contrato e experiência antes de consolidar um schema difícil de reverter.

O PostgreSQL está definido no Compose para tornar o próximo incremento reproduzível, mas a aplicação atual não abre conexão. Autenticação, autorização e persistência só entram após aprovação de seus modelos.

## ADR-006 — PostgreSQL local isolado por Docker Compose

**Status:** preparada; integração da aplicação pendente.

O Compose usa imagem versionada do PostgreSQL 18, porta local `5433`, healthcheck e volume nomeado sob o projeto `pagina-investimentos`. Não usa `container_name`, volumes de outros projetos ou autenticação `trust`.

Docker serve apenas para padronizar serviços de infraestrutura; Java e Node continuam executando diretamente no host para facilitar depuração no IntelliJ. Remover volumes é uma ação destrutiva e não faz parte do fluxo normal.

## ADR-007 — Bibliotecas principais de teste

**Status:** aceita e implementada.

- Backend: JUnit fornecido pelo Spring Boot Test, AssertJ e MockMvc.
- Frontend: Vitest e Testing Library.

Jest não é adicionado. E2E de navegador será considerado somente quando autenticação e persistência formarem um fluxo estável. Testcontainers será introduzido junto da primeira migration, evitando uma dependência sem uso.

## ADR-008 — OpenAPI gerada no backend

**Status:** aceita e implementada.

Usar `springdoc-openapi` 3.1, compatível com Spring Boot 4, para gerar o contrato e a Swagger UI. DTOs Java continuam sendo o contrato executável nesta fase. Se clientes gerados ou versionamento formal entrarem, a estratégia API-first versus code-first deverá ser reavaliada.

## Decisões pendentes

- Modelo final e ORM da primeira migration; recomendação inicial: Spring Data JPA, Flyway e PostgreSQL real em Testcontainers.
- Estratégia de autenticação; recomendação inicial: sessão HTTP segura no mesmo domínio, sem armazenar token no navegador.
- Regra de correção/exclusão de movimentações e nível de auditoria.
- Hospedagem e ambientes públicos.
- Primeiro provedor de cotações, seus termos e licença.

## Referências consultadas

Consulta realizada em 2 de setembro de 2026:

- [Requisitos do Spring Boot 4.1.1](https://docs.spring.io/spring-boot/system-requirements.html)
- [Versões suportadas do React](https://react.dev/versions)
- [Guia oficial do Vite](https://vite.dev/guide/)
- [Ciclo de releases do Node.js](https://nodejs.org/en/about/previous-releases)
- [Política de versões do PostgreSQL](https://www.postgresql.org/support/versioning/)
- [Compatibilidade do springdoc-openapi](https://springdoc.org/)
- [Configurações compartilháveis do IntelliJ IDEA](https://www.jetbrains.com/help/idea/run-debug-configuration.html)

Nenhuma fonte de dados financeiros foi consultada ou integrada neste incremento.
