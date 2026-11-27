package br.com.criptovision.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A API atrás do Caddy, como no perfil "producao" do docker-compose: com
 * {@code server.forward-headers-strategy=native}, o Tomcat aceita os cabeçalhos
 * X-Forwarded-* vindos de IPs internos (o proxy). Roda num Tomcat de verdade,
 * porque é ele quem aplica esses cabeçalhos.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "server.forward-headers-strategy=native",
        "seguranca.tentativas.login-por-ip.maximo=3",
        "seguranca.tentativas.login-por-ip.janela=1m"
    }
)
@ActiveProfiles("test")
class ProxyReversoHttpTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int porta;

    @Test
    void deveLimitarPeloIpDoClienteInformadoPeloProxy() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(login("203.0.113.10").statusCode()).isEqualTo(401);
        }

        assertThat(login("203.0.113.10").statusCode()).isEqualTo(429);

        // outro cliente atrás do mesmo proxy não é afetado:
        // sem os cabeçalhos, todos teriam o IP do proxy e dividiriam o limite
        assertThat(login("203.0.113.11").statusCode()).isEqualTo(401);
    }

    @Test
    void deveEnviarHstsQuandoOClienteChegouPorHttps() throws Exception {
        HttpResponse<String> resposta = get("https");

        assertThat(resposta.headers().firstValue("Strict-Transport-Security"))
            .hasValueSatisfying(valor -> assertThat(valor).contains("max-age="));
    }

    @Test
    void naoDeveEnviarHstsEmHttp() throws Exception {
        HttpResponse<String> resposta = get("http");

        assertThat(resposta.headers().firstValue("Strict-Transport-Security"))
            .isEmpty();
    }

    private HttpResponse<String> login(String ipDoCliente) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + porta + "/auth/login"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", ipDoCliente)
            .header("X-Forwarded-Proto", "https")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"login\": \"inexistente\", \"senha\": \"senha-errada\"}"
            ))
            .build();

        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String protocoloDoCliente) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + porta + "/v3/api-docs"))
            .header("X-Forwarded-Proto", protocoloDoCliente)
            .GET()
            .build();

        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
