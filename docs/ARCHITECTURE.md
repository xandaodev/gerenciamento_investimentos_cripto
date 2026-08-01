# Arquitetura do CriptoVision

## 1. Visão geral

O CriptoVision é uma API REST desenvolvida para registrar e acompanhar investimentos em criptomoedas.

O sistema permite armazenar compras e vendas, reconstruir a posição atual de cada ativo, calcular preço médio, patrimônio, lucro ou prejuízo não realizado e realizar simulações financeiras.

A aplicação utiliza atualmente:

* Java 25;
* Spring Boot 4;
* Spring Web MVC;
* Spring Data JPA;
* Hibernate;
* Spring Security;
* autenticação JWT;
* MySQL;
* Binance Public API;
* Swagger/OpenAPI;
* Maven;
* Docker.

Esta documentação descreve o comportamento atual da aplicação. Alguns pontos registrados aqui representam limitações conhecidas que serão corrigidas durante a evolução do projeto.

---

## 2. Organização em camadas

A aplicação segue uma organização baseada em camadas:

```text
Cliente HTTP
    ↓
Controllers
    ↓
Services
    ↓
Repositories
    ↓
MySQL
```

A integração com a Binance acontece por meio do `HttpService`:

```text
CarteiraService
    ↓
HttpService
    ↓
Binance Public API
```

### Responsabilidade das camadas

#### Controllers

Recebem requisições HTTP, extraem parâmetros, delegam operações às camadas apropriadas e constroem as respostas. O `TransacaoController` delega suas operações ao `CarteiraService`; o `AnaliseController` ainda consulta o `TransacaoRepository` diretamente.

#### Services

Concentram regras de negócio, cálculos financeiros, reconstrução da carteira, autenticação e comunicação com serviços externos.

#### Repositories

Realizam o acesso ao banco de dados utilizando Spring Data JPA.

#### Models

Representam entidades persistidas e objetos utilizados nos cálculos da carteira.

#### DTOs

Representam dados específicos enviados ou recebidos pela API, evitando que todas as respostas dependam diretamente das entidades do banco.

#### Security

Contém a configuração de autenticação, autorização, geração de JWT e filtro de validação dos tokens.

#### Exceptions

Centraliza exceções de negócio e o tratamento das respostas de erro da aplicação.

---

## 3. Estrutura de pacotes

```text
br.com.criptovision
├── controller
├── dto
├── exception
├── model
├── repository
├── security
└── service
```

### `controller`

Contém os endpoints HTTP da aplicação.

Classes atuais:

* `AutenticacaoController`;
* `TransacaoController`;
* `CarteiraController`;
* `AnaliseController`.

### `dto`

Contém objetos utilizados nas entradas e saídas da API.

Classes atuais:

* `DadosAutenticacao`;
* `TokenJwtDTO`;
* `ResumoAtivoDTO`;
* `ResumoCarteiraDTO`;
* `SimulacaoDCADTO`;
* `SimulacaoVendaDTO`;
* `AportePorMoedaProjection`;
* `TransacaoRequestDTO`.

### `exception`

Contém exceções personalizadas e o tratamento global de erros.

Classes atuais:

* `CriptoException`;
* `SaldoInsuficienteException`;
* `ArquivoNaoEncontradoException`;
* `BancoDeDadosException`;
* `GlobalExceptionHandler`;
* `TransacaoNaoEncontradaException`;
* `AlteracaoHistoricoInvalidaException`;
* `HistoricoInconsistenteException`.


### Erros relacionados às transações

O tratamento global utiliza os seguintes status:

- `400 Bad Request`: dados de entrada inválidos ou saldo insuficiente;
- `404 Not Found`: transação inexistente;
- `409 Conflict`: alteração ou exclusão que tornaria o histórico inconsistente;
- `500 Internal Server Error`: histórico já persistido inconsistente ou erro inesperado.

### `model`

Contém as entidades persistidas e os modelos utilizados na reconstrução da carteira.

Classes atuais:

* `Transacao`;
* `TipoTransacao`;
* `Usuario`;
* `Carteira`;
* `Moeda`.

### `repository`

Contém as interfaces de acesso ao banco.

Classes atuais:

* `TransacaoRepository`;
* `UsuarioRepository`.

### `security`

Contém os componentes relacionados à autenticação e autorização.

