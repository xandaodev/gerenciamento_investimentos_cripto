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
├── observabilidade
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
  "requestId": "3f2a9c1e-5b7d-4e8a-9c0f-1a2b3c4d5e6f",
  "erros": [
    { "campo": "quantidade", "mensagem": "A quantidade deve ser maior que zero." }
  ]
}
```

O campo `erros` aparece somente em falhas de validação de campos. O `requestId` é o mesmo do cabeçalho `X-Request-Id` e das linhas de log da requisição: num erro relatado por um usuário, ele leva direto ao log com a causa.

O `GlobalExceptionHandler` utiliza os seguintes status:

- `400 Bad Request`: campos inválidos, corpo ilegível, parâmetro ausente ou com tipo inválido, ticker inválido, saldo insuficiente ou `IllegalArgumentException`;
- `401 Unauthorized`: login ou senha inválidos;
- `404 Not Found`: transação inexistente ou pertencente a outro usuário, ou rota inexistente;
- `405 Method Not Allowed`: método HTTP não aceito pela rota;
- `409 Conflict`: login já cadastrado, ou alteração/exclusão que tornaria o histórico inconsistente;
- `415 Unsupported Media Type`: tipo de conteúdo não suportado;
- `500 Internal Server Error`: histórico já persistido inconsistente, erro de banco ou erro inesperado. O detalhe da resposta é genérico e a causa é registrada no log, com o ID da requisição. Cada `500` também incrementa a métrica `criptovision.erros.internos`, com o tipo da exceção;
- `503 Service Unavailable`: serviço de cotações indisponível e sem cotação anterior utilizável.

Requisições sem token válido em rotas protegidas são respondidas pelo `AutenticacaoEntryPoint` com `401` no mesmo formato, também com o `requestId`.

### `model`

Contém as entidades persistidas e os modelos utilizados na reconstrução da carteira.

Classes atuais:

* `Transacao`;
* `TipoTransacao`;
* `Usuario`;
* `Carteira`;
* `Moeda`.

### `observabilidade`

Contém o que liga as requisições aos logs e às métricas:

* `IdentificadorRequisicaoFilter`: o primeiro filtro de toda requisição. Dá a ela um ID, que vai para o MDC (e, portanto, para todas as linhas de log da requisição), para o cabeçalho `X-Request-Id` da resposta e para o campo `requestId` das respostas de erro. Um `X-Request-Id` recebido do cliente só é aproveitado se tiver até 64 caracteres entre letras, dígitos, `.`, `_` e `-`; qualquer outro valor é trocado por um UUID, para que ninguém escreva texto livre nos logs;
* `MetricasCriptoVision`: as métricas de negócio, descritas em "Observabilidade" na seção 12.

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

A data é a da operação, sempre em UTC. O cliente pode informá-la no `TransacaoRequestDTO` (`OffsetDateTime`, com fuso obrigatório), e o DTO a converte para UTC. O `CarteiraService.resolverData` completa a data ausente (o momento atual, vindo do `Clock` em UTC, ou a data anterior na alteração), recusa datas no futuro além de 1 minuto e anteriores a 2009-01-03, e trunca em microssegundos, a precisão da coluna `DATETIME(6)`. Não há mais `@CreationTimestamp`, que gravava o momento da persistência no fuso da JVM e ignorava qualquer data informada.

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

Quando uma moeda ainda não existe na carteira, o método `obterMoeda` cria um novo objeto com saldo e custo total iguais a zero, e portanto preço médio zero.

---

## 4.4 Moeda

A classe `Moeda` também não é persistida.

Ela representa o estado calculado de um ativo dentro da carteira.

Campos atuais:

```text
nome
ticker
saldo
custoTotal
variacao24h
```

O saldo e o custo total utilizam `BigDecimal`. O `custoTotal` é o custo de aquisição do saldo atual, acumulado sem arredondamento nas compras e reduzido proporcionalmente nas vendas.

O preço médio não é armazenado. `getPrecoMedio()` o calcula como `custoTotal ÷ saldo`, com 18 casas decimais e `HALF_UP`, e devolve zero quando não há saldo. Esse valor arredondado é só para leitura e nunca volta para o processamento das transações seguintes. As regras completas estão nas seções 6, 8 e 11 de `docs/BUSINESS_RULES.md`.

O campo `variacao24h` não é utilizado pelo resumo atual, que lê a variação diretamente da `CotacaoMercado`.

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
* busca de usuário pelo login, que devolve `Optional<Usuario>`;
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

Quando o login não existe, o `AutenticacaoService` lança `UsernameNotFoundException` (ele nunca devolve `null`). Antes de recusar, o `DaoAuthenticationProvider` compara a senha enviada com um hash BCrypt fictício. Por isso, um login inexistente leva praticamente o mesmo tempo para responder que uma senha errada, e as duas falhas devolvem a mesma resposta: `401 Unauthorized` com a mensagem genérica "Login ou senha inválidos.". Nem o conteúdo nem o tempo da resposta revelam quais logins estão cadastrados.

### Proteção contra força bruta

O `AutenticacaoController` consulta o `ProtecaoForcaBruta` antes de autenticar ou cadastrar:

```text
POST /auth/login
        ↓
