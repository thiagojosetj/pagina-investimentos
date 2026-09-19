# Desenvolvimento local

Este guia considera Windows 11, PowerShell 7 e IntelliJ IDEA Ultimate. Os comandos também funcionam em PowerShell 5.1, salvo indicação da própria ferramenta.

## 1. Ferramentas suportadas

| Ferramenta | Versão usada na validação | Papel |
| --- | --- | --- |
| JDK | 21.0.10 | Compilar e executar a API |
| Maven | 3.9.16 pelo Wrapper | Dependências, testes e build Java |
| Node.js | 24.15.0 LTS | Executar o toolchain web |
| npm | 11.12.1 | Instalação reproduzível pelo lockfile |
| Docker Desktop | 4.90.0 | Hospedar o PostgreSQL local e os bancos descartáveis dos testes |
| Docker Compose | 5.5.1 | Orquestrar o PostgreSQL local |
| Git | 2.53.0 | Versionamento |
| IntelliJ IDEA Ultimate | 2026.2.1 | IDE principal e integração dos dois módulos |

Não é necessário instalar Maven globalmente, pois `backend/mvnw.cmd` fixa o fluxo do projeto.

## 2. O que o Docker faz aqui

- **Imagem:** pacote versionado do PostgreSQL (`postgres:18.6-bookworm`).
- **Container:** processo isolado criado a partir da imagem.
- **Porta:** `127.0.0.1:5433` no Windows encaminha para `5432` no container.
- **Volume:** `pagina-investimentos_postgres-data` preserva dados entre reinícios.
- **Healthcheck:** `pg_isready` informa quando o banco aceita conexões.

Java e Node rodam diretamente no Windows para facilitar breakpoints e hot reload. A API agora abre conexão com o PostgreSQL ao iniciar: o Flyway cria ou valida o schema versionado, enquanto o Hibernate está impedido de gerar DDL e valida os mapeamentos JPA existentes.

O Docker tem dois usos distintos: manter o banco de desenvolvimento no volume do projeto e fornecer bancos temporários aos testes de integração. Testcontainers cria e remove apenas seus próprios containers de teste; não usa os dados do PostgreSQL do Compose. O Docker não busca cotações nem publica o site no GitHub. As verificações isoladas do frontend não dependem dele.

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
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose config --quiet
docker compose up -d --wait postgres
```

O arquivo `.env` é local e ignorado pelo Git. `.env.example` contém apenas credenciais fictícias de desenvolvimento.

O Docker Compose lê `.env`, mas Maven e IntelliJ não carregam esse arquivo automaticamente. Com os valores padrão, nenhuma variável adicional é necessária. Se personalizar host, porta, banco ou credenciais, configure os mesmos valores no processo Java. Exemplo temporário no PowerShell:

```powershell
$env:PORTFOLIO_POSTGRES_HOST = 'localhost'
$env:PORTFOLIO_POSTGRES_PORT = '5434'
$env:PORTFOLIO_POSTGRES_DB = 'portfolio'
$env:PORTFOLIO_POSTGRES_USER = 'portfolio'
$env:PORTFOLIO_POSTGRES_PASSWORD = 'portfolio_local'
```

Alterar usuário, senha ou banco no `.env` depois que o volume já foi inicializado não recria essas credenciais no PostgreSQL. Nesse caso, restaure os valores originais ou ajuste conscientemente o banco; não apague o volume como solução automática.

## 4. Executar pelo terminal

### Backend

Inicie primeiro o banco, a partir da raiz:

```powershell
docker compose up -d --wait postgres
```

Depois, no mesmo terminal ou em outro:

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

O Flyway aplica a migration pendente antes de a API aceitar requisições. Se o banco estiver indisponível ou incompatível, a inicialização falha sem criar um schema parcial pelo Hibernate.

Porta HTTP padrão: `8080`. Para alterá-la temporariamente:

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

Porta: `5173`. Durante o desenvolvimento, o proxy Vite encaminha `/api` para `http://127.0.0.1:8080`.

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

- `Backend`: executa/depura `PortfolioApiApplication` com o JDK do módulo; o PostgreSQL precisa estar ativo antes.
- `Frontend`: executa `npm run dev`.
- `Full stack`: inicia Backend e Frontend em paralelo.
- `Backend - Verify`: executa o goal Maven `verify` pelo wrapper.
- `Frontend - Checks`: executa `npm run check`.
- `PostgreSQL`: executa o serviço `postgres` do `compose.yaml`.

Se a configuração `PostgreSQL` não localizar o daemon em outro computador, crie uma conexão Docker chamada `Docker` nas configurações da IDE ou selecione a conexão local na configuração de execução. Esse nome é uma referência local da IDE, não uma credencial.

O frontend atual não consulta o PostgreSQL diretamente. `Full stack` inicia Backend e Frontend, mas não abre o Docker Desktop implicitamente. Quando precisar da API, execute primeiro `PostgreSQL` e aguarde o healthcheck. Essa separação torna a inicialização da infraestrutura uma ação intencional e evita pop-ups do Docker ao executar apenas código Java.

## 6. Verificações

Tudo, a partir da raiz:

```powershell
.\scripts\check.ps1
```

