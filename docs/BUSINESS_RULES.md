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
```

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

## Tipos de transação

O sistema trabalha com o enum `TipoTransacao`.

Valores permitidos:

```text
COMPRA
VENDA

### Comportamento atual

O motor de cálculos compara o valor exatamente com:

```text
COMPRA
VENDA

Valores recebidos pela API são normalizados para letras maiúsculas.
Espaços no início e no final são removidos.

Exemplos aceitos:

COMPRA
compra
 Compra
VENDA
venda

Qualquer outro valor é considerado inválido.
```

Uma transação com outro valor pode não ser processada corretamente.


---

## 4. Normalização de ticker

A aplicação converte alguns nomes conhecidos:

```text
BITCOIN  → BTC
ETHEREUM → ETH
SOLANA   → SOL
CHAINLINK → LINK
LNK      → LINK
```

Nos demais casos, o valor é:

1. convertido para letras maiúsculas;
2. utilizado diretamente como ticker.

### USDT

O USDT recebe tratamento especial:

```text
preço interno = 1.0
variação de 24 horas = 0.0
```


## Validação da criação de transações

O endpoint `POST /transacoes` exige:

- ticker obrigatório;
- ticker com no máximo 20 caracteres;
- ticker formado somente por letras e números;
- quantidade obrigatória e maior que zero;
- preço unitário obrigatório e maior que zero;
- tipo obrigatório;
- tipo restrito a `COMPRA` ou `VENDA`.

O ticker é normalizado antes da validação:

```text
" btc " → "BTC"
```

O ID e a data não são recebidos pelo DTO de criação.
Esses campos são controlados pela aplicação.

O `PUT` ainda será corrigido.


## Alteração de transações

Uma transação somente pode ser atualizada quando o histórico
resultante continuar financeiramente consistente.

Durante a atualização:

- o ID original é preservado;
- a data original é preservada;
- somente ticker, quantidade, preço unitário e tipo podem ser alterados;
- os novos dados passam pelas mesmas validações usadas na criação;
- o ticker é validado;
- uma cópia do histórico é reconstruída antes da persistência.

Exemplo de alteração rejeitada:

```text
Histórico original:

1. Compra de 2 BTC
2. Venda de 1 BTC

Tentativa:

Alterar a compra para 0,5 BTC

```



### Limitação

Uma indisponibilidade da Binance pode ser interpretada como se o ticker não existisse.

---

## 5. Regra de compra

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

## 6. Validações atuais de compra

O motor de cálculos ainda não valida explicitamente:

* quantidade nula;
* quantidade igual a zero;
* quantidade negativa;
* preço nulo;
* preço igual a zero;
* preço negativo;
* ticker nulo;
* tipo nulo.

Essas validações serão adicionadas antes da próxima expansão funcional do sistema.

---

## 7. Regra de venda

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

## 8. Validações atuais de venda

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

## 9. Lucro realizado de uma venda

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

## 10. Venda total da posição

Quando todo o saldo é vendido:

```text
novo saldo = 0
```

O preço médio permanece armazenado no objeto `Moeda`, mas o ativo deixa de aparecer no resumo porque somente moedas com saldo maior que zero são incluídas.

Uma regra explícita para zerar o preço médio quando o saldo chegar a zero ainda não foi implementada.

---

## 11. Reconstrução da carteira

A carteira atual é calculada a partir do histórico completo.

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

O histórico de transações é atualmente a fonte de verdade para:

* saldo;
* preço médio;
* posição atual;
* resumo;
* simulações.

### Limitação de ordenação

O histórico é obtido atualmente por `findAll()`.

Como não existe uma ordenação explícita por data e ID, a ordem retornada pelo banco não é formalmente garantida.

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

## 12. Registro de nova transação

Antes de registrar uma nova transação, o sistema:

1. valida o ticker na Binance;
2. cria uma carteira temporária;
3. busca todo o histórico existente;
4. reconstrói a carteira;
5. obtém o ativo correspondente;
6. processa a nova transação;
7. salva a transação no banco.

Esse fluxo evita que uma nova venda seja registrada quando não existe saldo suficiente.

### Limitação

A atualização e a exclusão não passam atualmente por esse mesmo fluxo.

---

## 13. Atualização de transação

O endpoint de atualização substitui diretamente:

```text
ticker
quantidade
precoUnitario
tipo
```

A data original é mantida.

### Limitação atual

Depois da atualização, o histórico não é reconstruído para verificar se continua válido.

Exemplo de inconsistência possível:

```text
Compra original: 2 BTC
Venda posterior: 1 BTC

A compra é alterada para: 0,5 BTC
```

O histórico passa a possuir uma venda de 1 BTC depois de uma compra de apenas 0,5 BTC.

Essa validação será adicionada posteriormente no service.

---

## 14. Exclusão de transação

A exclusão remove diretamente a transação pelo ID.

### Limitação atual

O restante do histórico não é validado após a exclusão.

Exemplo:

```text
Compra: 1 BTC
Venda: 1 BTC
```

Caso a compra seja excluída, permanece uma venda sem saldo correspondente.

---

## 15. Cálculo de lucro potencial

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

## 16. Porcentagem de PNL

Para cada ativo:

```text
porcentagem de PNL =
(PNL não realizado ÷ custo da posição) × 100
```

Caso o custo da posição seja zero, a porcentagem retornada é zero.

---

## 17. Valor atual da carteira

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

## 18. Endpoint `/carteira/total`

O método utilizado por esse endpoint não calcula o valor atual de mercado da carteira.

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

## 19. Resumo por ativo

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

## 20. PNL total da carteira

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

## 21. Variação estimada da carteira em 24 horas

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

## 22. Simulação de DCA

A simulação de DCA calcula o efeito de um novo aporte no preço médio.

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

## 23. Simulação de venda futura

A simulação de venda utiliza:

```text
ticker
preço-alvo
```

O sistema reconstrói a carteira, consulta o preço atual e calcula quanto a posição valeria no preço-alvo.

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

## 24. Total aportado por moeda

O endpoint de análise agrupa as compras por ticker.

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

## 25. Moeda de referência

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

## 26. Precisão numérica

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

## 27. Regras ainda não implementadas

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
* separação da carteira por usuário;
* múltiplas carteiras por usuário.

Esses recursos deverão ser definidos antes de serem implementados.

---

## 28. Princípios para futuras alterações

As próximas mudanças devem respeitar os seguintes princípios:

1. nenhuma transação inválida deve ser ignorada silenciosamente;
2. o histórico deve ser processado em ordem determinística;
3. toda alteração deve preservar a consistência do histórico;
4. criação, edição e exclusão devem passar pelo service;
5. cálculos financeiros devem evitar `double` quando precisão monetária for necessária;
6. falhas externas não devem ser transformadas silenciosamente em preço zero;
7. cada transação deve pertencer ao usuário autenticado;
8. regras financeiras devem possuir testes automatizados;
9. migrations devem controlar a evolução do banco;
10. a documentação deve ser atualizada junto com as regras.