Limite de login por IP (conta a tentativa)
        ↓
Limite de falhas do login (só consulta)
        ↓
AuthenticationManager
        ↓
Senha errada → registra uma falha do login → 401
Sucesso      → zera as falhas do login     → token
```

No cadastro, só o limite de cadastro por IP é consultado. Quando um limite é atingido, o controller lança `TentativasExcedidasException`, que o `GlobalExceptionHandler` transforma em `429 Too Many Requests` com Problem Details e o cabeçalho `Retry-After`. As regras e os valores padrão estão em `docs/BUSINESS_RULES.md`. Os limites vêm do record `TentativasProperties` (`seguranca.tentativas.*`), que impede a aplicação de subir se algum valor estiver ausente ou inválido.

Cada limite é um `LimiteDeTentativas`: um contador por chave (IP ou login normalizado) em janela fixa, com o tempo vindo do `Clock`. Detalhes de implementação:

* a atualização de cada contador é atômica (`compute` do mapa do Caffeine), então requisições simultâneas não passam juntas pela última vaga;
* os contadores ficam num cache Caffeine limitado a 100.000 chaves por limite, e a chave do login é cortada em 100 caracteres, para que um ataque com muitos IPs ou logins diferentes não esgote a memória;
* o log registra um aviso quando uma chave atinge o limite, e não um por tentativa recusada, para que um ataque não lote o log. A chave vai para o log só com caracteres seguros.

Limitações atuais:

* os contadores são locais à instância e se perdem quando ela reinicia. Com mais de uma instância, cada uma teria os próprios limites, e seria preciso um armazenamento compartilhado, como Redis;
* o IP é o `request.getRemoteAddr()`. Atrás de um proxy reverso, ele só é o do cliente se `server.forward-headers-strategy` estiver configurado (item #2 do roadmap). Sem isso, todos os clientes compartilhariam o limite do IP do proxy;
* o bloqueio por login permite que alguém bloqueie temporariamente a conta de outra pessoa errando a senha de propósito. É a troca usual para impedir a adivinhação de senhas, e o bloqueio dura no máximo a janela configurada.

O token é assinado com HMAC-SHA256 usando o segredo `JWT_SECRET` e contém:

| Claim | Valor |
| --- | --- |
| `iss` | `CriptoVision API` |
| `aud` | `criptovision-api` |
| `sub` | o login do usuário |
| `versao` | a versão dos tokens do usuário no momento da emissão |
| `iat` / `exp` | a emissão e a expiração, 2 horas depois |

O `TokenService` só aceita tokens com esse emissor, essa audiência e a claim `versao`, com o tempo vindo do `Clock`. Na inicialização, ele exige um `JWT_SECRET` de ao menos 32 bytes, o mínimo para o HMAC-SHA256 (RFC 7518), e recusa o valor de exemplo do `.env.example`. Com um segredo fraco, a aplicação não sobe.

### Revogação dos tokens

Cada usuário tem uma `versao_token` (migration `V4`). O token carrega a versão com que foi emitido, e o `SecurityFilter` só autentica a requisição se ela for igual à do banco. Isso não custa uma consulta extra, porque o filtro já carrega o usuário a cada requisição. Incrementar a versão revoga todos os tokens do usuário, em todos os dispositivos:

* `POST /auth/logout` incrementa a versão e responde `204`;
* `PUT /auth/senha` confere a senha atual, troca a senha, incrementa a versão e devolve um token novo para a sessão atual continuar. A senha atual errada responde `400` e conta como falha de login para a proteção contra força bruta, então quem roubar um token não consegue testar senhas por ali.

As duas operações leem o usuário com `SELECT ... FOR UPDATE` (`UsuarioRepository.bloquearPorId`), para que um logout e uma troca de senha simultâneos não percam o incremento um do outro.

O logout encerra todas as sessões, e não só a do dispositivo atual. Revogar um token específico exigiria guardar os identificadores dos tokens revogados até a expiração deles.

**Refresh token: avaliado e não adotado por enquanto.** Ele permitiria tokens de acesso mais curtos sem pedir login a cada 2 horas. Em troca, exigiria guardar os refresh tokens no banco, rotacioná-los a cada uso e detectar reuso para encerrar a família de tokens roubada. Com tokens de 2 horas e revogação pela versão, o ganho não compensa agora. Se o frontend precisar de sessões longas, este é o próximo passo.

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
TokenService valida assinatura, emissor, audiência, expiração e versão
        ↓
Usuário é buscado pelo login
        ↓
A versão do token precisa ser igual à do usuário
        ↓
Autenticação é registrada no SecurityContext
        ↓
Controller recebe o Usuario autenticado
        ↓
Requisição continua
```

