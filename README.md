# Planejador de Carteira

Projeto de uma plataforma full stack educacional para acompanhar uma carteira por categorias e ativos, analisar sua evolução e planejar aportes. O módulo disponível atualmente compara a alocação informada com metas definidas pelo próprio usuário e simula novos aportes de forma determinística.

> **Aviso:** este projeto é uma ferramenta de estudo e planejamento. Não seleciona ativos, não analisa perfil de investidor e não constitui recomendação de compra ou venda.

## Estado atual

### Versão publicada

O primeiro corte vertical funcional está implementado e publicado:

- formulário responsivo com exemplo totalmente fictício;
- classes, valores atuais, metas e novo aporte editáveis;
- API Java que calcula metas sobre o patrimônio projetado;
- distribuição proporcional aos déficits monetários, sem sugerir vendas;
- valores monetários processados em centavos inteiros e serializados como strings;
- distribuição determinística dos centavos residuais;
- respostas de erro em `application/problem+json`;
- documentação OpenAPI e Swagger UI;
- testes automatizados do domínio, da API e do fluxo crítico da interface.

### Incrementos locais em revisão e validação

- visão geral demonstrativa com total do cenário sintético, posições, hipótese de caixa e alocação por categoria;
- filtro de itens sintéticos por Ações, FIIs, ETFs, Renda fixa e Caixa;
- navegação acessível entre a visão geral e o simulador;
- modos claro e escuro, com preferência visual salva no navegador;
- fundação de persistência com PostgreSQL e Flyway, sem geração automática de DDL pelo Hibernate;
- primeira migration versionada para usuário, identidade externa, carteira e classes de alocação;
- mapeamentos JPA e repositories internos para carteira e classes de alocação;
- serviço transacional interno, ainda sem endpoint, que exige o proprietário em todas as operações e substitui integralmente as metas usando a versão da carteira como compare-and-set;
- validação de uma a vinte classes, percentuais com quatro casas e soma exata de `100.0000`;
- cinco testes puros da validação das metas aprovados;
- oito testes de integração do serviço e sete testes de migration/contexto aprovados com PostgreSQL real via Testcontainers; suíte backend completa com 37 testes e zero falhas.