Classes atuais:

* `SecurityConfigurations`;
* `SecurityFilter`;
* `TokenService`;
* `AutenticacaoService`.

### `service`

Contém as principais regras de negócio.

Classes atuais:

* `CarteiraService`;
* `HttpService`.

---

## 4. Modelos principais

## 4.1 Transação

A classe `Transacao` é uma entidade JPA persistida na tabela `transacoes`.

Campos atuais:

```text
id
ticker
quantidade
precoUnitario
data
tipo
```

O identificador é gerado automaticamente pelo banco.

A data é inicializada no construtor da entidade e o campo também utiliza `@CreationTimestamp` para o preenchimento durante a persistência.

Os valores de quantidade e preço unitário utilizam `BigDecimal`.

O tipo da transação é representado pelo enum `TipoTransacao`.

Valores permitidos:

```text
COMPRA
VENDA
```

Atualmente, a entidade `Transacao` não possui relacionamento com a entidade `Usuario`.

---

## 4.2 Usuário

A classe `Usuario` é uma entidade JPA persistida na tabela `usuarios`.

Campos atuais:

```text
id
login
senha
```

O login possui restrição de unicidade.

A entidade implementa `UserDetails`, permitindo sua utilização pelo Spring Security.

Todo usuário recebe atualmente a autoridade:

```text
ROLE_USER
```

Não existem diferentes níveis de permissão neste momento.

---

## 4.3 Carteira

A classe `Carteira` não é uma entidade persistida.

Ela representa uma carteira temporária criada em memória durante cada operação que necessita calcular saldos ou métricas.

Sua estrutura principal é:

```text
Map<String, Moeda>
```

A chave do mapa é o ticker da criptomoeda.

Exemplo:

```text
BTC → objeto Moeda do Bitcoin
ETH → objeto Moeda do Ethereum
SOL → objeto Moeda da Solana
```

Quando uma moeda ainda não existe na carteira, o método `obterMoeda` cria um novo objeto com saldo e preço médio iguais a zero.

---

## 4.4 Moeda

A classe `Moeda` também não é persistida.

Ela representa o estado calculado de um ativo dentro da carteira.

Campos atuais:

```text
nome
ticker
saldo
precoMedio
variacao24h
```

O saldo e o preço médio utilizam `BigDecimal`.

A carteira é reconstruída processando o histórico de transações sobre os objetos `Moeda`.

---

## 5. Persistência

O CriptoVision utiliza MySQL e Spring Data JPA.

A configuração atual utiliza:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Com essa configuração, o Hibernate tenta atualizar automaticamente a estrutura do banco de acordo com as entidades Java.

As entidades persistidas atualmente são:

```text
Usuario
Transacao
```

Os objetos `Carteira` e `Moeda` existem apenas durante a execução dos cálculos.

### Repositórios

O `TransacaoRepository` oferece:

* operações CRUD de transações;
* consulta do total aportado agrupado por ticker.

O `UsuarioRepository` oferece:

* operações CRUD de usuários;
* busca de usuário pelo login.

### Ordenação do histórico

As consultas utilizadas pelo `CarteiraService` solicitam ordenação crescente por `data` e, em caso de empate, por `id`.

Além disso, `reconstruirCarteira` ordena uma cópia da lista recebida antes do processamento. Essa segunda ordenação protege o motor mesmo quando ele recebe uma lista criada fora do repository.

---

## 6. Autenticação e autorização

A API utiliza autenticação stateless com JWT.

O endpoint público de login é:

```http
POST /auth/login
```

As rotas do Swagger também são públicas.

Todas as outras rotas exigem autenticação.

### Fluxo de login

```text
Cliente envia login e senha
        ↓
AutenticacaoController
        ↓
AuthenticationManager
        ↓
AutenticacaoService
        ↓
UsuarioRepository
        ↓
Validação da senha
        ↓
TokenService gera o JWT
        ↓
Token é devolvido ao cliente
```

O cliente deve enviar o token nas próximas requisições:

```http
Authorization: Bearer TOKEN
```

### Fluxo de validação

```text
Requisição protegida
        ↓
SecurityFilter
        ↓
TokenService valida o JWT
        ↓
Usuário é buscado pelo login
        ↓
Autenticação é registrada no SecurityContext
        ↓
Requisição continua
```