A aplicação utiliza sessões stateless, portanto o servidor não mantém uma sessão HTTP para o usuário. Tokens ausentes, expirados, malformados, com assinatura ou audiência inválida, revogados (versão antiga) ou emitidos para um login que não existe mais não autenticam a requisição e resultam em `401 Unauthorized`, respondido pelo `AutenticacaoEntryPoint` no formato Problem Details.

### CORS

As origens autorizadas vêm da propriedade `cors.allowed-origins`, alimentada pela variável de ambiente `CORS_ALLOWED_ORIGINS` (origens separadas por vírgula). Sem a variável, só o frontend local do React com Vite é aceito:

```text
http://localhost:5173
http://localhost:5174
```

Em produção, a variável deve conter só o domínio do frontend. As origens informadas substituem as locais, em vez de se somarem a elas.

O record `CorsProperties` valida a lista na inicialização. Ele remove espaços, barras finais e repetições, e impede a aplicação de subir quando a lista está vazia ou quando uma origem não tem o formato do cabeçalho `Origin` (esquema `http` ou `https`, host e porta opcional): sem esquema, com caminho, com query ou com o curinga `*`. A mensagem de erro mostra a origem inválida como foi digitada. Sem essa validação, uma origem mal escrita não casaria com nenhuma requisição, e o frontend falharia sem explicação.

Os métodos permitidos são `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`. As respostas de erro, como o `401`, também levam o cabeçalho `Access-Control-Allow-Origin`, para que o frontend consiga ler o Problem Details. Requisições de origens não autorizadas recebem `403`.

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
4. converte o DTO para `Transacao`, com a data informada já em UTC;
5. associa a nova transação ao usuário autenticado;
6. normaliza o ticker com o `TickerNormalizer` e valida sua existência por meio do `CotacaoService`;
7. completa e valida a data (`resolverData`);
8. inicia a transação e bloqueia o histórico do usuário;
9. busca somente o histórico desse usuário em ordem cronológica;
10. reconstrói uma carteira temporária **com as transações anteriores à data da nova** e processa a nova nesse ponto. Uma venda sem saldo naquela data lança `SaldoInsuficienteException` (400);
11. reproduz o histórico inteiro com a nova transação na posição da data dela. Se uma transação posterior ficar inválida, lança `AlteracaoHistoricoInvalidaException` (409);
12. persiste a transação;
13. retorna `201 Created`.

A etapa 11 é necessária porque a data pode ser retroativa: a nova transação não é necessariamente a última do histórico. As transações com a mesma data são ordenadas pelo `id`, e a nova, que ainda não tem `id`, fica depois das que já existiam com a mesma data.

O cliente pode informar a data, mas não o ID nem o proprietário da transação. O `TransacaoRequestDTO` não possui `usuarioId`.

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
Atualiza saldo e custo total de cada Moeda
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

- inconsistências do histórico vão para o log de erro (em JSON no docker-compose, com o ID da requisição) e para a métrica `criptovision.erros.internos`, mas ainda não disparam um alerta: isso depende de uma ferramenta de monitoramento, ainda não escolhida;
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
POST /auth/logout      (autenticado)
PUT  /auth/senha       (autenticado)
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

## Saúde

```http
GET /saude
```

