package br.com.criptovision.observabilidade;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No docker-compose, os logs saem em JSON no formato ECS
 * ({@code LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs}). Este teste usa o mesmo encoder
 * do Spring Boot para garantir que o ID da requisição, guardado no MDC pelo
 * {@link IdentificadorRequisicaoFilter}, vira um campo próprio do JSON: é por ele
 * que uma ferramenta de logs encontra tudo o que aconteceu numa requisição.
 */
class LogsEmJsonTest {

    @Test
    void idDaRequisicaoDeveVirarUmCampoDoJson() {
        LoggerContext contexto = new LoggerContext();
        contexto.putObject(
            Environment.class.getName(),
            new MockEnvironment().withProperty("spring.application.name", "criptovision-api")
        );

        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(contexto);
        encoder.setFormat("ecs");
        encoder.start();

        LoggingEvent evento = new LoggingEvent();
        evento.setLoggerContext(contexto);
        evento.setLoggerName("br.com.criptovision.exception.GlobalExceptionHandler");
        evento.setLevel(Level.ERROR);
        evento.setThreadName("http-nio-8080-exec-1");
        evento.setMessage("Erro inesperado ao processar a requisição /transacoes");
        evento.setTimeStamp(System.currentTimeMillis());
        evento.setMDCPropertyMap(Map.of(IdentificadorRequisicaoFilter.CHAVE_MDC, "3f2a9c1e-req"));

        String linha = new String(encoder.encode(evento), StandardCharsets.UTF_8);

        // um objeto JSON por linha, sem quebras no meio
        assertThat(linha.strip()).doesNotContain("\n");

        JsonNode json = JsonMapper.builder().build().readTree(linha);
        assertThat(json.path("requestId").asString()).isEqualTo("3f2a9c1e-req");
        assertThat(json.path("log").path("level").asString()).isEqualTo("ERROR");
        assertThat(json.path("service").path("name").asString()).isEqualTo("criptovision-api");
        assertThat(json.path("message").asString())
            .isEqualTo("Erro inesperado ao processar a requisição /transacoes");
    }
}