A aplicação utiliza sessões stateless, portanto o servidor não mantém uma sessão HTTP para o usuário.

### CORS

A configuração atual permite requisições das seguintes origens locais:

```text
http://localhost:5173
http://localhost:5174
```

Essas portas são utilizadas pelo frontend React com Vite.

---

## 7. Operações de transação

O `TransacaoController` recebe as requisições HTTP e delega as operações ao `CarteiraService`.

```text
TransacaoController
        ↓
CarteiraService
        ↓
TransacaoRepository
```

O controller de transações não realiza operações de persistência diretamente.

### Criação

O endpoint responsável pelo cadastro é:

```http
POST /transacoes
```

Fluxo:

1. recebe um `TransacaoRequestDTO`;
2. normaliza o ticker e valida os campos com Bean Validation;
3. converte o DTO para `Transacao`;
4. valida o ticker por meio da Binance;
5. busca o histórico em ordem cronológica;
6. reconstrói uma carteira temporária;
7. processa a nova transação;
8. valida o saldo em caso de venda;
9. persiste a transação;
10. retorna `201 Created`.

O cliente não pode definir diretamente o ID ou a data da transação.

### Consulta

Os endpoints de listagem e busca por ID também passam pelo `CarteiraService`.

A listagem utiliza a ordenação cronológica por data e ID. A busca de um ID inexistente lança `TransacaoNaoEncontradaException` e retorna `404 Not Found`.

### Atualização

O endpoint:

```http
PUT /transacoes/{id}
```

recebe o mesmo `TransacaoRequestDTO` validado utilizado na criação.

Fluxo:

1. busca a transação existente;
2. valida o novo ticker;
3. cria uma transação candidata independente;
4. preserva o ID e a data originais;
5. substitui a transação somente em uma cópia do histórico;
6. reconstrói uma carteira temporária;
7. modifica e salva a entidade existente apenas quando o histórico simulado é válido;
8. retorna `200 OK`.

A entidade gerenciada pelo JPA não é modificada antes da validação da cópia. O método é transacional.

Uma atualização que tornaria o histórico inconsistente lança `AlteracaoHistoricoInvalidaException` e retorna `409 Conflict`.

### Exclusão

O endpoint:

```http
DELETE /transacoes/{id}
```

executa o seguinte fluxo:

1. busca a transação;
2. cria uma cópia do histórico sem ela;
3. reconstrói uma carteira temporária;
4. exclui a entidade apenas quando o histórico restante é válido;
5. retorna `204 No Content`.

O método é transacional. Uma exclusão que tornaria o histórico inconsistente retorna `409 Conflict`.

---

## 8. Reconstrução da carteira

O estado atual da carteira não é armazenado diretamente no banco.

Sempre que um resumo ou simulação é solicitado, o sistema:

```text
Busca as transações em ordem cronológica
        ↓
Cria uma Carteira vazia
        ↓
Processa as transações individualmente
        ↓
Atualiza saldo e preço médio de cada Moeda
        ↓
Produz o resultado solicitado
```

Esse modelo faz com que o histórico de transações seja a fonte principal dos dados financeiros.

### Vantagens

* o estado pode ser recalculado;
* evita duplicação entre transações e saldos;
* facilita auditoria do histórico;
* permite reconstruir a carteira após mudanças nas regras.

### Comportamento atual

- o histórico é processado em ordem cronológica por data e ID;
- inconsistências durante a reconstrução interrompem o cálculo;
- a aplicação lança `HistoricoInconsistenteException`;
- a exceção identifica ID, ticker e tipo da transação;
- a causa original da inconsistência é preservada.

### Limitações atuais

- inconsistências do histórico ainda não possuem logs estruturados;
- a carteira ainda não é separada por usuário;
- a carteira é reconstruída repetidamente em diferentes endpoints.
---

## 9. Integração com a Binance

O `HttpService` realiza chamadas para a Binance Public API utilizando o `HttpClient` nativo do Java.

URL-base atual:

```text
https://api.binance.com/api/v3
```

Principais endpoints externos utilizados:

```text
/ticker/price
/ticker/24hr
```

A integração é utilizada para:

* consultar o preço atual;
* consultar a variação percentual das últimas 24 horas;
* validar a existência de um ticker;
* consultar o par USDT/BRL.

