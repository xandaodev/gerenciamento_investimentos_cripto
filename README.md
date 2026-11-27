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

* **👤 Contas e Autenticação:** Cadastro de usuários com senha criptografada (BCrypt) e login que devolve um token JWT válido por 2 horas, com limite de tentativas por IP e por login contra ataques de força bruta.
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
* **Qualidade:** Testes de unidade, de persistência (H2), de integração HTTP (MockMvc) e de integração com MySQL real (Testcontainers), executados no GitHub Actions

## 🔌 Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/auth/register` | Cria uma conta |
| `POST` | `/auth/login` | Retorna o token JWT |
| `POST` | `/auth/logout` | Encerra as sessões em todos os dispositivos |
| `PUT` | `/auth/senha` | Troca a senha e revoga os tokens antigos |
| `GET` | `/transacoes` | Lista as transações do usuário |
| `POST` | `/transacoes` | Registra uma compra ou venda |
| `GET` | `/transacoes/{id}` | Busca uma transação |
| `PUT` | `/transacoes/{id}` | Altera uma transação |
| `DELETE` | `/transacoes/{id}` | Exclui uma transação |
| `GET` | `/carteira/resumo` | Resumo da carteira com cotações atuais |
| `GET` | `/carteira/total` | Custo das posições abertas, total aportado e total recebido em vendas (USDT) |
| `GET` | `/carteira/simulador/dca` | Simulação de aporte |
| `GET` | `/carteira/simulador/venda` | Simulação de venda a um preço-alvo |
| `GET` | `/analise/aportes-por-moeda` | Total comprado por ativo |

Com exceção de `/auth/register`, `/auth/login` e do Swagger, todas as rotas exigem o cabeçalho `Authorization: Bearer <token>`.

Mais detalhes em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) e [`docs/BUSINESS_RULES.md`](docs/BUSINESS_RULES.md).

## 📋 Como Executar e Testar

### Variáveis de ambiente

