# Melhorias priorizadas — 27 de setembro de 2026

Revisão técnica do incremento de vendas simuladas e da demonstração pública. O objetivo é corrigir comportamento, reprodutibilidade e documentação; sugestões não são requisitos automáticos.

## Incluídas neste incremento

- Versionamento do perfil `without-docker`, tags PostgreSQL, script e execução portátil do IntelliJ; teste completo permanece obrigatório na CI.
- README, especificação, status e decisões distinguem vendas opcionais, demonstração publicada e planos futuros. O YAML do Render é referência, não comprovação das opções do serviço manual.
- Região de anúncio persistente, validade das metas em texto e soma exata sem ponto flutuante; percentual acima de quatro casas é sinalizado.
- Cancelamento explícito, timeout de 120 segundos e mensagens distintas. Cancelar não garante parar trabalho já recebido no servidor.
- Headers somente no perfil público, inclusive em 413; CSP compatível com os estilos atuais, sem comprometer Swagger local.
- Cache imutável só para assets com hash, HTML revalidável, compressão e limites conservadores do Tomcat.
- Bind/porta/memória definidos na imagem; CI valida esses defaults, headers, cache, gzip, rotas bloqueadas e compras/vendas simuladas.
- Cancelamento automático da CI apenas em PRs; histórico local de `main` atualizado por fast-forward, sem reescrita.

## Próximos três incrementos

1. **INC-008B:** implementação local de leitura consistente e nomes concluída; falta executar as regressões PostgreSQL e validar a CI antes da publicação.
2. **INC-010A:** revisar/aprovar [`AUTH_SECURITY_PLAN.md`](AUTH_SECURITY_PLAN.md) e seu ambiente antes de implementar sessão, CSRF e identidade. Sem dados reais ou endpoint anônimo de carteira.
3. **Primeira fatia autenticada:** após as duas etapas, criar/consultar carteira e metas com DTOs, erros uniformes e tela integrada. Cadastro de ativos e movimentações vem depois, sobre essa base.

## Adiadas e motivo

- Rate limiting por IP: definir proxy confiável e armazenamento limitado com expiração. A sugestão de mapa sem limite e uso livre de `X-Forwarded-For` não é segura.
- Erros HTTP adicionais e exceções de carteiras: acrescentar com testes e com o primeiro controller correspondente; não inventar contrato antes de seu consumidor.
- Revisão de leitura PUT/PATCH/DELETE e wrapper: manter testes de corpo atuais e ampliar com um fluxo real; não refatorar leitura assíncrona inexistente.
- Unificar perfis sem banco: duplicação pequena e explícita; não justifica ampliar a mudança agora.
- Nome fixo do JAR, build-info, digests de imagens e cache Maven: avaliar política de atualização e medir o build antes de acrescentar manutenção.
- Limpeza de branches e normalização global CRLF: não são correções funcionais; nenhum histórico compartilhado será reescrito.

## Visão de longo prazo — ainda não entregue

Carteira organizada por categoria e ativo, compras/vendas registradas, posição e custo derivados, cotações manuais com origem/data, dashboard conectado, rentabilidade e proventos. Provedores legais, catálogo com autocomplete, benchmarks e importação segura exigem decisões próprias. Web responsiva vem primeiro; PWA e cliente móvel dependem de necessidade e aprovação. A vitrine atual não tem login, ativos cadastrados, carteira salva ou cotações reais.
