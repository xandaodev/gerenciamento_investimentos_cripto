# Regras de Negócio do CriptoVision

## 1. Objetivo

Este documento registra as regras financeiras implementadas atualmente no CriptoVision.

Ele serve como referência para:

* criação de testes;
* identificação de inconsistências;
* refatoração segura;
* desenvolvimento do frontend;
* criação de novos recursos;
* validação dos resultados apresentados ao usuário.

As regras abaixo descrevem o comportamento atual. Quando uma regra ainda não está completamente implementada, isso é informado explicitamente.

---

## 2. Conceitos principais

### Transação

Representa uma operação de compra ou venda de criptomoeda.

Cada transação contém:

```text
ticker
quantidade
preço unitário
tipo
data
identificador
usuário proprietário
```

Cada transação pertence obrigatoriamente a um único usuário. O proprietário não é escolhido no corpo da requisição; ele é obtido do JWT autenticado.

### Ticker

É o código utilizado para identificar uma criptomoeda.

Exemplos:

```text
BTC
ETH
SOL
LINK
```

### Saldo

É a quantidade atual de uma criptomoeda mantida na carteira.

### Preço médio

É o custo médio unitário das unidades que permanecem na carteira. Ele não é armazenado: é calculado a partir do custo da posição sempre que é lido.

```text
preço médio = custo da posição ÷ saldo atual
```

Sem saldo, o preço médio é zero.

### Custo da posição

É o custo de aquisição atribuído ao saldo atual. Ele é acumulado transação a transação, sem arredondamento nas compras (veja as seções 6 e 8).

### Valor atual da posição

É o valor de mercado do saldo atual.

```text
valor atual = saldo atual × preço atual
```

### PNL não realizado

É o lucro ou prejuízo que existiria caso todo o saldo atual fosse vendido pelo preço de mercado informado.

```text
PNL não realizado = valor atual - custo da posição
```

### Lucro realizado

É o lucro ou prejuízo concretizado por meio de uma venda.

O código calcula esse valor durante o processamento de uma venda, mas ele ainda não é armazenado nem incluído nos resumos da carteira.

---

## 3. Isolamento por usuário

O CriptoVision mantém um histórico financeiro independente para cada usuário autenticado.

### Cadastro de conta

Uma conta é criada por `POST /auth/register` com login e senha:

- o login é obrigatório, possui de 3 a 100 caracteres e aceita somente letras, números, ponto, hífen e underline;
- o login é normalizado para letras minúsculas antes de ser salvo;
- a senha é obrigatória e possui de 8 a 64 caracteres;
- a senha é armazenada somente como hash BCrypt;
- um login já cadastrado é rejeitado com `409 Conflict`.

### Login

`POST /auth/login` recebe login e senha e devolve um token JWT válido por 2 horas.

Um login inexistente e uma senha incorreta recebem exatamente a mesma resposta, `401 Unauthorized` com a mensagem "Login ou senha inválidos.", e levam um tempo de resposta equivalente. Assim, não é possível descobrir quais logins estão cadastrados tentando entrar.

### Limite de tentativas

Para frear ataques de força bruta, o login e o cadastro têm limites de tentativas. Os valores padrão são:

| Limite | Padrão | O que conta |
| --- | --- | --- |
| Login por IP | 20 por minuto | toda tentativa de login vinda do IP |
| Falhas por login | 5 em 15 minutos | só as senhas erradas para aquele login |
| Cadastro por IP | 10 por hora | toda tentativa de cadastro vinda do IP, inclusive as recusadas com `409` |

Regras:

- depois de 5 senhas erradas, o login fica bloqueado até a janela de 15 minutos fechar, **mesmo que a senha certa seja informada**;
- um login com sucesso zera a contagem de falhas daquele login;
- `Alexandre`, ` alexandre ` e `ALEXANDRE` contam como o mesmo login, como no cadastro;
- um login inexistente é bloqueado do mesmo jeito que um existente, para que o bloqueio não revele quais contas existem;
- a janela começa na primeira tentativa, e tentativas recusadas não estendem o bloqueio;
- requisições recusadas pela validação (`400`) não contam.

Quando um limite é atingido, a API responde `429 Too Many Requests` com a mensagem "Muitas tentativas. Tente novamente em N minutos." (ou segundos) e o cabeçalho `Retry-After` em segundos. A resposta é a mesma para qualquer limite: ela não diz se o bloqueio veio do IP ou do login.

