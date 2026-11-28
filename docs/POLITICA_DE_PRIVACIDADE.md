> # ⚠️ RASCUNHO: NÃO PUBLICAR
>
> **Este documento é um rascunho e ainda não vale como política de privacidade.**
>
> - Não é aconselhamento jurídico. Revise com um advogado antes de publicar.
> - Os campos `[ENTRE COLCHETES]` precisam ser preenchidos.
> - Os itens marcados como **PLANEJADO** descrevem funcionalidades que ainda não existem. Mantenha-os só se elas estiverem disponíveis quando a política for publicada.
> - A seção "Notas para a revisão", no fim, lista o que ainda precisa ser decidido. Ela não faz parte do texto final.
> - Ao publicar, remova este aviso e as notas, e preencha a versão e a data abaixo.
>
> Rascunho escrito a partir do comportamento da API em 2026-11-27 (item #10 do ROADMAP).

# Política de Privacidade do CriptoVision

**Versão:** `[VERSÃO]`. **Vigente desde:** `[DATA DE PUBLICAÇÃO]`.

Esta política explica quais dados pessoais o CriptoVision trata, para quê, com qual base legal, por quanto tempo e com quem eles são compartilhados. Ela segue a Lei Geral de Proteção de Dados (Lei nº 13.709/2018, "LGPD").

## 1. Quem é o controlador

O controlador dos dados é `[NOME COMPLETO OU RAZÃO SOCIAL]`, `[CPF OU CNPJ, se for publicado]`, responsável pelo CriptoVision (`[ENDEREÇO DO SITE]`).

Para qualquer assunto sobre os seus dados, inclusive os pedidos da seção 7, escreva para **`[E-MAIL DE CONTATO]`**. `[ENCARREGADO: nome do encarregado pelo tratamento de dados, ou a frase "O canal acima faz as vezes de encarregado", se a dispensa para agentes de pequeno porte se aplicar; veja as notas.]`

## 2. Os dados que tratamos

A tabela mostra cada dado que o CriptoVision guarda ou usa, com a finalidade, a base legal (artigo 7º da LGPD) e o tempo de retenção. Nenhum dado é usado para outra finalidade além da indicada.

### 2.1 Dados da conta e do serviço

| Dado | Finalidade | Base legal | Retenção |
| --- | --- | --- | --- |
| **Login** (nome de usuário escolhido por você) | Identificar a sua conta e permitir o acesso | Execução de contrato (art. 7º, V) | Enquanto a conta existir. Apagado na exclusão da conta e, nos backups, em até 30 dias (seção 6) |
| **Senha**, guardada só como hash (bcrypt) | Autenticar o acesso. A senha em si nunca é guardada nem registrada em log | Execução de contrato (art. 7º, V) | Igual ao login |
| **Transações**: ativo, tipo (compra ou venda), quantidade, preço unitário e data | Calcular a carteira, o preço médio, o resultado (PNL), os totais e as simulações que você pede | Execução de contrato (art. 7º, V) | Igual ao login |
| **Versão das sessões**, um contador interno | Encerrar as sessões de todos os dispositivos no logout e na troca de senha | Execução de contrato (art. 7º, V) | Igual ao login |
| **Token de acesso** (JWT) | Manter a sessão aberta. Ele fica no seu dispositivo, não no servidor | Execução de contrato (art. 7º, V) | Expira em 2 horas, ou antes, no logout ou na troca de senha |

### 2.2 Dados de segurança e de funcionamento

| Dado | Finalidade | Base legal | Retenção |
| --- | --- | --- | --- |
| **Endereço IP e login** usados nas tentativas de login, cadastro, troca de senha e exclusão de conta | Bloquear ataques de força bruta (tentativas de adivinhar senhas ou de criar contas em massa) | Legítimo interesse (art. 7º, IX), para a segurança exigida pelo art. 46 | Só em memória, por no máximo 1 hora (a maior janela de bloqueio). Perdidos quando o servidor reinicia |
| **Registros técnicos (logs)**: IP e login quando um bloqueio acontece, rota acessada, identificador da requisição, horário e erros | Investigar falhas e incidentes de segurança e manter o serviço funcionando | Legítimo interesse (art. 7º, IX) | Os logs são sobrescritos por tamanho, até 50 MB por serviço. `[DEFINIR um prazo máximo em dias; veja a nota sobre o Marco Civil]` |
| **Cópias de segurança (backups)** do banco: login, hash da senha e transações | Recuperar o serviço e os seus dados numa falha ou num incidente | Legítimo interesse (art. 7º, IX), para a segurança exigida pelo art. 46 | 14 dias no servidor e 30 dias no armazenamento externo. Os backups são cifrados antes de sair do servidor |

### 2.3 Dados de funcionalidades planejadas

> **PLANEJADO:** as funcionalidades desta seção ainda não existem. Revise esta seção quando cada uma for lançada (ROADMAP #29).

| Dado | Finalidade | Base legal | Retenção |
| --- | --- | --- | --- |
| **Textos do diário** de investimentos | Guardar as anotações que você escrever ou ditar | Execução de contrato (art. 7º, V) | Enquanto a conta existir, ou até você apagar a anotação |
| **Áudio do ditado por voz** no diário | Transformar a sua fala em texto | Consentimento (art. 7º, I): você ativa o microfone a cada uso | **O CriptoVision não recebe nem guarda o áudio.** Ele é processado pelo serviço de voz do navegador (seção 4), e só o texto que você decidir salvar chega ao CriptoVision |
| **Endereço de e-mail** para alertas | Enviar os alertas de preço que você configurar | Consentimento (art. 7º, I), que você pode revogar desativando os alertas | Até você desativar os alertas por e-mail ou excluir a conta |
| **Identificador do Telegram** (chat ID) para alertas | Enviar os alertas que você configurar pelo Telegram | Consentimento (art. 7º, I), que você pode revogar desativando os alertas | Até você desativar os alertas pelo Telegram ou excluir a conta |
| **Configuração dos alertas**: ativo, condição e preço-alvo | Saber quando avisar você | Execução de contrato (art. 7º, V) | Até você apagar o alerta ou excluir a conta |

**Atenção ao diário:** evite escrever dados sensíveis (saúde, religião, opinião política etc.) e dados de outras pessoas. O diário é para anotações sobre os seus investimentos.

## 3. O que não fazemos

- Não pedimos nome, CPF, telefone, endereço nem documentos. O e-mail só será pedido se você ativar os alertas por e-mail (planejado).
- Não tratamos dados sensíveis (art. 5º, II) de propósito.
- Não vendemos, alugamos nem cedemos dados. Não há publicidade nem rastreamento para anúncios.
- A API não usa cookies. `[VERIFICAR no frontend: o token fica no localStorage, no sessionStorage ou num cookie? Há ferramenta de análise de uso, como o Google Analytics? Se houver, ela precisa entrar nas seções 2 e 4.]`
- Não tomamos decisões automatizadas sobre você (art. 20). As simulações e os cálculos da carteira são feitos a seu pedido e não são recomendação de investimento.

## 4. Serviços de terceiros

Alguns serviços externos participam do funcionamento do CriptoVision. A tabela diz o que cada um recebe.

| Serviço | Para quê | O que recebe | Dados pessoais? |
| --- | --- | --- | --- |
| **Hospedagem**: `[PROVEDOR DA VPS]`, em `[PAÍS/REGIÃO]` | Rodar a aplicação e o banco de dados | Todos os dados da seção 2, armazenados no servidor | Sim. Atua como operador, seguindo as nossas instruções |
| **Armazenamento dos backups**: `[PROVEDOR DO BUCKET: Cloudflare R2, Backblaze B2 ou AWS S3]`, em `[PAÍS/REGIÃO]` | Guardar uma cópia dos backups fora do servidor | Os backups, **cifrados com AES-256 antes do envio**. A chave nunca é enviada ao provedor, que não consegue ler o conteúdo | Sim, mas ilegíveis para o provedor. Atua como operador |
| **Binance** (dados públicos de mercado) | Obter os preços e a variação de 24 horas das criptomoedas | Só os códigos dos ativos (como `BTC`). A consulta é feita pelo servidor, não pelo seu navegador | **Não.** A Binance não recebe o seu login, o seu IP nem as suas transações |
| **PLANEJADO. Fonte das cotações do Ibovespa e do CDI**: `[FONTE, por exemplo B3 ou Banco Central (SGS)]` | Comparar a carteira com o Ibovespa e o CDI | Só os índices e as datas pedidos. A consulta é feita pelo servidor | **Não** |
| **PLANEJADO. Reconhecimento de voz do navegador** (Web Speech API) | Transcrever o ditado do diário | O **áudio** da sua fala. No Google Chrome, ele é enviado aos servidores do **Google**; no Microsoft Edge, aos da **Microsoft**. Outros navegadores podem usar outros provedores ou processar no próprio aparelho | Sim (a sua voz). O provedor do navegador trata o áudio sob a política dele: [Google](`[LINK DA POLÍTICA DO GOOGLE]`), [Microsoft](`[LINK DA POLÍTICA DA MICROSOFT]`). O ditado só começa quando você ativa o microfone, e o CriptoVision só recebe o texto |
| **PLANEJADO. Envio de e-mails**: `[PROVEDOR SMTP]`, em `[PAÍS/REGIÃO]` | Entregar os alertas por e-mail | O seu e-mail e o conteúdo do alerta (ativo e preço) | Sim. Atua como operador |
| **PLANEJADO. Telegram** | Entregar os alertas pelo Telegram | O seu chat ID e o conteúdo do alerta (ativo e preço) | Sim. O Telegram também trata os dados sob a própria política: [Telegram](`[LINK DA POLÍTICA DO TELEGRAM]`) |

Fora desses casos, os dados só são compartilhados por ordem judicial ou exigência legal.

### Transferência internacional

Alguns desses serviços ficam fora do Brasil, como a Binance, o Google, a Microsoft, o Telegram e, conforme a escolha, a hospedagem, os backups e o e-mail. Quando eles recebem dados pessoais, a transferência segue o art. 33 da LGPD. `[DEFINIR o mecanismo de cada transferência, por exemplo as cláusulas-padrão da ANPD ou o consentimento específico; veja as notas.]`

## 5. Segurança

- Todo acesso à API é cifrado (HTTPS), e a conexão entre a API e o banco também usa TLS.
- As senhas são guardadas só como hash (bcrypt).
- As sessões expiram em 2 horas e podem ser encerradas em todos os dispositivos.
- Tentativas repetidas de login, de cadastro e de confirmação de senha são bloqueadas por um tempo.
- Cada usuário só acessa os próprios dados. Uma transação de outra conta responde como se não existisse.
- O banco de dados e a área de administração não ficam expostos à internet.
- Os backups são cifrados, e a chave fica guardada fora do armazenamento externo.
- Os logs não registram senhas nem tokens.

Num incidente de segurança que possa trazer risco ou dano relevante, avisaremos a Autoridade Nacional de Proteção de Dados (ANPD) e as pessoas afetadas, como manda o art. 48. `[VERIFICAR o prazo atual da ANPD; veja as notas.]`

## 6. Exclusão da conta

Você pode excluir a sua conta a qualquer momento, pelo aplicativo (`[NOME DA OPÇÃO NO FRONTEND, como "Excluir conta"]`) ou pela API (`POST /conta/exclusao`), confirmando a senha atual. A exclusão:

- é **imediata e definitiva** no banco de dados: a conta e todas as transações são apagadas, e o seu login fica livre para um novo cadastro;
- encerra todas as sessões;
- alcança os **backups** quando eles expiram: em até 14 dias no servidor e 30 dias no armazenamento externo. Até lá, eles continuam cifrados e só seriam usados para recuperar o serviço numa falha. Se uma restauração acontecer nesse período, a conta excluída será apagada de novo;
- não apaga de imediato os logs técnicos, que são sobrescritos no prazo da seção 2.2.

Recomendamos exportar os seus dados (seção 7) antes de excluir a conta.

## 7. Os seus direitos

A LGPD (art. 18) garante os direitos abaixo. Os principais funcionam direto no aplicativo, sem precisar pedir.

| Direito | Como exercer |
| --- | --- |
| Confirmação de que tratamos os seus dados e acesso a eles | Exporte os seus dados: `[NOME DA OPÇÃO NO FRONTEND]` ou `GET /conta/exportacao`. O arquivo traz a conta e todas as transações |
| Portabilidade | A mesma exportação, num arquivo JSON estruturado, que pode ser lido por outros sistemas |
| Correção | Edite ou apague as transações no aplicativo, e troque a senha a qualquer momento. Para corrigir o login, escreva para o contato da seção 1 |
| Eliminação | Exclua a conta (seção 6) |
| Informação sobre compartilhamento | A seção 4 lista todos os serviços que recebem dados |
| Revogação do consentimento | Desative os alertas ou deixe de usar o ditado por voz (planejado). O que foi feito antes da revogação continua válido |
| Oposição, revisão e demais pedidos | Escreva para o contato da seção 1 |

Os pedidos enviados ao contato são respondidos em até 15 dias (art. 19). Se não ficar satisfeito, você pode reclamar à ANPD (www.gov.br/anpd).

## 8. Crianças e adolescentes

O CriptoVision trata de investimentos e não é destinado a menores de `[IDADE MÍNIMA, por exemplo 18]` anos.

## 9. Alterações desta política

Quando esta política mudar, a versão e a data no topo serão atualizadas. Mudanças relevantes, como um dado novo, uma finalidade nova ou um serviço de terceiros novo, serão avisadas com `[N]` dias de antecedência por `[CANAL, por exemplo um aviso no aplicativo]`. Toda funcionalidade nova que trate dados pessoais passa por uma revisão desta política antes de ser lançada.

---

## Notas para a revisão (remover antes de publicar)

Pontos que dependem de decisão do autor ou de um advogado:

1. **Marco Civil da Internet (Lei nº 12.965/2014, art. 15):** um provedor de aplicação constituído como pessoa jurídica, com fins econômicos, precisa guardar os registros de acesso (IP, data e hora) por 6 meses. Hoje os logs são sobrescritos por tamanho, e a API não guarda o IP de todo acesso. Se o CriptoVision se enquadrar, é preciso um registro de acesso com 6 meses de retenção, e as seções 2.2 e 6 mudam.
2. **Encarregado:** o art. 41 exige um encarregado. A Resolução CD/ANPD nº 2/2022 dispensa os agentes de tratamento de pequeno porte, desde que haja um canal de comunicação com o titular. Confirme se o CriptoVision se enquadra.
3. **Transferência internacional:** defina o mecanismo de cada serviço no exterior (art. 33 e o regulamento da ANPD sobre transferência internacional e cláusulas-padrão). Escolher hospedagem, backups e e-mail no Brasil reduz o problema.
4. **Papel dos provedores de voz e do Telegram:** confirme se o Google, a Microsoft e o Telegram atuam como controladores independentes (com a própria política) ou como operadores, e ajuste a seção 4.
5. **Prazo para comunicar incidentes:** confirme o prazo atual do regulamento da ANPD sobre comunicação de incidentes de segurança e escreva-o na seção 5.
6. **Contas inativas:** hoje uma conta fica guardada enquanto existir, mesmo sem uso. Avalie excluir contas inativas depois de um prazo (por exemplo, 2 anos com aviso prévio) e, se for o caso, escreva isso na seção 2.1.
7. **Prazos dos backups:** os 14 e os 30 dias vêm dos padrões `BACKUP_RETENCAO_DIAS` e `BACKUP_RETENCAO_REMOTA_DIAS`. Se mudar a configuração no `.env`, mude o texto também.
8. **Frontend:** confira o armazenamento do token, os cookies, as ferramentas de análise e os nomes das opções de exportar e excluir. Publique a política num endereço fixo e coloque o link no cadastro.
9. **Novas funcionalidades:** cada dado novo precisa entrar na tabela da seção 2, na exportação (`GET /conta/exportacao`) e na exclusão da conta. Veja o ROADMAP #29.