Público, para um monitor de disponibilidade: responde `{"status":"UP"}` com `200`, ou `DOWN` com `503` quando o banco não responde, sem nenhum outro detalhe. O health completo e as métricas ficam na porta de gerenciamento (veja "Observabilidade" na seção 12).

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
Caddyfile
```

O Docker Compose define:

* um container MySQL 8.0 (`db`);
* um container para a API construída a partir do código atual (`api`);
* um container Caddy (`caddy`), só no perfil `producao`;
* volumes persistentes para o banco (`mysql_data`) e para os certificados do Caddy (`caddy_data` e `caddy_config`);
* credenciais, segredos e configuração recebidos por variáveis de ambiente.

Há dois modos de execução:

| Comando | Modo | Acesso |
| --- | --- | --- |
| `docker compose up --build` | desenvolvimento | API em `http://localhost:8080` e MySQL em `localhost:3307`, sem HTTPS |
| `docker compose --profile producao up -d --build` | produção (VPS) | Caddy em `80` e `443`, com HTTPS no domínio de `DOMINIO` |

Em ambos os modos, as portas da API e do MySQL são publicadas só em `127.0.0.1`. Na VPS, o único acesso de fora é pelo Caddy. Isso importa porque o Docker publica portas por fora do firewall do sistema (`ufw`). Na VPS, definir `COMPOSE_PROFILES=producao` no `.env` faz os comandos do Compose incluírem o Caddy sem o `--profile`.

### HTTPS com o Caddy

```text
Internet
   │ 443 (HTTPS)      80 → redireciona para 443
   ▼
Caddy ──HTTP, rede interna do Docker──▶ api:8080 ──▶ db:3306
```

O `Caddyfile` usa o domínio de `DOMINIO`. Para ele, o Caddy emite e renova sozinho o certificado do Let's Encrypt, que fica no volume `caddy_data`, e redireciona HTTP para HTTPS. Sem `DOMINIO`, o container do Caddy termina com uma mensagem pedindo a variável. A checagem fica no container, e não no Compose, porque o Compose interpola o arquivo inteiro mesmo sem o perfil ativo, e exigir a variável ali quebraria o modo de desenvolvimento.

O Caddy repassa para a API o IP real do cliente em `X-Forwarded-For` e o protocolo original em `X-Forwarded-Proto`, descartando os valores que o próprio cliente enviar. No Compose, a API roda com `SERVER_FORWARD_HEADERS_STRATEGY=native`: o Tomcat só aceita esses cabeçalhos vindos de IPs internos, como o do Caddy na rede do Docker. Com isso:

* os limites por IP do login e do cadastro usam o IP do cliente, e não o do Caddy;
* a API reconhece que a requisição chegou por HTTPS, e o Spring Security envia o cabeçalho `Strict-Transport-Security` (HSTS).

Fora do Compose, a propriedade `server.forward-headers-strategy` fica em `none` por padrão. Sem um proxy na frente, aceitar esses cabeçalhos permitiria a um cliente forjar o próprio IP. O `ProxyReversoHttpTest` e o `SemProxyReversoHttpTest` cobrem os dois casos num Tomcat real. O passo a passo de deploy numa VPS está no README.

### Conexão da API com o MySQL

A conexão entre a API e o MySQL é cifrada com TLS:

* o MySQL roda com `--require-secure-transport=ON` e recusa qualquer conexão de rede sem TLS, inclusive de ferramentas locais em `localhost:3307`. A maioria dos clientes (o `mysql` de linha de comando, DBeaver, MySQL Workbench) já usa TLS por padrão. Conexões pelo socket local dentro do próprio container, como `docker compose exec db mysql`, continuam permitidas, porque não passam pela rede;
* a API conecta com `sslMode=REQUIRED`. Se o TLS não puder ser estabelecido, a conexão falha, em vez de seguir sem criptografia;
* o certificado do servidor é gerado pelo próprio MySQL 8 ao inicializar o banco e fica no volume `mysql_data`, então não há certificados para criar ou distribuir;
* sem `allowPublicKeyRetrieval`. O login padrão do MySQL 8 (`caching_sha2_password`) envia a senha dentro do TLS. Sem TLS, ele exigiria buscar a chave pública do servidor, e um servidor falso poderia entregar a própria chave e capturar a senha.

O modo `REQUIRED` cifra a conexão, mas não verifica o certificado do servidor. Aqui isso basta: a API e o MySQL estão no mesmo servidor e se falam pela rede interna do Docker, onde só um atacante que já controla a máquina conseguiria se passar pelo banco. Se o banco passar para outro servidor ou para um serviço gerenciado, use `sslMode=VERIFY_IDENTITY` com o certificado da autoridade do provedor num truststore.

Fora do Compose, a URL de `application.properties` não define `sslMode`, e o driver usa o padrão `PREFERRED` (TLS quando o servidor oferece). O `ConexaoSeguraMysqlIT` verifica, num MySQL que exige TLS, que o pool da aplicação conecta com TLS e que uma conexão sem TLS é recusada.

