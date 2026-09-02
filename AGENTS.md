# AGENTS.md — Investment Portfolio Tracker

Este arquivo orienta agentes de desenvolvimento que trabalhem neste repositório. Ele se aplica à raiz e a todos os módulos, salvo se um diretório receber futuramente um `AGENTS.md` mais específico.

## Estado inicial do projeto

- Nome de trabalho aprovado: Planejador de Carteira; slug do repositório: `pagina-investimentos`.
- Tipo: projeto pessoal, fictício, público e evolutivo para estudo e portfólio.
- Produto: plataforma web de acompanhamento e planejamento de carteira de investimentos.
- Objetivo profissional: demonstrar competências reais para vagas de Backend Java e Full Stack Júnior.
- Ambiente principal: Windows, PowerShell, IntelliJ IDEA, Git e Docker Desktop.
- Idioma: documentação e interface em português; código, APIs, contratos técnicos e commits preferencialmente em inglês.
- Stack aprovada: Java 21, Spring Boot, PostgreSQL e React/Vite/TypeScript, conforme `docs/DECISIONS.md`.
- Repositório remoto: `https://github.com/thiagojosetj/pagina-investimentos.git`; confirmar o conteúdo publicado e a CI antes de afirmar que a versão está disponível.

O projeto deve evoluir em incrementos úteis e verificáveis. Não adicionar tecnologia, abstração, código ou atividade Git apenas para parecer mais avançado.

## Hierarquia e fontes de verdade

Seguir, nesta ordem:

1. solicitação atual e decisões explícitas do usuário;
2. este `AGENTS.md`;
3. `docs/PROJECT_SPEC.md` para requisitos aprovados;
4. `docs/ROADMAP.md` para MVP, pós-MVP e ideias futuras;
5. `docs/DECISIONS.md` para decisões arquiteturais;
6. `docs/PROJECT_STATUS.md` para estado real e próximo trabalho;
7. manifests, lockfiles, migrations, código e testes existentes.

Não duplicar detalhes extensos em vários documentos. Funcionalidades pertencem à especificação/roadmap; decisões técnicas pertencem a `DECISIONS.md`; este arquivo contém regras permanentes de trabalho.

Antes de alegar que leu um arquivo, confirme que ele existe e leia-o integralmente. Instruções mais específicas em subdiretórios podem complementar ou substituir estas regras dentro de seu escopo.

## Regras inegociáveis

1. Este projeto é totalmente fictício, pessoal e original.
2. Nunca copiar, adaptar ou reutilizar código, dados, regras de negócio, nomes internos, schemas, URLs, endpoints, credenciais, pipelines, screenshots, documentação ou informações do estágio/TST, de empregadores, da faculdade, do StudyFlow ou de terceiros.
3. Nunca versionar senhas, tokens, chaves de API, certificados, credenciais bancárias/B3 ou qualquer outro segredo.
4. Nunca versionar carteira real, extratos, notas de corretagem, CPF, conta, agência, posições, movimentações ou arquivos reais de importação.
5. Seeds, fixtures, testes, demos, logs e screenshots devem usar apenas usuários, carteiras, posições, valores investidos e movimentações fictícios ou sintéticos. Ativos negociados publicamente e dados públicos de mercado podem ser reais quando seu uso for legalmente adequado e compatível com documentação, licença e termos aplicáveis. Nunca utilizar dados que revelem a carteira ou movimentações financeiras reais do usuário.
6. Não solicitar, armazenar ou automatizar login na área autenticada da B3; não realizar scraping autenticado nem tratar endpoint interno/não documentado como API pública.
7. Usar apenas integrações e dados legalmente adequados, documentados e compatíveis com licença e termos de uso.
8. Nunca criar commits vazios, mudanças artificiais ou código sem propósito para gerar atividade no GitHub.
9. Não afirmar que algo foi executado, testado, publicado ou validado sem evidência real.
10. Preservar alterações preexistentes do usuário; não descartá-las, sobrescrevê-las ou incluí-las inadvertidamente.
11. Não realizar operação destrutiva, reescrita de histórico ou exclusão de dados sem explicar alvo e impacto e obter aprovação explícita.
12. Antes de qualquer push, apresentar o estado exato que será enviado e aguardar aprovação explícita.