Os limites podem ser ajustados pelas propriedades `seguranca.tentativas.*`.

### Sessões, logout e troca de senha

- o token JWT vale por 2 horas;
- `POST /auth/logout` encerra **todas** as sessões do usuário, em todos os dispositivos: nenhum token emitido antes continua válido;
- `PUT /auth/senha` exige a senha atual e uma nova senha com 8 a 64 caracteres, diferente da atual. Depois da troca, todos os tokens emitidos antes deixam de valer, e a resposta traz um token novo para a sessão atual continuar;
- uma senha atual errada responde `400` sem revogar nada, mas conta como falha de login: depois de 5 falhas em 15 minutos, a troca de senha e o login daquela conta ficam bloqueados com `429` até a janela fechar.

### Associação obrigatória

Cada transação possui um proprietário obrigatório:

```text
Transacao.usuario → Usuario
transacoes.usuario_id → usuarios.id
```

A coluna `usuario_id` não aceita valor nulo. Na criação, a aplicação associa a transação ao `Usuario` presente no contexto de segurança.

O cliente não envia `usuarioId` e não pode registrar uma transação em nome de outra conta.

### Escopo das operações

As seguintes operações utilizam somente os dados do usuário autenticado:

* criação e listagem de transações;
* busca de transação por ID;
* atualização e exclusão;
* reconstrução da carteira;
* cálculo do total histórico;
* resumo da carteira;
* simulação de DCA;
* simulação de venda;
* total aportado por moeda.

A busca, a atualização e a exclusão exigem simultaneamente o ID da transação e o proprietário.

### Acesso a dados de outra conta

Quando um usuário tenta consultar, alterar ou excluir uma transação pertencente a outra conta, a API retorna:

```text
404 Not Found
```

A API usa `404`, em vez de revelar que o ID existe para outro usuário. Nenhuma alteração é realizada no recurso.

### Proteção das respostas

O campo `usuario` da entidade `Transacao` não é serializado nas respostas JSON. Dados como login e senha não são expostos junto das transações.

---

## 4. Tipos de transação

O sistema representa o tipo pelo enum `TipoTransacao`.

Valores permitidos:

```text
COMPRA
VENDA
```

No motor financeiro, as comparações são feitas com `TipoTransacao.COMPRA` e `TipoTransacao.VENDA`, e não com textos livres.

Na entrada JSON, valores como `"compra"` e `" Compra "` são normalizados. Qualquer outro valor, como `"TROCA"`, é rejeitado antes de chegar ao service e produz resposta `400 Bad Request`.

---

## 5. Normalização e validação de entrada

### Normalização do ticker

O `TransacaoRequestDTO` remove espaços nas extremidades e converte o ticker para letras maiúsculas.

Exemplo:

```text
" btc " → "BTC"
```

O ticker deve conter somente letras e números e possuir no máximo 20 caracteres.

Depois da validação do DTO, o `TickerNormalizer` converte alguns nomes conhecidos para o ticker oficial:

```text
BITCOIN   → BTC
ETHEREUM  → ETH
SOLANA    → SOL
CHAINLINK → LINK
LNK       → LINK
```

Essa conversão é aplicada na criação, na atualização, na reconstrução da carteira, nas simulações e nas consultas de cotação. Assim, uma transação enviada com `"bitcoin"` é salva com o ticker `BTC`. Os demais valores são usados como informados, em maiúsculas.

### USDT

O USDT recebe tratamento especial no `CotacaoService` e nunca é consultado na Binance:

```text
preço = 1
variação de 24 horas = 0
```

### Validação da criação e da atualização

Os endpoints `POST /transacoes` e `PUT /transacoes/{id}` utilizam `TransacaoRequestDTO` e exigem:

- ticker obrigatório;
- ticker com no máximo 20 caracteres;
- ticker formado somente por letras e números;
- quantidade obrigatória e maior que zero, com no máximo 20 dígitos inteiros e 18 casas decimais;
- preço unitário obrigatório e maior que zero, com no máximo 20 dígitos inteiros e 18 casas decimais;
- tipo obrigatório;
- tipo restrito a `COMPRA` ou `VENDA`;
- data opcional, com as regras da seção "Data da operação" abaixo.

O ID não é recebido pelo DTO: na criação ele é gerado pelo banco, e na atualização o original é preservado.

### Data da operação

O campo `data` permite lançar uma operação com a data real em que ela aconteceu, como uma compra antiga:

