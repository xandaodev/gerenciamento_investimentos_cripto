# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Responda sempre em português.

## Project

CriptoVision API: a Spring Boot REST API that tracks a user's crypto buy/sell transactions and works out average price, unrealized PNL, the portfolio summary and DCA/sell simulations, using Binance market data. The codebase, identifiers, commit messages and docs are in **Portuguese**, so keep new code and messages in Portuguese. Commits follow Conventional Commits (`feat:`, `fix:`, `test:`).

Stack: Java 25, Spring Boot 4 (`spring-boot-starter-webmvc`, `-restclient`), Spring Data JPA, Flyway, Spring Security with stateless JWT (`com.auth0:java-jwt`), MySQL, and springdoc OpenAPI.

`docs/ARCHITECTURE.md` and `docs/BUSINESS_RULES.md` hold the detailed reference for the financial formulas, endpoint flows and known technical debt (ARCHITECTURE §15). If they disagree with the code, trust the code. Update them, and the README, whenever you change behavior or rules (principle #10 in BUSINESS_RULES).

## Visão do produto

O CriptoVision é o principal projeto pessoal do autor. O objetivo é transformá-lo num produto sólido e publicá-lo para usuários reais. Por isso, segurança, confiabilidade dos cálculos financeiros e qualidade do código têm prioridade sobre velocidade de entrega.

## Commands

Use the Maven wrapper (`.\mvnw.cmd` on Windows, `./mvnw` elsewhere):

```bash
./mvnw clean test                                   # full suite (what CI runs, on JDK 25)
./mvnw test -Dtest=CarteiraServiceTest              # single test class
./mvnw test -Dtest=CarteiraServiceTest#nomeDoMetodo # single test method
./mvnw spring-boot:run                              # run locally (needs MySQL + env vars)
./mvnw clean package && docker compose up --build   # Dockerfile copies target/*.jar, so package first
```

Runtime env vars (see `.env.example`): `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (required), plus `MYSQL_ROOT_PASSWORD` for Compose, `BINANCE_API_BASE_URL` and `COTACOES_*` timeouts/TTL. Locally the datasource points to `localhost:3306/criptovision`. Docker Compose exposes MySQL on host port `3307`. Swagger UI is served at `/swagger-ui.html`.

## Architecture

Base package `br.com.criptovision`: controller → service → repository, plus `security`, `exception`, `dto`, `config` and the `cotacao` subsystem.

**Transaction history is the single source of truth.** Balances and average prices are never persisted. `Carteira` (a `Map<String, Moeda>`) and `Moeda` are in-memory objects. `CarteiraService.reconstruirCarteira` rebuilds them on every request by replaying the user's transactions, sorted by `data` and then `id` (it sorts a copy defensively). Consequences:
- `atualizarTransacao` and `excluirTransacao` first simulate the change on a *copy* of the history. They touch the managed entity only if the replay stays valid. Otherwise they throw `AlteracaoHistoricoInvalidaException` → 409.
- **Concurrency:** create, update and delete run their DB phase through `executarComHistoricoBloqueado`, a `TransactionTemplate` whose first query is `UsuarioRepository.bloquearPorId`, a `SELECT ... FOR UPDATE` on the user's row. This serializes writes per user, so two simultaneous sells can't both pass the balance check. The lock must stay the first query of the transaction, because under MySQL REPEATABLE READ the read snapshot is taken at the first plain read. Binance ticker validation runs *before* the transaction so the lock is never held during an HTTP call. `ConcorrenciaTransacoesTest` covers this with real threads on H2.
- A sale larger than the balance throws `SaldoInsuficienteException`. A replay failure on persisted data throws `HistoricoInconsistenteException` → 500. Invalid transactions must never be skipped silently.
- Create, update and delete must go through `CarteiraService`, never straight to the repository from controllers.

**Per-user isolation.** Controllers get the `Usuario` entity via `@AuthenticationPrincipal` (set by `SecurityFilter` from the JWT) and pass it down. Every repository query filters by user. Accessing another user's transaction returns **404, not 403**, so the API never reveals that the ID exists. `Transacao.usuario` is `@JsonIgnore`d. The request DTO has no user/id/date fields. Public routes: `/auth/login`, `/auth/register` and the Swagger paths. CORS allows `localhost:5173/5174` (the Vite frontend).

**Quotes (`cotacao` package).** `CotacaoService` → `CotacaoClient` (interface; `BinanceCotacaoClient` implementation using Spring `RestClient` against `binance.api.base-url`, default `data-api.binance.vision`) + `CotacaoCache` (in-memory, with a fresh TTL `cotacoes.cache.ttl` and a stale-fallback window `cotacoes.cache.max-stale`). Behavior:
- It batch-fetches the pending tickers, falls back to per-ticker requests on `TickerInvalidoException`, and falls back to stale cached quotes on `ServicoCotacaoIndisponivelException`.
- `ResultadoCotacoes` carries the found quotes plus the `indisponiveis` tickers. The summary degrades per asset (`ResumoAtivoDTO.semCotacao`) instead of failing.
- `USDT` is hard-coded to price 1 with 0% change. `TickerNormalizer` maps names like BITCOIN→BTC.
- Never turn provider failures into a zero price. Unknown ticker → 400; provider down with no fallback → 503.
- A `Clock` bean (`CotacaoConfiguration`) is injected for time-dependent logic, so tests can control time.

**Money.** Use `BigDecimal` (average price uses scale 8, `HALF_UP`). Migration `V3` widened the DB precision. Some simulation and total methods still use `double`, which is known debt, so don't add new `double` math.

**Errors.** `GlobalExceptionHandler` returns RFC 7807 `ProblemDetail` responses (with field errors for validation). `AutenticacaoEntryPoint` handles 401s. New business exceptions should get a handler there.

**Database.** Flyway migrations live in `src/main/resources/db/migration` (`V<n>__descricao.sql`), with `ddl-auto=validate`. Schema changes need a new migration, and the entity must match it.

## Tests

- Tests use the `test` profile (`src/test/resources/application-test.properties`): H2 in MySQL mode with `ddl-auto=create-drop`, **Flyway disabled** (so migrations are not exercised in tests), a fixed JWT secret, and the Binance URL pointed at `http://localhost`.
- Test styles in use:
  - pure Mockito unit tests (`@ExtendWith(MockitoExtension.class)`)
  - `@DataJpaTest` for repositories
  - `@SpringBootTest` + MockMvc HTTP tests that authenticate real users with JWT (e.g. `IsolamentoTransacoesHttpTest`, which uses `@MockitoBean` for quotes)
  - `MockRestServiceServer` for `BinanceCotacaoClient`
- Never hit the real Binance API in tests: mock `CotacaoClient`/`CotacaoService`.
- Most service and controller tests live in the `br.com.criptovision.test` package. Newer ones mirror the main package layout (e.g. `cotacao/`).