### Pares utilizados

As criptomoedas são consultadas principalmente por meio de pares com USDT.

Exemplos:

```text
BTCUSDT
ETHUSDT
SOLUSDT
LINKUSDT
```

O ativo USDT é tratado internamente com preço igual a `1.0`.

### Normalização atual de nomes

Alguns nomes são convertidos para ticker:

```text
BITCOIN → BTC
ETHEREUM → ETH
SOLANA → SOL
CHAINLINK → LINK
LNK → LINK
```

Tickers que não aparecem nessa lista são convertidos para letras maiúsculas e utilizados diretamente.

### Limitações atuais

* um novo `HttpClient` é criado em cada chamada;
* a resposta JSON é processada por manipulação de texto;
* falhas de rede retornam valores iguais a zero ou nulos;
* não existe cache de preços;
* não existe consulta em lote;
* uma falha da Binance pode ser confundida com ticker inválido;
* as chamadas são síncronas.

---

## 10. Endpoints atuais

## Autenticação

```http
POST /auth/login
```

## Transações

```http
GET    /transacoes
POST   /transacoes
GET    /transacoes/{id}
PUT    /transacoes/{id}
DELETE /transacoes/{id}
```

## Carteira

```http
GET /carteira/total
GET /carteira/resumo
GET /carteira/simulador/dca
GET /carteira/simulador/venda
```

## Análises

```http
GET /analise/aportes-por-moeda
```

---

## 11. Fluxo do resumo da carteira

O endpoint principal do dashboard é:

```http
GET /carteira/resumo
```

Fluxo:

```text
CarteiraController
        ↓
CarteiraService.obterResumoGeral
        ↓
Busca do histórico
        ↓
Reconstrução da carteira
        ↓
Consulta de preço e variação na Binance
        ↓
Cálculo das métricas por ativo
        ↓
Cálculo das métricas gerais
        ↓
ResumoCarteiraDTO
```

Para cada ativo com saldo positivo, o resumo contém atualmente:

```text
ticker
saldo
precoAtual
precoMedio
valorTotalUSD
porcentagemPNL
variacao24h
```

O resumo geral contém:

```text
valor total da carteira
PNL total não realizado
variação estimada da carteira em 24 horas
lista de ativos
```

---

## 12. Docker

O projeto possui:

```text
Dockerfile
docker-compose.yml
```

O Docker Compose define:

* um container MySQL;
* um container para a API;
* um volume persistente para o banco.

O MySQL é exposto localmente na porta:

```text
3307
```

A API é exposta na porta:

```text
8080
```

O Dockerfile espera que o JAR já tenha sido gerado em `target/`.

Fluxo atual:

```text
Maven gera o JAR
        ↓
Docker copia o JAR
        ↓
Java executa o arquivo app.jar
```

---

## 13. Decisões arquiteturais atuais

As seguintes decisões serão preservadas inicialmente:

* histórico de transações como fonte dos cálculos;
* uso de `BigDecimal` no núcleo financeiro;
* separação entre controllers, services e repositories;
* autenticação stateless com JWT;
* MySQL como banco principal;
* Binance como fonte inicial de preços;
* DTOs para respostas específicas;
* API REST independente do frontend.

---

## 14. Dívidas técnicas conhecidas

Os principais pontos identificados são:

1. as transações ainda não pertencem ao usuário autenticado;
2. o `AnaliseController` ainda acessa o repository diretamente;
3. a carteira é reconstruída repetidamente em diferentes endpoints;
4. inconsistências do histórico ainda não possuem logs estruturados;
5. parte dos cálculos e DTOs ainda utiliza `double`;
6. falhas da Binance podem retornar zero ou `null` e ser confundidas com ticker inválido;
7. a validação do ticker na atualização ocorre durante um método transacional;
8. não há migrations versionadas do banco;
9. `spring.jpa.hibernate.ddl-auto=update` ainda é utilizado;
10. a injeção de dependências é realizada por atributos;
11. erros de autenticação ainda não são totalmente padronizados;
12. não há paginação ou filtros no histórico;
13. não há lucro realizado persistido ou consolidado;
14. não há suporte a taxas;
15. não há cache nem consulta em lote de cotações;
16. o README ainda contém informações desatualizadas.

Esses itens serão tratados em commits pequenos e independentes.
