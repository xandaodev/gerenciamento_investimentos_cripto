# Arquitetura do CriptoVision

## 1. Visão geral

O CriptoVision é uma API REST desenvolvida para registrar e acompanhar investimentos em criptomoedas.

O sistema permite armazenar compras e vendas, reconstruir a posição atual de cada ativo, calcular preço médio, patrimônio, lucro ou prejuízo não realizado e realizar simulações financeiras.

Cada transação pertence obrigatoriamente a um usuário. O histórico, os cálculos, as simulações e as análises são isolados pelo usuário autenticado no JWT.

A aplicação utiliza atualmente:

* Java 25;
* Spring Boot 4;
* Spring Web MVC;
* Spring `RestClient`;
* Spring Data JPA;
* Hibernate;
* Flyway;
* Spring Security;
* autenticação JWT (`com.auth0:java-jwt`);
* MySQL 8;
* Binance Public API (dados de mercado);
* Swagger/OpenAPI (springdoc);
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

A integração com a Binance fica isolada no subsistema de cotações:

```text
CarteiraService
    ↓
CotacaoService ──→ CotacaoCache
    ↓
CotacaoClient (BinanceCotacaoClient)
    ↓
Binance Public API
```

### Responsabilidade das camadas

#### Controllers

Recebem requisições HTTP, extraem parâmetros, obtêm o usuário autenticado por meio de `@AuthenticationPrincipal`, delegam operações às camadas apropriadas e constroem as respostas. O `TransacaoController` e o `CarteiraController` delegam suas operações ao `CarteiraService`. O `AutenticacaoController` usa o `AuthenticationManager`, o `TokenService` e o `UsuarioService`. O `AnaliseController` ainda consulta o `TransacaoRepository` diretamente, mas utiliza uma consulta filtrada pelo usuário autenticado.

#### Services

Concentram regras de negócio, cálculos financeiros, reconstrução da carteira e cadastro de usuários.

#### Cotações

Obtêm preços e variações de 24 horas, com timeouts, cache em memória e fallback para cotações recentes quando o provedor está indisponível.

#### Repositories

Realizam o acesso ao banco de dados utilizando Spring Data JPA.

#### Models

Representam entidades persistidas e objetos utilizados nos cálculos da carteira.

#### DTOs

Representam dados específicos enviados ou recebidos pela API, evitando que todas as respostas dependam diretamente das entidades do banco.

#### Security

Contém a configuração de autenticação, autorização, geração e validação de JWT e a resposta padronizada para requisições não autenticadas.

#### Exceptions

Centraliza exceções de negócio e o tratamento das respostas de erro da aplicação.

---

## 3. Estrutura de pacotes

```text
br.com.criptovision
├── config
├── controller
├── cotacao
│   ├── cache
│   ├── client
│   ├── model
│   ├── service
│   └── util
├── dto
├── exception
├── model
├── repository
├── security
└── service
```

### `config`

* `CotacaoConfiguration`: expõe um bean `Clock` (UTC), injetado onde há lógica dependente de tempo, o que permite controlar o tempo nos testes;
* `OpenApiConfiguration`: metadados do Swagger, tags e o esquema de segurança `bearerAuth`.

### `controller`

Contém os endpoints HTTP da aplicação.

Classes atuais:

* `AutenticacaoController`;
* `TransacaoController`;
* `CarteiraController`;
* `AnaliseController`.

### `cotacao`

Contém a integração com o provedor de cotações.

Classes atuais:

* `CotacaoService`: ponto de entrada usado pelos demais services;
* `CotacaoCache`: cache em memória com TTL e janela de fallback;
* `CotacaoClient`: interface do provedor;
* `BinanceCotacaoClient`: implementação com `RestClient`;
* `BinanceTicker24hResponse`: resposta do endpoint `/ticker/24hr`;
* `CotacaoMercado`: cotação de um ativo (preço, variação de 24 horas, horário e indicação de cotação desatualizada);
* `ResultadoCotacoes`: resultado de uma consulta em lote, com as cotações obtidas e os tickers indisponíveis;
* `TickerNormalizer`: normalização de tickers e de nomes conhecidos.

