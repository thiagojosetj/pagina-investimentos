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

Use Node.js **24.15.0 ou posterior da linha 24**: as dependências de desenvolvimento do lockfile exigem essa versão mínima.

## 2. O que o Docker faz aqui

- **Imagem:** pacote versionado do PostgreSQL (`postgres:18.6-bookworm`).
- **Container:** processo isolado criado a partir da imagem.
- **Porta:** `127.0.0.1:5433` no Windows encaminha para `5432` no container.
- **Volume:** `pagina-investimentos_postgres-data` preserva dados entre reinícios.
- **Healthcheck:** `pg_isready` informa quando o banco aceita conexões.

Java e Node rodam diretamente no Windows para facilitar breakpoints e hot reload. A API agora abre conexão com o PostgreSQL ao iniciar: o Flyway cria ou valida o schema versionado, enquanto o Hibernate está impedido de gerar DDL e valida os mapeamentos JPA existentes.

O Docker tem dois usos locais: manter o banco de desenvolvimento no volume do projeto e fornecer bancos temporários aos testes de integração. Testcontainers cria e remove apenas seus próprios containers de teste; não usa os dados do PostgreSQL do Compose. O Docker não busca cotações nem publica o site no GitHub. As verificações isoladas do frontend e o perfil opcional `simulator` do backend não dependem dele. Esse perfil só serve à demonstração do cálculo: não habilita carteiras ou metas persistidas e não substitui a validação com PostgreSQL.

