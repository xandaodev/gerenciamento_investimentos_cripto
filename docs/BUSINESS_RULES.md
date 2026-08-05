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

É o custo médio unitário das unidades que permanecem na carteira.

### Custo da posição

É o custo atribuído ao saldo atual.

```text
custo da posição = saldo atual × preço médio
```

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

O `HttpService` também possui tratamentos para alguns nomes conhecidos em determinadas consultas de preço:

```text
BITCOIN   → BTC
ETHEREUM  → ETH
SOLANA    → SOL
CHAINLINK → LINK
LNK       → LINK
```

No fluxo atual de criação e atualização, a entrada é normalizada para maiúsculas e validada como ticker. Por isso, os símbolos oficiais, como `BTC`, `ETH` e `SOL`, são a forma recomendada de entrada.

### USDT

O USDT recebe tratamento especial nas consultas internas:

```text
preço interno = 1.0
variação de 24 horas = 0.0
```

### Validação da criação e da atualização

Os endpoints `POST /transacoes` e `PUT /transacoes/{id}` utilizam `TransacaoRequestDTO` e exigem:

- ticker obrigatório;
- ticker com no máximo 20 caracteres;
- ticker formado somente por letras e números;
- quantidade obrigatória e maior que zero;
- preço unitário obrigatório e maior que zero;
- tipo obrigatório;
- tipo restrito a `COMPRA` ou `VENDA`.

O ID e a data não são recebidos pelo DTO. Na criação, esses campos são controlados pela aplicação. Na atualização, o ID e a data originais são preservados.

### Limitação da validação externa

Uma indisponibilidade da Binance pode ser interpretada como se o ticker não existisse, pois o `HttpService` ainda converte diferentes falhas externas em retorno nulo.

---

## 6. Regra de compra

Uma compra aumenta o saldo e recalcula o preço médio.

Considere:

```text
SA = saldo atual
PMA = preço médio atual
QC = quantidade comprada
PC = preço unitário da compra
```

### Custo anterior

```text
custo anterior = SA × PMA
```

### Custo da nova compra

```text
custo da nova compra = QC × PC
```

### Novo saldo

```text
novo saldo = SA + QC
```

### Novo preço médio

```text
novo preço médio =
(custo anterior + custo da nova compra) ÷ novo saldo
```

O código utiliza `BigDecimal` e arredondamento:

```text
escala = 8 casas decimais
modo = HALF_UP
```

### Exemplo

Posição atual:

```text
Saldo: 1 BTC
Preço médio: 50.000 USDT
```

Nova compra:

```text
Quantidade: 1 BTC
Preço: 60.000 USDT
```

Cálculo:

```text
Custo anterior = 1 × 50.000 = 50.000
Custo da compra = 1 × 60.000 = 60.000
Novo saldo = 1 + 1 = 2
Novo preço médio = 110.000 ÷ 2 = 55.000
```

Resultado:

```text
Saldo: 2 BTC
Preço médio: 55.000 USDT
```

---

## 7. Validações da compra

Nas requisições HTTP de criação e atualização, o `TransacaoRequestDTO` rejeita quantidade e preço nulos, iguais a zero ou negativos. Também rejeita ticker e tipo ausentes.

O método interno `processarTransacao`, isoladamente, não repete todas essas verificações para compras. Portanto, a proteção completa depende atualmente de a criação e a atualização entrarem pelos fluxos validados do controller e do service.

Essa duplicação de proteção no núcleo financeiro ainda pode ser adicionada futuramente como defesa adicional.

---

## 8. Regra de venda

Uma venda reduz o saldo do ativo.

Considere:

```text
SA = saldo atual
PM = preço médio
QV = quantidade vendida
PV = preço unitário da venda
```

### Novo saldo

```text
novo saldo = SA - QV
```

### Preço médio após a venda

O preço médio das unidades restantes não é alterado.

Exemplo:

