# Planejador de Carteira

Projeto de uma plataforma full stack educacional para acompanhar uma carteira por categorias e ativos, analisar sua evolução e planejar aportes. O módulo disponível atualmente compara a alocação informada com metas definidas pelo próprio usuário e simula novos aportes de forma determinística.

> **Aviso:** este projeto é uma ferramenta de estudo e planejamento. Não seleciona ativos, não analisa perfil de investidor e não constitui recomendação de compra ou venda.

## Estado atual

O primeiro corte vertical funcional está implementado:

- formulário responsivo com exemplo totalmente fictício;
- classes, valores atuais, metas e novo aporte editáveis;
- API Java que calcula metas sobre o patrimônio projetado;
- distribuição proporcional aos déficits monetários, sem sugerir vendas;
- valores monetários processados em centavos inteiros e serializados como strings;
- distribuição determinística dos centavos residuais;
- respostas de erro em `application/problem+json`;
- documentação OpenAPI e Swagger UI;
- testes automatizados do domínio, da API e do fluxo crítico da interface.

O código está publicado em [`thiagojosetj/pagina-investimentos`](https://github.com/thiagojosetj/pagina-investimentos), e a primeira execução real da CI foi aprovada nos jobs de backend e frontend.

Ainda **não** existem contas, autenticação, persistência de carteiras, ativos, movimentações ou cotações. O PostgreSQL está preparado no Compose para os próximos incrementos, mas não é necessário para executar o simulador atual.

A visão do produto inclui um dashboard por categorias e ativos, posições derivadas de movimentações, patrimônio, custos, resultados, proventos e gráficos. Esses recursos estão planejados, mas ainda não devem ser interpretados como funcionalidades entregues. Qualquer cotação externa dependerá de pesquisa e aprovação do provedor, licença, limites e defasagem.

## Stack implementada

- Backend: Java 21, Spring Boot 4.1, Maven Wrapper e JUnit 6.
- Frontend: React 19, Vite 8, TypeScript 6, Vitest e Testing Library.
- Banco preparado: PostgreSQL 18 em Docker Compose.
- Qualidade: Spotless, Oxlint, Prettier, typecheck e GitHub Actions.

O sistema começa como um monólito modular com frontend separado. O backend é o único proprietário das regras financeiras; o frontend coleta entradas e apresenta resultados.

## Executar localmente

Pré-requisitos:

- JDK 21;
- Node.js 24 LTS e npm;
- Git;
- Docker Desktop apenas para o PostgreSQL opcional.

No PowerShell, abra dois terminais na raiz do projeto.

Terminal 1 — API:

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

Terminal 2 — interface:

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

`npm run check` executa format-check, lint, typecheck, testes e build. O `verify` do Maven compila, testa, empacota e confirma a formatação Java.

## PostgreSQL com Docker

O banco ainda não é consumido pelo simulador, mas o ambiente isolado já pode ser validado:

```powershell
Copy-Item .env.example .env
docker compose up -d --wait postgres
docker compose ps
docker compose stop postgres
```

O volume é exclusivo deste projeto e é preservado pelo comando `stop`. Não use `docker compose down -v` sem entender que esse comando apaga os dados locais do banco.

## IntelliJ IDEA

Abra a pasta raiz `pagina-investimentos`, não apenas `backend` ou `frontend`. As configurações portáveis em `.run/` oferecem:

- `Backend` — executar ou depurar a API;
- `Frontend` — executar a interface;
- `Full stack` — iniciar os dois processos;
- `Backend - Verify` — validar o módulo Java;
- `Frontend - Checks` — validar o módulo web;
- `PostgreSQL` — subir o serviço pelo Docker Compose.

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
