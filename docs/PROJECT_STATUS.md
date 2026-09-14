# Status do projeto

**Atualizado em:** 14 de setembro de 2026

## Estado real

O primeiro corte vertical está implementado, validado localmente e publicado. Ele oferece uma interface React responsiva conectada a uma API Spring Boot que calcula a distribuição de um novo aporte por déficit monetário projetado.

O primeiro push em `main` foi o commit `2dcdc84976f11c7ab98f6bcef26f4c24d8df98d6`, cuja CI terminou com sucesso nos jobs Backend e Frontend. Desde então `main` recebeu e publicou os incrementos seguintes: persistência de carteira e metas, visão geral demonstrativa e o endurecimento de rede da API. A CI aprovou os jobs Backend e Frontend nas execuções mais recentes de `main`, já com JPA, Flyway e Testcontainers no pipeline. O Dependabot está ativo e abriu suas primeiras pull requests de atualização.

O INC-008 está concluído e validado localmente. Os mapeamentos JPA e o serviço interno de carteiras/metas existem, mas ainda não há endpoint para essas operações por decisão de escopo.

## Entregue

- Regra pura e determinística em centavos inteiros.
- Contrato JSON com strings decimais e moeda BRL.
- Validação de 1 a 20 classes, IDs únicos, valores não negativos e soma exata das metas.
- Respostas uniformes para erros de domínio, Bean Validation e JSON malformado.
- Endpoint de status, OpenAPI e Swagger UI.
- Interface com exemplo sintético, classes editáveis, comparação visual e estados de erro/loading/resultado.
- Visão geral demonstrativa com Ações, FIIs, ETFs, Renda fixa e Caixa, filtro por categoria e navegação para o simulador.
- Modos claro e escuro, com preferência visual persistida somente no navegador.
- PostgreSQL 18 isolado em Docker Compose e integrado à inicialização da API.
- Flyway como proprietário do schema e V1 limitada a usuário, identidade externa, carteira e classes de alocação.
- Hibernate impedido de gerar DDL e configurado para validar os mapeamentos JPA em PostgreSQL real descartável via Testcontainers.
- Configurações portáveis do IntelliJ e workflow de CI.
- Documentação de produto, decisões, roadmap e desenvolvimento local.
- Repositório público clonável com primeira CI validada.

## Entregue no INC-008

- Entidades e repositories JPA internos para `portfolio` e `allocation_class`.
- Serviço sem endpoint que exige ownership na criação, leitura e substituição das metas.
- Substituição integral transacional de uma a vinte metas, soma exata `100.0000` e compare-and-set pela versão da carteira.
- A substituição recria os UUIDs das metas; esse comportamento ainda não é contrato público e deverá mudar ou ser ratificado antes de `portfolio_asset`.

## Incremento em revisão — HARD-001 e WEB-001

Implementado na branch `fix/request-body-limit`, a partir de `e3f8ada`, sem merge automático em `main`. Em 14 de setembro, `git fetch origin` encontrou quatro commits posteriores em `main` (até `e6794e2`), com mudanças de documentação/toolchain, não dos arquivos de código deste incremento. A CI de `main` nesse SHA foi consultada e estava aprovada. As mudanças da outra revisão foram preservadas; o PR deverá validar a combinação antes de qualquer merge.

- Limite padrão de 65.536 bytes aplicado ao corpo real dos comandos JSON síncronos, inclusive sem tamanho declarado/chunked. Pré-leitura de até limite + 1 byte, rejeição 413 antes do MVC e corpo aceito preservado para o conversor.
- Layout do simulador e posições ajustado para telas estreitas, com rótulos visíveis e controles de toque maiores. Adicionar classe foca seu nome; remover foca a próxima ou a anterior.
- Proposta de evolução web/celular e colaboração com outra IA em `WEB_MOBILE_PLAN.md`; nenhum framework móvel, login, provedor ou deploy acrescentado.

Validação em 14 de setembro de 2026:

- `./scripts/check.ps1`: exit 0. Backend com 58 testes, zero falhas/erros/ignorados, Spotless e build aprovados. São 15 testes PostgreSQL/Testcontainers e 43 sem banco (incluindo 21 do filtro e três de integração filtro/controller).
- Frontend: format-check, lint, typecheck, 18 testes e build aprovados. Quatro regressões de foco foram acrescentadas.
- API real: Actuator `UP` e POST JSON chunked acima de 64 KiB retornou 413 pelo Tomcat, sem depender apenas de mocks.
- Navegador Edge headless: 320, 360, 390, 768 e 1440 px, claro/escuro, visão geral, formulário e resultado integrado à API (30 combinações). Sem overflow horizontal ou controles fora do painel nos cenários verificados; foco ao adicionar/remover validado no navegador.
- Screenshot de 320 px inspecionada; a verificação com viewport emulado não substitui teste em aparelho Android/iOS físico. Evidências temporárias ficaram em `backend/target`, ignorado pelo Git.