```text
Antes da venda:
Saldo = 2 BTC
Preço médio = 50.000 USDT

Venda:
Quantidade = 0,5 BTC

Depois da venda:
Saldo = 1,5 BTC
Preço médio = 50.000 USDT
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
custo da parte vendida = quantidade vendida × preço médio
```

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
```

O preço médio permanece armazenado no objeto `Moeda`, mas o ativo deixa de aparecer no resumo porque somente moedas com saldo maior que zero são incluídas.

Uma regra explícita para zerar o preço médio quando o saldo chegar a zero ainda não foi implementada.

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

1. o ticker é convertido para letras maiúsculas;
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

---

## 13. Registro de nova transação

Antes de registrar uma nova transação, o sistema:

1. identifica o usuário autenticado;
2. associa a nova transação a esse usuário;
3. valida o ticker na Binance;
4. cria uma carteira temporária;
5. busca somente o histórico do usuário;
6. reconstrói a carteira;
7. obtém o ativo correspondente;
8. processa a nova transação;
9. salva a transação no banco.

Esse fluxo evita que uma nova venda seja registrada quando não existe saldo suficiente.

A atualização e a exclusão também simulam o histórico resultante antes de confirmar qualquer mudança no banco.

---

## 14. Atualização de transação

Uma transação somente pode ser atualizada quando o histórico resultante continuar financeiramente consistente.

O endpoint recebe um `TransacaoRequestDTO`, portanto os novos valores passam pelas mesmas validações da criação.

Durante a atualização:

1. a transação é buscada pelo ID e pelo usuário autenticado;
2. o novo ticker é validado;
3. uma transação candidata independente é criada;
4. o ID original é preservado;
5. a data original é preservada;
6. o proprietário original é preservado;
7. somente ticker, quantidade, preço unitário e tipo podem mudar;
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

A alteração produziria uma venda superior ao saldo disponível. Nesse caso, nada é salvo e a API retorna:

```text
409 Conflict
```

Um ID inexistente ou pertencente a outro usuário retorna `404 Not Found`. A operação é executada em método `@Transactional`.

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

Um ID inexistente ou pertencente a outro usuário retorna `404 Not Found`. A operação é executada em método `@Transactional`.

---

## 16. Cálculo de lucro potencial

O lucro potencial representa o PNL não realizado.

Se o saldo for menor ou igual a zero:

```text
lucro potencial = 0
```

Caso exista saldo:

```text
valor investido = saldo × preço médio
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

No resumo da carteira, o valor atual é calculado somando o valor de mercado dos ativos com saldo positivo.

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

O método utilizado por esse endpoint considera somente as transações do usuário autenticado e não calcula o valor atual de mercado da carteira.

Ele realiza:

```text
total das compras - total das vendas
```

Para cada transação:

```text
COMPRA → soma quantidade × preço unitário
VENDA  → subtrai quantidade × preço unitário
```

Portanto, essa métrica representa aproximadamente um fluxo financeiro líquido histórico, e não o patrimônio atual.

### Inconsistência atual

A resposta apresenta:

```text
Patrimônio Total Investido: R$
```

Porém:

* não é utilizado o preço atual dos ativos;
* não existe conversão obrigatória para reais;
* os preços das operações podem estar em USDT;
* o significado da métrica não corresponde exatamente a “patrimônio”.

Esse endpoint será renomeado ou substituído posteriormente.

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
```

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

O PNL total é a soma do PNL não realizado de todos os ativos com saldo positivo.

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

---

## 23. Simulação de DCA

A simulação de DCA calcula o efeito de um novo aporte no preço médio da carteira do usuário autenticado.

Entradas:

```text
ticker
valor do aporte
preço de mercado usado na simulação
```

### Quantidade comprada

```text
quantidade comprada =
valor do aporte ÷ preço de mercado
```

### Custo atual

```text
custo atual =
saldo atual × preço médio atual
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

### Limitações atuais

Ainda não há validação explícita para:

* aporte igual a zero;
* aporte negativo;
* preço igual a zero;
* preço negativo;
* ativo sem posição anterior;
* preço médio atual igual a zero.

Esses casos podem produzir divisão por zero ou resultados sem significado financeiro.

---

## 24. Simulação de venda futura

A simulação de venda utiliza:

```text
ticker
preço-alvo
```

O sistema reconstrói a carteira do usuário autenticado, consulta o preço atual e calcula quanto a posição valeria no preço-alvo.

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

O núcleo das entidades `Transacao` e `Moeda` utiliza `BigDecimal`.

O cálculo do preço médio também utiliza `BigDecimal`.

Porém, vários cálculos posteriores convertem os valores para `double`, incluindo:

* PNL;
* patrimônio;
* resumos;
* simulações;
* preços recebidos da Binance;
* variações percentuais.

Assim, a aplicação ainda não utiliza `BigDecimal` de ponta a ponta.

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