A aplicação lê as credenciais de variáveis de ambiente. O arquivo [`.env.example`](.env.example) serve de modelo:

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `DB_USER` / `DB_PASSWORD` | Sim | Credenciais do MySQL |
| `JWT_SECRET` | Sim | Segredo para assinar os tokens, com ao menos 32 bytes (gere com `openssl rand -base64 48`). A aplicação não sobe com um segredo menor nem com o valor de exemplo |
| `MYSQL_ROOT_PASSWORD` | Só no Docker | Senha do root do container MySQL |
| `CORS_ALLOWED_ORIGINS` | Em produção | Origens do frontend autorizadas pelo CORS, separadas por vírgula (padrão: `http://localhost:5173,http://localhost:5174`) |
| `DOMINIO` | Em produção | Domínio público da API, para o qual o Caddy emite o certificado HTTPS (perfil `producao`) |
| `COMPOSE_PROFILES` | Não | Use `producao` na VPS para que o Compose inclua o Caddy e o backup |
| `BACKUP_SENHA` | Em produção | Senha que cifra os backups do MySQL (ao menos 20 caracteres). Guarde uma cópia fora do servidor |
| `BACKUP_S3_*` | Recomendado | Bucket S3 para a cópia dos backups fora do servidor (veja [Backups do banco](#backups-do-banco)) |
| `BACKUP_HORARIO`, `BACKUP_RETENCAO_DIAS`, `BACKUP_RETENCAO_REMOTA_DIAS`, `BACKUP_TESTE_SEMANAL` | Não | Horário (UTC), retenção e teste semanal dos backups |
| `BINANCE_API_BASE_URL` | Não | URL da API de mercado da Binance |
| `COTACOES_CONNECT_TIMEOUT`, `COTACOES_READ_TIMEOUT`, `COTACOES_CACHE_TTL`, `COTACOES_CACHE_MAX_STALE` | Não | Timeouts e cache das cotações |

### Com Docker (desenvolvimento)

1.  Copie `.env.example` para `.env` e preencha os valores, incluindo `MYSQL_ROOT_PASSWORD`. Use em `DB_USER` um usuário diferente de `root`, pois a imagem do MySQL não aceita `root` como usuário adicional. Deixe `DOMINIO` vazio.
2.  Gere o JAR (o `Dockerfile` copia o arquivo de `target/`) e suba os containers:
    ```bash
    ./mvnw clean package
    docker compose up --build
    ```
3.  A API fica em `http://localhost:8080` e o MySQL em `localhost:3307`, sem HTTPS. As duas portas só aceitam conexões da própria máquina. O Flyway cria as tabelas na primeira execução.

O MySQL só aceita conexões cifradas com TLS. Ao abrir o banco numa ferramenta como DBeaver ou MySQL Workbench, mantenha o SSL ativado, que costuma ser o padrão. O certificado é gerado pelo próprio MySQL, então a ferramenta não deve exigir a verificação dele.

Para produção, com HTTPS, veja [Deploy em produção](#-deploy-em-produção-vps-com-https).

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
./mvnw clean test     # testes de unidade e HTTP, com H2 em memória
./mvnw clean verify   # também roda os testes de integração contra um MySQL 8 real
```

O `clean test` usa um banco H2 em memória e não acessa a Binance, então não precisa de MySQL nem de Docker.

O `clean verify` sobe um MySQL 8 em container com o Testcontainers, aplica as migrations do Flyway e roda os testes `*IT.java`. Ele precisa do Docker rodando e é o que o GitHub Actions executa.

## 🚀 Deploy em produção (VPS com HTTPS)

O `docker-compose.yml` tem um perfil `producao` que coloca o [Caddy](https://caddyserver.com) na frente da API. O Caddy emite e renova sozinho o certificado HTTPS do Let's Encrypt e redireciona HTTP para HTTPS. A API e o MySQL continuam acessíveis só de dentro do servidor.

```text
Internet ──443 (HTTPS)──▶ Caddy ──rede interna──▶ API ──▶ MySQL
```

### O que você precisa

* Uma VPS com Linux, por exemplo Ubuntu 24.04 LTS, com pelo menos 2 GB de RAM por causa da JVM e do MySQL.
* Um domínio. A API fica num subdomínio, como `api.seudominio.com.br`.

### Passo a passo

1.  **Aponte o domínio para a VPS.** No painel de DNS do seu domínio, crie um registro `A` para `api.seudominio.com.br` com o IP público da VPS (e um `AAAA`, se ela tiver IPv6). Confira com o comando abaixo, que deve mostrar o IP da VPS. A propagação pode levar alguns minutos.
    ```bash
    dig +short api.seudominio.com.br
    ```

2.  **Prepare o servidor.** Acesse por SSH, instale o Docker com o plugin do Compose seguindo o [guia oficial](https://docs.docker.com/engine/install/ubuntu/) e libere no firewall só SSH, HTTP e HTTPS:
    ```bash
    sudo ufw allow OpenSSH
    sudo ufw allow 80/tcp
    sudo ufw allow 443
    sudo ufw enable
    ```
    A regra `443` libera TCP e UDP (o UDP é usado pelo HTTP/3). Atenção: o Docker publica portas por fora do `ufw`. Por isso o Compose prende a API e o MySQL a `127.0.0.1`. Não troque isso por portas abertas.

3.  **Baixe o projeto e configure o `.env`.**
    ```bash
    git clone https://github.com/xandaodev/gerenciamento_investimentos_cripto.git
    cd gerenciamento_investimentos_cripto
    cp .env.example .env
    nano .env
    ```
    No `.env`:
    * `DOMINIO=api.seudominio.com.br`;
    * `CORS_ALLOWED_ORIGINS` com o endereço do frontend, por exemplo `https://app.seudominio.com.br`;
    * senhas fortes em `MYSQL_ROOT_PASSWORD` e `DB_PASSWORD` e um segredo longo em `JWT_SECRET`. Para gerar valores aleatórios, use `openssl rand -base64 48`;
    * descomente `COMPOSE_PROFILES=producao`, para que os comandos do Compose incluam o Caddy.

4.  **Gere o JAR.** Para não instalar o JDK no servidor, gere-o dentro de um container. O volume `criptovision-maven` guarda as dependências baixadas para as próximas vezes:
    ```bash
    docker run --rm -v "$PWD":/app -v criptovision-maven:/root/.m2 -w /app \
      eclipse-temurin:25-jdk sh ./mvnw -B clean package -DskipTests
    ```
    Os testes são pulados aqui porque já rodam no GitHub Actions a cada pull request.

5.  **Suba os containers.**
    ```bash
    docker compose up -d --build
    ```

6.  **Confira.** Na primeira subida, o Caddy obtém o certificado em alguns segundos:
    ```bash
    docker compose logs caddy
    curl -I https://api.seudominio.com.br/swagger-ui.html
    ```
    O log do Caddy deve mostrar `certificate obtained successfully`, e a resposta do `curl` deve vir por HTTPS com o cabeçalho `Strict-Transport-Security`.

### Atualizar a versão

```bash
git pull
docker run --rm -v "$PWD":/app -v criptovision-maven:/root/.m2 -w /app \
  eclipse-temurin:25-jdk sh ./mvnw -B clean package -DskipTests
docker compose up -d --build
```

### Se o certificado não for emitido

* Confira se o DNS já aponta para a VPS (`dig +short`) e se as portas 80 e 443 estão abertas: o Let's Encrypt valida o domínio por elas.
* Veja o motivo em `docker compose logs caddy`. Sem `DOMINIO` no `.env`, o log pede para defini-lo.
* Não apague o volume `caddy_data`, que guarda os certificados. Emitir muitos certificados seguidos esbarra nos limites do Let's Encrypt.

### Backups do banco

No perfil `producao`, o serviço `backup` faz um backup do MySQL por dia, às 03:00 UTC (meia-noite em Brasília). Cada backup é comprimido, **cifrado com a sua senha** (`gpg`, AES-256) e conferido antes de ser aceito. Os backups ficam 14 dias no servidor e, se você configurar um bucket, também 30 dias fora dele. Todo domingo, ele testa a restauração do backup mais recente num MySQL temporário.

> **Guarde a `BACKUP_SENHA` fora do servidor**, num gerenciador de senhas. Ela não vai para o bucket, e sem ela nenhum backup pode ser restaurado.

#### 1. Crie a senha dos backups

```bash
openssl rand -base64 32
```

Coloque o valor em `BACKUP_SENHA` no `.env` e guarde uma cópia no seu gerenciador de senhas.

#### 2. Crie o bucket e uma credencial que só acessa ele

Qualquer serviço compatível com S3 funciona. O Cloudflare R2 e o Backblaze B2 têm camada gratuita de 10 GB, o que sobra para este projeto. Os nomes dos menus podem mudar, então confira a documentação do [R2](https://developers.cloudflare.com/r2/) ou do [B2](https://www.backblaze.com/docs/cloud-storage) se algo estiver diferente.

**Cloudflare R2**
1. No painel da Cloudflare, abra **R2 Object Storage** e crie um bucket, por exemplo `criptovision-backups`.
2. Em **Manage API tokens**, crie um token com a permissão **Object Read & Write**, aplicada **só a esse bucket** (*Apply to specific buckets only*).
3. Anote o **Access Key ID**, o **Secret Access Key** e o endpoint S3, no formato `https://<ID_DA_CONTA>.r2.cloudflarestorage.com`. O secret só aparece uma vez.
4. No `.env`: `BACKUP_S3_PROVEDOR=Cloudflare`, `BACKUP_S3_REGIAO=auto`, mais o endpoint, o bucket e as duas chaves.

**Backblaze B2**
1. Em **Buckets**, crie um bucket **privado**, por exemplo `criptovision-backups`, e anote o **Endpoint** dele, como `s3.us-west-004.backblazeb2.com`.
2. Em **Application Keys**, crie uma chave com acesso **só a esse bucket** e permissão **Read and Write**.
3. Anote o **keyID** e a **applicationKey**. A applicationKey só aparece uma vez.
4. No `.env`: `BACKUP_S3_PROVEDOR=Other`, `BACKUP_S3_ENDPOINT=https://s3.<regiao>.backblazeb2.com`, `BACKUP_S3_REGIAO=<regiao>`, mais o bucket e as duas chaves.

A credencial não precisa listar nem criar buckets: o serviço só lê e escreve dentro do bucket informado.

#### 3. Suba e confira

```bash
docker compose up -d --build
docker compose logs backup
```

Ao subir, o serviço já faz um backup. O log deve mostrar `Backup conferido` e `Enviado para o bucket`. Sem bucket configurado, ele avisa que o backup ficou só no servidor. Se os backups pararem, o `docker compose ps` mostra o serviço como `unhealthy` depois de 26 horas sem backup.

#### Comandos

```bash
docker compose exec backup backup listar          # backups no servidor e no bucket
docker compose exec backup backup agora           # faz um backup agora
docker compose exec backup backup testar          # restaura o mais recente num MySQL temporário e confere
docker compose cp backup:/backups ./meus-backups  # copia os backups para a sua máquina
```

Para conferir um backup no seu computador, decifre-o com o `gpg` e a `BACKUP_SENHA`:

```bash
gpg --decrypt criptovision-AAAAMMDDTHHMMSSZ.sql.gz.gpg | gunzip > backup.sql
```

#### Restaurar

A restauração substitui todos os dados do banco. Antes de restaurar, o serviço faz um backup do estado atual.

```bash
docker compose stop api
docker compose exec backup backup listar
docker compose exec backup backup restaurar criptovision-AAAAMMDDTHHMMSSZ.sql.gz.gpg --confirmar
docker compose start api
```

**Se a VPS for perdida**, prepare uma VPS nova seguindo o passo a passo de deploy, com o **mesmo `.env`** (a mesma `BACKUP_SENHA` e a mesma credencial do bucket). Antes de subir a API, restaure do bucket:

```bash
docker compose up -d db
docker compose run --rm backup listar
docker compose run --rm backup restaurar criptovision-AAAAMMDDTHHMMSSZ.sql.gz.gpg --confirmar
docker compose up -d --build
```

O backup é diário, então numa perda total você recupera o banco como estava no último backup. Isso pode deixar de fora até um dia de transações.

**Para mais segurança**, crie a credencial sem permissão de apagar arquivos, use `BACKUP_RETENCAO_REMOTA_DIAS=0` e configure no próprio bucket uma regra de ciclo de vida que apague os arquivos com mais de 30 dias. Assim, mesmo quem invadir a VPS não consegue apagar os backups do bucket.

### Ainda não incluído

O [roadmap](docs/ROADMAP.md) lista o que falta para produção depois do pacote de deploy, como observabilidade (#14) e a imagem Docker de produção (#15).

---
⭐ *Desenvolvido por **Alexandre Vital** - Estudante de Ciência da Computação na UFSJ, em busca de desafios como Desenvolvedor Back-End.*