- o formato é ISO 8601 **com fuso**, como `2025-03-10T14:00:00-03:00` ou `2025-03-10T17:00:00Z`. Uma data sem fuso responde `400`, porque valeria horários diferentes conforme o lugar;
- a data é convertida e guardada em **UTC**, com precisão de microssegundos. As respostas mostram a data em UTC;
- na criação, sem `data`, vale o momento do cadastro, também em UTC;
- na alteração, sem `data`, a transação **mantém a data que já tinha**. Uma edição nunca move uma compra antiga para o momento atual sem querer;
- a data não pode estar no futuro, com 1 minuto de tolerância para a diferença entre o relógio do cliente e o do servidor;
- a data não pode ser anterior a 2009-01-03, quando o Bitcoin foi criado. Isso pega erros de digitação, como o ano 0202;
- uma data fora dessas regras responde `400 Data inválida`.

A carteira é sempre reconstruída na ordem dessas datas (seção 12), então uma operação lançada depois, mas com data anterior, entra no meio do histórico.

### Validação externa do ticker

Além da validação de formato, o ticker precisa existir como par com USDT na Binance:

- ticker rejeitado pela Binance → `400 Bad Request` ("Ticker inválido");
- Binance indisponível, mas com cotação do ticker obtida há no máximo `cotacoes.cache.max-stale` (15 minutos por padrão) → o ticker é aceito;
- Binance indisponível e sem cotação recente → `503 Service Unavailable`, e a transação não é salva.

Uma indisponibilidade do provedor não é mais confundida com ticker inexistente.

---

## 6. Regra de compra

Uma compra aumenta o saldo e o custo da posição. O preço médio muda como consequência.

Considere:

```text
SA = saldo atual
CA = custo da posição atual
QC = quantidade comprada
PC = preço unitário da compra
```

### Custo da nova compra

```text
custo da nova compra = QC × PC
```

### Novo saldo

```text
novo saldo = SA + QC
```

### Novo custo da posição

```text
novo custo = CA + custo da nova compra
```

### Novo preço médio

```text
novo preço médio = novo custo ÷ novo saldo
```

### Precisão

Multiplicação e soma em `BigDecimal` são exatas, então o custo da posição é acumulado sem nenhum arredondamento.

O preço médio só é arredondado quando é lido:

```text
escala = 18 casas decimais (a mesma do preço unitário no banco)
modo = HALF_UP
```

Esse valor arredondado serve apenas para exibição e nunca volta para o cálculo da transação seguinte. Por isso, o erro de arredondamento não se acumula de uma compra para outra, o que é decisivo em moedas de preço muito baixo.

### Exemplo

Posição atual:

```text
Saldo: 1 BTC
Custo da posição: 50.000 USDT
Preço médio: 50.000 USDT
```

Nova compra:

```text
Quantidade: 1 BTC
Preço: 60.000 USDT
```

Cálculo:

```text
Custo da compra = 1 × 60.000 = 60.000
Novo saldo = 1 + 1 = 2
Novo custo = 50.000 + 60.000 = 110.000
Novo preço médio = 110.000 ÷ 2 = 55.000
```

Resultado:

```text
Saldo: 2 BTC
Custo da posição: 110.000 USDT
Preço médio: 55.000 USDT
```

### Exemplo com preço muito baixo

```text
Compra: 1.000.000 PEPE a 0,0000012345 USDT
Custo da posição = 1.000.000 × 0,0000012345 = 1,2345
Preço médio = 1,2345 ÷ 1.000.000 = 0,0000012345
```

Com apenas 8 casas decimais, esse preço médio seria exibido como `0,00000123`.

---

## 7. Validações da compra

Nas requisições HTTP de criação e atualização, o `TransacaoRequestDTO` rejeita quantidade e preço nulos, iguais a zero ou negativos. Também rejeita ticker e tipo ausentes.

O método interno `processarTransacao`, isoladamente, não repete todas essas verificações para compras. Portanto, a proteção completa depende atualmente de a criação e a atualização entrarem pelos fluxos validados do controller e do service.

Essa duplicação de proteção no núcleo financeiro ainda pode ser adicionada futuramente como defesa adicional.

---

## 8. Regra de venda

Uma venda reduz o saldo do ativo e o custo da posição.

Considere:

```text
SA = saldo atual
CA = custo da posição atual
QV = quantidade vendida
PV = preço unitário da venda
```

### Novo saldo

```text
novo saldo = SA - QV
```