No Render, o Docker tem um terceiro papel: construir uma imagem com Vite e Java no mesmo serviço. O Render faz esse build a partir do `Dockerfile` no GitHub; o Docker Desktop da sua máquina pode continuar desligado. A CI também compila e testa a imagem em um runner separado. O perfil público `demo` é diferente do `simulator` local: ambos dispensam banco, mas o público também desativa Swagger e expõe somente o healthcheck. A demonstração publicada está em [pagina-investimentos-demo.onrender.com](https://pagina-investimentos-demo.onrender.com); instruções em [`render-deployment.md`](render-deployment.md).

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

### Somente o simulador, sem Docker

Para testar o formulário e o cálculo enquanto o Docker Desktop estiver indisponível, não inicie o PostgreSQL. Em um terminal PowerShell separado, a partir da raiz:

```powershell
Set-Location .\backend
$env:SPRING_PROFILES_ACTIVE = 'simulator'
.\mvnw.cmd spring-boot:run
```

O perfil `simulator` inicia a mesma API em `127.0.0.1:8080`, mas sem DataSource, Flyway, JPA ou serviço de carteira persistida. O endpoint de simulação, o status e a validação continuam ativos. Não use esse perfil como substituto do modo completo nem como ambiente de produção. Ao voltar ao modo completo, abra um terminal novo sem `SPRING_PROFILES_ACTIVE=simulator` (ou remova essa variável da sessão atual) e inicie o PostgreSQL antes da API.

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
- `Backend - Simulator`: executa/depura apenas a API do simulador com o perfil `simulator`, sem Docker.
- `Backend - Local Wallet`: executa a primeira carteira persistida, com PostgreSQL e identidade fictícia restrita ao loopback.
- `Frontend`: executa `npm run dev`.
- `Full stack`: inicia Backend e Frontend em paralelo.
- `Simulator + Frontend`: inicia o backend no perfil `simulator` e o frontend em paralelo, sem Docker.
- `Local Wallet + Frontend`: inicia a API privada de desenvolvimento e o frontend; execute o PostgreSQL antes.
- `Backend - Verify`: executa o goal Maven `verify` pelo wrapper, incluindo a cobertura PostgreSQL/Testcontainers.
- `Backend - Verify without Docker`: executa `verify` com o perfil Maven `without-docker`, excluindo apenas os testes marcados com `postgres`.
- `Frontend - Checks`: executa `npm run check`.
- `PostgreSQL`: executa o serviço `postgres` do `compose.yaml`.

Se a configuração `PostgreSQL` não localizar o daemon em outro computador, crie uma conexão Docker chamada `Docker` nas configurações da IDE ou selecione a conexão local na configuração de execução. Esse nome é uma referência local da IDE, não uma credencial.

O frontend atual não consulta o PostgreSQL diretamente. `Full stack` inicia Backend e Frontend, mas não abre o Docker Desktop implicitamente. Para o modo completo, execute primeiro `PostgreSQL` e aguarde o healthcheck. Para apenas simular, escolha `Simulator + Frontend`; essa opção não abre o Docker Desktop. A separação torna a inicialização da infraestrutura uma ação intencional.

## 6. Verificações

Tudo, a partir da raiz:

```powershell
.\scripts\check.ps1
```

O script valida a sintaxe do Compose e verifica se o Docker Engine responde antes de executar as verificações completas de backend e frontend. Se o daemon estiver indisponível, encerra com uma orientação; não abre o Docker Desktop nem tenta reiniciá-lo.

Sem Docker, para uma validação parcial:

```powershell
.\scripts\check.ps1 -SkipDocker
```

Esse modo não chama Docker nem Docker Compose. Ele executa o backend com o perfil Maven `without-docker`, que exclui somente os testes de PostgreSQL/Testcontainers marcados com `postgres`, e executa todas as verificações do frontend. Não substitui a validação completa: migrations, mapeamentos e integração PostgreSQL continuam obrigatórios no modo normal com Docker ou no job Backend da CI.

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

O Vite pode responder HTTP 502 quando a API na porta `8080` não está em execução. Confirme `http://localhost:8080/api/v1/system/status` e os logs do backend. Para apenas simular, use o perfil `simulator` acima, sem Docker. Para o modo completo, inicie primeiro o PostgreSQL. O build público do Render serve o Vite e a API pelo mesmo processo Java, sem usar o proxy de desenvolvimento.

## 9. Testar a primeira carteira salva

Esta fatia salva **nome, classes e metas**; ainda não registra ativos, compras, vendas, cotações ou saldo. Use somente exemplos fictícios. Não altera os ativos sintéticos da demonstração nem preenche automaticamente o simulador.

1. Inicie o Docker Desktop intencionalmente e, na raiz, execute `docker compose up -d --wait postgres`.
2. No IntelliJ, escolha `Local Wallet + Frontend`. Não deixe outro backend ocupando a porta 8080.
3. Abra <http://localhost:5173>, entre na aba **Carteira** e escolha **Entrar com usuário fictício**.
4. Crie uma carteira de exemplo; ela começa com Ações 40%, FIIs 25%, ETFs 15% e Renda fixa 20%.
5. Edite nomes e percentuais mantendo a soma exatamente 100%; salve e recarregue a página. Os dados devem permanecer no PostgreSQL.
6. Teste soma inválida, adição/remoção de classes e duas abas salvando a mesma carteira. Uma versão antiga deve receber conflito, sem sobrescrever silenciosamente a nova.
7. Saia e recarregue: dados privados não devem continuar aparecendo sem uma nova sessão. Este perfil usa sempre a mesma identidade fictícia; não demonstra dois usuários distintos pela interface.

Pelo terminal, substitua somente a execução do backend:

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local-wallet'
```

O frontend continua com `npm run dev`. O perfil `local-wallet` não pode ser combinado com `wallet`, `demo` ou `simulator`; ele limita o servidor a `127.0.0.1` e não deve ser publicado. A V2 cria as tabelas operacionais de sessão JDBC, sem alterar ou apagar as tabelas da V1. As sessões também ficam no banco; o navegador recebe apenas um cookie HttpOnly e solicita o token CSRF para comandos protegidos.

### Login Google: implementação preparada, operação externa pendente

O perfil `wallet` usa Google OpenID Connect e requer três variáveis no **processo Java**, sem fallback: `PORTFOLIO_PUBLIC_ORIGIN` (origem HTTPS fixa, sem caminho), `PORTFOLIO_GOOGLE_CLIENT_ID` e `PORTFOLIO_GOOGLE_CLIENT_SECRET`. Cadastre o callback exato `<origem>/login/oauth2/code/google` em um cliente web do Google Cloud. Nunca copie o secret para o frontend, chat ou arquivos versionados.

Não altere o serviço gratuito de demonstração para esse perfil: login real exige um ambiente privado aprovado, PostgreSQL durável, backups, secrets e teste real do callback. O cookie desse ambiente é `Secure`, HttpOnly e SameSite=Lax; a sessão tem 30 minutos de inatividade e limite absoluto de 8 horas. O backend usa Authorization Code + PKCE, identifica contas por provedor e `sub`, e não guarda tokens Google na sessão final. O proxy Vite inclui `/oauth2` e `/login/oauth2` para desenvolvimento controlado, mas não substitui HTTPS nem a origem registrada.

Não reutilize o banco do `local-wallet` no ambiente Google. Use banco e credenciais separados por ambiente; o servidor também invalida sessões emitidas em outro modo ou sem origem registrada. Após essa atualização, uma sessão criada pela versão anterior deve entrar novamente.

No Render atual e nos perfis `demo`/`simulator`, a aba Carteira explica que a persistência não está habilitada. Nenhum cadastro é simulado como se tivesse sido salvo.
