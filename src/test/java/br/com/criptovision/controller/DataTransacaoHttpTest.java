package br.com.criptovision.controller;

import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import br.com.criptovision.security.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A data da transação pode ser informada (compras antigas, com a data real) e a
 * carteira é reconstruída na ordem dessas datas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataTransacaoHttpTest {

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

    private String token;

    @BeforeEach
    void prepararUsuario() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("datas", "hash"));
        token = tokenService.gerarToken(usuario);

        when(cotacaoService.normalizarTicker(anyString()))
            .thenAnswer(chamada -> chamada.<String>getArgument(0).trim().toUpperCase());
    }

    @Test
    void deveGuardarADataInformadaEmUtc() throws Exception {
        registrar("BTC", "1", "50000", "COMPRA", "2025-03-10T14:00:00-03:00")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value("2025-03-10T17:00:00"));
    }

    @Test
    void deveUsarOMomentoDoCadastroQuandoADataNaoForInformada() throws Exception {
        String json = registrar("BTC", "1", "50000", "COMPRA", null)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        LocalDateTime data = LocalDateTime.parse(JsonPath.read(json, "$.data"));
        LocalDateTime agoraEmUtc = LocalDateTime.now(ZoneOffset.UTC);

        assertThat(Duration.between(data, agoraEmUtc).abs()).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void devePermitirLancarUmaCompraAntigaDepoisDaVenda() throws Exception {
        // sem saldo, a venda é recusada...
        registrar("BTC", "1", "60000", "VENDA", "2025-06-01T10:00:00Z")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Saldo insuficiente"));

        // ...mas a compra antiga, lançada com a data real, vem antes dela no histórico
        registrar("BTC", "1", "50000", "COMPRA", "2025-01-15T10:00:00Z")
            .andExpect(status().isCreated());
        registrar("BTC", "1", "60000", "VENDA", "2025-06-01T10:00:00Z")
            .andExpect(status().isCreated());

        mockMvc.perform(get("/transacoes").header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipo").value("COMPRA"))
            .andExpect(jsonPath("$[1].tipo").value("VENDA"));
    }

    @Test
    void deveRecusarUmaVendaAntigaQueDeixariaUmaVendaPosteriorSemSaldo() throws Exception {
        registrar("BTC", "1", "50000", "COMPRA", "2025-01-01T10:00:00Z")
            .andExpect(status().isCreated());
        registrar("BTC", "1", "60000", "VENDA", "2025-03-01T10:00:00Z")
            .andExpect(status().isCreated());

        // em fevereiro ainda havia 1 BTC, mas a venda de março ficaria sem saldo
        registrar("BTC", "0.5", "55000", "VENDA", "2025-02-01T10:00:00Z")
            .andExpect(status().isConflict());

        mockMvc.perform(get("/transacoes").header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deveRecusarDataNoFuturo() throws Exception {
        String amanha = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).toString();

        registrar("BTC", "1", "50000", "COMPRA", amanha)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Data inválida"));
    }

    @Test
    void deveRecusarDataAnteriorAoBitcoin() throws Exception {
        registrar("BTC", "1", "50000", "COMPRA", "2008-12-31T23:59:59Z")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Data inválida"));
    }

    @Test
    void deveRecusarDataSemFuso() throws Exception {
        // sem o fuso, a mesma data valeria horários diferentes em cada lugar
        registrar("BTC", "1", "50000", "COMPRA", "2025-03-10T14:00:00")
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveMoverATransacaoParaANovaDataNaAlteracao() throws Exception {
        Long id = idDe(registrar("BTC", "1", "50000", "COMPRA", "2025-03-10T10:00:00Z"));

        alterar(id, "BTC", "1", "50000", "COMPRA", "2025-02-01T09:30:00Z")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("2025-02-01T09:30:00"));
    }

    @Test
    void deveManterADataQuandoAAlteracaoNaoInformaOutra() throws Exception {
        Long id = idDe(registrar("BTC", "1", "50000", "COMPRA", "2025-03-10T10:00:00Z"));

        alterar(id, "BTC", "2", "50000", "COMPRA", null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("2025-03-10T10:00:00"));
    }

    @Test
    void deveRecusarMoverUmaCompraParaDepoisDaVendaQueDependeDela() throws Exception {
        Long compra = idDe(registrar("BTC", "1", "50000", "COMPRA", "2025-01-01T10:00:00Z"));
        registrar("BTC", "1", "60000", "VENDA", "2025-03-01T10:00:00Z")
            .andExpect(status().isCreated());

        alterar(compra, "BTC", "1", "50000", "COMPRA", "2025-04-01T10:00:00Z")
            .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------------

    private ResultActions registrar(
        String ticker, String quantidade, String preco, String tipo, String data
    ) throws Exception {
        return mockMvc.perform(post("/transacoes")
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo(ticker, quantidade, preco, tipo, data)));
    }

    private ResultActions alterar(
        Long id, String ticker, String quantidade, String preco, String tipo, String data
    ) throws Exception {
        return mockMvc.perform(put("/transacoes/" + id)
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo(ticker, quantidade, preco, tipo, data)));
    }

    private static String corpo(
        String ticker, String quantidade, String preco, String tipo, String data
    ) {
        String campoData = data == null ? "" : ", \"data\": \"" + data + "\"";
        return """
            {"ticker": "%s", "quantidade": %s, "precoUnitario": %s, "tipo": "%s"%s}
            """.formatted(ticker, quantidade, preco, tipo, campoData);
    }

    private static Long idDe(ResultActions resposta) throws Exception {
        String json = resposta.andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
