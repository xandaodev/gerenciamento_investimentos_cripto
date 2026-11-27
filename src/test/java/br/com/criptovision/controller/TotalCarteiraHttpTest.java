package br.com.criptovision.controller;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TotalCarteiraHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private TokenService tokenService;

    private Usuario usuario;

    @BeforeEach
    void prepararUsuario() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuario = usuarioRepository.saveAndFlush(new Usuario("totais", "hash"));
    }

    @Test
    void deveDevolverOsTresTotaisEmJsonComAMoeda() throws Exception {
        registrar("BTC", "1", "50000", TipoTransacao.COMPRA);
        registrar("BTC", "0.5", "40000", TipoTransacao.VENDA);

        String json = consultarTotal()
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.moeda").value("USDT"))
            .andReturn().getResponse().getContentAsString();

        // o cálculo antigo dava 50.000 - 20.000 = 30.000 e chamava isso de patrimônio
        assertThat(numero(json, "$.custoDasPosicoes")).isEqualByComparingTo("25000");
        assertThat(numero(json, "$.totalAportado")).isEqualByComparingTo("50000");
        assertThat(numero(json, "$.totalRecebidoEmVendas")).isEqualByComparingTo("20000");
    }

    @Test
    void naoDeveFicarNegativoDepoisDeVenderComLucro() throws Exception {
        registrar("ETH", "1", "1000", TipoTransacao.COMPRA);
        registrar("ETH", "1", "3000", TipoTransacao.VENDA);

        String json = consultarTotal()
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // o cálculo antigo dava 1.000 - 3.000 = -2.000
        assertThat(numero(json, "$.custoDasPosicoes")).isEqualByComparingTo("0");
        assertThat(numero(json, "$.totalAportado")).isEqualByComparingTo("1000");
        assertThat(numero(json, "$.totalRecebidoEmVendas")).isEqualByComparingTo("3000");
    }

    @Test
    void deveDevolverZerosParaCarteiraVazia() throws Exception {
        String json = consultarTotal()
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.moeda").value("USDT"))
            .andReturn().getResponse().getContentAsString();

        assertThat(numero(json, "$.custoDasPosicoes")).isEqualByComparingTo("0");
        assertThat(numero(json, "$.totalAportado")).isEqualByComparingTo("0");
        assertThat(numero(json, "$.totalRecebidoEmVendas")).isEqualByComparingTo("0");
    }

    private void registrar(String ticker, String quantidade, String preco, TipoTransacao tipo) {
        Transacao transacao = new Transacao(
            ticker, new BigDecimal(quantidade), new BigDecimal(preco), tipo
        );
        transacao.setUsuario(usuario);
        transacaoRepository.saveAndFlush(transacao);
    }

    private ResultActions consultarTotal() throws Exception {
        return mockMvc.perform(get("/carteira/total")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.gerarToken(usuario)));
    }

    private static BigDecimal numero(String json, String caminho) {
        Object valor = JsonPath.read(json, caminho);
        assertThat(valor).as(caminho).isNotNull();
        return new BigDecimal(valor.toString());
    }
}