### Backup do MySQL

O serviço `backup` (perfis `producao` e `backup`) é construído a partir de `backup/Dockerfile`. A imagem parte da `mysql:8.0`, para que o `mysqldump` seja da mesma versão do servidor, e recebe o binário do rclone da imagem oficial `rclone/rclone`. Toda a lógica está em `backup/backup.sh`.

```text
todo dia em BACKUP_HORARIO (UTC)
        ↓
mysqldump --single-transaction (TLS, usuário da aplicação)
        ↓
gzip → gpg simétrico AES-256 com BACKUP_SENHA
        ↓
confere: decifra, descompacta e exige a linha final "-- Dump completed"
        ↓
volume mysql_backups (retenção BACKUP_RETENCAO_DIAS)
        ↓
rclone → bucket S3 (retenção BACKUP_RETENCAO_REMOTA_DIAS), se BACKUP_S3_BUCKET estiver definido
```

Decisões:

* **cifra antes de sair do servidor**: o bucket só recebe arquivos cifrados. A `BACKUP_SENHA` existe só no ambiente do container e nunca é enviada. O formato é OpenPGP padrão, então um backup pode ser decifrado com qualquer `gpg`, sem depender do rclone nem deste projeto;
* **nenhum backup é aceito sem conferência**: o arquivo é gerado com nome temporário e só ganha o nome final depois de ser decifrado e de mostrar que o dump terminou. Um `mysqldump` interrompido nunca vira um backup válido;
* **nenhuma falha silenciosa**: sem `BACKUP_SENHA`, o serviço não roda. Sem bucket, cada backup registra um aviso. Falhas aparecem no log como `ERRO`, e o `HEALTHCHECK` deixa o container `unhealthy` se o último backup tiver mais de 26 horas;
* **credencial mínima no bucket**: o rclone é configurado só por variáveis de ambiente, sem arquivo com as chaves, e com `no_check_bucket`, para que a credencial precise apenas ler e escrever no bucket;
* **a senha do banco vai por `MYSQL_PWD`**, e não por um arquivo de opções do MySQL, cujo leitor corta valores com `#`;
* **teste de restauração real**: `backup testar` cria um MySQL temporário dentro do container, restaura o backup, confere as tabelas `usuarios`, `transacoes` e `flyway_schema_history` e descarta tudo. Roda sozinho todo domingo;
* **restauração segura**: `backup restaurar` exige `--confirmar` e, antes de substituir os dados, faz um backup do estado atual. Se o arquivo não estiver no servidor, ele é baixado do bucket, como numa VPS nova.

Limitações atuais:

* o backup é diário: numa perda total, podem ficar de fora até 24 horas de transações. Não há recuperação até um instante qualquer (*point-in-time recovery*) com o binlog;
* o backup usa o usuário da aplicação, que também pode apagar dados. Um usuário só de leitura para o backup exigiria criá-lo no banco;
* com a credencial de leitura e escrita, quem invadir a VPS também pode apagar os backups do bucket. O README descreve a alternativa sem permissão de apagar, usando uma regra de ciclo de vida do bucket;
* a imagem do MySQL descarta `\` nas senhas ao criar o usuário (`MYSQL_PASSWORD`), então as senhas do banco não devem ter `\` nem `'`. O `.env.example` recomenda gerá-las com `openssl rand -hex`.

O `BackupMysqlIT` cobre o serviço com containers de verdade: um MySQL que exige TLS, com as migrations do Flyway, e um bucket S3 servido pelo `rclone serve s3`.

O Dockerfile usa a imagem `eclipse-temurin:25-jdk-alpine` e espera que o JAR já tenha sido gerado em `target/`.

