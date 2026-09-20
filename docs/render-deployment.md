# Demonstração pública no Render Free

Esta é uma vitrine educacional sem banco, login ou carteira salva. O frontend React compilado é servido pelo mesmo processo Java que calcula o aporte. O código no computador continua independente: editar ou executar localmente não modifica o site público. A versão online só muda depois de commits enviados à `main`, CI aprovada e deploy concluído.

## Antes de criar o serviço

1. Confirme que a branch `main` contém `Dockerfile`, `render.yaml` e o perfil `demo`, com CI verde. O Render lerá essa branch; uma branch local ou PR não alteram a versão pública.
2. Entre em [dashboard.render.com](https://dashboard.render.com/) com sua própria conta e conecte o GitHub via autorização OAuth. Autorize o acesso ao repositório `thiagojosetj/pagina-investimentos`. Não cole senha nem token no chat ou no repositório.
3. No painel Render, escolha **New → Blueprint** e selecione esse repositório. Confira o plano **Free** antes de confirmar; não aceite uma troca para plano pago sem decidir conscientemente. O Blueprint cria um Web Service `pagina-investimentos-demo`, sem banco.
4. Aguarde o build e o healthcheck. O painel mostrará a URL HTTPS `*.onrender.com` efetivamente atribuída; não suponha a URL apenas pelo nome do serviço.

O arquivo `render.yaml` define Docker, branch `main`, perfil `demo`, bind em `0.0.0.0`, healthcheck e deploy automático **após** os checks do GitHub Actions passarem. `PORT` é fornecida pelo Render e já é lida pelo Spring Boot. Não configure `DATABASE_URL`, credenciais ou um PostgreSQL para esta demonstração.

## Verificar a publicação

Substitua `<url-do-render>` pelo endereço exato mostrado no painel:

```powershell
Invoke-WebRequest 'https://<url-do-render>/'
Invoke-RestMethod 'https://<url-do-render>/actuator/health'
```

Abra a URL no navegador. Veja a visão geral sintética, acione **Simular esta demonstração** e execute um aporte fictício. O resultado deve aparecer na mesma página. Nenhum dado é salvo como carteira. Não use valores pessoais reais na demonstração pública.

Swagger e `/actuator/info` ficam desativados nesse perfil. O Render pode levar aproximadamente um minuto para acordar o serviço após inatividade; durante esse período, aguarde a página de carregamento. Uma falha persistente deve ser conferida nos logs e eventos do serviço, sem publicar corpos de requisição ou dados pessoais.

## Atualizar a versão online

1. Desenvolva e teste localmente em uma branch. O modo completo local pode usar PostgreSQL no Docker; o perfil `simulator` permite testar o cálculo sem ele.
2. Revise o diff, valide o código e faça commits pequenos. Antes de qualquer push, confirme branch, remoto, commits, arquivos e testes; peça aprovação para o envio.
3. Faça push para a branch aprovada, abra/revise um Pull Request e aguarde a CI. Mescle na `main` somente após autorização.
4. O Render observa `main` e, com `autoDeployTrigger: checksPass`, constrói a nova imagem após os checks passarem. Acompanhe **Events/Deploys** até o estado ativo e teste a URL novamente. Se a CI falhar, o deploy automático não começa; se o build do Render falhar, a versão anterior continua sendo a referência a investigar no painel.

Um `git pull` em outro dispositivo atualiza o código daquele dispositivo; não publica. Alterações locais sem push/merge também não publicam. O Render Free é para demonstração, não garante disponibilidade contínua, usa armazenamento efêmero e pode suspender por limites do plano. Nunca use `docker compose down -v` para atualizar o site; esse comando apaga o volume PostgreSQL local e não tem relação com o Render.

## Limites e próxima arquitetura

- A interface atual mostra uma carteira **inteiramente fictícia**. O simulador aceita entradas manuais, transmite-as à API para cálculo e não as persiste. Use apenas valores fictícios.
- A versão online não oferece cadastro, rentabilidade, dividendos, cotações ou integração financeira. O PostgreSQL existente é apenas local e de testes neste lançamento.
- O limite de corpo JSON reduz consumo por requisição, mas não há rate limiting completo; monitorar abuso é parte da operação.
- Contas e dados reais exigirão autenticação, autorização, banco durável, política de privacidade, backups/restauração, limites operacionais e nova aprovação de arquitetura/custo.

Fontes consultadas em 19 de setembro de 2026: [Render Free](https://render.com/docs/free), [Blueprint](https://render.com/docs/blueprint-spec), [Docker](https://render.com/docs/docker) e [deploy após CI](https://render.com/docs/deploys).
