# Status do projeto

**Atualizado em:** 27 de setembro de 2026

## Estado real

O primeiro corte vertical está implementado, validado e publicado no GitHub e no Render. A demonstração pública está em [pagina-investimentos-demo.onrender.com](https://pagina-investimentos-demo.onrender.com) e oferece uma interface React responsiva conectada a uma API Spring Boot que calcula a distribuição de um novo aporte por déficit monetário projetado.

O primeiro push em `main` foi o commit `2dcdc84976f11c7ab98f6bcef26f4c24d8df98d6`, cuja CI terminou com sucesso nos jobs Backend e Frontend. Desde então `main` recebeu persistência de carteira e metas, visão geral demonstrativa e endurecimento de rede da API. A CI atual possui três jobs: Backend, Frontend e Public demo image. PostgreSQL real, JPA, Flyway e Testcontainers continuam obrigatórios no Backend. O Dependabot está ativo; atualizações de dependências são revisadas separadamente das funcionalidades.

O INC-008 está concluído e validado localmente. Os mapeamentos JPA e o serviço interno de carteiras/metas existem, mas ainda não há endpoint para essas operações por decisão de escopo.

## RELEASE-001 — demonstração pública no Render Free

O autor aprovou uma primeira versão online com frontend e API Java no mesmo serviço gratuito, sem banco, login ou carteiras salvas. O `Dockerfile` incorpora o build Vite ao JAR. `render.yaml` declara plano Free, branch `main`, healthcheck e deploy após a CI, mas é apenas referência para o serviço criado manualmente: a opção efetiva deve ser conferida no painel. O perfil `demo` desativa a persistência e as rotas de documentação da API. A interface avisa que somente valores fictícios devem ser usados e que as entradas são transmitidas à API para cálculo, sem salvar carteira.

**Estado:** publicação inicial concluída e verificada no Render Free, originalmente a partir do commit `270fa663bf1136992b8893f3ca18183163587e98` da `main`. Continua sem banco, login, cotações externas ou dados persistidos. Atualizações posteriores exigem um novo deploy concluído; o procedimento está em [`render-deployment.md`](render-deployment.md).

**Verificações reais:** a CI da `main` aprovou os jobs Backend, Frontend e Public demo image. O deploy terminou como `Live`; a página pública respondeu HTTP 200, `/actuator/health` respondeu `UP` e uma simulação com valores inteiramente fictícios retornou as parcelas `40.00` e `60.00`.

O Docker Desktop local permaneceu indisponível durante a preparação. A validação completa com PostgreSQL foi executada pela CI; o novo modo local `scripts/check.ps1 -SkipDocker` executa somente a cobertura que não depende de Docker, sem iniciar, parar ou remover serviços.

## DEV-001 — simulação local sem Docker

Em 19 de setembro de 2026, o frontend respondeu em `127.0.0.1:5173`, mas a API em `127.0.0.1:8080` estava desligada porque o Docker Engine/PostgreSQL local não estava disponível. O proxy Vite devolvia HTTP 502 sem JSON e a interface exibia uma mensagem genérica. O frontend agora indica que a API está indisponível. O perfil opt-in `simulator` mantém o cálculo no backend sem iniciar DataSource, Flyway, JPA ou o serviço de carteiras persistidas; o modo completo permanece inalterado. Há configurações portáveis de IntelliJ para iniciar esse perfil sem Docker.

O teste de contexto do perfil confirmou ausência de persistência, health `UP`, cálculo e validação. Uma requisição real com as cinco categorias sintéticas, enviada pelo proxy Vite, retornou `currentTotal=60000.00`, `contribution=2000.00`, `projectedTotal=62000.00` e cinco parcelas que somam exatamente `2000.00`. `npm run check` aprovou format-check, lint, typecheck, testes e build. O Maven Wrapper aprovou a cobertura sem Docker, `spotless:check` e build. Esse modo é somente local e não salva carteira; a cobertura PostgreSQL permanece obrigatória na CI e no modo completo.

## DEV-002 — validação local sem Docker

`./scripts/check.ps1 -SkipDocker` ignora o preflight do Docker e executa Maven com o perfil `without-docker`, que exclui somente as duas classes marcadas com a tag `postgres` por dependerem de Testcontainers. O frontend continua totalmente validado. Em 21 de setembro de 2026, esse fluxo aprovou 52 testes backend, build e Spotless, além de 24 testes frontend, format-check, lint, typecheck e build. Ele é uma validação parcial intencional: migrations, mapeamentos e integração PostgreSQL continuam cobertos pela CI e pela execução normal com Docker.

## WEB-003 — refinamento visual acessível

Em 22 de setembro de 2026, a navegação ganhou um indicador deslizante que preserva os painéis já montados; a composição do cenário se revela por segmentos; filtros de categoria anunciam a quantidade visível; e os cartões elevam-se discretamente em dispositivos com mouse. O tema alterna com ícones sobrepostos e transição breve.

O resultado do simulador agora expõe `aria-busy` e uma mensagem de estado enquanto calcula; ao responder, revela resumo, comparação e linhas da distribuição em etapas curtas. Não há contagem animada de moeda, alteração do cálculo, dado persistido ou nova dependência.

As animações ficam restritas à preferência `no-preference`; com a preferência do sistema para reduzir movimento, transições e animações não essenciais são reduzidas. `scripts/check.ps1 -SkipDocker` aprovou 52 testes backend sem PostgreSQL/Testcontainers e 24 testes frontend, com format-check, lint, typecheck e build.

## WEB-004 — acabamento dos controles monetários

Em 24 de setembro de 2026, o anel de foco dos controles compostos passou a envolver o campo inteiro, inclusive os prefixos `R$` e sufixos `%`. Ao confirmar um valor monetário com Enter ou ao sair do campo, uma entrada inteira recebe automaticamente os centavos exibidos, por exemplo `4800` para `4800,00`; uma única casa decimal é completada sem arredondamento implícito.

O Enter encerra a edição sem disparar a simulação. Valores inválidos ou com mais de duas casas decimais continuam visíveis para a validação da API, preservando o backend como fonte de verdade para regras financeiras. No frontend, 25 testes Vitest, format-check, lint, typecheck e build passaram.

## Entregue

- Regra pura e determinística em centavos inteiros.
- Contrato JSON com strings decimais e moeda BRL.
- Validação de 1 a 20 classes, IDs únicos, valores não negativos e soma exata das metas.
- Respostas uniformes para erros de domínio, Bean Validation e JSON malformado.
- Endpoint de status, OpenAPI e Swagger UI.
- Interface com exemplo sintético, classes editáveis, comparação visual e estados de erro/loading/resultado.
- Visão geral demonstrativa com Ações, FIIs, ETFs, Renda fixa e Caixa, filtro por categoria e navegação para o simulador.
- Modos claro e escuro, microinterações de navegação, barras e resultados, com preferência visual persistida somente no navegador e redução de movimento respeitada.
- PostgreSQL 18 isolado em Docker Compose e integrado à inicialização da API.
- Flyway como proprietário do schema e V1 limitada a usuário, identidade externa, carteira e classes de alocação.
- Hibernate impedido de gerar DDL e configurado para validar os mapeamentos JPA em PostgreSQL real descartável via Testcontainers.
- Configurações portáveis do IntelliJ e workflow de CI.
- Documentação de produto, decisões, roadmap e desenvolvimento local.
- Repositório público clonável com CI e demonstração pública no Render verificadas.

## WEB-002 — cenário demonstrativo no simulador

Implementado e validado localmente: a ação explícita “Simular esta demonstração” preenche o simulador com as cinco categorias e metas sintéticas da visão geral, inclusive Caixa apenas como hipótese, e um aporte inicial editável de R$ 2.000,00. A pessoa revisa ou altera os campos antes de enviar o cálculo ao backend; a transferência por si só não faz requisição. Navegar normalmente entre as abas preserva o rascunho, enquanto acionar a transferência novamente substitui as entradas e limpa o resultado anterior. Isso não representa carteira salva, login, cotação externa nem saldo de caixa persistido.

No frontend, `npm run check` passou em 19 de setembro de 2026: format-check, lint, typecheck, 22 testes e build. A CI da `main` após o PR #8 também aprovou Backend, Frontend e Public demo image; o backend não mudou neste incremento.

## Entregue no INC-008

- Entidades e repositories JPA internos para `portfolio` e `allocation_class`.
- Serviço sem endpoint que exige ownership na criação, leitura e substituição das metas.
- Substituição integral transacional de uma a vinte metas, soma exata `100.0000` e compare-and-set pela versão da carteira.
- O INC-008A preserva UUID e data de criação das classes mantidas na substituição interna. A integração com ativos e a API de carteira ainda não existem.

## INC-008A — IDs estáveis das classes (PR #7)

A substituição interna agora distingue classes existentes pelo UUID e novas por `id = null`. Classes omitidas são removidas; renomeação, reordenação e ajuste de meta preservam identidade. O serviço valida IDs contra a carteira após reivindicar sua versão e usa uma etapa transacional temporária para evitar conflitos dos índices únicos em trocas de nomes ou ordem. Não há migration, endpoint novo, login nem mudança no cálculo do simulador.

Em 19 de setembro, a branch partiu de `origin/main` em `e6794e2`. O Docker Engine local estava indisponível naquela sessão. `spotless:apply test-compile`, 24 testes backend sem Docker, `spotless:check` e o build Java passaram; no frontend, `npm run check` passou com 14 testes, lint, format-check, typecheck e build. No PR #7, a CI executou 44 testes backend sem falhas, incluindo 13 testes de integração do serviço com PostgreSQL/Testcontainers; os jobs Backend e Frontend passaram. Não houve deploy público.

## HARD-001 e WEB-001 — proteção JSON e interface móvel (PR #6)

Implementado originalmente na branch `fix/request-body-limit`, a partir de `e3f8ada`. O PR #6 foi mesclado em `main` em 19 de setembro, após os PRs #4 e #5 de dependências. A CI da combinação na `main` aprovou Backend e Frontend.

- Limite padrão de 65.536 bytes aplicado ao corpo real dos comandos JSON síncronos, inclusive sem tamanho declarado/chunked. Pré-leitura de até limite + 1 byte, rejeição 413 antes do MVC e corpo aceito preservado para o conversor.
- Layout do simulador e posições ajustado para telas estreitas, com rótulos visíveis e controles de toque maiores. Adicionar classe foca seu nome; remover foca a próxima ou a anterior.
- Proposta de evolução web/celular em `WEB_MOBILE_PLAN.md`; nenhum framework móvel, login, provedor ou deploy acrescentado.

Validação em 14 de setembro de 2026:

- `./scripts/check.ps1`: exit 0. Backend com 58 testes, zero falhas/erros/ignorados, Spotless e build aprovados. São 15 testes PostgreSQL/Testcontainers e 43 sem banco (incluindo 21 do filtro e três de integração filtro/controller).
- Frontend: format-check, lint, typecheck, 18 testes e build aprovados. Quatro regressões de foco foram acrescentadas.
- API real: Actuator `UP` e POST JSON chunked acima de 64 KiB retornou 413 pelo Tomcat, sem depender apenas de mocks.
- Navegador Edge headless: 320, 360, 390, 768 e 1440 px, claro/escuro, visão geral, formulário e resultado integrado à API (30 combinações). Sem overflow horizontal ou controles fora do painel nos cenários verificados; foco ao adicionar/remover validado no navegador.
- Screenshot de 320 px inspecionada; a verificação com viewport emulado não substitui teste em aparelho Android/iOS físico. Evidências temporárias ficaram em `backend/target`, ignorado pelo Git.

Limites: esta proteção cobre JSON síncrono em POST/PUT/PATCH/DELETE e o tamanho declarado das demais requisições; não implementa upload, leitura assíncrona, rate limit ou proteção contra clientes lentos. O domínio financeiro, migrations, autenticação e contrato válido do simulador não mudaram.

## Histórico de verificações anteriores

Validação completa repetida em 8 de setembro de 2026 com `./scripts/check.ps1`: exit code 0.

- Backend: Maven `verify`, 37 testes, 0 falhas e artefato gerado.
- Backend/PostgreSQL: 15 testes com PostgreSQL 18.6 real via Testcontainers — sete de migration/contexto e oito do serviço de carteiras/metas.
- Backend sem infraestrutura: 22 testes aprovados — 14 do simulador/API, cinco da validação das metas e três do limite de tamanho de requisição.
- Frontend: format-check, Oxlint, TypeScript, 14 testes Vitest e build Vite.
- Integração local (3 de setembro): frontend, proxy Vite, API, Actuator e OpenAPI responderam HTTP 200; o exemplo retornou `0 / 1200 / 400 / 400`. Esse smoke test HTTP não foi repetido na manutenção de 8 de setembro.
- Docker: Compose validado e PostgreSQL 18.6 saudável em 8 de setembro, com o volume de desenvolvimento preservado. A aplicação da V1 nesse volume havia sido verificada em 3 de setembro; os testes de 8 de setembro reaplicaram a migration em bancos descartáveis.
- Preflight de `check.ps1`: com daemon desligado, encerrou com orientação antes do Maven, sem iniciar o Docker; com daemon ativo, a validação completa passou.
- Configurações `.run/`: seis XMLs analisados sem erro; `git diff --check` sem problemas de whitespace.

Os números desta seção são históricos; as verificações mais recentes de INC-008A e HARD-001/WEB-001 estão nas respectivas seções acima.

## Limitações conhecidas

- A fundação do schema e a camada JPA interna existem, mas ainda não há endpoint de carteira nem autenticação.
- A política de exclusão de classes já referenciadas por ativos precisa ser definida antes de criar essas referências. A leitura de carteira e metas em duas consultas ainda precisa de garantia de snapshot consistente.
- Valores atuais são informados manualmente por classe; não existem ativos ou movimentações.
- Nenhum dado de mercado ou provedor externo.
- Apenas BRL.
- A interface usa conversão numérica somente para formatação/gráficos; contratos financeiros continuam sendo strings e o backend é a fonte de verdade.
- O teste de rollback usa `@MockitoSpyBean`, e o Mockito emite um aviso sobre carregamento dinâmico de agente no JDK. A suíte passa no JDK 21; a configuração do agente deverá ser revista antes de uma migração para um JDK que proíba esse comportamento.
- A visão geral usa fixture fixa e não representa um dashboard conectado; posições persistidas, rentabilidade e proventos ainda não existem.
- O simulador ainda não consegue carregar uma carteira salva; esse fluxo depende de persistência, autenticação e posições derivadas.
- A demonstração pública gratuita não persiste entradas, pode dormir após inatividade e ainda não tem limite por taxa de requisições. Não deve ser usada para dados financeiros reais.

## SIM-001 — equalização opcional com vendas simuladas

Implementado e validado na CI do PR #10 em 27 de setembro de 2026: a opção “Incluir vendas para equalizar classes” começa desmarcada e permite simular compras e vendas monetárias para atingir as metas por classe sobre o patrimônio após o aporte. O modo padrão mantém a distribuição proporcional de dinheiro novo. Não há ordem, seleção de ativo, nova persistência ou provedor.

A API preserva chamadas sem a opção e o campo `suggestedContribution`, acrescentando compras e vendas explícitas. Aporte zero permite transferências hipotéticas; metas monetárias usam centavos inteiros e desempate estável por identificador. Regra e limites em [`class-rebalancing.md`](class-rebalancing.md), decisão na ADR-014.

**Validação backend:** `./mvnw.cmd --no-transfer-progress -Pwithout-docker verify` aprovou 62 testes, Spotless e build. PostgreSQL/Testcontainers foram excluídos explicitamente; o Docker Engine local não estava disponível e nenhum recurso Docker foi iniciado ou removido.

**Validação frontend:** format-check, lint, typecheck e build aprovados; a suíte final executou 29 testes com `node node_modules/vitest/vitest.mjs run --pool=forks --maxWorkers=1 --testTimeout=30000 --hookTimeout=30000`. Execuções locais anteriores apresentaram falhas intermitentes de inicialização dos workers e timeout de cinco segundos em testes existentes. A execução com um processo por vez e prazo maior passou; não houve alteração de dependência, configuração de teste versionada ou configuração global.

**Integração real:** a API local respondeu health `UP`, página HTTP 200 e compra/venda do exemplo de R$ 500,00. O navegador Edge headless confirmou os modos padrão/com vendas e aporte zero, em 320 e 1280 px, claro/escuro, sem overflow horizontal ou erros JavaScript nas quatro combinações. O foco envolve o controle monetário inteiro e o aporte inteiro foi completado com centavos. Evidências sintéticas ficaram em `backend/target`, ignorado pelo Git; isso não substitui teste em celular físico.

**Publicação:** os resultados locais não comprovam atualização do Render; confirmar CI e deploy de cada publicação. A última versão pública inicialmente registrada foi `270fa663`.

## WEB-005 e HARD-002 — acessibilidade, cancelamento e demonstração defensiva

Em 27 de setembro de 2026, o simulador ganhou região de anúncio persistente, mensagem textual sobre validade das metas, soma exata em unidades inteiras, cancelamento explícito e timeout de 120 segundos. A validação visual não bloqueia a API nem substitui suas regras. Cancelar abandona a espera no cliente, sem prometer interromper trabalho já recebido pelo servidor.

O perfil `demo` aplica headers de segurança antes do filtro de corpo, também em 413. Arquivos Vite com hash têm cache imutável; HTML revalida. Compressão e limites conservadores do Tomcat reduzem custo sem substituir rate limiting. A imagem contém defaults de bind/porta/memória; Swagger local continua disponível. Decisões nas ADRs 015–017 e prioridades em [`REVIEW_FOLLOW_UP.md`](REVIEW_FOLLOW_UP.md).

**Validação local:** backend `-Pwithout-docker verify` com 68 testes, zero falhas/erros, Spotless e build. Frontend completo com 41 testes e format-check, lint, typecheck e build. HTTP real do JAR demonstrativo confirmou página 200/no-cache e JavaScript com cache imutável/gzip. PostgreSQL e smoke da imagem dependem da CI; nenhum Docker local foi iniciado.

**Validação completa na CI:** [execução 36326271598 do PR #10](https://github.com/thiagojosetj/pagina-investimentos/actions/runs/36326271598), sobre `0da5a32`, aprovou Backend, Frontend e Public demo image. Backend: 88 testes sem falhas, incluindo 20 com PostgreSQL real (sete de contexto/migration e 13 do serviço de carteiras). Frontend: 41 testes e demais verificações. Imagem: inicialização limitada a 512 MB, health, modos padrão/com vendas, 413 protegido, cache/gzip e documentação desativada. Esse resultado não comprova sozinho o deploy no Render.

## Próximo incremento recomendado

Garantir snapshot consistente de carteira/metas e alinhar nomes de 80 versus 60 caracteres (INC-008B); depois concluir o threat model do login Google (INC-010A). Só então entregar a primeira tela e API autenticadas de carteira/metas. Cadastro de ativos, rentabilidade, proventos e provedores permanecem incrementos posteriores, não funcionalidades prontas.
