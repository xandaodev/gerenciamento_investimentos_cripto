package br.com.criptovision.test;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import br.com.criptovision.security.TokenService;
import br.com.criptovision.service.HttpService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    properties = {
        "api.security.token.secret="
            + "segredo-exclusivo-dos-testes-criptovision-2026"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IsolamentoTransacoesHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private HttpService httpService;

    private Usuario alexandre;
    private Usuario outroUsuario;

    private String tokenAlexandre;
    private String tokenOutroUsuario;

    @BeforeEach
    public void prepararBancoEUsuarios() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();

        alexandre = usuarioRepository.saveAndFlush(
            new Usuario(
                "alexandre",
                "senha-criptografada-alexandre"
            )
        );

        outroUsuario = usuarioRepository.saveAndFlush(
            new Usuario(
                "outro-usuario",
                "senha-criptografada-outro"
            )
        );

        tokenAlexandre =
            tokenService.gerarToken(alexandre.getLogin());

        tokenOutroUsuario =
            tokenService.gerarToken(outroUsuario.getLogin());

        when(httpService.validarTicker(anyString()))
            .thenReturn(true);
    }

    @Test
    public void deveRetornar401QuandoTokenNaoForEnviado()
        throws Exception {

        mockMvc.perform(
                get("/transacoes")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void deveRetornar401QuandoTokenForInvalido()
        throws Exception {

        mockMvc.perform(
                get("/transacoes")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer token-invalido"
                    )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void deveListarSomenteTransacoesDoUsuarioAutenticado()
        throws Exception {

        salvarTransacao(
            alexandre,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        salvarTransacao(
            outroUsuario,
            "ETH",
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(3000),
            TipoTransacao.COMPRA
        );

        mockMvc.perform(
                get("/transacoes")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(
                jsonPath("$[0].ticker")
                    .value("BTC")
            )
            .andExpect(
                jsonPath("$[0].usuario")
                    .doesNotExist()
            );
    }

    @Test
    public void deveRetornar404AoBuscarTransacaoDeOutroUsuario()
        throws Exception {

        Transacao transacaoDoOutro =
            salvarTransacao(
                outroUsuario,
                "ETH",
                BigDecimal.ONE,
                BigDecimal.valueOf(3000),
                TipoTransacao.COMPRA
            );

        mockMvc.perform(
                get(
                    "/transacoes/{id}",
                    transacaoDoOutro.getId()
                )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.title")
                    .value(
                        "Recurso não encontrado"
                    )
            );
    }

    @Test
    public void deveImpedirAtualizacaoDeTransacaoDeOutroUsuario()
        throws Exception {

        Transacao transacaoDoOutro =
            salvarTransacao(
                outroUsuario,
                "ETH",
                BigDecimal.ONE,
                BigDecimal.valueOf(3000),
                TipoTransacao.COMPRA
            );

        mockMvc.perform(
                put(
                    "/transacoes/{id}",
                    transacaoDoOutro.getId()
                )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 2,
                                          "precoUnitario": 50000,
                                          "tipo": "COMPRA"
                                        }
                                        """)
            )
            .andExpect(status().isNotFound());

        Transacao transacaoPersistida =
            transacaoRepository
                .findById(transacaoDoOutro.getId())
                .orElseThrow();

        assertEquals(
            "ETH",
            transacaoPersistida.getTicker()
        );

        assertEquals(
            outroUsuario.getId(),
            transacaoPersistida
                .getUsuario()
                .getId()
        );
    }

    @Test
    public void deveImpedirExclusaoDeTransacaoDeOutroUsuario()
        throws Exception {

        Transacao transacaoDoOutro =
            salvarTransacao(
                outroUsuario,
                "SOL",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(150),
                TipoTransacao.COMPRA
            );

        mockMvc.perform(
                delete(
                    "/transacoes/{id}",
                    transacaoDoOutro.getId()
                )
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
            )
            .andExpect(status().isNotFound());

        assertTrue(
            transacaoRepository.existsById(
                transacaoDoOutro.getId()
            )
        );
    }

    @Test
    public void deveAssociarNovaTransacaoAoUsuarioDoToken()
        throws Exception {

        mockMvc.perform(
                post("/transacoes")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 0.5,
                                          "precoUnitario": 60000,
                                          "tipo": "COMPRA"
                                        }
                                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.ticker")
                    .value("BTC")
            )
            .andExpect(
                jsonPath("$.usuario")
                    .doesNotExist()
            );

        List<Transacao> transacoesDoAlexandre =
            transacaoRepository
                .findAllByUsuarioOrderByDataAscIdAsc(
                    alexandre
                );

        List<Transacao> transacoesDoOutro =
            transacaoRepository
                .findAllByUsuarioOrderByDataAscIdAsc(
                    outroUsuario
                );

        assertEquals(1, transacoesDoAlexandre.size());
        assertEquals(0, transacoesDoOutro.size());

        assertEquals(
            alexandre.getId(),
            transacoesDoAlexandre
                .getFirst()
                .getUsuario()
                .getId()
        );
    }

    @Test
    public void deveCalcularTotalSomenteComHistoricoDoUsuario()
        throws Exception {

        salvarTransacao(
            alexandre,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(100),
            TipoTransacao.COMPRA
        );

        salvarTransacao(
            outroUsuario,
            "ETH",
            BigDecimal.ONE,
            BigDecimal.valueOf(900),
            TipoTransacao.COMPRA
        );

        mockMvc.perform(
                get("/carteira/total")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(
                content().string(
                    "Patrimônio Total Investido: "
                        + "R$ 100.0"
                )
            );
    }

    @Test
    public void deveSepararAportesPorMoedaEntreUsuarios()
        throws Exception {

        salvarTransacao(
            alexandre,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        salvarTransacao(
            outroUsuario,
            "ETH",
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(3000),
            TipoTransacao.COMPRA
        );

        mockMvc.perform(
                get("/analise/aportes-por-moeda")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(tokenAlexandre)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(
                jsonPath("$[0].ticker")
                    .value("BTC")
            );
    }

    private Transacao salvarTransacao(
        Usuario usuario,
        String ticker,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        TipoTransacao tipo
    ) {
        Transacao transacao = new Transacao(
            ticker,
            quantidade,
            precoUnitario,
            tipo
        );

        transacao.setUsuario(usuario);

        return transacaoRepository
            .saveAndFlush(transacao);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
