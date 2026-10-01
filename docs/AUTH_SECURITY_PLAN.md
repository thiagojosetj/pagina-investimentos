# Proposta de autenticação e autorização web

**Incremento:** INC-010A. **Consulta:** 27 de setembro de 2026. **Status:** proposta para aprovação; não implementada.

A direção Google para identidade foi aceita na ADR-009. Este documento propõe os detalhes ainda não aprovados: fluxo, sessão, proteção CSRF, contrato HTTP e condições de operação. Não acrescenta dependências, endpoints, credenciais ou contas ao aplicativo. A demonstração pública continua sem login, banco ou carteiras pessoais.

## 1. Recomendação e alternativas

Usar Spring Security OAuth2 Login no Java, com OpenID Connect e fluxo Authorization Code. React e API compartilham uma origem; o navegador mantém apenas o cookie da sessão. O servidor troca o código e valida a identidade. Escopos iniciais: `openid email profile`; sem Sheets, Drive, serviços financeiros ou acesso offline. O login não é fonte de cotações. [Google: fluxo web](https://developers.google.com/identity/protocols/oauth2/web-server), [Spring: OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).

| Alternativa | Benefício | Motivo para não priorizar |
| --- | --- | --- |
| Sessão no Java | Aproveita a arquitetura aprovada; revogação e regras centralizadas | Exige CSRF e política de expiração; reiniciar um processo pode encerrar sessões |
| JWT administrado pelo navegador | Pode atender clientes independentes | Acrescenta armazenamento, renovação e revogação; não resolve uma necessidade da web atual |
| Senha local | Não depende de login Google | Exige recuperação, verificação, armazenamento de senha e proteção adicional fora da primeira fatia |

A sessão web não determina o login do futuro aplicativo nativo. Esse cliente exigirá decisão própria; não abrir autenticação Google em WebView embutida. Ver [`WEB_MOBILE_PLAN.md`](WEB_MOBILE_PLAN.md).

## 2. Fluxo proposto e identidade local

1. O usuário inicia o login por navegação de primeiro nível ao backend, não enviando senha ou token ao React.
2. Spring registra a tentativa em sessão e produz `state` e `nonce` aleatórios vinculados ao navegador; o retorno precisa corresponder à tentativa e não pode ser reaproveitado.
3. Google retorna um código a um redirect previamente registrado, exato por ambiente. Não aceitar redirect arbitrário, curinga ou URL fornecida pelo usuário.
4. Java troca o código; a infraestrutura OIDC valida assinatura, emissor, audiência, validade temporal e `nonce`. Não implementar decodificação JWT caseira nem tratar claims não validados como identidade.
5. Uma transação procura ou cria o vínculo `external_identity(provider, subject)` e o `app_user` local. A restrição única resolve a identidade; testar callbacks concorrentes sem duplicar usuário.
6. O principal autenticado passa a representar o UUID local confiável. Nome e e-mail são atributos atualizáveis, não chaves; não unir contas automaticamente por e-mail.

O `sub` estável, não o e-mail, identifica a conta Google. O desenho reutiliza as tabelas existentes; não precisa de coluna de senha. [Google: identidade e validação OIDC](https://developers.google.com/identity/openid-connect/openid-connect).

Propor PKCE com `S256`, gerado pela infraestrutura de segurança. Spring documenta `requireProofKey` para Authorization Code; a referência Google OIDC menciona PKCE, mas isso não comprova a configuração do futuro cliente confidencial. Antes de habilitar o login, verificar compatibilidade do registro Google e da versão efetiva de Spring, testar challenge/verifier e registrar o resultado. Não omitir silenciosamente a proteção caso haja incompatibilidade: revisar a proposta antes de publicar. [Spring: PKCE](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html), [Google: referência OIDC](https://developers.google.com/identity/openid-connect/reference), [OWASP: OAuth2](https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_Cheat_Sheet.html).

## 3. Sessão, navegador e encerramento

- Cookie de sessão proposto em produção: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/`, sem `Domain`; avaliar nome com prefixo `__Host-`. `Lax` permite o retorno Google por navegação GET, mas não substitui CSRF.
- Rotacionar o ID da sessão ao autenticar, usando a proteção de session fixation do framework. Não aceitar sessão pela URL.
- Proposta de expiração: 30 minutos de inatividade e limite absoluto de oito horas desde a autenticação. O limite absoluto requer verificação explícita no servidor; não presumir que o timeout do container o implementa. Usar relógio controlável nos testes.
- Não adicionar “lembrar de mim” nem pedir refresh token. Não guardar token, ID de sessão ou credencial em `localStorage`/`sessionStorage`; tema visual pode continuar local.
- Tokens recebidos pelo backend não entram em DTOs, logs ou tabelas financeiras. Validar o armazenamento mínimo do principal e do cliente OAuth2 na implementação; nenhum token de longa duração será persistido nessa fatia.
- Começar com sessão em memória em uma única instância, se aprovado: reinício/deploy encerra login. Banco durável preserva carteiras, não torna automaticamente a sessão durável. Não adicionar Redis ou novas tabelas de sessão sem necessidade.
- Logout por POST com CSRF válido: invalidar sessão no servidor, limpar contexto e cookie e descartar estado privado na interface. Isso não encerra a conta Google no navegador nem exclui a carteira.

Fontes: [Spring: sessões e rotação](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html), [OWASP: gestão de sessão](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

## 4. CSRF e contrato HTTP a aprovar

Manter CSRF ativo para operações autenticadas que alterem estado. Propor token sincronizado em sessão: um recurso JSON da mesma origem fornece token e nome do header; React mantém esse token apenas em memória e o envia no header dos comandos. O cookie autenticado permanece `HttpOnly`. Após login/logout, obter token novo, pois o anterior é invalidado; seguir o tratamento de token adiado e proteção BREACH suportado por Spring. [Spring: CSRF e JavaScript](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [OWASP: CSRF](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

Os nomes abaixo são exemplos para discussão, não endpoints existentes ou contrato aprovado:

| Operação proposta | Comportamento |
| --- | --- |
| Início e callback Google | Redirect somente no fluxo explícito de login; falha genérica, sem código/token na tela ou log |
| `GET /api/v1/session` | Estado mínimo da sessão; nunca devolver tokens ou claims completos |
| `GET /api/v1/csrf` | Token/header JSON, mesma origem, sem cache público |
| `POST /api/v1/logout` | CSRF obrigatório; encerra somente a sessão atual |
| Recursos privados de carteira | `401` JSON sem redirect se sessão ausente/expirada; erros uniformes |

Propor `Cache-Control: private, no-store` em respostas de sessão, CSRF e dados privados, inclusive erros. Assets com hash continuam separados do cache privado. Nenhum service worker poderá guardar carteiras ou autenticação. Fetch usa credenciais da mesma origem; produção não precisa de CORS amplo. Desenvolvimento usa o proxy Vite existente, em vez de liberar origens arbitrárias.

## 5. Autorização e fronteiras de operação

O servidor obtém `ownerId` exclusivamente do principal local validado, nunca de corpo JSON, query ou header do usuário. Cada consulta/comando verifica ownership por usuário e carteira; UUID imprevisível não substitui autorização. Propor `404` uniforme para carteira inexistente ou pertencente a outro usuário, sem revelar existência. A concorrência e os IDs estáveis das classes continuam protegidos pelo serviço transacional atual. [OWASP: autorização](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html).

Separar o perfil público `demo`, sem autenticação/persistência privada, de um futuro ambiente autenticado. Credenciais ausentes devem impedir a inicialização do ambiente privado, não liberar recursos como fallback. O simulador público continuará sem acessar registros pessoais; “usar minha carteira” será um recurso privado futuro.

Produção precisa de HTTPS, origem canônica e redirect fixos. Cabeçalhos `Forwarded`/`X-Forwarded-*` só poderão influenciar esquema/host quando houver política comprovada de proxy confiável e remoção de valores externos na borda. Não deduzir redirect livremente de `Host` ou headers encaminhados. Client secret fica apenas no ambiente backend; credenciais diferentes para desenvolvimento e produção. Logs devem omitir cookies, códigos, tokens, e-mail e dados de carteira.

Contas públicas exigem PostgreSQL durável e privado, orçamento/provedor aprovados, backups com restauração testada, política de retenção/exclusão e limites contra abuso. O filesystem efêmero da demonstração não serve para dados pessoais. Esta proposta não aprova nova hospedagem, cobrança ou uso de carteira real.

## 6. Ameaças e verificações

| Ameaça | Defesa proposta | Evidência necessária |
| --- | --- | --- |
| Login CSRF, replay ou código interceptado | `state`, `nonce`, PKCE verificado; redirects exatos | Callback sem tentativa, estado/nonce incorretos, replay e verifier errado rejeitados |
| IDOR entre usuários | UUID local do principal e ownership servidor | Usuário B não lê/altera a carteira A; entrada com owner forjado não concede acesso |
| CSRF de edição/logout | Token de sessão em header; SameSite adicional | Comandos sem token/incorreto falham; token novo após login/logout |
| Sessão fixada, roubada ou eterna | Rotação, cookie protegido e expiração servidor | ID antigo inválido; limites de tempo e logout realmente invalidam acesso |
| XSS/cache/log expondo dados | Sem tokens no React; CSP; no-store; logs mínimos | Respostas/arquivos/logs sem segredo; XSS ainda pode agir com sessão e não é eliminado por HttpOnly |
| Proxy/redirect malicioso e indisponibilidade | Origem fixa, confiança limitada e falha fechada | Host/forwarded falsos não mudam redirect; indisponibilidade Google não cria identidade |

## 7. Incrementos condicionais de uma a três horas

| ID | Objetivo e dependência | Aceite, verificação e conclusão |
| --- | --- | --- |
| INC-010B | Aprovar fluxo/contrato e configurar segurança local; depende de INC-010A aprovado | Política de perfis explícita, API privada retorna 401 JSON, demo segue público; build e testes sem Google real |
| INC-010C | Vínculo OIDC → UUID local; depende de B | Identidade por provider/sub, callback inválido sem escrita, concorrência sem duplicação; testes com provedor/JWKS sintéticos e PostgreSQL |
| INC-010D | Cookie, CSRF, expiração e logout; depende de C | Rotação, token renovado, limites controláveis e logout; testes HTTP positivos/negativos, sem segredos |
| INC-010E | Tela de entrada e recurso de carteira isolado; depende de D e contrato aprovado | Estados sem sessão/loading/falha; dois usuários fictícios isolados; frontend e fluxo integrado estável |
| INC-010F | Validar Google e preparar ambiente privado; depende de E e operação aprovada | Redirect/PKCE reais verificados sem registrar tokens; HTTPS, banco e backup revisados; publicação somente após aceite |

Todos os itens encerram com diff revisado, verificações executadas, documentação do estado real e limitações registradas. JUnit é o framework backend; Vitest cobre interface e clientes. Usar fakes OIDC e relógio controlado na CI, sem conta Google, segredo real ou chamadas externas. E2E só após a fatia vertical estável; mocks de principal não substituem testes de validação do callback/protocolo.

## 8. Escolhas necessárias antes do login, não para o incremento atual

1. Aprovar esta proposta: sessão Java, contrato definitivo, 30 minutos/oito horas, logout e testes de isolamento.
2. Escolher o primeiro ambiente privado: apenas local com dados fictícios ou publicação com contas sintéticas, provedor/banco duráveis, domínio/origem e orçamento.
3. Definir quem poderá entrar inicialmente: lista de testadores fictícios ou cadastro aberto. Dados financeiros reais continuam sujeitos a revisão própria de segurança e privacidade.

Somente depois dessas escolhas serão solicitadas as ações no console Google para cadastro do cliente e consentimento. Nunca compartilhar client secret, cookies ou tokens em mensagens; configuração sensível fica no ambiente apropriado.