### Novo custo da posição

O custo é reduzido na mesma proporção do saldo:

```text
novo custo = CA × novo saldo ÷ SA
```

Essa é a única divisão do motor financeiro. Ela é feita com 34 dígitos significativos e `HALF_UP`, então o erro dela é desprezível. Quando todo o saldo é vendido, o novo custo é exatamente zero (veja a seção 11).

### Preço médio após a venda

Como saldo e custo diminuem na mesma proporção, o preço médio das unidades restantes não é alterado.

Exemplo:

```text
Antes da venda:
Saldo = 2 BTC
Custo da posição = 100.000 USDT
Preço médio = 50.000 USDT

Venda:
Quantidade = 0,5 BTC

Depois da venda:
Saldo = 1,5 BTC
Custo da posição = 100.000 × 1,5 ÷ 2 = 75.000 USDT
Preço médio = 75.000 ÷ 1,5 = 50.000 USDT
```

Essa é a regra utilizada atualmente pelo sistema.

---

## 9. Validações atuais de venda

### Quantidade positiva

A quantidade vendida deve ser maior que zero.

```text
quantidade da venda > 0
```

Caso contrário, o sistema lança uma exceção.

### Saldo suficiente

A quantidade vendida não pode ser maior que o saldo atual.

```text
quantidade vendida ≤ saldo atual
```

Caso contrário, o sistema lança `SaldoInsuficienteException`.

Exemplo inválido:

```text
Saldo: 0,5 BTC
Tentativa de venda: 1 BTC
```

---

## 10. Lucro realizado de uma venda

Durante o processamento da venda, o sistema calcula:

### Custo da parte vendida

```text
custo da parte vendida = custo da posição antes da venda - novo custo
```

Isso equivale a quantidade vendida × preço médio, mas sem usar o preço médio arredondado.

### Valor recebido

```text
valor recebido = quantidade vendida × preço da venda
```

### Lucro da operação

```text
lucro realizado = valor recebido - custo da parte vendida
```

### Exemplo

```text
Preço médio: 50.000 USDT
Quantidade vendida: 0,5 BTC
Preço da venda: 60.000 USDT
```

Cálculo:

```text
Custo da parte vendida = 0,5 × 50.000 = 25.000
Valor recebido = 0,5 × 60.000 = 30.000
Lucro realizado = 30.000 - 25.000 = 5.000
```

### Limitação atual

O valor é calculado em uma variável local, mas depois é descartado.

Atualmente ele:

* não é salvo na transação;
* não é acumulado na carteira;
* não aparece no resumo;
* não aparece nas análises.

---

## 11. Venda total da posição

Quando todo o saldo é vendido:

```text
novo saldo = 0
novo custo = 0
preço médio = 0
```

O ativo deixa de aparecer no resumo, porque somente moedas com saldo maior que zero são incluídas. Uma compra posterior começa uma posição nova, sem herdar o custo nem o preço médio da anterior.

---

## 12. Reconstrução da carteira

A carteira atual é calculada a partir do histórico completo do usuário autenticado.

Fluxo:

```text
Carteira vazia
    ↓
Primeira transação
    ↓
Segunda transação
    ↓
Terceira transação
    ↓
Estado atual da carteira
```

Para cada transação:

1. o ticker é normalizado pelo `TickerNormalizer`;
2. o ativo é obtido ou criado na carteira;
3. a transação é processada sem ser salva novamente.

### Fonte de verdade

O histórico de transações do usuário autenticado é atualmente a fonte de verdade para:

* saldo;
* preço médio;
* posição atual;
* resumo;
* simulações.

### Ordenação determinística

O histórico utilizado pelo `CarteiraService` contém somente as transações do usuário autenticado e é consultado em ordem crescente por `data` e `id`.

Além disso, `reconstruirCarteira` ordena uma cópia da lista recebida antes de processá-la. Assim, o resultado não depende da ordem acidental fornecida pelo banco ou por um teste.

### Tratamento de inconsistências

Quando uma transação não pode ser processada durante a reconstrução,
o cálculo é interrompido.

A aplicação lança `HistoricoInconsistenteException`, informando:

- ID da transação;
- ticker;
- tipo;
- motivo original da falha.

Uma transação inválida não é mais ignorada silenciosamente.