## Limites do produto financeiro

O produto é educacional e de planejamento. Não é corretora, consultoria, recomendação de investimento nem promessa de retorno.

O simulador de aportes:

- usa exclusivamente posições e metas definidas pelo usuário;
- produz resultado determinístico e explicável;
- não seleciona ativos;
- não analisa perfil de investidor;
- não prevê preços ou retornos;
- não recomenda compra ou venda;
- prioriza correção por novos aportes, sem vendas por padrão;
- mostra premissas, moeda, escala, arredondamento e possíveis limitações.

Quando dados de mercado forem exibidos, informar fonte, data/hora e possível defasagem. Nunca representar cotação atrasada ou simulada como tempo real.

Regras fiscais, contábeis ou regulatórias só podem ser descritas como oficiais quando verificadas em fonte primária atual. Simplificações devem ser rotuladas claramente.

## Descoberta antes da implementação inicial

Até o usuário aprovar stack, arquitetura e roadmap, trabalhar somente em leitura e análise.

Antes de alterar qualquer arquivo:

1. confirmar a pasta do novo projeto e garantir que não seja outro repositório;
2. ler este arquivo e a documentação existente;
3. inspecionar `git status`, branch, log, remotos e upstream;
4. verificar alterações do usuário;
5. levantar versões e disponibilidade de Git, GCM, IntelliJ, Docker, JDK/Maven e Node/package manager;
6. identificar portas e serviços já em uso quando relevante;
7. fazer somente perguntas bloqueantes que o ambiente não possa responder;
8. propor stack, arquitetura, modelo inicial, MVP, fora de escopo, roadmap, testes e estratégia de Git;
9. aguardar aprovação explícita.

Não instalar ferramentas, alterar configuração global, inicializar repositório, criar remoto, gerar código, commitar ou fazer push durante essa fase somente de leitura.

## Decisões que exigem aprovação prévia

Explicar contexto, alternativas, vantagens, desvantagens, recomendação e impacto antes de mudar materialmente:

- linguagem ou framework;
- banco ou ORM;
- arquitetura principal;
- autenticação/autorização;
- contrato público da API;
- estratégia de cálculo financeiro;
- provedor externo estrutural;
- deploy/hospedagem;
- escopo do MVP;
- modelo de dados difícil de reverter;
- operação destrutiva;
- estratégia de branches/commits ainda não aprovada.

Pequenas decisões reversíveis dentro de um incremento já aprovado podem ser implementadas sem interrupção e explicadas na entrega.

## Stack e arquitetura

Antes de gerar código, comparar opções compatíveis com o objetivo profissional, incluindo:

- Java 21, Spring Boot, PostgreSQL e React/Vite/TypeScript para aprofundar a stack principal;
- NestJS, PostgreSQL e React/Next.js/TypeScript para ampliar conhecimentos;
- alternativa híbrida somente quando houver benefício claro.

Registrar a decisão em `docs/DECISIONS.md` e atualizar aqui os comandos e limites específicos depois da aprovação.

Não usar tecnologia apenas por popularidade. Se houver backend separado, ele deve ser o proprietário das regras de negócio. Evitar lógica financeira duplicada em frontend, BFF, API Routes ou Server Actions.

Adotar inicialmente monólito modular organizado por domínio/feature. Não introduzir microserviços, mensageria, cache distribuído ou infraestrutura complexa sem necessidade comprovada e aprovação.

Criar contrato/adaptador para um provedor externo apenas quando o primeiro provedor entrar em um incremento real. Não gerar abstrações vazias para possibilidades futuras.

Controllers tratam transporte HTTP; services/use cases coordenam aplicação; regras financeiras centrais devem permanecer testáveis e independentes da interface. Não expor modelos de persistência diretamente quando isso revelar detalhes internos; usar DTOs/contratos explícitos.

