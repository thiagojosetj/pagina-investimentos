# Demonstração pública no Render Free

Esta é uma vitrine educacional sem banco, login ou carteira salva. O frontend React compilado é servido pelo mesmo processo Java que calcula o aporte. O código no computador continua independente: editar ou executar localmente não modifica o site público. A versão online só muda depois de commits enviados à `main` e de um novo deploy concluído; a CI deve ser conferida para cada atualização.

## Serviço publicado

- **URL:** [https://pagina-investimentos-demo.onrender.com](https://pagina-investimentos-demo.onrender.com)
- **Serviço:** `pagina-investimentos-demo`, um Web Service Docker no plano Free, sem banco, login ou carteira salva.
- **Origem inicial:** commit `270fa663bf1136992b8893f3ca18183163587e98` da `main`, publicado em 21 de setembro de 2026. Para atualizações posteriores, confira o último deploy concluído no painel.
- **Verificações reais:** reconfirmadas em 27/09/2026, 11h28 (UTC−03): página HTTP 200 e health `UP`; POST com valores atuais fictícios `60.00`/`40.00`, metas `50.0000`/`50.0000` e aporte `100.00` retornou parcelas `40.00`/`60.00`. O asset servido nessa verificação foi `/assets/index-Cqhwipg4.js`, da versão anterior a este incremento.

O `Dockerfile` e o perfil `demo` continuam a definir o pacote da demonstração. O `render.yaml` é a configuração declarativa de referência; como o serviço foi criado manualmente no painel, a configuração efetiva de Auto-Deploy deve ser conferida no próprio Render. `PORT` é fornecida pelo Render e já é lida pelo Spring Boot. Não configure `DATABASE_URL`, credenciais ou um PostgreSQL para esta demonstração.

A nova imagem contém defaults de `SERVER_ADDRESS=0.0.0.0`, `PORT=10000` e opções de memória da JVM; o Render pode sobrescrevê-los. O perfil `demo` aplica headers, cache específico para assets, revalidação do HTML, compressão e limites Tomcat. Confirme o deploy efetivo antes de afirmar que essas proteções estão online. O formulário permite até 120 segundos de espera e cancelamento; a disponibilidade do plano gratuito continua limitada.

## Verificar a publicação

Use o endereço público efetivamente atribuído ao serviço:

```powershell
Invoke-WebRequest 'https://pagina-investimentos-demo.onrender.com/'
Invoke-RestMethod 'https://pagina-investimentos-demo.onrender.com/actuator/health'
```

Abra a URL no navegador. Veja a visão geral sintética, acione **Simular esta demonstração** e execute um aporte fictício. O resultado deve aparecer na mesma página. Nenhum dado é salvo como carteira. Não use valores pessoais reais na demonstração pública.

Swagger e `/actuator/info` ficam desativados nesse perfil. O Render pode levar aproximadamente um minuto para acordar o serviço após inatividade; durante esse período, aguarde a página de carregamento. Uma falha persistente deve ser conferida nos logs e eventos do serviço, sem publicar corpos de requisição ou dados pessoais.

## Atualizar a versão online

1. Desenvolva e teste localmente em uma branch. O modo completo local pode usar PostgreSQL no Docker; o perfil `simulator` permite testar o cálculo sem ele.
2. Revise o diff, valide o código e faça commits pequenos. Antes de qualquer push, confirme branch, remoto, commits, arquivos e testes; peça aprovação para o envio.
3. Faça push para a branch aprovada, abra/revise um Pull Request e aguarde a CI. Mescle na `main` somente após autorização.
4. Após o merge em `main`, confira a configuração de Auto-Deploy do serviço no painel e acompanhe **Events/Deploys** até o estado ativo. Como este serviço foi criado manualmente, não presuma que alterações futuras em `render.yaml` mudem a configuração já existente. Se a CI ou o build do Render falhar, investigue os logs antes de considerar a versão atualizada.

Um `git pull` em outro dispositivo atualiza o código daquele dispositivo; não publica. Alterações locais sem push/merge também não publicam. O Render Free é para demonstração, não garante disponibilidade contínua, usa armazenamento efêmero e pode suspender por limites do plano. Nunca use `docker compose down -v` para atualizar o site; esse comando apaga o volume PostgreSQL local e não tem relação com o Render.

## Limites e próxima arquitetura

- A interface atual mostra uma carteira **inteiramente fictícia**. O simulador aceita entradas manuais, transmite-as à API para cálculo e não as persiste. Use apenas valores fictícios.
- A versão online não oferece cadastro, rentabilidade, dividendos, cotações ou integração financeira. O PostgreSQL existente é apenas local e de testes neste lançamento.
- O limite de corpo JSON reduz consumo por requisição, mas não há rate limiting completo; monitorar abuso é parte da operação.
- Contas e dados reais exigirão autenticação, autorização, banco durável, política de privacidade, backups/restauração, limites operacionais e nova aprovação de arquitetura/custo.

Fontes consultadas em 19 de setembro de 2026: [Render Free](https://render.com/docs/free), [Blueprint](https://render.com/docs/blueprint-spec), [Docker](https://render.com/docs/docker) e [deploy após CI](https://render.com/docs/deploys).