O repositório [`thiagojosetj/pagina-investimentos`](https://github.com/thiagojosetj/pagina-investimentos) é público. A primeira versão publicada corresponde ao simulador, e sua CI foi aprovada; os incrementos locais seguintes só ficarão disponíveis no GitHub após revisão e novo push aprovado.

O schema inicial e a camada interna de persistência de carteiras/metas existem localmente, mas ainda **não** há contas acessíveis, autenticação nem endpoint para salvar ou consultar carteiras. Também não existem ativos, movimentações ou cotações. A visão geral continua sendo uma demonstração visual explicitamente sintética, e não um dashboard conectado.

Na implementação interna atual, substituir as metas recria seus UUIDs. Essa escolha ainda não faz parte de contrato público e deverá ser revista antes de `portfolio_asset` referenciar classes de alocação ou de esses identificadores serem expostos pela API.

A visão do produto inclui um dashboard por categorias e ativos, posições derivadas de movimentações, patrimônio, custos, resultados, proventos e gráficos. Esses recursos estão planejados, mas ainda não devem ser interpretados como funcionalidades entregues. Qualquer cotação externa dependerá de pesquisa e aprovação do provedor, licença, limites e defasagem.

## Stack implementada

- Backend: Java 21, Spring Boot 4.1, Spring Data JPA, Flyway, Maven Wrapper e JUnit 6.
- Frontend: React 19, Vite 8, TypeScript 6, Vitest e Testing Library.
- Banco: PostgreSQL 18 em Docker Compose e Testcontainers nos testes de integração.
- Qualidade: Spotless, Oxlint, Prettier, typecheck e GitHub Actions.

O sistema começa como um monólito modular com frontend separado. O backend é o único proprietário das regras financeiras; o frontend coleta entradas e apresenta resultados.

## Executar localmente

Pré-requisitos:

- JDK 21;
- Node.js 24 LTS e npm;
- Git;
- Docker Desktop para o PostgreSQL local e para os testes de integração do backend.

No PowerShell, abra três terminais na raiz do projeto.

Terminal 1 — PostgreSQL:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d --wait postgres
```

Terminal 2 — API:

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

Terminal 3 — interface:

```powershell
Set-Location .\frontend
npm ci
npm run dev
```

Acesse:

- Interface: <http://localhost:5173>
- Status da API: <http://localhost:8080/api/v1/system/status>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

O servidor Vite encaminha requisições iniciadas por `/api` para a API na porta `8080`.

## Testes e verificações

Validação completa pelo PowerShell, a partir da raiz:

```powershell
.\scripts\check.ps1
```

Ou por módulo:

```powershell
Set-Location .\backend
.\mvnw.cmd --no-transfer-progress verify

Set-Location ..\frontend
npm run check
```

O script completo valida também a sintaxe do Docker Compose e verifica se o Docker Engine responde, sem iniciá-lo automaticamente. `npm run check` executa format-check, lint, typecheck, testes e build. O `verify` do Maven compila, testa, empacota, confirma a formatação Java e aplica a migration em um PostgreSQL descartável. O Docker Desktop deve estar ativo; o PostgreSQL do Compose não precisa estar iniciado para os testes.

No estado atual do INC-008, `mvn verify` executa 37 testes: 22 testes sem infraestrutura e 15 testes com PostgreSQL real via Testcontainers. A verificação local mais recente terminou com zero falhas.

## PostgreSQL com Docker

A API usa o banco desde a inicialização. O Flyway cria ou valida automaticamente a migration `V1`; o Hibernate está impedido de criar tabelas e valida os mapeamentos JPA já escritos contra PostgreSQL real:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d --wait postgres
docker compose ps
```

Depois de interromper a API, `docker compose stop postgres` para somente o banco deste projeto e preserva seu volume. Não use `docker compose down -v` sem entender que esse comando apaga os dados locais do banco.

Se o Docker Desktop falhar ao abrir com `sailor-ingest.sock` e erro 1920, consulte a recuperação manual limitada em [`docs/local-development.md`](docs/local-development.md#docker-desktop-falha-ao-abrir-com-sailor-ingestsock-e-erro-1920). Não use reset de fábrica ou limpeza de volumes como tentativa de correção.

## IntelliJ IDEA

Abra a pasta raiz `pagina-investimentos`, não apenas `backend` ou `frontend`. As configurações portáveis em `.run/` oferecem:

- `Backend` — executar ou depurar a API, supondo que o PostgreSQL já esteja ativo;
- `Frontend` — executar a interface;
- `Full stack` — iniciar os dois processos;
- `Backend - Verify` — validar o módulo Java;
- `Frontend - Checks` — validar o módulo web;
- `PostgreSQL` — subir somente o serviço pelo Docker Compose.

O Docker não é iniciado implicitamente por `Backend` ou `Full stack`. Inicie primeiro a configuração `PostgreSQL` quando precisar da API. Essa separação evita que uma execução Java abra o Docker Desktop sem intenção.

Na primeira abertura, confirme JDK 21 no módulo Maven, Node.js 24 como interpretador do projeto e a conexão com o Docker Desktop. Detalhes e troubleshooting estão em [`docs/local-development.md`](docs/local-development.md).

## Contrato do simulador

Exemplo resumido:

```http
POST /api/v1/allocation-simulations/contributions
Content-Type: application/json
```

```json
{
  "currency": "BRL",
  "contribution": "2000.00",
  "allocations": [
    {
      "classId": "stocks",
      "name": "Ações",
      "currentAmount": "4800.00",
      "targetPercentage": "40.0000"
    },
    {
      "classId": "real-estate-funds",
      "name": "FIIs",
      "currentAmount": "1800.00",
      "targetPercentage": "25.0000"
    },
    {
      "classId": "etfs",
      "name": "ETFs",
      "currentAmount": "1400.00",
      "targetPercentage": "15.0000"
    },
    {
      "classId": "fixed-income",
      "name": "Renda fixa",
      "currentAmount": "2000.00",
      "targetPercentage": "20.0000"
    }
  ]
}
```

Decimais no JSON usam ponto e são enviados como strings. A interface aceita ponto ou vírgula e normaliza apenas o formato; nenhum cálculo financeiro crítico é feito no navegador.

## Documentação do projeto

- [`docs/PROJECT_SPEC.md`](docs/PROJECT_SPEC.md) — escopo e modelo proposto.
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — incrementos pequenos e critérios de aceite.
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — decisões e alternativas avaliadas.
- [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) — estado real e próximo passo.
- [`docs/local-development.md`](docs/local-development.md) — ambiente local detalhado.
- [`AGENTS.md`](AGENTS.md) — regras permanentes de colaboração e segurança.

## Dados e licença

O exemplo, os testes e a interface usam somente valores sintéticos. Nenhum dado de mercado ou provedor externo é consumido nesta versão.

Código distribuído sob a [licença MIT](LICENSE).

## English summary

Educational full-stack portfolio planning app built with Java, Spring Boot, React and TypeScript. The current release provides a deterministic contribution simulator based exclusively on user-defined allocation targets. It is not financial advice and uses synthetic examples only.
