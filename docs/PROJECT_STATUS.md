# Status do projeto

**Atualizado em:** 2 de setembro de 2026

## Estado real

O primeiro corte vertical está implementado, validado localmente e publicado. Ele oferece uma interface React responsiva conectada a uma API Spring Boot que calcula a distribuição de um novo aporte por déficit monetário projetado.

O repositório público [`thiagojosetj/pagina-investimentos`](https://github.com/thiagojosetj/pagina-investimentos) recebeu o primeiro push em `main`. O `HEAD` local e `origin/main` coincidem em `2dcdc84976f11c7ab98f6bcef26f4c24d8df98d6`, e a primeira execução real da CI terminou com sucesso nos jobs Backend e Frontend.

## Entregue

- Regra pura e determinística em centavos inteiros.
- Contrato JSON com strings decimais e moeda BRL.
- Validação de 1 a 20 classes, IDs únicos, valores não negativos e soma exata das metas.
- Respostas uniformes para erros de domínio, Bean Validation e JSON malformado.
- Endpoint de status, OpenAPI e Swagger UI.
- Interface com exemplo sintético, classes editáveis, comparação visual e estados de erro/loading/resultado.
- Ambiente PostgreSQL isolado em Docker Compose, ainda não integrado à API.
- Configurações portáveis do IntelliJ e workflow de CI.
- Documentação de produto, decisões, roadmap e desenvolvimento local.
- Repositório público clonável com primeira CI validada.

## Verificações locais registradas

- Backend: Maven `verify`, 15 testes, 0 falhas, artefato gerado.
- Frontend: format-check, Oxlint, TypeScript, 5 testes Vitest e build Vite.
- Integração local: frontend, proxy Vite, API, endpoint de status e OpenAPI responderam HTTP 200; o exemplo retornou `0 / 1200 / 400 / 400`.
- Docker: Compose validado; PostgreSQL 18.6 iniciou com healthcheck saudável, aceitou conexão e foi interrompido sem remover seu volume.

Esses números devem ser atualizados caso novos testes sejam incluídos antes do commit final.

## Limitações conhecidas

- Nenhuma persistência, conta ou autenticação.
- Valores atuais são informados manualmente por classe; não existem ativos ou movimentações.
- Nenhum dado de mercado ou provedor externo.
- Apenas BRL.
- A interface usa conversão numérica somente para formatação/gráficos; contratos financeiros continuam sendo strings e o backend é a fonte de verdade.
- O teste de contexto do Spring emite um aviso do Mockito sobre carregamento dinâmico de agente no JDK; a suíte passa no JDK 21 e não utiliza mock inline diretamente. Isso deverá ser revisto antes de uma migração para um JDK que proíba esse comportamento.
- O simulador é apenas o módulo inicial; o dashboard, posições por ativo, rentabilidade e proventos ainda não existem.

## Próximo incremento recomendado

Alinhar o significado dos indicadores do dashboard e revisar o modelo de usuário, carteira, categoria, ativo, movimentação, cotação e provento antes de criar a primeira migration com Flyway e testes de integração em PostgreSQL real.
