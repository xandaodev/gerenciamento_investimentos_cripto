package br.com.criptovision.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Com CORS_ALLOWED_ORIGINS definida, como em produção, só as origens
 * informadas são aceitas: elas substituem as origens locais.
 */
@SpringBootTest(
    properties = "cors.allowed-origins=https://app.criptovision.com.br"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsOrigensConfiguradasHttpTest {

    private static final String FRONTEND_PRODUCAO =
        "https://app.criptovision.com.br";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePermitirPreflightDaOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/transacoes")
                .header(HttpHeaders.ORIGIN, FRONTEND_PRODUCAO)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                FRONTEND_PRODUCAO
            ));
    }

    @Test
    void deveRecusarFrontendLocalQuandoNaoEstiverConfigurado() throws Exception {
        mockMvc.perform(options("/transacoes")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
            ));
    }

    @Test
    void deveIncluirCabecalhoCorsMesmoNaRespostaDeErro() throws Exception {
        // sem o cabeçalho, o navegador esconde o 401 do frontend,
        // que não conseguiria ler a mensagem de erro nem redirecionar ao login
        mockMvc.perform(get("/transacoes")
                .header(HttpHeaders.ORIGIN, FRONTEND_PRODUCAO))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                FRONTEND_PRODUCAO
            ));
    }
}
