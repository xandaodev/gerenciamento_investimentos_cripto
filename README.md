# 🚀 CriptoVision API - Gerenciador de Investimentos

<p align="left"> 
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"> 
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot"> 
  <img src="https://img.shields.io/badge/SQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="SQL"> 
  <img src="https://img.shields.io/badge/API_Binance-F3BA2F?style=for-the-badge&logo=binance&logoColor=white" alt="Binance API">
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger">
  <img src="https://img.shields.io/badge/Status-Em%20Evolu%C3%A7%C3%A3o-green?style=for-the-badge" alt="Status"> 
</p>

O **CriptoVision API** é uma solução Back-End robusta desenvolvida em **Java + Spring Boot** para investidores que desejam centralizar e automatizar a gestão de seus ativos digitais. O sistema rastreia rentabilidade, calcula métricas financeiras com precisão absoluta e fornece dados em tempo real através da integração direta com a API da Binance.

## 📖 A História do Projeto: Por que eu criei?

Como um entusiasta do mercado financeiro e investidor em criptoativos, eu precisava de uma ferramenta robusta para monitorar meu Preço Médio (PM) e meu Lucro/Prejuízo (PNL) sem depender de planilhas manuais sujeitas a erros.

Para calcular o Preço Médio das minhas criptomoedas, projetar simulações de lucros e saber meu PNL em cada uma delas, era todo um trabalho de anotação, cálculos feitos com ajuda de IAs, uma grande dor de cabeça para ter resultados sem nenhuma precisão.

Nasceu então a necessidade de criar meu próprio ecossistema. O projeto me ajudou não apenas a ter controle absoluto sobre minha carteira, mas tornou-se meu principal laboratório prático para aplicar conceitos de Ciência da Computação, estruturação de dados e padrões de Engenharia de Software no mundo real.

## 🌱 Evolução e Desafios Superados (A Jornada para a API REST)

O desenvolvimento deste sistema foi marcado por ciclos de melhoria contínua e refatorações profundas:

* **V1 (A Origem - Arquivos CSV):** Inicialmente, construí o sistema utilizando persistência local em arquivos `.csv`. Foi um excelente desafio para entender fluxos de I/O em Java, mas logo esbarrei em problemas de concorrência e manipulação estruturada.
* **V2 (O Salto SQL - JDBC Manual):** Para escalar o projeto, refatorei a camada de persistência para uma arquitetura conectada a um banco MySQL. Criei DAOs do zero para lidar com as queries e garantir a integridade das transações.
* **V3 (API REST e Padrões do Mercado - Atual):** A grande revolução do projeto. Migrei a aplicação para o ecossistema **Spring Boot**, transformando-a em uma API RESTful completa.
    * 🧮 **Precisão Financeira:** Substituição de tipos primitivos por `BigDecimal`, blindando o motor de cálculos contra imprecisões de arredondamento.
    * 🛡️ **Resiliência:** Implementação de um `@ControllerAdvice` (Global Exception Handler) para interceptar regras de negócio violadas (ex: Saldo Insuficiente) e retornar JSONs padronizados (HTTP 400).
    * 🔒 **Segurança e ORM:** Adoção do Spring Data JPA (Hibernate) para mapeamento objeto-relacional e Spring Security com JWT para proteção dos endpoints.
    * 👥 **Multiusuário:** Cadastro de contas e isolamento completo do histórico, dos cálculos e das análises de cada usuário.
    * 🗃️ **Migrations:** Schema do banco versionado com Flyway.
    * 📡 **Cotações resilientes:** Cache, consulta em lote e fallback para que uma instabilidade da Binance não derrube a carteira.

## 🚀 Funcionalidades Principais

* **👤 Contas e Autenticação:** Cadastro de usuários com senha criptografada (BCrypt) e login que devolve um token JWT válido por 2 horas.
* **🧾 Histórico de Transações:** Cadastro, consulta, edição e exclusão de compras e vendas. Edições e exclusões que deixariam o histórico inconsistente (ex: uma venda sem saldo) são bloqueadas.
* **📈 Cotações da Binance:** Preço e variação de 24 horas consultados em lote, com timeout, cache de 60 segundos e uso da última cotação conhecida (até 15 minutos) quando a Binance está fora do ar.
* **📊 Resumo da Carteira:** Saldo, preço médio, valor atual, PNL e variação de 24 horas por ativo e da carteira inteira, em USD (pares com USDT). Ativos sem cotação continuam aparecendo, e a resposta indica quando os dados são parciais.
* **💰 Simuladores:** Impacto de um novo aporte (DCA) no preço médio e no *break-even*, e resultado de vender a posição a um preço-alvo.
* **📖 Documentação Interativa:** Interface gerada com **Swagger/OpenAPI** para testar as rotas da API.
* **🔄 Normalização de Tickers:** Nomes comuns (ex: "Bitcoin") são convertidos para o ticker oficial ("BTC").
* **⚠️ Erros Padronizados:** Todas as respostas de erro seguem o formato Problem Details (RFC 7807).