## Precisão financeira

Nunca usar representação binária de ponto flutuante (`float`, `double` ou `number`) para persistência ou regras monetárias/percentuais críticas.

Antes de implementar regra financeira relevante:

1. descrever fórmula e premissas;
2. definir moeda, escala e modo de arredondamento;
3. definir representação apropriada para a stack, como `BigDecimal`, `Decimal`, `NUMERIC` ou unidade mínima inteira;
4. definir serialização segura no contrato da API, usando string quando necessário;
5. definir comportamento para centavos residuais e empates;
6. criar casos normais, limites, zero, valores inválidos e arredondamentos;
7. manter relógio, cotações e providers controláveis para testes determinísticos.

Metas de alocação devem validar soma de 100% segundo tolerância explícita. O resultado do simulador deve ser determinístico e explicável.

Regras financeiras devem residir no domínio/backend. O frontend pode formatar e apresentar valores, mas não redefinir a regra.

## Dados, banco e migrations

Antes da primeira migration relevante, apresentar:

- entidades e relacionamentos;
- dados persistidos e derivados;
- histórico e reconstrução de posições;
- idempotência de importações;
- auditoria necessária;
- limites do modelo no MVP.

Introduzir entidades conforme o roadmap; não criar todas antecipadamente.

Migrations versionadas registram toda mudança compartilhada de schema. Não reescrever migration já compartilhada/aplicada; criar uma nova. Se Prisma for adotado, `schema.prisma` descreve o modelo atual e migrations registram sua evolução; `prisma db push` não é o fluxo persistente de ambientes compartilhados.

Drop de tabela/coluna, reset de banco, perda de dados ou alteração destrutiva exige aprovação explícita.

## Segurança e privacidade

- Manter secrets somente no ambiente ou no gerenciador apropriado.
- Versionar `.env.example` apenas com placeholders e valores locais fictícios.
- Ignorar arquivos `.env` reais.
- Nunca enviar secret ao frontend.
- Não imprimir secrets em logs, testes ou CI.
- Validar entradas no backend.
- Aplicar autenticação, autorização e ownership no servidor para evitar IDOR.
- Não expor stack traces, SQL, tokens ou dados financeiros em respostas de erro.
- Configurar CORS de forma restrita.
- Adotar limites de requisição quando o risco justificar.
- Limitar e validar MIME, tamanho, estrutura e conteúdo de uploads CSV/Excel.
- Prevenir formula injection ao exportar CSV.
- Sanitizar anexos e logs antes de compartilhá-los ou versioná-los.
- Dados reais em deploy público só podem ser considerados após revisão específica de segurança e privacidade.

## APIs e provedores externos

Antes de adotar serviço externo:

1. consultar documentação oficial e atual;
2. verificar licença, termos, atribuição e uso permitido;
3. documentar data da consulta, limites, custos e riscos;
4. manter URL e credenciais fora do domínio;
5. aplicar timeout, tratamento de indisponibilidade e rate limit;
6. impedir que falha externa corrompa dados internos;
7. usar fake controlável em testes;
8. cobrir o adapter e as regras consumidoras.

Começar preferencialmente com cadastro manual, CSV sintético e provider fake/determinístico. Integrações com B3, Open Finance ou instituições financeiras são incrementos futuros sujeitos a pesquisa e aprovação.

## API e interface

A API deve evoluir com:

- versionamento coerente;
- DTO validation;
- formato uniforme de erros;
- paginação, filtros e ordenação;
- OpenAPI/Swagger quando aplicável;
- idempotência em importações e comandos sensíveis;
- autenticação, autorização e isolamento entre usuários.

A interface deve ser original, responsiva, acessível e clara. Priorizar informação útil, estados de loading/empty/error e tratamento consistente de falhas. Não copiar identidade visual de plataformas existentes.

Screenshots e demonstrações só podem exibir funcionalidades reais e dados sintéticos.

