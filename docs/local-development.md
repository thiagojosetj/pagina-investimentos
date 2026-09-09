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

## 7. Parar serviços sem perder dados

Interrompa Java/Vite com `Ctrl+C` nos respectivos terminais. Para o banco:

```powershell
docker compose stop postgres
```

O volume continua presente. Não pare ou remova containers, imagens, redes ou volumes que não tenham o prefixo deste projeto.

## 8. Troubleshooting

### Docker Desktop falha ao abrir com `sailor-ingest.sock` e erro 1920

No ambiente Windows investigado, arquivos de comunicação interna do Docker (`sockets`) ficaram inacessíveis. O log do backend do Docker registrou falha ao renomear `sailor-ingest.sock` para `.stale`, e `Get-Acl` retornou o erro 1920. Isso aconteceu antes de o banco ou a aplicação iniciarem. O motivo original de os sockets ficarem inacessíveis ainda não foi determinado.

A atualização de 4.89.0 para 4.90.0 não resolveu a recorrência neste computador: o erro reapareceu após uma parada normal seguida de reabertura. As [notas oficiais da versão 4.90](https://docs.docker.com/desktop/release-notes/#4900) mencionam uma correção relacionada a sockets após encerramento abrupto, mas esse texto não comprova que o caso local esteja corrigido. Consulta: 8 de setembro de 2026.

Não fique repetindo a inicialização e não use **Reset to factory defaults**, `prune`, remoção de volumes ou `wsl --unregister` para contornar esse erro. As tentativas explícitas de reabrir o Desktop reproduzem a janela de falha. As configurações compartilhadas de Backend/Full stack não possuem tarefa de abertura automática do Docker.

#### Recuperação manual e limitada

O script `scripts/repair-docker-runtime.ps1` é uma mitigação, não uma correção definitiva do Docker. Ele atua no runtime global do Docker do usuário, por isso só deve ser executado quando nenhum outro projeto estiver usando o Docker:

1. Encerre o Docker Desktop por **Quit** e confirme que seus processos e o serviço `com.docker.service` estão parados. O script verifica isso e não encerra serviços por conta própria.
2. Na raiz do projeto, visualize a operação antes de aplicá-la:

   ```powershell
   .\scripts\repair-docker-runtime.ps1 -WhatIf
   ```

3. Somente se o diagnóstico corresponder ao erro descrito, execute e confira os alvos antes de confirmar:

   ```powershell
   .\scripts\repair-docker-runtime.ps1
   ```

4. Abra o Docker Desktop uma vez, aguarde e valide:

   ```powershell
   docker info --format '{{.ServerVersion}}'
   docker compose up -d --wait postgres
   docker compose ps
   ```

O utilitário só aceita os diretórios normais `%LOCALAPPDATA%\Docker\run` e `%LOCALAPPDATA%\docker-secrets-engine`, contendo exclusivamente os sockets conhecidos e vazios. Exige evidência do erro 1920 e valida todos os alvos antes da alteração. Renomeia esses diretórios no mesmo local, acrescentando `.stale-...`, para que o Docker recrie o runtime. Não apaga os backups, não toca em `Docker\wsl`, discos `.vhdx`, containers, imagens ou volumes e recusa conteúdo inesperado. Não está ligado à IDE, ao build ou à CI.

As duas renomeações não são uma operação atômica: se a segunda falhar, a primeira permanece no backup informado. Não restaure backups sobre diretórios recriados pelo Docker. Para validar apenas as proteções do script, sem acessar o runtime real, execute `./scripts/test-repair-docker-runtime.ps1`; seus 26 checks usam operações simuladas e foram executados no PowerShell 7 e no Windows PowerShell 5.1.

Na investigação foi preservada uma cópia externa ao repositório do disco de dados, com o Docker parado e SHA-256 idêntico ao original, seguindo a [orientação oficial de backup](https://docs.docker.com/desktop/settings-and-maintenance/backup-and-restore/). Esse backup pode conter dados de outros projetos e não deve ser versionado ou enviado junto de diagnósticos. Consulta: 8 de setembro de 2026.

Se a falha persistir, preserve os backups e interrompa as tentativas. Uma investigação adicional com o suporte do Docker ou troca de versão exige uma avaliação separada; relatórios de diagnóstico devem ser revisados quanto à privacidade antes de qualquer envio.

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