O ambiente utiliza um arquivo `.env`, ignorado pelo Git, para fornecer `MYSQL_ROOT_PASSWORD`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`, além de `CORS_ALLOWED_ORIGINS` e `DOMINIO` em produção e das variáveis opcionais `BINANCE_API_BASE_URL` e `COTACOES_*`. O arquivo `.env.example` serve de modelo. Dentro da rede do Compose, a API acessa o banco pelo hostname `db` e pela porta interna `3306`.

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

### Observabilidade

O Actuator fica numa **porta de gerenciamento separada** (`8081`, `MANAGEMENT_SERVER_PORT`), que por padrão só escuta no loopback (`MANAGEMENT_SERVER_ADDRESS=127.0.0.1`). No docker-compose, isso quer dizer de dentro do próprio container: a porta não é publicada e o Caddy só repassa a `8080`. Por isso o Spring Security libera os endpoints do Actuator (`EndpointRequest.toAnyEndpoint()`): quem os alcança já está dentro do servidor. O `ConfiguracaoDeProducaoTest` trava esses padrões, porque os testes HTTP usam uma porta de gerenciamento aleatória e não os veriam.

| Onde | Caminho | Conteúdo |
| --- | --- | --- |
| porta da API (pública) | `GET /saude` | só `UP` (`200`) ou `DOWN` (`503`); inclui o banco |
| porta de gerenciamento | `/actuator/health` | health completo, com o banco, o disco e as probes |
| porta de gerenciamento | `/actuator/health/liveness` e `/actuator/health/readiness` | probes, para o `HEALTHCHECK` do container e orquestradores |
| porta de gerenciamento | `/actuator/metrics` e `/actuator/prometheus` | métricas, no formato do Spring e no do Prometheus |

Só `health`, `metrics` e `prometheus` são expostos. Endpoints como `env`, `configprops` e `heapdump` revelariam segredos ou a memória da JVM.

Dentro do container, as métricas podem ser lidas com:

```bash
docker compose exec api wget -qO- http://127.0.0.1:8081/actuator/prometheus
```

Além das métricas que o Spring já publica (`http_server_requests`, com o status de cada rota; JVM; pool de conexões do Hikari), o `MetricasCriptoVision` publica:

| Métrica | Tag | Quando sobe |
| --- | --- | --- |
| `criptovision.erros.internos` | `excecao` (tipo da exceção) | toda resposta `500`, inclusive `HistoricoInconsistenteException` |
| `criptovision.tentativas.bloqueadas` | `limite` (`login-por-ip`, `falhas-por-login`, `cadastro-por-ip`) | toda recusa `429` da proteção contra força bruta |
| `criptovision.cotacoes.provedor.indisponivel` | `resultado` (`cotacao_anterior`, `sem_cotacao`) | toda falha da Binance, pelo que a API fez: usou uma cotação anterior ou ficou sem cotação |

As tags nunca levam IDs, tickers ou logins, para que o número de séries não cresça sem limite. Ticker desconhecido (`400`) não conta como falha do provedor.

**Logs.** Cada linha de log de uma requisição leva o ID dela: no texto, como `[id] ` antes do nome da classe (`logging.pattern.correlation`); no docker-compose, em JSON no formato ECS (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`), um objeto por linha, com o ID no campo `requestId`. O SQL do Hibernate só vai para o log com `JPA_SHOW_SQL=true`, desligado por padrão. Todos os serviços do compose usam o driver `json-file` com rotação (5 arquivos de 10 MB por container), para que os logs não encham o disco da VPS.

**Alertas.** Ainda não há alerta ativo: os sinais estão prontos (a métrica `criptovision.erros.internos`, o `/saude` e os logs de `ERROR`) para quando houver uma ferramenta de monitoramento. Um monitor externo de disponibilidade (como o UptimeRobot) pode consultar `https://<DOMINIO>/saude` sem nenhuma mudança na API.

---

## 13. Testes automatizados

A aplicação possui testes de unidade, persistência e integração HTTP, além de testes de integração contra um MySQL real (veja "Testes de integração com MySQL" abaixo).

Os testes executados por `mvn test` usam o perfil `test` (`application-test.properties`):

* banco H2 em memória no modo MySQL;
* schema criado pelo Hibernate (`ddl-auto=create-drop`), com o Flyway desativado;
* segredo JWT fixo;
* URL da Binance apontando para `http://localhost`, para que nenhum teste dependa do provedor real.

Principais grupos:

* testes de unidade do motor financeiro e da criação, atualização e exclusão de transações no `CarteiraService`;
* `CarteiraServicePrecoMedioTest`, que verifica que o custo da posição não acumula erro de arredondamento: moedas de preço muito baixo, compras seguidas, venda parcial com divisão não exata, venda total e o PNL do resumo;
* `CotacaoServiceTest` para cache, lote, fallback e tickers indisponíveis;
* `BinanceCotacaoClientTest` com `MockRestServiceServer` para o mapeamento das respostas e dos erros da Binance;
* `CarteiraResumoCotacoesTest` para o resumo com cotações parciais;
* `TransacaoRepositoryUsuarioTest` (`@DataJpaTest`) para as consultas restritas ao proprietário;
* `IsolamentoTransacoesHttpTest`, que inicializa o contexto Spring Boot, autentica dois usuários com JWT e verifica por `MockMvc` que um usuário não lista, consulta, altera ou exclui transações do outro, nem acessa a carteira e as análises do outro;
* `ConcorrenciaTransacoesTest`, que usa duas threads para verificar que uma venda aguarda o bloqueio de outra venda em andamento e é rejeitada por saldo insuficiente depois do commit;
* `AutenticacaoServiceTest`, que verifica que um login inexistente lança `UsernameNotFoundException` e que o `DaoAuthenticationProvider` ainda compara a senha com o hash fictício, sem deixar o tempo de resposta revelar quais logins existem;
* `CarteiraServiceDataTest`, para as bordas da validação da data com relógio fixo (1 minuto de tolerância no futuro, o dia 2009-01-03, a data automática e a precisão de microssegundos); e `DataTransacaoHttpTest`, que verifica por `MockMvc` a conversão para UTC, a compra antiga lançada depois da venda, a venda antiga recusada com 409 por deixar uma venda posterior sem saldo, as datas recusadas (futuro, antes de 2009, sem fuso) e a alteração que move, mantém ou recusa a data;
* `CarteiraServiceTotaisTest`, para os totais de `/carteira/total` (custo das posições, total aportado e total recebido em vendas) com vários ativos, posição totalmente vendida, valores de 18 casas e histórico inconsistente; e `TotalCarteiraHttpTest`, que verifica o JSON com a moeda `USDT` e que o custo das posições não fica negativo depois de uma venda com lucro;
* `CarteiraServiceSimulacaoTest`, para as simulações de DCA e de venda com valores exatos em `BigDecimal`: moeda de preço muito baixo, primeira compra sem posição, posição totalmente vendida, venda com lucro e prejuízo, arredondamento do percentual e venda sem saldo; e `SimuladoresHttpTest`, que verifica por `MockMvc` que os simuladores recusam valores zero, negativos, `NaN`, `Infinity` e parâmetros ausentes com `400`, e nunca devolvem `NaN`;
* `TokenServiceTest`, para a validação do `JWT_SECRET` na subida, a audiência, a versão, a assinatura e a expiração com relógio fixo; e `SessaoHttpTest`, que verifica por `MockMvc` que o logout revoga os tokens de todos os dispositivos, que a troca de senha revoga os tokens antigos e devolve um novo, os erros da troca de senha e o bloqueio por força bruta pela troca de senha;
* `LimiteDeTentativasTest`, para a janela fixa com relógio ajustável e para a atomicidade com tentativas simultâneas; `ProtecaoForcaBrutaTest`, para a normalização da chave do login, o tempo do `Retry-After` e a validação dos limites; e `ProtecaoForcaBrutaHttpTest`, que verifica por `MockMvc` os bloqueios por login e por IP, o reset depois de um login com sucesso e a resposta `429`;
* `ProxyReversoHttpTest` e `SemProxyReversoHttpTest`, que sobem um Tomcat real: com `server.forward-headers-strategy=native`, o limite por IP usa o IP informado pelo proxy e o HSTS é enviado em HTTPS; sem a propriedade, um `X-Forwarded-For` forjado é ignorado;
* `CorsPropertiesTest`, para a leitura e a validação de `CORS_ALLOWED_ORIGINS`, inclusive a falha na inicialização com uma origem inválida, e `CorsOrigensPadraoHttpTest` e `CorsOrigensConfiguradasHttpTest`, que verificam por `MockMvc` o preflight de origens permitidas e recusadas, com as origens padrão e com origens configuradas;
* `ObservabilidadeHttpTest`, que sobe um Tomcat real com as duas portas: o `/saude` público só com o status, o Actuator ausente da porta da API, e o health completo, as probes e as métricas do Prometheus na porta de gerenciamento; `ConfiguracaoDeProducaoTest`, que lê o `application.properties` e trava os padrões de produção (porta `8081` no loopback, só `health`, `metrics` e `prometheus` expostos, `/saude` sem detalhes, `show-sql` desligado); `IdentificadorRequisicaoHttpTest`, para o ID gerado, o aproveitado do cliente, o inseguro trocado e o `requestId` nos erros `400` e `401`; `ErrosInternosHttpTest`, que verifica que um `500` conta na métrica com o tipo da exceção e vai para o log com o ID da requisição; e `LogsEmJsonTest`, que usa o encoder do Spring Boot para garantir que o ID vira o campo `requestId` do JSON. As métricas da força bruta e das cotações são verificadas no `ProtecaoForcaBrutaHttpTest` e no `CotacaoServiceTest`;
* `CadastroUsuarioHttpTest`, `PadronizacaoErrosHttpTest` e `OpenApiDocumentationTest` para o cadastro, o login, o formato dos erros e a documentação.