## Roadmap e tamanho dos incrementos

Manter backlog em `docs/ROADMAP.md`, separado em MVP, pós-MVP e ideias futuras. Uma ideia futura não é requisito aprovado.

Cada item deve ter:

- ID;
- objetivo;
- prioridade;
- dependências;
- critérios de aceite;
- testes/verificações;
- Definition of Done.

Preferir incrementos de 1 a 3 horas e fatias verticais úteis. Evitar scaffolding massivo sem comportamento verificável.

Ao fechar etapa relevante, atualizar `docs/PROJECT_STATUS.md` com estado real, testes, limitações, pendências e próximo incremento.

## Código e dependências

Tratar correção, segurança e privacidade como restrições permanentes. Depois, priorizar clareza, testabilidade, manutenção, consistência e desempenho relevante.

- Usar nomes descritivos em inglês no código.
- Evitar duplicação sem criar abstração prematura.
- Não deixar código morto, logs de depuração ou TODOs vagos.
- Comentários explicam motivos e regras não óbvias.
- Procurar todos os usos antes de alterar componente compartilhado.
- Atualizar documentação afetada por comportamento, configuração ou arquitetura.

Antes de adicionar dependência:

1. verificar se a stack existente já resolve;
2. justificar benefício;
3. avaliar manutenção e licença;
4. evitar pacote pouco mantido em função crítica;
5. registrar dependência estrutural em `docs/DECISIONS.md`.

Não atualizar dependências em massa sem relação com a tarefa. Atualização major ou substituição estrutural exige análise e aprovação.

## Testes e validação

Testes fazem parte da implementação. Escolher um framework principal por camada; não misturar Jest e Vitest sem benefício documentado.

A suíte deve evoluir para cobrir:

- dinheiro, percentuais, arredondamento e alocação;
- casos-limite e distribuição de centavos;
- migrations e persistência com PostgreSQL real, preferencialmente via Testcontainers;
- APIs, validação e erros;
- autenticação, autorização, ownership e isolamento entre usuários;
- providers/adapters com fakes;
- fluxos críticos do frontend;
- E2E somente após existir fluxo vertical estável.

Para bugs, preferir:

`reproduzir → identificar causa → criar teste de regressão → corrigir → executar novamente`

Antes de concluir uma tarefa:

1. executar formatter ou format-check;
2. executar lint;
3. executar typecheck quando aplicável;
4. executar testes pertinentes;
5. executar build quando aplicável;
6. revisar `git diff` e `git status`;
7. verificar arquivos gerados/acidentais;
8. confirmar ausência de segredos e dados reais.

Se algo não puder ser executado, informar exatamente o quê, por quê e o risco residual.

## Desenvolvimento local, Docker e IntelliJ

Manter `docs/local-development.md` com versões suportadas, pré-requisitos, comandos exatos, portas, URLs, healthchecks, testes e troubleshooting.

Docker Compose deve usar nomes, rede e volumes próprios do projeto e subir apenas seus serviços. Não parar ou remover containers, imagens, redes ou volumes alheios.

São proibidos sem aprovação explícita:

```text
docker system prune
docker volume prune
docker compose down -v
reset destrutivo do banco
```

Explicar o papel de container, imagem, volume, porta e healthcheck. Documentar quando executar componentes pela IDE e quando usar Compose.

Preparar o monorepositório para abertura pela raiz no IntelliJ IDEA. Verificar executável Git, SDK/JDK ou Node interpreter, Maven/package manager e Docker.

Versionar preferencialmente configurações portáveis em `.run/` para desenvolvimento, debug, testes, lint e build. Nunca incluir caminho absoluto ou secret. Ignorar `.idea/` salvo decisão consciente sobre arquivo realmente portátil e necessário.

## Git e GitHub

Antes de alterações relevantes:

1. executar `git status`;
2. verificar branch atual;
3. verificar remoto e upstream;
4. executar `git fetch` quando houver remoto;
5. verificar divergência com `origin/main`;
6. preservar trabalho preexistente.

