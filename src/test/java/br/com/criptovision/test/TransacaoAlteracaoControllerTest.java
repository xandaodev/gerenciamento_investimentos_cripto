package br.com.criptovision.test;

import br.com.criptovision.controller.TransacaoController;
import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.exception.AlteracaoHistoricoInvalidaException;
import br.com.criptovision.exception.GlobalExceptionHandler;
import br.com.criptovision.exception.TransacaoNaoEncontradaException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TransacaoAlteracaoControllerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

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
            .setControllerAdvice(
                new GlobalExceptionHandler()
            )
            .setValidator(validator)
            .build();
    }

    @AfterEach
    public void encerrarValidador() {
        validator.close();
    }

    @Test
    public void deveAtualizarTransacaoValidaERetornar200()
        throws Exception {

        Transacao atualizada = new Transacao(
            "ETH",
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(3000),
            TipoTransacao.COMPRA
        );

        atualizada.setId(7L);

        when(carteiraService.atualizarTransacao(
            eq(7L),
            any(TransacaoRequestDTO.class)
        )).thenReturn(atualizada);

        mockMvc.perform(
                put("/transacoes/{id}", 7L)
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "ticker": " eth ",
                                          "quantidade": 2,
                                          "precoUnitario": 3000,
                                          "tipo": "compra"
                                        }
                                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.ticker").value("ETH"))
            .andExpect(jsonPath("$.tipo").value("COMPRA"));

        ArgumentCaptor<TransacaoRequestDTO> captor =
            ArgumentCaptor.forClass(
                TransacaoRequestDTO.class
            );

        verify(carteiraService).atualizarTransacao(
            eq(7L),
            captor.capture()
        );

        assertEquals("ETH", captor.getValue().ticker());
        assertEquals(
            TipoTransacao.COMPRA,
            captor.getValue().tipo()
        );
    }

    @Test
    public void deveRetornar400QuandoAtualizacaoForInvalida()
        throws Exception {

        mockMvc.perform(
                put("/transacoes/{id}", 7L)
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "ticker": "ETH",
                                          "quantidade": 0,
                                          "precoUnitario": 3000,
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
    public void deveRetornar404AoAtualizarTransacaoInexistente()
        throws Exception {

        when(carteiraService.atualizarTransacao(
            eq(999L),
            any(TransacaoRequestDTO.class)
        )).thenThrow(
            new TransacaoNaoEncontradaException(999L)
        );

        mockMvc.perform(
                put("/transacoes/{id}", 999L)
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 1,
                                          "precoUnitario": 50000,
                                          "tipo": "COMPRA"
                                        }
                                        """)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.erro")
                    .value("Transação não encontrada")
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "Transação não encontrada "
                            + "para o ID 999."
                    )
            );
    }

    @Test
    public void deveRetornar409QuandoAtualizacaoQuebrarHistorico()
        throws Exception {

        when(carteiraService.atualizarTransacao(
            eq(1L),
            any(TransacaoRequestDTO.class)
        )).thenThrow(
            new AlteracaoHistoricoInvalidaException(
                "A alteração tornaria o histórico inconsistente.",
                new RuntimeException()
            )
        );

        mockMvc.perform(
                put("/transacoes/{id}", 1L)
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 0.5,
                                          "precoUnitario": 50000,
                                          "tipo": "COMPRA"
                                        }
                                        """)
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.erro")
                    .value("Conflito no histórico")
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "A alteração tornaria o "
                            + "histórico inconsistente."
                    )
            );
    }

    @Test
    public void deveExcluirTransacaoValidaERetornar204()
        throws Exception {

        mockMvc.perform(
                delete("/transacoes/{id}", 2L)
            )
            .andExpect(status().isNoContent());

        verify(carteiraService).excluirTransacao(2L);
    }

    @Test
    public void deveRetornar404AoExcluirTransacaoInexistente()
        throws Exception {

        doThrow(
            new TransacaoNaoEncontradaException(999L)
        ).when(carteiraService).excluirTransacao(999L);

        mockMvc.perform(
                delete("/transacoes/{id}", 999L)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.erro")
                    .value("Transação não encontrada")
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "Transação não encontrada "
                            + "para o ID 999."
                    )
            );
    }

    @Test
    public void deveRetornar409QuandoExclusaoQuebrarHistorico()
        throws Exception {

        doThrow(
            new AlteracaoHistoricoInvalidaException(
                "A exclusão tornaria o histórico inconsistente.",
                new RuntimeException()
            )
        ).when(carteiraService).excluirTransacao(1L);

        mockMvc.perform(
                delete("/transacoes/{id}", 1L)
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.erro")
                    .value("Conflito no histórico")
            )
            .andExpect(
                jsonPath("$.mensagem")
                    .value(
                        "A exclusão tornaria o "
                            + "histórico inconsistente."
                    )
            );
    }
}
