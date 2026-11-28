package br.com.criptovision.observabilidade;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentificadorRequisicaoHttpTest {

    private static final String UUID =
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveGerarUmIdQuandoOClienteNaoEnvia() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/carteira/resumo"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("X-Request-Id", matchesPattern(UUID)))
            .andReturn();

        // o 401 do Spring Security também leva o ID, igual ao do cabeçalho
        String id = resultado.getResponse().getHeader("X-Request-Id");
        assertThat(resultado.getResponse().getContentAsString())
            .contains("\"requestId\":\"" + id + "\"");
    }

    @Test
    void deveAproveitarOIdEnviadoPeloCliente() throws Exception {
        mockMvc.perform(get("/carteira/resumo").header("X-Request-Id", "front-42.a_b"))
            .andExpect(header().string("X-Request-Id", "front-42.a_b"))
            .andExpect(jsonPath("$.requestId").value("front-42.a_b"));
    }

    @Test
    void deveTrocarUmIdInseguroPorUmNovo() throws Exception {
        // texto livre nos logs permitiria forjar linhas ou poluir a busca
        for (String inseguro : new String[] {
            "id com espaço", "id\r\nERROR falso", "a".repeat(65), "<script>", ""
        }) {
            mockMvc.perform(get("/carteira/resumo").header("X-Request-Id", inseguro))
                .andExpect(header().string("X-Request-Id", matchesPattern(UUID)));
        }
    }

    @Test
    void errosDeValidacaoDevemTrazerOIdDaRequisicao() throws Exception {
        mockMvc.perform(post("/auth/register")
                .header("X-Request-Id", "cadastro-invalido-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.requestId").value("cadastro-invalido-1"));
    }

    @Test
    void cadaRequisicaoDeveTerUmIdDiferente() throws Exception {
        String primeiro = mockMvc.perform(get("/carteira/resumo"))
            .andReturn().getResponse().getHeader("X-Request-Id");
        String segundo = mockMvc.perform(get("/carteira/resumo"))
            .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(primeiro).isNotEqualTo(segundo);
    }
}