O script valida a sintaxe do Compose e verifica se o Docker Engine responde antes de executar as verificações completas de backend e frontend. Se o daemon estiver indisponível, encerra com uma orientação; não abre o Docker Desktop, não tenta reiniciá-lo e não ignora os testes de integração.

Backend isolado:

```powershell
Set-Location .\backend
.\mvnw.cmd --no-transfer-progress verify
```

Esse comando exige o Docker Desktop ativo em modo Linux containers. Testcontainers cria outro PostgreSQL efêmero em porta aleatória e o remove ao terminar; o serviço `postgres` do Compose não precisa estar ligado.

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

### Conferir as correções de requisições e interface

Com Backend e Frontend ativos, abra a interface e escolha **Simulador**. No modo responsivo das ferramentas do navegador, teste 320, 360 e 390 px nos temas claro/escuro. Campos, rótulos, botão de remoção e resultados devem permanecer dentro do painel.

Clique em **Adicionar classe** e digite: o nome novo deve receber foco. Ao remover FIIs, o foco deve ir para ETFs; ao remover a última classe, para a anterior. O botão de remover a única classe permanece desabilitado. Testar viewport emulado não substitui teste físico de teclado virtual e toque em Android/iOS.

Para rodar somente os testes do limite JSON, na raiz:

```powershell
Push-Location backend
try {
    .\mvnw.cmd --no-transfer-progress '-Dtest=RequestSizeLimitFilterTest,RequestSizeLimitFilterMvcTest' test
} finally {
    Pop-Location
}
```

Com a API local ativa, este teste sintético envia um corpo acima do limite usando transferência chunked; o resultado esperado é `413`:

```powershell
(' ' * 65537) | curl.exe --silent --show-error --http1.1 --max-time 15 --output NUL --write-out '%{http_code}' --header 'Content-Type: application/json' --header 'Transfer-Encoding: chunked' --data-binary '@-' 'http://127.0.0.1:8080/api/v1/allocation-simulations/contributions'
```

O padrão é 65.536 bytes. A propriedade `portfolio.api.max-request-bytes` permite configurar um inteiro positivo menor que `Integer.MAX_VALUE`; qualquer aumento exige avaliar memória e concorrência. O filtro verifica o tamanho declarado de qualquer request e conta bytes reais de JSON em POST/PUT/PATCH/DELETE, lendo no máximo limite + 1 antes de chamar o MVC. A leitura aceita é síncrona; novos endpoints de upload, streaming ou async precisam de política própria. Isto não substitui timeouts, rate limit ou autenticação no futuro ambiente público.

## 7. Parar serviços sem perder dados

Interrompa Java/Vite com `Ctrl+C` nos respectivos terminais. Para o banco:

```powershell
docker compose stop postgres
```

O volume continua presente. Não pare ou remova containers, imagens, redes ou volumes que não tenham o prefixo deste projeto.

## 8. Troubleshooting

### Docker Desktop não inicia

Confira se o Docker Desktop está atualizado e se o WSL 2 está habilitado. Não use **Reset to factory defaults**, `prune`, remoção de volumes ou `wsl --unregister` como tentativa de correção, porque esses caminhos podem apagar dados de outros projetos. Consulte a [documentação oficial de solução de problemas](https://docs.docker.com/desktop/troubleshoot-and-support/troubleshoot/).

### Porta 8080 ou 5173 ocupada

Identifique o processo antes de decidir encerrá-lo:

```powershell
Get-NetTCPConnection -LocalPort 8080,5173 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Não encerre processos desconhecidos automaticamente. Para o frontend, use `npm run dev -- --port 5174`; nesse caso, mantenha a API na porta esperada pelo proxy ou ajuste localmente com consciência.

### Porta 5433 ocupada

Altere o `.env` local e passe a mesma porta para o processo Java:

```dotenv
PORTFOLIO_POSTGRES_PORT=5434
```

```powershell
$env:PORTFOLIO_POSTGRES_PORT = '5434'
```

### A API não conecta ao PostgreSQL

Confirme primeiro o estado do serviço e a porta publicada:

```powershell
docker compose ps
docker compose logs postgres --tail 50
```

Se o container estiver saudável, compare `PORTFOLIO_POSTGRES_HOST`, `PORTFOLIO_POSTGRES_PORT`, `PORTFOLIO_POSTGRES_DB`, `PORTFOLIO_POSTGRES_USER` e `PORTFOLIO_POSTGRES_PASSWORD` do processo Java com os valores do Compose. Não publique os logs se tiver substituído as credenciais fictícias por segredos reais.

### Os testes não encontram o Docker

Abra o Docker Desktop em modo Linux containers e valide:

```powershell
docker version
docker info
```

Não defina `DOCKER_HOST` apenas para contornar a detecção. Uma variável antiga ou incorreta pode impedir o Testcontainers de localizar o daemon.

### Maven usa outro Java

```powershell
java -version
.\backend\mvnw.cmd -version
```

Ambos devem apontar para JDK 21. Ajuste o `JAVA_HOME` do seu ambiente/IDE; não grave caminho absoluto no repositório.

### Interface informa que não encontrou a API

Confirme `http://localhost:8080/api/v1/system/status` e os logs do backend. O build estático de produção não possui servidor configurado neste MVP; hospedagem é uma decisão futura.
