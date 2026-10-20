# Roadmap para produção

Este roadmap organiza em fases o que falta para publicar o CriptoVision para usuários reais. Ele nasceu de uma auditoria de segurança, autenticação, testes, tratamento de erros, performance e infraestrutura, e incorpora as dívidas técnicas de `docs/ARCHITECTURE.md` (§15).

A numeração dos itens (#1 a #28) é a da auditoria original. Ela é mantida para facilitar referências em issues, branches e commits. Marque o checkbox quando o item estiver concluído e documentado.

Prioridade das fases:

| Fase | Objetivo | Quando |
|------|----------|--------|
| 1 | Confiabilidade financeira | Antes de qualquer outra coisa |
| 2 | Pacote de deploy | Bloqueia o lançamento |
| 3 | Antes de abrir para usuários | Antes de convidar usuários reais |
| 4 | Qualidade e escala | Logo após o lançamento |
| 5 | Evolução do produto | Contínuo |

---

## Fase 1 — Confiabilidade financeira

- [ ] **#1 — O preço médio perde precisão e o erro se acumula**
  - **Problema:** o preço médio é arredondado para 8 casas e esse valor arredondado volta para o cálculo da compra seguinte (`saldo × precoMedio`). Em moedas de preço baixo isso distorce o resultado: PEPE a `0.0000012345` vira `0.00000123`, um erro de cerca de 0,4% no custo, que cresce a cada compra. O banco guarda 18 casas, mas o cálculo usa 8.
  - **Arquivo:** `src/main/java/br/com/criptovision/service/CarteiraService.java` (`processarTransacao`), `src/main/java/br/com/criptovision/model/Moeda.java`
  - **Correção:** acumular o custo total em `BigDecimal` sem arredondar e calcular o preço médio só na hora de exibir. Cobrir com testes de moedas de preço muito baixo e de várias compras seguidas.

- [ ] **#6 — As migrations não são testadas, e a garantia de concorrência também não é testada no banco real**
  - **Problema:** os testes rodam em H2 com `create-drop` e Flyway desligado. Por isso:
    - divergências entre as migrations e as entidades só aparecem no boot em produção (o `ddl-auto=validate` derruba a aplicação);
    - o `ConcorrenciaTransacoesTest` prova o lock no H2, mas a premissa do lock (`SELECT ... FOR UPDATE` sob REPEATABLE READ do MySQL) nunca é exercitada;
    - a futura migration `NOT NULL` (#18) vai rodar sobre dados reais sem nenhum teste antes.
  - **Arquivo:** `src/test/resources/application-test.properties`, `src/test/java/br/com/criptovision/test/ConcorrenciaTransacoesTest.java`, `pom.xml`, `.github/workflows/backend-tests.yml`
  - **Correção:** adicionar Testcontainers com MySQL 8, Flyway ligado e `ddl-auto=validate`, ao menos num perfil de integração rodando no CI. Executar o teste de concorrência também contra o MySQL.

- [ ] **#8 — `AutenticacaoService` retorna `null` para login inexistente**
  - **Problema:** `loadUserByUsername` retorna `null` em vez de lançar `UsernameNotFoundException`. Isso quebra o contrato do Spring e pula a mitigação de timing do `DaoAuthenticationProvider`: um login inexistente responde visivelmente mais rápido que um existente, o que permite enumerar usuários.
  - **Arquivo:** `src/main/java/br/com/criptovision/security/AutenticacaoService.java`
  - **Correção:** lançar `UsernameNotFoundException` quando o usuário não existir e ajustar `UsuarioRepository.findByLogin` para devolver `Optional`. Adicionar um teste garantindo que a resposta continua sendo 401 genérico.

---

## Fase 2 — Pacote de deploy

- [ ] **#2 — Não há HTTPS**
  - **Problema:** o Compose expõe a porta 8080 em HTTP puro, então o JWT e a senha trafegam em texto claro.
  - **Arquivo:** `docker-compose.yml`, `src/main/resources/application.properties`
  - **Correção:** colocar um proxy reverso com TLS (Caddy/Nginx ou o do provedor) na frente da API e configurar `server.forward-headers-strategy`. A API não deve ficar acessível diretamente pela internet.

- [ ] **#3 — O MySQL fica exposto e sem TLS**
  - **Problema:** a porta `3307` é publicada em todas as interfaces, e a URL do datasource usa `useSSL=false&allowPublicKeyRetrieval=true`.
  - **Arquivo:** `docker-compose.yml`
  - **Correção:** não publicar a porta do MySQL em produção (ou prendê-la a `127.0.0.1`), habilitar TLS na conexão e remover `allowPublicKeyRetrieval`.

- [ ] **#4 — Não há proteção contra força bruta**
  - **Problema:** `/auth/login` e `/auth/register` não têm rate limit nem bloqueio de conta.
  - **Arquivo:** `src/main/java/br/com/criptovision/security/SecurityConfigurations.java`, `src/main/java/br/com/criptovision/controller/AutenticacaoController.java`
  - **Correção:** limitar tentativas por IP e por login (Bucket4j ou no proxy reverso), responder 429 com `ProblemDetail` e registrar as tentativas bloqueadas.

- [ ] **#5 — Não há estratégia de backup**
  - **Problema:** o histórico de transações é a única fonte da verdade, e o volume `mysql_data` não tem backup nem teste de restauração.
  - **Arquivo:** `docker-compose.yml` (volume `mysql_data`)
  - **Correção:** configurar backups automáticos (dump agendado ou backup gerenciado do provedor), guardá-los fora do servidor, definir retenção e testar a restauração periodicamente.

- [ ] **#7 — O CORS está fixo em `localhost`**
  - **Problema:** as origens permitidas são só `localhost:5173` e `localhost:5174`, então o frontend de produção não funciona.
  - **Arquivo:** `src/main/java/br/com/criptovision/security/SecurityConfigurations.java` (`corsConfigurationSource`)
  - **Correção:** ler as origens permitidas de uma propriedade alimentada por variável de ambiente (por exemplo `CORS_ALLOWED_ORIGINS`) e documentá-la no `.env.example`.

---

## Fase 3 — Antes de abrir para usuários

- [ ] **#9 — O JWT não tem revogação**
  - **Problema:** não há logout, refresh nem troca de senha. O tamanho do `JWT_SECRET` não é validado na subida (o HMAC256 aceita segredos curtos) e o token não tem `audience`.
  - **Arquivo:** `src/main/java/br/com/criptovision/security/TokenService.java`, `src/main/java/br/com/criptovision/security/SecurityFilter.java`, `src/main/java/br/com/criptovision/model/Usuario.java`
  - **Correção:** validar o tamanho mínimo do segredo na inicialização, adicionar `audience` e um mecanismo de invalidação (por exemplo, um `tokenVersion` no usuário verificado no filtro). Avaliar refresh token e criar troca de senha.

- [ ] **#10 — Requisitos mínimos da LGPD**
  - **Problema:** não existe exclusão de conta, a FK `fk_transacoes_usuario` não tem `ON DELETE`, e não há política de privacidade nem exportação de dados.
  - **Arquivo:** `src/main/resources/db/migration/V1__cria_schema_inicial.sql` (FK), `src/main/java/br/com/criptovision/controller/AutenticacaoController.java`, `src/main/java/br/com/criptovision/service/UsuarioService.java`
  - **Correção:** criar endpoints de exclusão de conta e de exportação dos dados do usuário, ajustar a FK em nova migration (ou excluir as transações explicitamente no serviço) e publicar uma política de privacidade.

- [ ] **#11 — A data da transação é sempre o momento do cadastro**
  - **Problema:** `Transacao.data` usa `@CreationTimestamp`, então o usuário não consegue lançar compras antigas com a data real. Se lançar o histórico fora de ordem, uma venda legítima pode ser rejeitada por saldo insuficiente.
  - **Arquivo:** `src/main/java/br/com/criptovision/model/Transacao.java`, `src/main/java/br/com/criptovision/dto/TransacaoRequestDTO.java`, `src/main/java/br/com/criptovision/service/CarteiraService.java`
  - **Correção:** aceitar a data da operação no DTO (validada, sem datas futuras) e deixar a reconstrução e as validações de alteração usarem essa data. Atualizar `docs/BUSINESS_RULES.md`.

- [ ] **#12 — Os simuladores aceitam qualquer entrada**
  - **Problema:** os parâmetros `double` aceitam `NaN`, `Infinity`, zero e valores negativos, sem `@Positive`. Uma moeda sem saldo faz `pmAtual = 0` ou `custoBase = 0` e o resultado sai como divisão por zero, devolvendo `NaN`/`Infinity` no JSON. Só o caminho feliz do DCA tem teste; a simulação de venda não tem nenhum.
  - **Arquivo:** `src/main/java/br/com/criptovision/controller/CarteiraController.java` (`simularAporteDCA`, `simularVendaFutura`), `src/main/java/br/com/criptovision/service/CarteiraService.java` (`simularDCA`, `simularVendaFutura`)
  - **Correção:** migrar para `BigDecimal` (dívida #4 do §15), validar os parâmetros com `@Positive` e `@Validated`, tratar explicitamente o caso sem saldo e cobrir os casos de borda com testes.

- [ ] **#13 — `/carteira/total` está semanticamente errado**
  - **Problema:** o cálculo é compras menos vendas ao preço de venda, então depois de uma venda com lucro o "patrimônio investido" fica menor ou até negativo. Além disso usa `double`, mostra o rótulo "R$" embora os valores estejam em USDT (dívidas #4 e #13 do §15) e responde texto puro em vez de JSON.
  - **Arquivo:** `src/main/java/br/com/criptovision/controller/CarteiraController.java` (`obterTotal`), `src/main/java/br/com/criptovision/service/CarteiraService.java` (`calcularPatrimonioTotal`)
  - **Correção:** definir a regra correta (por exemplo, custo da posição atual a partir da carteira reconstruída), calcular em `BigDecimal`, devolver um DTO JSON com a moeda explícita e documentar a regra.

- [ ] **#14 — Não há observabilidade**
  - **Problema:** faltam Actuator (health/readiness), métricas, logs estruturados e alertas para `HistoricoInconsistenteException` (dívida #3 do §15). O `spring.jpa.show-sql=true` está ligado em produção.
  - **Arquivo:** `pom.xml`, `src/main/resources/application.properties`, `src/main/java/br/com/criptovision/exception/GlobalExceptionHandler.java`
  - **Correção:** adicionar o Actuator expondo só `health` publicamente, métricas (Micrometer), logs em JSON com ID de correlação, alerta para erros 500 e desligar o `show-sql` fora do ambiente local.

- [ ] **#15 — A imagem Docker não está pronta para produção**
  - **Problema:** a imagem usa o JDK completo em vez do JRE, roda como root, não tem build multi-stage nem `HEALTHCHECK`. O `depends_on` não espera o MySQL ficar saudável e não há limites de memória da JVM.
  - **Arquivo:** `Dockerfile`, `docker-compose.yml`
  - **Correção:** build multi-stage com imagem JRE, usuário sem privilégios, `HEALTHCHECK` usando o Actuator, `depends_on` com `condition: service_healthy` e `-XX:MaxRAMPercentage` configurado.

- [ ] **#16 — A integração com a Binance tem riscos de disponibilidade e latência**
  - **Problema:**
    - a `data-api.binance.vision` pode responder 451 em algumas regiões de nuvem (EUA);
    - no fallback individual, N tickers viram N chamadas sequenciais com timeout de 5s cada, então o resumo pode levar N×5s;
    - não há circuit breaker nem proteção contra rajada de requisições quando o cache expira.
  - **Arquivo:** `src/main/java/br/com/criptovision/cotacao/service/CotacaoService.java` (`buscarIndividualmente`), `src/main/java/br/com/criptovision/cotacao/client/BinanceCotacaoClient.java`, `src/main/java/br/com/criptovision/cotacao/cache/CotacaoCache.java`
  - **Correção:** validar a região do deploy contra a Binance, limitar o tempo total do fallback individual (ou paralelizá-lo com limite), adicionar circuit breaker (Resilience4j) e evitar consultas duplicadas ao mesmo ticker quando o cache expira.

---

## Fase 4 — Qualidade e escala

- [ ] **#17 — Performance da reconstrução e das consultas**
  - **Problema:** a carteira inteira é reconstruída a cada requisição (dívida #2 do §15), falta um índice composto para a ordenação do histórico, não há paginação (dívida #6), o `SecurityFilter` consulta o banco a cada requisição e o `open-in-view` está no padrão (dívida #9). Não é problema hoje, mas cresce de forma linear com o histórico.
  - **Arquivo:** `src/main/java/br/com/criptovision/service/CarteiraService.java`, `src/main/java/br/com/criptovision/repository/TransacaoRepository.java`, `src/main/java/br/com/criptovision/security/SecurityFilter.java`, `src/main/resources/application.properties`
  - **Correção:** criar o índice `(usuario_id, data, id)` em nova migration, paginar a listagem de transações, desligar `spring.jpa.open-in-view` e medir antes de otimizar a reconstrução ou a carga do usuário no filtro.

- [ ] **#18 — Colunas `NULL` em `transacoes`**
  - **Problema:** as colunas de dados da tabela `transacoes` ainda aceitam `NULL` no banco (dívida #10 do §15).
  - **Arquivo:** `src/main/resources/db/migration/V1__cria_schema_inicial.sql`, `src/main/resources/db/migration/V3__aumenta_precisao_valores_cripto.sql`, `src/main/java/br/com/criptovision/model/Transacao.java`
  - **Correção:** depois do #6, criar uma migration `V4` com `NOT NULL` (verificando antes se há dados nulos) e alinhar a entidade com `nullable = false`.

- [ ] **#19 — O handler devolve a mensagem de qualquer `IllegalArgumentException`**
  - **Problema:** erros internos de bibliotecas podem vazar texto técnico para o cliente.
  - **Arquivo:** `src/main/java/br/com/criptovision/exception/GlobalExceptionHandler.java` (`tratarArgumentoInvalido`)
  - **Correção:** criar exceções de negócio próprias para os casos esperados e responder com mensagem genérica (e log) para as demais `IllegalArgumentException`.

- [ ] **#20 — O login não é normalizado para minúsculas na autenticação**
  - **Problema:** o cadastro normaliza o login, mas `DadosAutenticacao` não. Hoje funciona por causa da collation `_ci` do MySQL, não pelo código.
  - **Arquivo:** `src/main/java/br/com/criptovision/dto/DadosAutenticacao.java`
  - **Correção:** aplicar a mesma normalização (`trim` + `toLowerCase(Locale.ROOT)`) no construtor compacto e adicionar um teste.

- [ ] **#21 — Existe um `processarTransacao` público que salva sem lock**
  - **Problema:** a sobrecarga `processarTransacao(Moeda, Transacao)` persiste a transação sem passar por `executarComHistoricoBloqueado`. É uma armadilha para uso futuro.
  - **Arquivo:** `src/main/java/br/com/criptovision/service/CarteiraService.java`
  - **Correção:** remover a sobrecarga e separar o cálculo (puro) da persistência.

- [ ] **#22 — `AnaliseController` acessa o repository diretamente**
  - **Problema:** o controller usa o `TransacaoRepository` sem passar por um serviço, e há injeção por campo com `@Autowired` (dívidas #1 e #5 do §15). Além disso, "aportes por moeda" ignora as vendas e não tem teste HTTP.
  - **Arquivo:** `src/main/java/br/com/criptovision/controller/AnaliseController.java`, `CarteiraController.java`, `TransacaoController.java`, `src/main/java/br/com/criptovision/security/SecurityFilter.java`
  - **Correção:** mover a consulta para um serviço, trocar a injeção por construtor, documentar se o indicador deve considerar vendas e cobrir o endpoint com teste HTTP.

- [ ] **#23 — O CI só roda os testes**
  - **Problema:** faltam análise de dependências, cobertura e build da imagem.
  - **Arquivo:** `.github/workflows/backend-tests.yml`
  - **Correção:** habilitar Dependabot (ou OWASP Dependency-Check), publicar cobertura (JaCoCo) e validar o build da imagem Docker no CI.

- [ ] **#24 — O Swagger é público em produção**
  - **Problema:** `/swagger-ui.html` e `/v3/api-docs/**` ficam abertos para qualquer pessoa.
  - **Arquivo:** `src/main/java/br/com/criptovision/security/SecurityConfigurations.java`, `src/main/resources/application.properties`
  - **Correção:** decidir se a documentação fica pública. Se não ficar, desligá-la por perfil (`springdoc.api-docs.enabled=false`) ou exigir autenticação.

- [ ] **#25 — As datas usam `LocalDateTime` com o fuso padrão da JVM**
  - **Problema:** o container roda em UTC, mas uma máquina local não, então a mesma operação pode ganhar horários diferentes.
  - **Arquivo:** `src/main/java/br/com/criptovision/model/Transacao.java`
  - **Correção:** usar `Instant` (ou `OffsetDateTime` em UTC) de ponta a ponta, com migration se o tipo da coluna mudar.

---

## Fase 5 — Evolução do produto

- [ ] **#26 — Taxas e lucro realizado**
  - **Problema:** não há suporte a taxas nem lucro realizado consolidado (dívidas #7 e #8 do §15). O lucro da venda é calculado em `processarTransacao` e descartado.
  - **Arquivo:** `src/main/java/br/com/criptovision/service/CarteiraService.java`, `src/main/java/br/com/criptovision/model/Transacao.java`
  - **Correção:** adicionar taxa à transação (com migration), incorporá-la ao custo, e expor o lucro realizado por ativo no resumo. Documentar as fórmulas em `docs/BUSINESS_RULES.md`.

- [ ] **#27 — Política de senha mais forte**
  - **Problema:** o cadastro só exige mínimo de 8 caracteres e não verifica senhas vazadas.
  - **Arquivo:** `src/main/java/br/com/criptovision/dto/DadosCadastroUsuario.java`
  - **Correção:** aumentar o mínimo, bloquear senhas comuns ou vazadas (por exemplo, com a API k-anonymity do Have I Been Pwned) e manter o limite de 72 bytes do BCrypt.

- [ ] **#28 — Cache de cotações distribuído**
  - **Problema:** o cache de cotações é local à instância (dívida #12 do §15). Só importa quando houver mais de uma instância.
  - **Arquivo:** `src/main/java/br/com/criptovision/cotacao/cache/CotacaoCache.java`
  - **Correção:** quando escalar horizontalmente, migrar para um cache compartilhado (por exemplo, Redis) mantendo as regras de TTL e fallback.