## 🛠️ Tecnologias e Stack

* **Linguagem:** Java 25
* **Framework Principal:** Spring Boot 4 (Web MVC, `RestClient`, Validation)
* **Persistência:** Spring Data JPA / Hibernate
* **Banco de Dados:** MySQL 8, com migrations do Flyway
* **Segurança:** Spring Security com JWT (stateless)
* **Cotações:** Binance Public API (`data-api.binance.vision`)
* **Documentação:** Springdoc OpenAPI (Swagger)
* **Infraestrutura:** Docker e Docker Compose
* **Qualidade:** Testes de unidade, de persistência (H2) e de integração HTTP (MockMvc), executados no GitHub Actions

## 🔌 Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/auth/register` | Cria uma conta |
| `POST` | `/auth/login` | Retorna o token JWT |
| `GET` | `/transacoes` | Lista as transações do usuário |
| `POST` | `/transacoes` | Registra uma compra ou venda |
| `GET` | `/transacoes/{id}` | Busca uma transação |
| `PUT` | `/transacoes/{id}` | Altera uma transação |
| `DELETE` | `/transacoes/{id}` | Exclui uma transação |
| `GET` | `/carteira/resumo` | Resumo da carteira com cotações atuais |
| `GET` | `/carteira/total` | Total líquido de compras e vendas |
| `GET` | `/carteira/simulador/dca` | Simulação de aporte |
| `GET` | `/carteira/simulador/venda` | Simulação de venda a um preço-alvo |
| `GET` | `/analise/aportes-por-moeda` | Total comprado por ativo |

Com exceção de `/auth/*` e do Swagger, todas as rotas exigem o cabeçalho `Authorization: Bearer <token>`.

Mais detalhes em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) e [`docs/BUSINESS_RULES.md`](docs/BUSINESS_RULES.md).

## 📋 Como Executar e Testar

### Variáveis de ambiente

A aplicação lê as credenciais de variáveis de ambiente. O arquivo [`.env.example`](.env.example) serve de modelo:

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `DB_USER` / `DB_PASSWORD` | Sim | Credenciais do MySQL |
| `JWT_SECRET` | Sim | Segredo para assinar os tokens (use um valor longo e aleatório) |
| `MYSQL_ROOT_PASSWORD` | Só no Docker | Senha do root do container MySQL |
| `BINANCE_API_BASE_URL` | Não | URL da API de mercado da Binance |
| `COTACOES_CONNECT_TIMEOUT`, `COTACOES_READ_TIMEOUT`, `COTACOES_CACHE_TTL`, `COTACOES_CACHE_MAX_STALE` | Não | Timeouts e cache das cotações |

### Com Docker

1.  Copie `.env.example` para `.env` e preencha os valores, incluindo `MYSQL_ROOT_PASSWORD`. Use em `DB_USER` um usuário diferente de `root`, pois a imagem do MySQL não aceita `root` como usuário adicional.
2.  Gere o JAR (o `Dockerfile` copia o arquivo de `target/`) e suba os containers:
    ```bash
    ./mvnw clean package
    docker compose up --build
    ```
3.  A API fica em `http://localhost:8080` e o MySQL em `localhost:3307`. O Flyway cria as tabelas na primeira execução.

### Localmente

1.  **Pré-requisitos:** JDK 25 e um MySQL rodando em `localhost:3306` com um banco chamado `criptovision`.
2.  Exporte `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` no terminal (o Spring não lê o arquivo `.env` automaticamente).
3.  Na raiz do projeto, rode:
    ```bash
    ./mvnw spring-boot:run
    ```
    No Windows, use `.\mvnw.cmd spring-boot:run`.

### Acessando a API

Abra a documentação do Swagger para testar os endpoints:
👉 `http://localhost:8080/swagger-ui.html`

Crie uma conta em `/auth/register`, faça login em `/auth/login` e clique em **Authorize** para informar o token.

### Testes

```bash
./mvnw clean test
```

Os testes usam um banco H2 em memória e não acessam a Binance, então não precisam de MySQL rodando.

---
⭐ *Desenvolvido por **Alexandre Vital** - Estudante de Ciência da Computação na UFSJ, em busca de desafios como Desenvolvedor Back-End.*