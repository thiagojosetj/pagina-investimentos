# Desenvolvimento local

Este guia considera Windows 11, PowerShell 7 e IntelliJ IDEA Ultimate. Os comandos também funcionam em PowerShell 5.1, salvo indicação da própria ferramenta.

## 1. Ferramentas suportadas

| Ferramenta | Versão usada na validação | Papel |
| --- | --- | --- |
| JDK | 21.0.10 | Compilar e executar a API |
| Maven | 3.9.16 pelo Wrapper | Dependências, testes e build Java |
| Node.js | 24.15.0 LTS | Executar o toolchain web |
| npm | 11.12.1 | Instalação reproduzível pelo lockfile |
| Docker Desktop | 4.89.0 | Hospedar somente serviços de infraestrutura |
| Docker Compose | 5.5.0 | Orquestrar o PostgreSQL local |
| Git | 2.53.0 | Versionamento |
| IntelliJ IDEA Ultimate | 2026.2.1 | IDE principal e integração dos dois módulos |

Não é necessário instalar Maven globalmente, pois `backend/mvnw.cmd` fixa o fluxo do projeto.

## 2. O que o Docker faz aqui

- **Imagem:** pacote versionado do PostgreSQL (`postgres:18.6-bookworm`).
- **Container:** processo isolado criado a partir da imagem.
- **Porta:** `127.0.0.1:5433` no Windows encaminha para `5432` no container.
- **Volume:** `pagina-investimentos_postgres-data` preserva dados entre reinícios.
- **Healthcheck:** `pg_isready` informa quando o banco aceita conexões.

Java e Node rodam diretamente no Windows para facilitar breakpoints e hot reload. O banco ainda não é usado pelo simulador atual; o Compose prepara o próximo incremento.

O projeto nunca precisa de `docker system prune`. `docker compose down -v` remove o volume e exige autorização explícita.

## 3. Preparar um clone

```powershell
git clone https://github.com/thiagojosetj/pagina-investimentos.git
Set-Location .\pagina-investimentos

Set-Location .\frontend
npm ci
Set-Location ..
```

O Maven baixa as dependências no primeiro comando do backend. Para preparar também o banco:

```powershell
Copy-Item .env.example .env
docker compose config --quiet
docker compose up -d --wait postgres
```

O arquivo `.env` é local e ignorado pelo Git. `.env.example` contém apenas credenciais fictícias de desenvolvimento.

## 4. Executar pelo terminal

### Backend

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

Porta padrão: `8080`. Para alterá-la temporariamente:

```powershell
$env:PORT = '8081'
.\mvnw.cmd spring-boot:run
Remove-Item Env:PORT
```

### Frontend

Em outro terminal:

```powershell
Set-Location .\frontend
npm run dev
```

Porta: `5173`. Durante o desenvolvimento, o proxy Vite encaminha `/api` para `http://localhost:8080`.

### URLs úteis

| Recurso | URL |
| --- | --- |
| Aplicação web | <http://localhost:5173> |
| Status da API | <http://localhost:8080/api/v1/system/status> |
| Health do Spring | <http://localhost:8080/actuator/health> |
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

## 5. IntelliJ IDEA

1. Escolha **File → Open** e selecione a raiz `pagina-investimentos`.
2. Aguarde a importação de `backend/pom.xml` como módulo Maven.
3. Em **File → Project Structure → Project**, confirme SDK 21.
4. Em **Settings → Build Tools → Maven**, mantenha o Maven Wrapper do projeto.
5. Em **Settings → Languages & Frameworks → JavaScript Runtime**, selecione Node.js 24 do sistema.
6. Em **Settings → Build, Execution, Deployment → Docker**, confirme a conexão `Docker` com o Docker Desktop.
7. Não versione `.idea/`; preferências pessoais continuam locais.

Configurações compartilhadas em `.run/`:

- `Backend`: executa/depura `PortfolioApiApplication` com o JDK do módulo.
- `Frontend`: executa `npm run dev`.
- `Full stack`: inicia Backend e Frontend em paralelo.
- `Backend - Verify`: executa o goal Maven `verify` pelo wrapper.
- `Frontend - Checks`: executa `npm run check`.
- `PostgreSQL`: executa o serviço `postgres` do `compose.yaml`.

Se a configuração `PostgreSQL` não localizar o daemon em outro computador, crie uma conexão Docker chamada `Docker` nas configurações da IDE ou selecione a conexão local na configuração de execução. Esse nome é uma referência local da IDE, não uma credencial.

O frontend atual não depende do PostgreSQL. Portanto, `Full stack` inicia apenas API e interface.

## 6. Verificações

Tudo, a partir da raiz:

```powershell
.\scripts\check.ps1
```

Backend isolado:

```powershell
Set-Location .\backend
.\mvnw.cmd --no-transfer-progress verify
```

Frontend isolado:

```powershell
Set-Location .\frontend
npm run format:check
npm run lint
npm run typecheck
npm run test:run
npm run build
```

O servidor de testes do Vitest não precisa ficar aberto; `test:run` encerra após uma execução. `npm test` permanece observando alterações e é útil durante desenvolvimento.

## 7. Parar serviços sem perder dados

Interrompa Java/Vite com `Ctrl+C` nos respectivos terminais. Para o banco:

```powershell
docker compose stop postgres
```

O volume continua presente. Não pare ou remova containers, imagens, redes ou volumes que não tenham o prefixo deste projeto.

## 8. Troubleshooting

### Porta 8080 ou 5173 ocupada

Identifique o processo antes de decidir encerrá-lo:

```powershell
Get-NetTCPConnection -LocalPort 8080,5173 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Não encerre processos desconhecidos automaticamente. Para o frontend, use `npm run dev -- --port 5174`; nesse caso, mantenha a API na porta esperada pelo proxy ou ajuste localmente com consciência.

### Porta 5433 ocupada

Altere apenas o `.env` local:

```dotenv
PORTFOLIO_POSTGRES_PORT=5434
```

### Maven usa outro Java

```powershell
java -version
.\backend\mvnw.cmd -version
```

Ambos devem apontar para JDK 21. Ajuste o `JAVA_HOME` do seu ambiente/IDE; não grave caminho absoluto no repositório.

### Interface informa que não encontrou a API

Confirme `http://localhost:8080/api/v1/system/status` e os logs do backend. O build estático de produção não possui servidor configurado neste MVP; hospedagem é uma decisão futura.
