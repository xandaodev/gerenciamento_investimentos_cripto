package br.com.criptovision.observabilidade;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O Actuator num Tomcat de verdade, com as duas portas da produção: a da API
 * (que o Caddy publica) e a de gerenciamento (privada). Na porta da API, só o
 * {@code /saude} responde, e sem detalhes. O health completo e as métricas ficam
 * na porta de gerenciamento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ObservabilidadeHttpTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int portaDaApi;

    @LocalManagementPort
    private int portaDeGerenciamento;

    @Test
    void saudePublicaDeveInformarSoOStatus() throws Exception {
        HttpResponse<String> resposta = get(portaDaApi, "/saude");

        assertThat(resposta.statusCode()).isEqualTo(200);
        // nada de componentes, versões ou caminhos para quem está na internet
        assertThat(resposta.body()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void portaDaApiNaoDeveExporOActuator() throws Exception {
        for (String caminho : new String[] {
            "/actuator", "/actuator/health", "/actuator/metrics", "/actuator/prometheus"
        }) {
            HttpResponse<String> resposta = get(portaDaApi, caminho);

            assertThat(resposta.statusCode())
                .as("GET %s na porta da API", caminho)
                .isIn(401, 404);
            assertThat(resposta.body()).doesNotContain("\"UP\"", "jvm_", "http_server_requests");
        }
    }

    @Test
    void portaDeGerenciamentoDeveTerOHealthCompleto() throws Exception {
        HttpResponse<String> resposta = get(portaDeGerenciamento, "/actuator/health");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body())
            .contains("\"status\":\"UP\"")
            .contains("\"db\"")
            .contains("\"livenessState\"")
            .contains("\"readinessState\"");
    }

    @Test
    void portaDeGerenciamentoDeveTerAsProbesDeLivenessEReadiness() throws Exception {
        for (String probe : new String[] {"liveness", "readiness"}) {
            HttpResponse<String> resposta = get(
                portaDeGerenciamento,
                "/actuator/health/" + probe
            );

            assertThat(resposta.statusCode()).as(probe).isEqualTo(200);
            assertThat(resposta.body()).as(probe).contains("\"status\":\"UP\"");
        }
    }

    @Test
    void portaDeGerenciamentoDevePublicarMetricasNoFormatoDoPrometheus() throws Exception {
        get(portaDaApi, "/saude");

        HttpResponse<String> resposta = get(portaDeGerenciamento, "/actuator/prometheus");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body())
            .contains("http_server_requests_seconds_count")
            .contains("hikaricp_connections")
            .contains("jvm_memory_used_bytes")
            // o estado do circuit breaker da Binance, para um alerta quando ele abrir
            .contains("resilience4j_circuitbreaker_state{name=\"binance\",state=\"closed\"} 1.0");
    }

    @Test
    void respostasDaApiDevemTrazerOIdDaRequisicao() throws Exception {
        HttpResponse<String> resposta = get(portaDaApi, "/saude");

        assertThat(resposta.headers().firstValue(IdentificadorRequisicaoFilter.CABECALHO))
            .hasValueSatisfying(id -> assertThat(id).hasSize(36));
    }

    private HttpResponse<String> get(int porta, String caminho) throws Exception {
        // 127.0.0.1, e não localhost: a porta de gerenciamento só escuta no IPv4 do loopback
        HttpRequest requisicao = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + porta + caminho))
            .GET()
            .build();

        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