Manter `main` estável. Usar branch de feature quando trouxer clareza ou aprendizado real; não criar branch para ajuste trivial.

Usar Conventional Commits em inglês:

```text
feat: ...
fix: ...
test: ...
docs: ...
refactor: ...
chore: ...
```

Commits devem ser pequenos, lógicos e verificáveis, nunca artificiais.

Antes de cada commit:

1. revisar diff;
2. executar validações proporcionais;
3. procurar secrets e dados financeiros reais;
4. excluir arquivos não relacionados.

Antes de uma fase, apresentar estratégia de commits e pedir autorização. Dentro da fase aprovada, podem ser criados somente os commits locais planejados. Não commitar automaticamente fora dessa autorização.

## Push e publicação

Nunca fazer push silenciosamente.

Antes de cada push, mostrar:

- branch atual;
- remoto e URL exata;
- commits que serão enviados;
- resumo dos arquivos e alterações;
- testes executados e resultados;
- CI esperada.

Aguardar aprovação explícita correspondente ao estado apresentado. Depois usar push normal, sem force.

São proibidos por padrão:

```text
git reset --hard
git clean -fd
git checkout -- .
git restore .
git push --force
git push --force-with-lease
```

Merge, rebase, limpeza destrutiva e reescrita de histórico exigem explicação e aprovação.

Antes da primeira publicação, confirmar nome, slug, descrição, licença e visibilidade. Quando já houver histórico local, criar remoto vazio, sem README, `.gitignore` ou licença automáticos. Configurar `origin`, `main` e upstream.

Verificar identidade Git antes de commitar. Preferir configuração local do repositório; não mudar configuração global sem necessidade e autorização.

Usar Git Credential Manager/OAuth. Nunca pedir senha do GitHub, solicitar token no chat ou expor credencial. O Git/GCM do sistema atende terminal e operações Git do IntelliJ; login GitHub da IDE é opcional para funções adicionais.

Após push aprovado, verificar:

- resultado do push;
- igualdade entre HEAD local e remoto;
- árvore de trabalho limpa;
- execução da CI;
- eventuais falhas e logs relevantes.

Não criar mudança artificial apenas para disparar CI.

## Integração contínua

Configurar workflows para pull requests, pushes relevantes e execução manual com `workflow_dispatch` quando houver filtros por caminho.

A CI deve usar lockfile/wrapper e executar, conforme a stack:

- instalação/build reproduzível;
- format-check;
- lint;
- typecheck;
- testes unitários;
- testes de integração com PostgreSQL;
- build final.

Usar cache sem armazenar secrets. Fixar versões das Actions de maneira segura. Secrets de CI ficam no GitHub Secrets e nunca são impressos.

Não marcar o item de CI como concluído até observar ao menos uma execução real relevante e relatar o resultado.

## README e documentação

O `README.md` deve evoluir junto do produto e conter:

- problema e proposta;
- status honesto;
- funcionalidades realmente disponíveis;
- stack implementada;
- arquitetura;
- instalação e configuração;
- execução local e pelo IntelliJ;
- Docker;
- testes;
- deploy, quando existir;
- screenshots reais com dados sintéticos;
- fontes e licenças de dados;
- roadmap resumido;
- aviso educacional e de ausência de recomendação financeira;
- resumo curto em inglês quando útil.

Não descrever roadmap como funcionalidade pronta nem usar badge sem verificação real.

## Comunicação ao concluir uma tarefa

Ao concluir incremento relevante, informar:

1. resultado;
2. como funciona e por que a abordagem foi escolhida;
3. principais arquivos alterados;
4. comandos executados;
5. testes e resultados reais;
6. commits criados;
7. limitações ou problemas conhecidos;
8. próximo pequeno incremento recomendado.

O usuário deseja aprender. Explicar conceitos novos com clareza técnica depois da implementação dentro de escopo já aprovado, sem transformar cada entrega em documentação excessiva.
