# Evolução para web e celular

**Revisão:** 13 de setembro de 2026. **Status:** proposta para aprovação, não uma mudança de stack ou autorização de deploy.

## Direção recomendada

Manter Java/Spring como proprietário das regras financeiras, identidade e autorização. Evoluir o React/Vite atual primeiro para uma experiência web completa e responsiva. Depois avaliar instalação como PWA; abrir uma frente de aplicativo nativo somente quando houver uma necessidade concreta ou prioridade explícita de distribuição nas lojas.

| Opção | O que aproveita | Custo e limite |
| --- | --- | --- |
| Web responsiva | Toda a interface atual e a API Java | Exige validação de toque, teclado virtual, zoom, foco, rede e telas estreitas |
| PWA online-first | A interface web, com manifest e ícones próprios | Instalação varia por navegador/dispositivo; não significa automaticamente presença nas lojas ou funcionamento offline |
| React Native + Expo | Conhecimento React/TypeScript e contrato da API | Componentes nativos, navegação, armazenamento e testes próprios; HTML/CSS não são telas nativas reutilizáveis |
| Capacitor | Maior reutilização da interface web | Ainda exige builds/plugins, avaliação das lojas e fluxo de autenticação adequado ao ambiente nativo |

A preferência por web → PWA é uma recomendação de custo/manutenção para este projeto, não uma superioridade universal. Nenhum framework móvel, service worker, pacote compartilhado ou provedor de hospedagem foi adicionado.

Referências primárias consultadas nesta revisão: [instalação de PWAs](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Guides/Making_PWAs_installable), [componentes do React Native](https://reactnative.dev/docs/intro-react-native-components), [Expo](https://docs.expo.dev/workflow/overview/) e [Capacitor](https://capacitorjs.com/docs).

## Fronteiras que devemos preservar

- Um backend modular, um PostgreSQL e migrations versionadas; não criar microserviços por causa de um segundo cliente.
- Clientes recebem contratos explícitos com strings decimais; não reimplementar cálculo financeiro em TypeScript para compartilhar entre web e celular.
- OpenAPI, códigos de erro e tipos de transporte podem ser reutilizados quando o segundo consumidor existir de fato. Não extrair bibliotecas sem consumidor.
- Sessão, armazenamento seguro e integração com o sistema operacional não são automaticamente portáveis entre navegador e app nativo.
- Para a web publicada, propor frontend e `/api` na mesma origem HTTPS e banco privado. Provedor, domínio, orçamento e operação ainda precisam ser escolhidos.
- Um banco hospedado atenderá dispositivos autenticados; GitHub sincroniza código, não os bancos Docker locais.

## Portões antes de publicar

1. **Persistência coerente:** decidir IDs estáveis de classes; testar leitura consistente de versão/metas e compatibilidade dos nomes com o simulador antes do contrato HTTP. Não renomear/truncar dados silenciosamente.
2. **Autenticação:** concluir o INC-010 com fluxo Google OIDC, redirects exatos, sessão backend, CSRF, logout, expiração, vínculo por `provider + subject` e isolamento entre usuários. A direção Google já foi aprovada, mas a configuração detalhada não.
3. **Operação:** HTTPS, secrets por ambiente, banco sem exposição pública, restauração de backup testada, limites de corpo/tempo/taxa, logs sem dados pessoais e CI observada. A proteção de tamanho de JSON não substitui timeout ou rate limit.
4. **Privacidade:** demonstração pública inicialmente sintética. Dados reais exigem revisão específica de acesso, retenção/exclusão e backups; fonte de cotação exige licença de exibição aprovada separadamente.
5. **PWA:** aprovação de escopo, manifest/ícones/HTTPS e teste de instalação nos dispositivos escolhidos. Começar online-first, sem cache de carteiras, respostas privadas ou tokens. Instalação não exige por si só um service worker, conforme a referência MDN.
6. **App nativo:** novo ADR de login e distribuição. Não abrir o login Google em WebView embutida; verificar fluxo com navegador do sistema/SDK suportado, PKCE, redirects e armazenamento seguro. A sessão web não resolve isso sozinha. Referências: [Google — aplicativos instalados](https://developers.google.com/identity/protocols/oauth2/native-app) e [RFC 8252](https://www.rfc-editor.org/info/rfc8252/).

Não há publicação do site nem aplicativo móvel entregue nesta revisão. Requisitos e custos de lojas serão pesquisados novamente quando houver plataforma escolhida.

## Colaboração com outra IA

- Antes de começar e antes de publicar commits, executar `git fetch origin` e conferir branch, upstream, divergência, alterações locais e PRs abertos.
- Usar uma branch por incremento; não deixar dois agentes editarem simultaneamente os mesmos arquivos/checkout. Se houver trabalho paralelo independente, usar checkout/worktree separado dentro deste projeto.
- Entregar SHA-base, commits, arquivos, comandos/resultados reais e limitações no PR. Revisar o diff, não apenas o resumo do agente.
- Se `main` avançar durante o trabalho, inspecionar as diferenças e combinar a integração antes de merge/rebase. Nunca sobrescrever trabalho com reset ou force-push.
- Nesta rodada, o usuário autorizou commits e push quando necessário. Publicar a branch para revisão não implica autorização para mesclar, contratar hospedagem ou fazer deploy.
- As tarefas pequenas e dependências ficam em `ROADMAP.md`; o que realmente passou fica em `PROJECT_STATUS.md`.

## Escolhas futuras do usuário

Nenhuma destas perguntas bloqueia as correções locais atuais. Antes de abrir a frente correspondente, confirmar:

- Celular significa acesso pelo navegador/ícone instalado ou presença nas lojas? Prioridade Android, iOS ou ambos?
- Qual orçamento mensal e domínio para o primeiro ambiente publicado?
- A primeira publicação continuará apenas demonstrativa ou já terá contas e persistência privadas? Uso de dados reais permanece sujeito à revisão de segurança.
