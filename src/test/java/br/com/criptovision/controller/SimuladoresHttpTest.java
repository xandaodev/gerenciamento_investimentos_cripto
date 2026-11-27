package br.com.criptovision.controller;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import br.com.criptovision.security.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os simuladores de DCA e de venda recusam entradas sem sentido financeiro e
 * nunca devolvem NaN nem Infinity.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimuladoresHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private CotacaoService cotacaoService;

    private Usuario usuario;
    private String token;

    @BeforeEach
    void prepararUsuario() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = usuarioRepository.saveAndFlush(new Usuario("simulador", "hash"));
        token = tokenService.gerarToken(usuario);

        when(cotacaoService.normalizarTicker(anyString()))
            .thenAnswer(chamada -> chamada.<String>getArgument(0).trim().toUpperCase());
    }

    // --- DCA -------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100"})
    void deveRecusarAporteQueNaoSejaPositivo(String aporte) throws Exception {
        simularDca("BTC", aporte, "50000")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Dados inválidos"))
            .andExpect(jsonPath("$.erros[*].campo").value(hasItem("aporte")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void deveRecusarPrecoQueNaoSejaPositivo(String preco) throws Exception {
        simularDca("BTC", "100", preco)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erros[*].campo").value(hasItem("preco")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "abc"})
    void deveRecusarValoresQueNaoSaoNumerosFinitos(String valor) throws Exception {
        simularDca("BTC", valor, "50000")
            .andExpect(status().isBadRequest())
            .andExpect(content().string(not(containsString("NaN"))));
    }

    @Test
    void deveRecusarParametroAusente() throws Exception {
        mockMvc.perform(get("/carteira/simulador/dca")
                .param("ticker", "BTC")
                .param("aporte", "100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveSimularAporteNumaPosicaoExistente() throws Exception {
        comprar("ETH", "2", "3000"); // custo 6.000

        String json = simularDca("ETH", "3000", "1500")
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(numero(json, "$.qtdComprada")).isEqualByComparingTo("2");
        assertThat(numero(json, "$.saldoAtual")).isEqualByComparingTo("2");
        assertThat(numero(json, "$.novoSaldoTotal")).isEqualByComparingTo("4");
        assertThat(numero(json, "$.pmAtual")).isEqualByComparingTo("3000");
        assertThat(numero(json, "$.novoPM")).isEqualByComparingTo("2250");
        assertThat(numero(json, "$.diferencaPM")).isEqualByComparingTo("-25");
        assertThat(numero(json, "$.valorizacaoNecessaria")).isEqualByComparingTo("50");
    }

    @Test
    void deveSimularAPrimeiraCompraSemPosicaoAnterior() throws Exception {
        String json = simularDca("SOL", "100", "50")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pmAtual").isEmpty())
            .andExpect(jsonPath("$.diferencaPM").isEmpty())
            .andExpect(content().string(not(containsString("NaN"))))
            .andExpect(content().string(not(containsString("Infinity"))))
            .andReturn().getResponse().getContentAsString();

        assertThat(numero(json, "$.qtdComprada")).isEqualByComparingTo("2");
        assertThat(numero(json, "$.saldoAtual")).isEqualByComparingTo("0");
        assertThat(numero(json, "$.novoPM")).isEqualByComparingTo("50");
        assertThat(numero(json, "$.valorizacaoNecessaria")).isEqualByComparingTo("0");
    }

    // --- venda -------------------------------------------------------------

    @Test
    void deveSimularVendaComLucro() throws Exception {
        comprar("ETH", "1", "2000");
        cotacaoAtual("ETH", "2500");

        String json = simularVenda("ETH", "3000")
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(numero(json, "$.lucroEstimado")).isEqualByComparingTo("1000");
        assertThat(numero(json, "$.porcentagemLucro")).isEqualByComparingTo("50");
        assertThat(numero(json, "$.valorTotalFicticio")).isEqualByComparingTo("3000");
        assertThat(numero(json, "$.valorTotalAtual")).isEqualByComparingTo("2500");
    }

    @Test
    void deveSimularVendaComPrejuizo() throws Exception {
        comprar("ETH", "1", "2000");
        cotacaoAtual("ETH", "2500");

        String json = simularVenda("ETH", "1500")
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(numero(json, "$.lucroEstimado")).isEqualByComparingTo("-500");
        assertThat(numero(json, "$.porcentagemLucro")).isEqualByComparingTo("-25");
    }

    @Test
    void deveRecusarVendaDeAtivoSemSaldo() throws Exception {
        cotacaoAtual("SOL", "150");

        simularVenda("SOL", "200")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Sem saldo para simular"))
            .andExpect(content().string(not(containsString("NaN"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10", "NaN", "Infinity"})
    void deveRecusarPrecoAlvoInvalido(String precoAlvo) throws Exception {
        comprar("ETH", "1", "2000");
        cotacaoAtual("ETH", "2500");

        simularVenda("ETH", precoAlvo)
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------

    private void comprar(String ticker, String quantidade, String preco) {
        Transacao compra = new Transacao(
            ticker, new BigDecimal(quantidade), new BigDecimal(preco), TipoTransacao.COMPRA
        );
        compra.setUsuario(usuario);
        transacaoRepository.saveAndFlush(compra);
    }

    private void cotacaoAtual(String ticker, String preco) {
        when(cotacaoService.buscarCotacao(ticker)).thenReturn(new CotacaoMercado(
            ticker, new BigDecimal(preco), BigDecimal.ZERO, Instant.now(), false
        ));
    }

    private ResultActions simularDca(String ticker, String aporte, String preco)
        throws Exception {

        return mockMvc.perform(get("/carteira/simulador/dca")
            .param("ticker", ticker)
            .param("aporte", aporte)
            .param("preco", preco)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions simularVenda(String ticker, String precoAlvo) throws Exception {
        return mockMvc.perform(get("/carteira/simulador/venda")
            .param("ticker", ticker)
            .param("precoAlvo", precoAlvo)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static BigDecimal numero(String json, String caminho) {
        Object valor = JsonPath.read(json, caminho);
        assertThat(valor).as(caminho).isNotNull();
        return new BigDecimal(valor.toString());
    }
}