A resposta é um `500` com uma mensagem genérica e o `requestId` da requisição. A causa vai para o log de erro, com o mesmo ID, e cada ocorrência incrementa a métrica `criptovision.erros.internos` (tag `excecao=HistoricoInconsistenteException`). Assim, um histórico corrompido aparece no monitoramento em vez de ficar escondido, e o usuário pode informar o ID para localizar o caso.

---

## 13. Registro de nova transação

Antes de registrar uma nova transação, o sistema:

1. identifica o usuário autenticado;
2. associa a nova transação a esse usuário;
3. normaliza o ticker e valida sua existência na Binance;
4. define a data: a informada, validada e convertida para UTC, ou o momento do cadastro;
5. busca somente o histórico do usuário;
6. reconstrói a carteira **até a data da nova transação** e a processa nesse ponto;
7. reproduz o histórico inteiro com a nova transação na posição da data dela;
8. salva a transação no banco.

O passo 6 recusa uma venda sem saldo **naquela data** com `400 Saldo insuficiente`, como antes. O passo 7 existe por causa das datas retroativas: uma venda antiga pode caber no saldo da data dela e ainda assim deixar uma venda posterior sem saldo. Nesse caso, nada é salvo e a API responde `409 Conflict`, como na alteração e na exclusão.

Exemplo rejeitado:

```text
Histórico:
1. 2025-01-01  compra de 1 BTC
2. 2025-03-01  venda de 1 BTC

Tentativa:
2025-02-01  venda de 0,5 BTC
```

Em fevereiro havia 1 BTC, mas a venda de março ficaria sem saldo.

Exemplo aceito: lançar hoje uma compra com data de janeiro e, depois, uma venda com data de junho. A compra entra antes da venda no histórico, mesmo cadastrada depois.

### Operações simultâneas

Criação, atualização e exclusão de transações de um mesmo usuário são executadas uma de cada vez. Cada operação bloqueia o registro do usuário no banco antes de ler o histórico, e as demais aguardam o término dela.

Assim, quando duas vendas de 1 BTC chegam ao mesmo tempo para um saldo de 1 BTC, a segunda é validada depois que a primeira foi salva e é rejeitada por saldo insuficiente.

Operações de usuários diferentes não bloqueiam umas às outras.

A atualização e a exclusão também simulam o histórico resultante antes de confirmar qualquer mudança no banco.

---

## 14. Atualização de transação

Uma transação somente pode ser atualizada quando o histórico resultante continuar financeiramente consistente.

O endpoint recebe um `TransacaoRequestDTO`, portanto os novos valores passam pelas mesmas validações da criação.

Durante a atualização:

1. a transação é buscada pelo ID e pelo usuário autenticado;
2. o novo ticker é normalizado e validado;
3. uma transação candidata independente é criada;
4. o ID original é preservado;
5. a data muda se uma nova for informada (validada e convertida para UTC) e, caso contrário, é preservada;
6. o proprietário original é preservado;
7. somente ticker, quantidade, preço unitário, tipo e data podem mudar;
8. a candidata substitui a original somente em uma cópia do histórico do usuário;
9. a carteira temporária é reconstruída;
10. a entidade persistida só é modificada e salva quando a simulação é válida.

Exemplo rejeitado:

```text
Histórico:
1. Compra de 2 BTC
2. Venda de 1 BTC

Tentativa:
Alterar a compra para 0,5 BTC
```

A alteração produziria uma venda superior ao saldo disponível. O mesmo acontece ao mover a data da compra para depois da venda. Nesse caso, nada é salvo e a API retorna:

```text
409 Conflict
```

Um ID inexistente ou pertencente a outro usuário retorna `404 Not Found`. A validação do ticker na Binance ocorre antes da transação no banco; a simulação e a gravação ocorrem numa transação com o histórico do usuário bloqueado.

---

## 15. Exclusão de transação

Uma transação somente pode ser excluída quando o histórico restante continuar consistente.

Fluxo:

1. a transação é buscada pelo ID e pelo usuário autenticado;
2. uma cópia do histórico desse usuário é criada sem a transação;
3. a carteira temporária é reconstruída;
4. a exclusão é executada somente quando a simulação é válida.

Exemplo rejeitado:

```text
1. Compra de 1 BTC
2. Venda de 1 BTC
```

Excluir a compra deixaria uma venda sem saldo correspondente. Nesse caso, nenhuma exclusão ocorre e a API retorna:

```text
409 Conflict
```

Um ID inexistente ou pertencente a outro usuário retorna `404 Not Found`. A operação é executada numa transação com o histórico do usuário bloqueado.

