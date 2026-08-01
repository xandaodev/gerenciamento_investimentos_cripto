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

Recebem requisições HTTP, extraem parâmetros, chamam os serviços ou repositórios e constroem as respostas.

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


Na seção de tratamento de erros, acrescente:

```markdown
### Erros relacionados às transações

- `400 Bad Request`: dados de entrada inválidos;
- `404 Not Found`: transação inexistente;
- `409 Conflict`: alteração ou exclusão que tornaria o histórico inconsistente;
- `500 Internal Server Error`: histórico persistido já inconsistente.

```

### `model`

Contém as entidades persistidas e os modelos utilizados na reconstrução da carteira.

Classes atuais:

* `Transacao`;
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

A data é preenchida pelo Hibernate por meio de `@CreationTimestamp`.

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

### Limitação atual

A ordem das transações retornadas por `findAll()` não é garantida explicitamente.

Como os cálculos dependem da ordem histórica de compras e vendas, uma ordenação determinística será adicionada posteriormente.

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

## 7. Fluxo de registro de transação

O endpoint responsável pelo cadastro é:

```http
POST /transacoes
```

O endpoint `POST /transacoes` recebe um `TransacaoRequestDTO`.

Antes de chegar às regras de negócio:

1. o ticker é normalizado;
2. os campos são validados pelo Bean Validation;
3. o DTO é convertido para uma entidade `Transacao`;
4. o service valida o ticker e a consistência financeira;
5. a transação é persistida.

O cliente não pode definir diretamente o ID ou a data da transação.

---

Fluxo atual:

```text
TransacaoController
        ↓
CarteiraService.registrarNovaTransacao
        ↓
Validação do ticker na Binance
        ↓
Busca de todas as transações
        ↓
Reconstrução de uma carteira temporária
        ↓
Processamento da nova transação
        ↓
Validação de saldo em caso de venda
        ↓
Persistência no MySQL
```

## Fluxo das operações de transação

O `TransacaoController` recebe as requisições HTTP e delega as
operações ao `CarteiraService`.

```text
TransacaoController
        ↓
CarteiraService
        ↓
TransacaoRepository


O cadastro passa pelo `CarteiraService`, permitindo que vendas sem saldo suficiente sejam rejeitadas.

### Limitação atual

A atualização e a exclusão de transações utilizam diretamente o repository.

Portanto, os endpoints `PUT` e `DELETE` ainda não revalidam todo o histórico financeiro após uma alteração.

---

## 8. Reconstrução da carteira

O estado atual da carteira não é armazenado diretamente no banco.

Sempre que um resumo ou simulação é solicitado, o sistema:

```text
Busca todas as transações
        ↓
Cria uma Carteira vazia
        ↓
Processa as transações individualmente
        ↓
Atualiza saldo e preço médio de cada Moeda
        ↓
Produz o resultado solicitado

O controller não realiza diretamente operações de persistência.
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

1. transações ainda não pertencem a usuários;
2.
3. atualização e exclusão ignoram regras financeiras;
4. inconsistências do histórico ainda não possuem logs estruturados;
5. o endpoint de atualização ainda não utiliza um DTO validado;
6.
7. parte dos cálculos e DTOs utiliza `double`;
8. falhas da Binance podem retornar preço zero;
9. não há migrations do banco;
10. `ddl-auto=update` é utilizado;
11. testes dependem do ambiente local;
12. injeção de dependências é realizada por atributos;
13. erros de autenticação ainda não são totalmente padronizados;
14. não há paginação do histórico;
15. não há lucro realizado persistido ou consolidado;
16. não há suporte a taxas;
17. não há cache de cotações;
18. o README ainda contém informações desatualizadas.

Esses itens serão tratados em commits pequenos e independentes.
