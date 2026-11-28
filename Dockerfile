# Imagem de produção da API, em dois estágios:
#
#   build    compila o JAR com o Maven Wrapper, dentro do Docker. Não é preciso
#            ter Java nem Maven na máquina (nem na VPS) para gerar a imagem
#   runtime  só o JRE e a aplicação, rodando com um usuário sem privilégios
#
# Os testes não rodam aqui: o CI os executa antes (mvn verify).

# ---------------------------------------------------------------- build
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build

# as dependências mudam menos que o código: com o pom.xml copiado antes do src,
# esta camada fica em cache e um build só de código não baixa tudo de novo
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# até 3 tentativas: o Maven Central às vezes recusa downloads por um tempo
# (HTTP 429), o que derrubaria o build por um problema passageiro
RUN for tentativa in 1 2 3; do \
        sh ./mvnw --batch-mode --no-transfer-progress dependency:go-offline && exit 0; \
        [ "$tentativa" -lt 3 ] \
            && echo "Download das dependências falhou (tentativa $tentativa de 3). Tentando de novo..." \
            && sleep $((tentativa * 30)); \
    done; \
    exit 1

# compila sem os testes (o CI já os rodou) e separa o JAR em camadas
# (dependências, loader, aplicação), para que uma nova versão só troque a
# camada pequena da aplicação
COPY src/main/ src/main/
RUN sh ./mvnw --batch-mode --no-transfer-progress package -Dmaven.test.skip=true \
    && mv target/criptovision-api-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extraido

# -------------------------------------------------------------- runtime
FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="Alexandre Vital"

# usuário e grupo sem privilégios, com UID fixo, sem shell de login nem home
RUN addgroup -S -g 10001 criptovision \
    && adduser -S -D -H -u 10001 -G criptovision -s /sbin/nologin criptovision

WORKDIR /app

# da camada que muda menos para a que muda mais. Os arquivos ficam com o dono
# root: o usuário da aplicação só lê, não consegue alterar o próprio código
COPY --from=build /build/extraido/dependencies/ ./
COPY --from=build /build/extraido/spring-boot-loader/ ./
COPY --from=build /build/extraido/snapshot-dependencies/ ./
COPY --from=build /build/extraido/application/ ./

USER criptovision

# 8080: API (repassada pelo Caddy). 8081: Actuator, só no loopback do container
EXPOSE 8080

# o health completo inclui o banco: sem MySQL, o container fica "unhealthy".
# Usa a porta de gerenciamento, que só escuta dentro do container
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${MANAGEMENT_SERVER_PORT:-8081}/actuator/health" || exit 1

# MaxRAMPercentage: o heap acompanha o limite de memória do container, deixando
# 25% para o restante da JVM (metaspace, threads, buffers). Num OutOfMemoryError,
# a JVM encerra e o Docker reinicia o container, em vez de seguir num estado ruim.
# Formato exec: o Java é o PID 1 e recebe o SIGTERM do "docker stop", então o
# Spring encerra com graceful shutdown (termina as requisições em andamento)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