Limites: esta proteção cobre JSON síncrono em POST/PUT/PATCH/DELETE e o tamanho declarado das demais requisições; não implementa upload, leitura assíncrona, rate limit ou proteção contra clientes lentos. O domínio financeiro, migrations, autenticação e contrato válido do simulador não mudaram.

## Histórico de verificações anteriores

Validação completa repetida em 8 de setembro de 2026 com `./scripts/check.ps1`, após a recuperação do Docker: exit code 0.

- Backend: Maven `verify`, 37 testes, 0 falhas e artefato gerado.
- Backend/PostgreSQL: 15 testes com PostgreSQL 18.6 real via Testcontainers — sete de migration/contexto e oito do serviço de carteiras/metas.
- Backend sem infraestrutura: 22 testes aprovados — 14 do simulador/API, cinco da validação das metas e três do limite de tamanho de requisição.
- Frontend: format-check, Oxlint, TypeScript, 14 testes Vitest e build Vite.
- Integração local (3 de setembro): frontend, proxy Vite, API, Actuator e OpenAPI responderam HTTP 200; o exemplo retornou `0 / 1200 / 400 / 400`. Esse smoke test HTTP não foi repetido na manutenção de 8 de setembro.
- Docker: Compose validado e PostgreSQL 18.6 saudável em 8 de setembro, com o volume de desenvolvimento preservado. A aplicação da V1 nesse volume havia sido verificada em 3 de setembro; os testes de 8 de setembro reaplicaram a migration em bancos descartáveis.
- Recuperação do Docker: 26 verificações sintéticas do utilitário aprovadas no PowerShell 7 e Windows PowerShell 5.1; `-WhatIf` real não alterou os diretórios, a execução explícita recuperou o daemon e a proteção recusou nova execução com Docker ativo.
- Preflight de `check.ps1`: com daemon desligado, encerrou com orientação antes do Maven, sem iniciar o Docker; com daemon ativo, a validação completa passou.
- Configurações `.run/`: seis XMLs analisados sem erro; `git diff --check` sem problemas de whitespace.

Os números desta seção são históricos. A execução mais recente deste incremento está na seção anterior.

## Limitações conhecidas

- Docker Desktop: a atualização de 4.89.0 para 4.90.0 não eliminou o erro de sockets inacessíveis neste computador; ele foi reproduzido após parada normal e reabertura. A recuperação manual dos diretórios de runtime restaurou o Engine 29.7.2 e o PostgreSQL do projeto com seu volume preservado. Há backup externo do disco de dados, verificado por SHA-256 antes da atualização. O utilitário de recuperação exige Docker parado e diagnóstico específico; não é uma correção definitiva nem roda automaticamente. Procedimento em `local-development.md`.

- A fundação do schema e a camada JPA interna existem, mas ainda não há endpoint de carteira nem autenticação.
- A substituição integral recria IDs de metas; isso deve ser revisto antes de classes serem referenciadas por ativos ou expostas em contrato público.
- Valores atuais são informados manualmente por classe; não existem ativos ou movimentações.
- Nenhum dado de mercado ou provedor externo.
- Apenas BRL.
- A interface usa conversão numérica somente para formatação/gráficos; contratos financeiros continuam sendo strings e o backend é a fonte de verdade.
- O teste de rollback usa `@MockitoSpyBean`, e o Mockito emite um aviso sobre carregamento dinâmico de agente no JDK. A suíte passa no JDK 21; a configuração do agente deverá ser revista antes de uma migração para um JDK que proíba esse comportamento.
- A visão geral usa fixture fixa e não representa um dashboard conectado; posições persistidas, rentabilidade e proventos ainda não existem.
- O simulador ainda não consegue carregar uma carteira salva; esse fluxo depende de persistência, autenticação e posições derivadas.

## Próximo incremento recomendado

Ratificar a estratégia de IDs estáveis para metas e concluir o threat model do login Google antes de expor carteiras em um contrato HTTP autenticado.