---

## 16. Cálculo de lucro potencial

O lucro potencial representa o PNL não realizado.

Se o saldo for menor ou igual a zero:

```text
lucro potencial = 0
```

Caso exista saldo:

```text
valor investido = custo da posição
valor atual = saldo × preço atual
lucro potencial = valor atual - valor investido
```

### Exemplo de lucro

```text
Saldo: 2 ETH
Preço médio: 2.000 USDT
Preço atual: 2.500 USDT
```

```text
Valor investido = 2 × 2.000 = 4.000
Valor atual = 2 × 2.500 = 5.000
PNL = 5.000 - 4.000 = 1.000
```

### Exemplo de prejuízo

```text
Saldo: 2 ETH
Preço médio: 2.000 USDT
Preço atual: 1.500 USDT
```

```text
Valor investido = 4.000
Valor atual = 3.000
PNL = 3.000 - 4.000 = -1.000
```

---

## 17. Porcentagem de PNL

Para cada ativo:

```text
porcentagem de PNL =
(PNL não realizado ÷ custo da posição) × 100
```

Caso o custo da posição seja zero, a porcentagem retornada é zero.

---

## 18. Valor atual da carteira

No resumo da carteira, o valor atual é calculado somando o valor de mercado dos ativos com saldo positivo e com cotação disponível (atual ou de fallback).

Para cada ativo:

```text
valor do ativo = saldo × preço atual
```

Valor total:

```text
valor total da carteira =
soma do valor atual de todos os ativos
```

Os preços são consultados em pares com USDT.

Portanto, o valor resultante é atualmente denominado principalmente em USDT, tratado de forma aproximada como USD.

---

## 19. Endpoint `/carteira/total`

O endpoint devolve, em JSON, três totais calculados pelos preços das operações do usuário autenticado. Ele não usa o preço atual de mercado: o valor atual da carteira está no resumo (seção 20).

```json
{
  "custoDasPosicoes": 25000.00,
  "totalAportado": 50000.00,
  "totalRecebidoEmVendas": 20000.00,
  "moeda": "USDT"
}
```

| Campo | Regra |
| --- | --- |
| `custoDasPosicoes` | soma do custo da posição (seção 2) de cada ativo ainda na carteira: quanto do dinheiro está aplicado hoje, pelo preço pago |
| `totalAportado` | soma de quantidade × preço unitário de todas as compras desde o início |
| `totalRecebidoEmVendas` | soma de quantidade × preço unitário de todas as vendas |
| `moeda` | sempre `USDT`, a moeda dos preços (seção 26). Não há conversão para reais |

Exemplo: comprar 1 BTC por 50.000 USDT e depois vender 0,5 BTC por 40.000 USDT dá custo das posições de 25.000 (o custo do 0,5 BTC restante), total aportado de 50.000 e total recebido em vendas de 20.000.

Regras:

* o custo das posições nunca é negativo, e é zero depois de vender toda a posição, mesmo que a venda tenha dado lucro;
* o total aportado não diminui com as vendas;
* o custo das posições vem da carteira reconstruída. Por isso, um histórico inconsistente responde `500`, como nos outros endpoints da carteira;
* todos os valores são exatos (`BigDecimal`), sem arredondamento.

O cálculo antigo era compras menos vendas ao preço de venda, chamado de "Patrimônio Total Investido: R$". Ele ficava menor, ou até negativo, depois de uma venda com lucro, e mostrava "R$" para valores em USDT.