Essa suíte rápida, que não precisa de banco externo nem de Docker, pode ser executada com:

```powershell
.\mvnw.cmd clean test
```

### Testes de integração com MySQL

Os testes com sufixo `IT`, no pacote `br.com.criptovision.integracao`, rodam contra um MySQL 8 real, criado pelo Testcontainers com a mesma imagem do `docker-compose.yml` (`mysql:8.0`). Eles usam o perfil `integracao` (`application-integracao.properties`):

* o schema é criado pelas migrations do Flyway, como em produção;
* o Hibernate roda com `ddl-auto=validate`, então uma migration que divirja das entidades faz o teste falhar;
* a conexão vem do container por `@ServiceConnection` (`MysqlContainerConfiguration`), e um único container é compartilhado por todos os testes da execução.

Grupos atuais:

* `BackupMysqlIT`, que constrói a imagem de `backup/Dockerfile` e verifica, contra um MySQL que exige TLS e um bucket S3 de teste (`rclone serve s3`): que o backup chega cifrado ao servidor e ao bucket, sem nenhum texto do banco legível; o teste de restauração num MySQL temporário; a restauração com os valores de 18 casas exatos; a exigência de `--confirmar`; a recusa de uma senha errada; a restauração a partir do bucket quando o servidor perdeu os arquivos; e a recusa de rodar sem `BACKUP_SENHA`;
* `ConexaoSeguraMysqlIT`, que verifica que o pool da aplicação conecta com TLS num MySQL que exige transporte seguro e que uma conexão sem TLS é recusada. O container dos testes roda com `--require-secure-transport=ON`, e a aplicação conecta com `sslMode=REQUIRED` e sem `allowPublicKeyRetrieval`, como no `docker-compose.yml`;
* `MigracoesMysqlIT`, que verifica que todas as migrations foram aplicadas, que `quantidade` e `preco_unitario` são `DECIMAL(38, 18)` (o `validate` do Hibernate não confere precisão nem escala) e que valores no limite da validação são gravados e lidos sem perda;
* `ConcorrenciaTransacoesMysqlIT`, que executa o mesmo cenário do `ConcorrenciaTransacoesTest` (os dois herdam de `CenarioConcorrenciaTransacoes`) sob o REPEATABLE READ do InnoDB. Só ele detecta uma leitura comum feita antes do `SELECT ... FOR UPDATE`, porque o H2 não reproduz o snapshot de leitura do MySQL.

Esses testes exigem Docker e rodam na fase `verify` do Maven, pelo plugin failsafe:

```powershell
.\mvnw.cmd clean verify            # testes unitários e de integração (precisa de Docker)
.\mvnw.cmd clean verify -DskipITs  # somente os testes unitários
```

O GitHub Actions executa `clean verify` com JDK 25 em cada push e pull request para a branch `main`. O runner já tem Docker disponível.

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
3. inconsistências do histórico ainda não disparam alerta. Já vão para o log em JSON, com o ID da requisição, e para a métrica `criptovision.erros.internos`, mas o alerta depende de uma ferramenta de monitoramento, ainda não escolhida;
4. ~~as simulações de DCA e de venda e o endpoint `/carteira/total` ainda utilizam `double`~~ (resolvida: todo o cálculo financeiro usa `BigDecimal`);
5. parte da injeção de dependências ainda é realizada por atributos (`@Autowired` em `CarteiraController`, `TransacaoController` e `AnaliseController`);
6. não há paginação ou filtros no histórico;
7. não há lucro realizado persistido ou consolidado;
8. não há suporte a taxas;
9. `spring.jpa.open-in-view` ainda utiliza a configuração padrão;
10. as colunas de dados da tabela `transacoes` ainda aceitam `NULL` no banco;
11. ~~os testes usam schema gerado pelo Hibernate, então as migrations do Flyway não são exercitadas na suíte~~ (resolvida: os testes `*IT.java` aplicam as migrations num MySQL real; o `mvn test` continua usando o schema do Hibernate no H2);
12. o cache de cotações é local à instância;
13. ~~`/carteira/total` retorna texto simples com o rótulo "R$", embora os valores não sejam convertidos para reais~~ (resolvida: devolve JSON com o custo das posições, o total aportado, o total recebido em vendas e a moeda `USDT`).

Esses itens serão tratados em commits pequenos e independentes.
