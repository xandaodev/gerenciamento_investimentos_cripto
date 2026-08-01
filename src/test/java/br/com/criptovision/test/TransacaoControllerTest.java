package br.com.criptovision.test;

import br.com.criptovision.controller.TransacaoController;
import br.com.criptovision.exception.GlobalExceptionHandler;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.service.CarteiraService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TransacaoControllerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @Mock
    private TransacaoRepository repository;

    @Mock
    private CarteiraService carteiraService;

    @InjectMocks
    private TransacaoController controller;

    @BeforeEach
    public void configurarMockMvc() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @AfterEach
    public void encerrarValidador() {
        validator.close();
    }

    @Test
    public void deveCriarTransacaoValidaERetornarStatus201()
        throws Exception {

        Transacao transacaoSalva = new Transacao(
            "BTC",
            BigDecimal.valueOf(0.5),
            BigDecimal.valueOf(60000),
            TipoTransacao.COMPRA
        );

        transacaoSalva.setId(1L);

        when(carteiraService.registrarNovaTransacao(
            any(Transacao.class)
        )).thenReturn(transacaoSalva);

        mockMvc.perform(
                post("/transacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                        {
                                          "ticker": " btc ",
                                          "quantidade": 0.5,
                                          "precoUnitario": 60000,
                                          "tipo": "compra"
                                        }
                                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.ticker").value("BTC"))
            .andExpect(jsonPath("$.tipo").value("COMPRA"));

        ArgumentCaptor<Transacao> captor =
            ArgumentCaptor.forClass(Transacao.class);

        verify(carteiraService)
            .registrarNovaTransacao(captor.capture());

        Transacao recebidaPeloService = captor.getValue();

        assertEquals("BTC", recebidaPeloService.getTicker());
        assertEquals(
            TipoTransacao.COMPRA,
            recebidaPeloService.getTipo()
        );
    }

    @Test
    public void deveRetornar400QuandoQuantidadeForZero()
        throws Exception {

        mockMvc.perform(
                post("/transacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 0,
                                          "precoUnitario": 60000,
                                          "tipo": "COMPRA"
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.erro")
                    .value("Dados inválidos")
            )
            .andExpect(
                jsonPath("$.campos.quantidade")
                    .value(
                        "A quantidade deve ser maior que zero."
                    )
            );

        verifyNoInteractions(carteiraService);
    }

    @Test
    public void deveRetornar400QuandoTipoForInvalido()
        throws Exception {

        mockMvc.perform(
                post("/transacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 1,
                                          "precoUnitario": 60000,
                                          "tipo": "TROCA"
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.erro")
                    .value("Corpo da requisição inválido")
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        containsString(
                            "Tipo de transação inválido"
                        )
                    )
            );

        verifyNoInteractions(carteiraService);
    }
}