O lucro realizado nas vendas ainda não é calculado (item #26 do roadmap).

---

## 20. Resumo por ativo

Cada ativo com saldo positivo possui no resumo:

```text
ticker
saldo
precoAtual
precoMedio
valorTotalUSD
porcentagemPNL
variacao24h
cotacaoDisponivel
cotacaoDesatualizada
cotacaoAtualizadaEm
```

### Cotações usadas no resumo

As cotações são obtidas pelo `CotacaoService`:

- uma cotação com até 60 segundos (`cotacoes.cache.ttl`) é reutilizada do cache;
- as demais são consultadas em lote na Binance;
- se a Binance estiver indisponível, é usada a última cotação com até 15 minutos (`cotacoes.cache.max-stale`), e o ativo fica com `cotacaoDesatualizada = true`;
- se não houver cotação nem fallback, o ativo continua no resumo com `cotacaoDisponivel = false`, e `precoAtual`, `valorTotalUSD`, `porcentagemPNL`, `variacao24h` e `cotacaoAtualizadaEm` ficam nulos.

Uma cotação nunca é substituída por preço zero.

A espera pela Binance é limitada: os tickers que não respondem dentro do prazo da consulta individual (5 segundos) entram como indisponíveis e usam a cotação anterior, como num provedor fora do ar. Com a Binance falhando seguidamente, o circuit breaker faz o resumo responder na hora com as cotações anteriores, em vez de esperar os timeouts. A regra é a mesma: cotação anterior marcada como desatualizada, ou ativo sem cotação, e nunca preço zero.

Além dos ativos, o resumo informa:

- `cotacoesAtualizadasEm`: horário da cotação mais antiga utilizada;
- `cotacoesParciais`: verdadeiro quando algum ativo está sem cotação ou usa fallback;
- `ativosSemCotacao`: tickers sem cotação nem fallback.

### Valor total do ativo

```text
valorTotalUSD = saldo × preço atual
```

### Porcentagem de PNL

```text
porcentagemPNL =
((valor atual - custo da posição) ÷ custo da posição) × 100
```

### Variação de 24 horas

É a variação percentual informada pela Binance para o par do ativo com USDT.

---

## 21. PNL total da carteira

O PNL total é a soma do PNL não realizado de todos os ativos com saldo positivo e com cotação disponível. Ativos sem cotação ficam fora dessa soma.

```text
PNL total =
soma dos lucros e prejuízos não realizados
```

O PNL total atual não inclui:

* lucro realizado;
* taxas;
* impostos;
* spread;
* custos de saque;
* conversão cambial.

---

## 22. Variação estimada da carteira em 24 horas

Para cada ativo, o sistema estima o valor anterior utilizando:

```text
valor anterior estimado =
valor atual ÷ (1 + variação percentual ÷ 100)
```

O patrimônio anterior total é a soma dos valores anteriores estimados.

Depois:

```text
variação total =
((patrimônio atual - patrimônio anterior)
÷ patrimônio anterior) × 100
```

### Observação

A métrica considera o peso atual de cada ativo e a variação de 24 horas recebida da Binance.

Ela é uma estimativa e não utiliza snapshots históricos próprios da carteira.

Somente ativos com cotação disponível participam do cálculo. Quando o patrimônio anterior estimado é zero, a variação retornada é zero.

---

## 23. Simulação de DCA

A simulação de DCA calcula o efeito de um novo aporte no preço médio da carteira do usuário autenticado.

Entradas:

```text
ticker
valor do aporte
preço de mercado usado na simulação
```

O preço de mercado é informado pelo cliente; a simulação não consulta a Binance nem valida a existência do ticker.

### Quantidade comprada

```text
quantidade comprada =
valor do aporte ÷ preço de mercado
```

### Custo atual

```text
custo atual =
custo da posição atual
```

### Novo saldo

```text
novo saldo =
saldo atual + quantidade comprada
```

### Novo custo

```text
novo custo =
custo atual + valor do aporte
```

### Novo preço médio

```text
novo preço médio =
novo custo ÷ novo saldo
```

### Diferença percentual do preço médio

```text
diferença do preço médio =
((novo preço médio - preço médio atual)
÷ preço médio atual) × 100
```

### Valorização necessária para o break-even

```text
valorização necessária =
((novo preço médio ÷ preço de mercado) - 1) × 100
```

### Validação das entradas

O aporte e o preço de mercado precisam ser números maiores que zero, com no máximo 20 dígitos inteiros e 18 casas decimais. Valores zero ou negativos respondem `400 Dados inválidos`, com o parâmetro na lista `erros`. Textos que não são números finitos, como `NaN`, `Infinity` e `abc`, respondem `400 Parâmetro inválido`. A API nunca devolve `NaN` nem `Infinity`.

### Ativo sem posição

Sem posição anterior, seja porque o ativo nunca foi comprado ou porque foi totalmente vendido, a simulação é a de uma primeira compra:

```text
preço médio atual = null
diferença do preço médio = null
novo preço médio = preço de mercado
valorização necessária = 0
```

### Precisão

O cálculo usa `BigDecimal` e parte do custo exato da posição. As divisões usam 34 dígitos significativos. A quantidade, o novo saldo e o novo preço médio são devolvidos com 18 casas, e os percentuais com 8 casas, todos com `HALF_UP`. A diferença do preço médio é calculada sobre o preço médio exato, e não sobre o valor arredondado para exibição.

---

## 24. Simulação de venda futura

A simulação de venda utiliza:

```text
ticker
preço-alvo
```

O sistema reconstrói a carteira do usuário autenticado, consulta o preço atual pelo `CotacaoService` (com cache e fallback) e calcula quanto a posição valeria no preço-alvo.

Se não houver cotação atual nem de fallback, a simulação retorna `503 Service Unavailable`. Um ticker rejeitado pela Binance retorna `400 Bad Request`.

### Lucro simulado

```text
lucro simulado =
valor da posição no preço-alvo - custo da posição
```

### Percentual simulado

```text
percentual simulado =
(lucro simulado ÷ custo da posição) × 100
```

### Valor total no preço-alvo

```text
valor futuro =
saldo × preço-alvo
```

### Valor total atual

```text
valor atual =
saldo × preço atual de mercado
```

### Validação e ativo sem saldo

O preço-alvo precisa ser um número maior que zero, com no máximo 20 dígitos inteiros e 18 casas decimais. Os erros respondem como na simulação de DCA.

Sem saldo do ativo, não há o que vender: a simulação responde `400 Sem saldo para simular`, antes de consultar a cotação.

### Precisão

O cálculo usa `BigDecimal` e o custo exato da posição. O lucro e os valores totais são exatos, e o percentual tem 8 casas com `HALF_UP`.

### Limitação atual

A simulação considera a venda de todo o saldo.

Ainda não é possível informar uma quantidade parcial para a simulação.

---

## 25. Total aportado por moeda

O endpoint de análise agrupa por ticker somente as compras do usuário autenticado.

Para cada ativo:

```text
total aportado =
soma de quantidade × preço unitário das compras
```

Somente transações do tipo `COMPRA` são consideradas.

### Limitação conceitual

Essa métrica representa o total histórico de compras.

Ela não desconta:

* vendas;
* retiradas;
* taxas;
* lucro realizado;
* posição já encerrada.

Portanto, ela não representa o custo atual da posição.

---

## 26. Moeda de referência

A maior parte das cotações utiliza pares com USDT.

Consequentemente, as métricas são calculadas principalmente em:

```text
USDT
```

Alguns nomes de DTOs e textos utilizam `USD`, tratando USDT como aproximação do dólar.

### Limitação atual

Não existe uma definição centralizada de moeda de referência.

Também não existe conversão consistente de todos os valores para BRL.

---

## 27. Precisão numérica

As entidades `Transacao` e `Moeda` utilizam `BigDecimal`. No banco, quantidade e preço unitário são armazenados como `DECIMAL(38, 18)`.

Também utilizam `BigDecimal`:

* o custo da posição, acumulado sem arredondamento nas compras;
* o preço médio, derivado do custo só na leitura (escala 18, `HALF_UP`);
* as cotações recebidas da Binance;
* o resumo da carteira: valor dos ativos, PNL, percentuais e variação de 24 horas;
* as simulações de DCA e de venda;
* os totais de `/carteira/total`;
* o total aportado por moeda.

Todo o cálculo financeiro usa `BigDecimal` de ponta a ponta. Nenhum valor passa por `double`.

---

## 28. Regras ainda não implementadas

A aplicação ainda não possui regras completas para:

* taxas de compra;
* taxas de venda;
* taxas de saque;
* impostos;
* lucro realizado acumulado;
* custo por exchange;
* transferências entre carteiras;
* depósitos;
* retiradas;
* conversão entre duas criptomoedas;
* staking;
* rendimentos;
* airdrops;
* compras em reais;
* conversão cambial histórica;
* múltiplas carteiras para um mesmo usuário.

Esses recursos deverão ser definidos antes de serem implementados.

---

## 29. Princípios para futuras alterações

As próximas mudanças devem respeitar os seguintes princípios:

1. nenhuma transação inválida deve ser ignorada silenciosamente;
2. o histórico deve ser processado em ordem determinística;
3. toda alteração deve preservar a consistência do histórico;
4. criação, edição e exclusão devem passar pelo service;
5. cálculos financeiros devem evitar `double` quando precisão monetária for necessária;
6. falhas externas não devem ser transformadas silenciosamente em preço zero;
7. o isolamento entre usuários deve ser preservado em toda consulta, cálculo e alteração;
8. regras financeiras devem possuir testes automatizados;
9. migrations devem controlar a evolução do banco;
10. a documentação deve ser atualizada junto com as regras.
