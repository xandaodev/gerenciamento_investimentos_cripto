package br.com.criptovision.observabilidade;

import br.com.criptovision.exception.HistoricoInconsistenteException;
import br.com.criptovision.security.ProtecaoForcaBruta;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um erro 500 precisa deixar três rastros: a métrica (para o alerta), o log de
 * ERROR com o ID da requisição e o mesmo ID na resposta, para o usuário informar.
 * A falha é simulada na proteção contra força bruta, a primeira coisa que o
 * cadastro chama.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ErrosInternosHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private ProtecaoForcaBruta protecaoForcaBruta;

    @Test
    void erroInesperadoDeveContarNaMetricaELogarComOIdDaRequisicao(CapturedOutput saida)
        throws Exception {

        doThrow(new IllegalStateException("falha simulada"))
            .when(protecaoForcaBruta).verificarCadastro(any());
        double antes = errosInternos("IllegalStateException");

        cadastrar("erro-inesperado-1")
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.requestId").value("erro-inesperado-1"))
            // o detalhe técnico fica só no log
            .andExpect(jsonPath("$.detail").value(
                "Ocorreu um erro inesperado. Tente novamente mais tarde."
            ));

        assertThat(errosInternos("IllegalStateException")).isEqualTo(antes + 1);
        assertThat(saida.getOut())
            .containsPattern("ERROR .*\\[erro-inesperado-1\\] .*Erro inesperado ao processar a requisição /auth/register")
            .contains("falha simulada");
    }

    @Test
    void historicoInconsistenteDeveContarNaMetricaComOProprioTipo() throws Exception {
        doThrow(new HistoricoInconsistenteException(7L, "BTC", "VENDA", new IllegalStateException()))
            .when(protecaoForcaBruta).verificarCadastro(any());
        double antes = errosInternos("HistoricoInconsistenteException");

        cadastrar("historico-1")
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.requestId").value("historico-1"));

        assertThat(errosInternos("HistoricoInconsistenteException")).isEqualTo(antes + 1);
    }

    private ResultActions cadastrar(String idDaRequisicao) throws Exception {
        return mockMvc.perform(post("/auth/register")
            .header("X-Request-Id", idDaRequisicao)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"login": "usuario.erro", "senha": "SenhaSegura@2026"}
                """));
    }

    private double errosInternos(String excecao) {
        Counter contador = meterRegistry
            .find(MetricasCriptoVision.ERROS_INTERNOS)
            .tag("excecao", excecao)
            .counter();

        return contador == null ? 0 : contador.count();
    }
}