### `dto`

Contém objetos utilizados nas entradas e saídas da API.

Classes atuais:

* `DadosAutenticacao`;
* `DadosCadastroUsuario`;
* `UsuarioCadastradoDTO`;
* `TokenJwtDTO`;
* `TransacaoRequestDTO`;
* `ResumoAtivoDTO`;
* `ResumoCarteiraDTO`;
* `SimulacaoDCADTO`;
* `SimulacaoVendaDTO`;
* `AportePorMoedaProjection`;
* `ProblemaApiDTO` e `ErroCampoDTO` (usados para documentar no Swagger o formato das respostas de erro).

### `exception`

Contém exceções personalizadas e o tratamento global de erros.

Classes atuais:

* `CriptoException`;
* `SaldoInsuficienteException`;
* `TransacaoNaoEncontradaException`;
* `AlteracaoHistoricoInvalidaException`;
* `HistoricoInconsistenteException`;
* `LoginJaCadastradoException`;
* `TickerInvalidoException`;
* `ServicoCotacaoIndisponivelException`;
* `ArquivoNaoEncontradoException`;
* `BancoDeDadosException`;
* `GlobalExceptionHandler`.

### Formato e status das respostas de erro

Todas as respostas de erro seguem o formato Problem Details (RFC 7807), com o tipo de conteúdo `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos possuem valores inválidos.",
  "instance": "/transacoes",
  "dataHora": "2026-08-05T20:00:00Z",
  "erros": [
    { "campo": "quantidade", "mensagem": "A quantidade deve ser maior que zero." }
  ]
}
```

O campo `erros` aparece somente em falhas de validação de campos.

O `GlobalExceptionHandler` utiliza os seguintes status:

- `400 Bad Request`: campos inválidos, corpo ilegível, parâmetro ausente ou com tipo inválido, ticker inválido, saldo insuficiente ou `IllegalArgumentException`;
- `401 Unauthorized`: login ou senha inválidos;
- `404 Not Found`: transação inexistente ou pertencente a outro usuário, ou rota inexistente;
- `405 Method Not Allowed`: método HTTP não aceito pela rota;
- `409 Conflict`: login já cadastrado, ou alteração/exclusão que tornaria o histórico inconsistente;
- `415 Unsupported Media Type`: tipo de conteúdo não suportado;
- `500 Internal Server Error`: histórico já persistido inconsistente, erro de banco ou erro inesperado. O detalhe da resposta é genérico e a causa é registrada no log;
- `503 Service Unavailable`: serviço de cotações indisponível e sem cotação anterior utilizável.

Requisições sem token válido em rotas protegidas são respondidas pelo `AutenticacaoEntryPoint` com `401` no mesmo formato.

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
* `AutenticacaoService`;
* `AutenticacaoEntryPoint`.

### `service`

Contém as principais regras de negócio.

Classes atuais:

* `CarteiraService`;
* `UsuarioService`.

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
usuario
```

O identificador é gerado automaticamente pelo banco.

A data é inicializada no construtor da entidade e o campo também utiliza `@CreationTimestamp` para o preenchimento durante a persistência.

Os valores de quantidade e preço unitário utilizam `BigDecimal` e são armazenados como `DECIMAL(38, 18)`.

O tipo da transação é representado pelo enum `TipoTransacao`.

Valores permitidos:

```text
COMPRA
VENDA
```

A entidade `Transacao` possui relacionamento obrigatório `@ManyToOne` com `Usuario`. A coluna `usuario_id` é `NOT NULL` e possui chave estrangeira para `usuarios.id`.

O relacionamento utiliza carregamento `LAZY` e o campo `usuario` é ignorado na serialização JSON. Assim, as respostas de transação não expõem a entidade `Usuario`, o login ou a senha.

---

## 4.2 Usuário

A classe `Usuario` é uma entidade JPA persistida na tabela `usuarios`.

Campos atuais:

```text
id
login
senha
```

O login é obrigatório, possui no máximo 100 caracteres e restrição de unicidade. A senha é obrigatória e armazenada como hash BCrypt.

A entidade implementa `UserDetails`, permitindo sua utilização pelo Spring Security.

Todo usuário recebe atualmente a autoridade:

```text
ROLE_USER
```

Não existem diferentes níveis de permissão neste momento.

Um usuário pode possuir várias transações, enquanto cada transação pertence a exatamente um usuário. O usuário associado à operação é definido pela aplicação a partir do contexto de segurança, e não por um campo enviado pelo cliente.

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

O saldo e o preço médio utilizam `BigDecimal`. O campo `variacao24h` não é utilizado pelo resumo atual, que lê a variação diretamente da `CotacaoMercado`.

A carteira é reconstruída processando o histórico de transações sobre os objetos `Moeda`.

---

## 5. Persistência

O CriptoVision utiliza MySQL e Spring Data JPA.

O schema é versionado com Flyway. As migrations ficam em:

```text
src/main/resources/db/migration
```

Migrations atuais:

```text
V1__cria_schema_inicial.sql              tabelas usuarios e transacoes
V2__torna_credenciais_obrigatorias.sql   login (100) e senha obrigatórios
V3__aumenta_precisao_valores_cripto.sql  quantidade e preço com DECIMAL(38, 18)
```

O Hibernate apenas valida o schema criado pelo Flyway:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Qualquer mudança de schema exige uma nova migration, e as entidades precisam continuar compatíveis com ela.

As entidades persistidas atualmente são:

```text
Usuario
Transacao
```

Os objetos `Carteira` e `Moeda` existem apenas durante a execução dos cálculos.

No schema atual, `transacoes.usuario_id` é obrigatório e referencia `usuarios.id`. Essa restrição também protege a integridade do vínculo no banco de dados. As colunas `ticker`, `quantidade`, `preco_unitario`, `data` e `tipo` ainda aceitam `NULL` no banco; a obrigatoriedade desses campos é garantida pela validação da API.

### Repositórios

O `TransacaoRepository` oferece:

* listagem cronológica das transações de um usuário;
* busca de uma transação por ID e usuário;
* persistência e exclusão de transações;
* consulta do total aportado agrupado por ticker e filtrado por usuário.

O `UsuarioRepository` oferece:

* operações CRUD de usuários;
* busca de usuário pelo login;
* verificação de existência de um login.

### Ordenação do histórico

As consultas utilizadas pelo `CarteiraService` recuperam somente as transações do usuário autenticado e solicitam ordenação crescente por `data` e, em caso de empate, por `id`.

Além disso, `reconstruirCarteira` ordena uma cópia da lista recebida antes do processamento. Essa segunda ordenação protege o motor mesmo quando ele recebe uma lista criada fora do repository.

---

## 6. Autenticação e autorização

A API utiliza autenticação stateless com JWT.

Os controllers protegidos recebem a própria entidade `Usuario` autenticada pelo `SecurityContext`. Esse usuário é repassado aos services e repositories para limitar todas as operações ao proprietário dos dados.

As rotas públicas são:

```http
POST /auth/register
POST /auth/login
```

As rotas do Swagger (`/swagger-ui.html`, `/swagger-ui/**` e `/v3/api-docs/**`) também são públicas.

Todas as outras rotas exigem autenticação.

### Cadastro

```text
Cliente envia login e senha
        ↓
Bean Validation (DadosCadastroUsuario)
        ↓
AutenticacaoController
        ↓
UsuarioService
        ↓
Verificação de login existente
        ↓
Senha criptografada com BCrypt
        ↓
Usuário salvo
        ↓
201 Created com id e login
```

O login é normalizado para letras minúsculas. Um login já cadastrado, inclusive em cadastros concorrentes detectados pela restrição de unicidade do banco, retorna `409 Conflict`.

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
Validação da senha (BCrypt)
        ↓
TokenService gera o JWT
        ↓
Token é devolvido ao cliente
```

O token é assinado com HMAC256 usando o segredo `JWT_SECRET`, possui o emissor `CriptoVision API`, o login como `subject` e expira em 2 horas.

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
Controller recebe o Usuario autenticado
        ↓
Requisição continua
```

A aplicação utiliza sessões stateless, portanto o servidor não mantém uma sessão HTTP para o usuário. Tokens ausentes, expirados, malformados ou com assinatura inválida não autenticam a requisição e resultam em `401 Unauthorized`, respondido pelo `AutenticacaoEntryPoint` no formato Problem Details.

### CORS

A configuração atual permite requisições das seguintes origens locais:

```text
http://localhost:5173
http://localhost:5174
```

Essas portas são utilizadas pelo frontend React com Vite. Os métodos permitidos são `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`.

---

## 7. Operações de transação

O `TransacaoController` recebe as requisições HTTP, obtém o `Usuario` autenticado e delega as operações ao `CarteiraService`.

```text
TransacaoController
        ↓
CarteiraService
        ↓
TransacaoRepository
```

O controller de transações não realiza operações de persistência diretamente.

### Concorrência

Criação, atualização e exclusão alteram o histórico que valida as próprias operações. Para que duas requisições simultâneas do mesmo usuário não validem contra o mesmo saldo, o `CarteiraService` executa a etapa de banco de cada uma por meio de `executarComHistoricoBloqueado`:

```text
TransactionTemplate inicia a transação
        ↓
UsuarioRepository.bloquearPorId (SELECT ... FOR UPDATE na linha do usuário)
        ↓
leitura do histórico, simulação e gravação
        ↓
commit libera o bloqueio
```

O bloqueio é a primeira consulta da transação. No MySQL (isolamento `REPEATABLE READ`), o snapshot de leitura é criado na primeira leitura comum; como ela ocorre depois do bloqueio, a operação enxerga tudo o que as anteriores gravaram.

A validação do ticker na Binance é feita antes de a transação começar, para que uma chamada HTTP lenta não mantenha o bloqueio nem uma conexão do banco ocupada.

O bloqueio vale por usuário: operações de usuários diferentes continuam paralelas.

### Criação

O endpoint responsável pelo cadastro é:

```http
POST /transacoes
```

Fluxo:

1. recebe um `TransacaoRequestDTO`;
2. obtém o usuário autenticado pelo Spring Security;
3. normaliza o ticker para maiúsculas e valida os campos com Bean Validation;
4. converte o DTO para `Transacao`;
5. associa a nova transação ao usuário autenticado;
6. normaliza o ticker com o `TickerNormalizer` e valida sua existência por meio do `CotacaoService`;
7. inicia a transação e bloqueia o histórico do usuário;
8. busca somente o histórico desse usuário em ordem cronológica;
9. reconstrói uma carteira temporária;
10. processa a nova transação;
11. valida o saldo em caso de venda;
12. persiste a transação;
13. retorna `201 Created`.

O cliente não pode definir diretamente o ID, a data ou o proprietário da transação. O `TransacaoRequestDTO` não possui `usuarioId`.

Um ticker inexistente na Binance retorna `400 Bad Request`. Se a Binance estiver indisponível e não houver cotação recente do ticker em cache, a criação retorna `503 Service Unavailable`.

### Consulta

Os endpoints de listagem e busca por ID também passam pelo `CarteiraService`.

A listagem utiliza a ordenação cronológica por data e ID e retorna somente as transações do usuário autenticado. A busca por ID exige simultaneamente o identificador e o proprietário.

Um ID inexistente ou pertencente a outro usuário lança `TransacaoNaoEncontradaException` e retorna `404 Not Found`. A resposta não informa se o recurso existe para outra conta.

### Atualização

O endpoint:

```http
PUT /transacoes/{id}
```

recebe o mesmo `TransacaoRequestDTO` validado utilizado na criação.

Fluxo:

1. verifica se a transação existe para o usuário autenticado, sem carregar a entidade, para responder `404` sem consultar a Binance;
2. normaliza e valida o novo ticker;
3. inicia a transação e bloqueia o histórico do usuário;
4. busca novamente a transação pelo ID e pelo usuário (ela pode ter sido excluída enquanto a operação aguardava o bloqueio);
5. cria uma transação candidata independente;
6. preserva o ID, a data e o proprietário originais;
7. substitui a transação somente em uma cópia do histórico do usuário;
8. reconstrói uma carteira temporária;
9. modifica e salva a entidade existente apenas quando o histórico simulado é válido;
10. retorna `200 OK`.

A entidade gerenciada pelo JPA não é modificada antes da validação da cópia.

Uma atualização que tornaria o histórico inconsistente lança `AlteracaoHistoricoInvalidaException` e retorna `409 Conflict`. Uma tentativa de alterar a transação de outro usuário retorna `404 Not Found` e não modifica o recurso.

### Exclusão

O endpoint:

```http
DELETE /transacoes/{id}
```

executa o seguinte fluxo:

1. inicia a transação e bloqueia o histórico do usuário;
2. busca a transação pelo ID e pelo usuário autenticado;
3. cria uma cópia do histórico desse usuário sem a transação;
4. reconstrói uma carteira temporária;
5. exclui a entidade apenas quando o histórico restante é válido;
6. retorna `204 No Content`.

Uma exclusão que tornaria o histórico inconsistente retorna `409 Conflict`. Uma tentativa de excluir a transação de outro usuário retorna `404 Not Found` e não remove o recurso.

---

## 8. Reconstrução da carteira

O estado atual da carteira não é armazenado diretamente no banco.

Sempre que um resumo ou simulação é solicitado, o sistema:

```text
Busca as transações do usuário em ordem cronológica
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

- somente o histórico do usuário autenticado participa da reconstrução;
- o histórico é processado em ordem cronológica por data e ID;
- o ticker de cada transação é normalizado pelo `TickerNormalizer`;
- inconsistências durante a reconstrução interrompem o cálculo;
- a aplicação lança `HistoricoInconsistenteException`;
- a exceção identifica ID, ticker e tipo da transação;
- a causa original da inconsistência é preservada e registrada no log pelo `GlobalExceptionHandler`.

### Limitações atuais

- inconsistências do histórico são registradas apenas como log de erro, sem logs estruturados ou alertas;
- a carteira é reconstruída repetidamente em diferentes endpoints.

---

## 9. Integração com a Binance

O subsistema `cotacao` consulta a Binance Public API por meio do `RestClient` do Spring, com timeouts de conexão e de leitura configuráveis.

URL-base padrão (endpoint público de dados de mercado):

```text
https://data-api.binance.vision/api/v3
```

Endpoint externo utilizado:

```text
/ticker/24hr
```

Ele é chamado com `symbol` (um par) ou `symbols` (lote de até 20 pares por requisição).

A integração é utilizada para:

* consultar o preço atual;
* consultar a variação percentual das últimas 24 horas;
* validar a existência de um ticker.

### Pares utilizados

As criptomoedas são consultadas por meio de pares com USDT.

Exemplos:

```text
BTCUSDT
ETHUSDT
SOLUSDT
LINKUSDT
```

O ativo USDT não é consultado: o `CotacaoService` retorna preço `1` e variação `0`.

### Tratamento de respostas

* `400` da Binance é interpretado como ticker inválido (`TickerInvalidoException`);
* outros erros HTTP, timeouts, falhas de rede, respostas vazias ou dados inesperados geram `ServicoCotacaoIndisponivelException`;
* uma cotação com preço menor ou igual a zero é rejeitada;
* falhas do provedor nunca são convertidas em preço zero.

### Cache e fallback

O `CotacaoCache` mantém em memória a última cotação obtida para cada ticker:

```properties
cotacoes.cache.ttl=60s         # cotação considerada atual
cotacoes.cache.max-stale=15m   # idade máxima para uso como fallback
```

O fluxo de uma consulta é:

1. cotações dentro do TTL são servidas pelo cache;
2. os tickers restantes são consultados em lote;
3. se o lote for rejeitado por ticker inválido, os tickers são consultados individualmente, e os inválidos são marcados como indisponíveis;
4. se a Binance estiver indisponível, é usada a última cotação com idade até `max-stale`, marcada como desatualizada;
5. sem cotação nem fallback, o ticker é marcado como indisponível.

Na validação de ticker durante a criação e a atualização de transações, uma cotação dentro da janela de fallback também é aceita quando o provedor está indisponível.

O cache é local a cada instância da aplicação e é perdido quando ela reinicia.

### Normalização de nomes

O `TickerNormalizer` remove espaços, converte para maiúsculas e mapeia alguns nomes conhecidos:

```text
BITCOIN → BTC
ETHEREUM → ETH
SOLANA → SOL
CHAINLINK → LINK
LNK → LINK
```

Tickers que não aparecem nessa lista são utilizados diretamente.

### Configuração

```properties
binance.api.base-url=${BINANCE_API_BASE_URL:https://data-api.binance.vision/api/v3}
cotacoes.http.connect-timeout=${COTACOES_CONNECT_TIMEOUT:3s}
cotacoes.http.read-timeout=${COTACOES_READ_TIMEOUT:5s}
cotacoes.cache.ttl=${COTACOES_CACHE_TTL:60s}
cotacoes.cache.max-stale=${COTACOES_CACHE_MAX_STALE:15m}
```

### Limitações atuais

* as chamadas são síncronas;
* o cache não é compartilhado entre instâncias;
* somente pares com USDT são suportados.

---

## 10. Endpoints atuais

## Autenticação

```http
POST /auth/register
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
GET /carteira/simulador/dca?ticker=&aporte=&preco=
GET /carteira/simulador/venda?ticker=&precoAlvo=
```

## Análises

```http
GET /analise/aportes-por-moeda
```

Todos os endpoints de transações, carteira e análises exigem autenticação e operam exclusivamente sobre os dados do usuário identificado pelo JWT.

A documentação interativa fica em `/swagger-ui.html` e a especificação OpenAPI em `/v3/api-docs`.

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
Busca do histórico do usuário autenticado
        ↓
Reconstrução da carteira
        ↓
CotacaoService.buscarCotacoes (lote, cache e fallback)
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
cotacaoDisponivel
cotacaoDesatualizada
cotacaoAtualizadaEm
```

Quando não há cotação nem fallback para um ativo, ele continua na lista com `cotacaoDisponivel = false`, e `precoAtual`, `valorTotalUSD`, `porcentagemPNL`, `variacao24h` e `cotacaoAtualizadaEm` ficam nulos.

O resumo geral contém:

```text
valorTotalCarteira          valor atual das posições com cotação
pnlGeral                    PNL não realizado das posições com cotação
variacao24hCarteira         variação estimada em 24 horas
ativos                      lista de ativos
cotacoesAtualizadasEm       horário da cotação mais antiga utilizada
cotacoesParciais            há ativos sem cotação ou com cotação de fallback
ativosSemCotacao            tickers sem cotação nem fallback
```

Uma falha de cotação não derruba o resumo: os demais ativos continuam sendo calculados e a resposta informa que os dados são parciais.

---

## 12. Docker

O projeto possui:

```text
Dockerfile
docker-compose.yml
```

O Docker Compose define:

* um container MySQL 8.0;
* um container para a API construída a partir do código atual;
* um volume persistente para o banco;
* credenciais, segredos e configuração das cotações recebidos por variáveis de ambiente.

O MySQL é exposto localmente na porta:

```text
3307
```

A API é exposta na porta:

```text
8080
```

O Dockerfile usa a imagem `eclipse-temurin:25-jdk-alpine` e espera que o JAR já tenha sido gerado em `target/`.

O ambiente local utiliza um arquivo `.env`, ignorado pelo Git, para fornecer `MYSQL_ROOT_PASSWORD`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`, além das variáveis opcionais `BINANCE_API_BASE_URL` e `COTACOES_*`. O arquivo `.env.example` serve de modelo. Dentro da rede do Compose, a API acessa o banco pelo hostname `db` e pela porta interna `3306`.

Fluxo atual:

```text
Maven gera o JAR
        ↓
Docker copia o JAR
        ↓
Java executa o arquivo app.jar
        ↓
Flyway aplica as migrations pendentes
```

---

## 13. Testes automatizados

A aplicação possui testes de unidade, persistência e integração HTTP.

Os testes de integração usam o perfil `test` (`application-test.properties`):

* banco H2 em memória no modo MySQL;
* schema criado pelo Hibernate (`ddl-auto=create-drop`), com o Flyway desativado;
* segredo JWT fixo;
* URL da Binance apontando para `http://localhost`, para que nenhum teste dependa do provedor real.

Principais grupos:

* testes de unidade do motor financeiro e da criação, atualização e exclusão de transações no `CarteiraService`;
* `CotacaoServiceTest` para cache, lote, fallback e tickers indisponíveis;
* `BinanceCotacaoClientTest` com `MockRestServiceServer` para o mapeamento das respostas e dos erros da Binance;
* `CarteiraResumoCotacoesTest` para o resumo com cotações parciais;
* `TransacaoRepositoryUsuarioTest` (`@DataJpaTest`) para as consultas restritas ao proprietário;
* `IsolamentoTransacoesHttpTest`, que inicializa o contexto Spring Boot, autentica dois usuários com JWT e verifica por `MockMvc` que um usuário não lista, consulta, altera ou exclui transações do outro, nem acessa a carteira e as análises do outro;
* `ConcorrenciaTransacoesTest`, que usa duas threads para verificar que uma venda aguarda o bloqueio de outra venda em andamento e é rejeitada por saldo insuficiente depois do commit;
* `CadastroUsuarioHttpTest`, `PadronizacaoErrosHttpTest` e `OpenApiDocumentationTest` para o cadastro, o formato dos erros e a documentação.

A suíte completa pode ser executada com:

```powershell
.\mvnw.cmd clean test
```

O GitHub Actions executa a mesma suíte com JDK 25 em cada push e pull request para a branch `main`.

---

## 14. Decisões arquiteturais atuais

As seguintes decisões serão preservadas inicialmente:

* histórico de transações como fonte dos cálculos;
* uso de `BigDecimal` no núcleo financeiro;
* separação entre controllers, services e repositories;
* autenticação stateless com JWT;
* associação obrigatória de cada transação ao usuário autenticado;
* isolamento de históricos, cálculos e análises por usuário;
* MySQL como banco principal, com schema versionado pelo Flyway;
* Binance como fonte inicial de preços, isolada atrás da interface `CotacaoClient`;
* falhas do provedor de cotações tratadas explicitamente, sem preços zerados;
* respostas de erro no formato Problem Details;
* DTOs para respostas específicas;
* API REST independente do frontend.

---

## 15. Dívidas técnicas conhecidas

Os principais pontos identificados são:

1. o `AnaliseController` ainda acessa o repository diretamente;
2. a carteira é reconstruída repetidamente em diferentes endpoints;
3. inconsistências do histórico não possuem logs estruturados nem alertas;
4. as simulações de DCA e de venda e o endpoint `/carteira/total` ainda utilizam `double`;
5. parte da injeção de dependências ainda é realizada por atributos (`@Autowired` em `CarteiraController`, `TransacaoController`, `AnaliseController` e `SecurityFilter`);
6. não há paginação ou filtros no histórico;
7. não há lucro realizado persistido ou consolidado;
8. não há suporte a taxas;
9. `spring.jpa.open-in-view` ainda utiliza a configuração padrão;
10. as colunas de dados da tabela `transacoes` ainda aceitam `NULL` no banco;
11. os testes usam schema gerado pelo Hibernate, então as migrations do Flyway não são exercitadas na suíte;
12. o cache de cotações é local à instância;
13. `/carteira/total` retorna texto simples com o rótulo "R$", embora os valores não sejam convertidos para reais.

Esses itens serão tratados em commits pequenos e independentes.
