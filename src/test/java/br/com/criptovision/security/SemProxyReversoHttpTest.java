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
 * A API sem proxy na frente, como ao rodar com {@code mvn spring-boot:run}:
 * sem {@code server.forward-headers-strategy}, os cabeçalhos X-Forwarded-*
 * são ignorados, e um cliente não consegue fugir do limite por IP trocando
 * o X-Forwarded-For a cada tentativa.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "seguranca.tentativas.login-por-ip.maximo=3",
        "seguranca.tentativas.login-por-ip.janela=1m"
    }
)
@ActiveProfiles("test")
class SemProxyReversoHttpTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int porta;

    @Test
    void deveIgnorarXForwardedForForjadoPeloCliente() throws Exception {
        for (int i = 1; i <= 3; i++) {
            assertThat(login("198.51.100." + i).statusCode()).isEqualTo(401);
        }

        // um IP "novo" no cabeçalho não adianta: o limite é do IP real
        assertThat(login("198.51.100.4").statusCode()).isEqualTo(429);
    }

    private HttpResponse<String> login(String ipForjado) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + porta + "/auth/login"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", ipForjado)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"login\": \"inexistente\", \"senha\": \"senha-errada\"}"
            ))
            .build();

        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
