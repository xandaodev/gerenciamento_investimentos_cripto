package br.com.criptovision.test;

import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.exception.AlteracaoHistoricoInvalidaException;
import br.com.criptovision.exception.TransacaoNaoEncontradaException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CarteiraServiceTransacaoTest {

    @Mock
    private TransacaoRepository transacaoRepo;

    @Mock
    private HttpService httpService;

    @InjectMocks
    private CarteiraService service;

    @Test
    public void deveAtualizarTransacaoValidaPreservandoIdEData() {
        LocalDateTime dataOriginal =
            LocalDateTime.of(2026, 1, 10, 14, 30);

        Transacao existente = criarTransacao(
            1L,
            dataOriginal,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            " eth ",
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(3000),
            TipoTransacao.COMPRA
        );

        when(transacaoRepo.findById(1L))
            .thenReturn(Optional.of(existente));

        when(httpService.validarTicker("ETH"))
            .thenReturn(true);

        when(transacaoRepo.findAll(any(Sort.class)))
            .thenReturn(List.of(existente));

        when(transacaoRepo.save(same(existente)))
            .thenReturn(existente);

        Transacao resultado =
            service.atualizarTransacao(1L, dados);

        assertSame(existente, resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(dataOriginal, resultado.getData());
        assertEquals("ETH", resultado.getTicker());

        assertEquals(
            0,
            BigDecimal.valueOf(2)
                .compareTo(resultado.getQuantidade())
        );

        assertEquals(
            0,
            BigDecimal.valueOf(3000)
                .compareTo(resultado.getPrecoUnitario())
        );

        assertEquals(
            TipoTransacao.COMPRA,
            resultado.getTipo()
        );

        verify(transacaoRepo).save(same(existente));
    }

    @Test
    public void deveRejeitarAtualizacaoQueTornaHistoricoInconsistente() {
        Transacao compra = criarTransacao(
            1L,
            LocalDateTime.of(2026, 1, 1, 10, 0),
            "BTC",
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        Transacao venda = criarTransacao(
            2L,
            LocalDateTime.of(2026, 1, 1, 11, 0),
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(60000),
            TipoTransacao.VENDA
        );

        TransacaoRequestDTO dadosInvalidos =
            new TransacaoRequestDTO(
                "BTC",
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(50000),
                TipoTransacao.COMPRA
            );

        when(transacaoRepo.findById(1L))
            .thenReturn(Optional.of(compra));

        when(httpService.validarTicker("BTC"))
            .thenReturn(true);

        when(transacaoRepo.findAll(any(Sort.class)))
            .thenReturn(List.of(compra, venda));

        AlteracaoHistoricoInvalidaException excecao =
            assertThrows(
                AlteracaoHistoricoInvalidaException.class,
                () -> service.atualizarTransacao(
                    1L,
                    dadosInvalidos
                )
            );

        assertEquals(
            "A alteração da transação de ID 1 "
                + "tornaria o histórico inconsistente.",
            excecao.getMessage()
        );

        verify(transacaoRepo, never())
            .save(any(Transacao.class));
    }

    @Test
    public void deveRejeitarAtualizacaoDeTransacaoInexistente() {
        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        when(transacaoRepo.findById(999L))
            .thenReturn(Optional.empty());

        TransacaoNaoEncontradaException excecao =
            assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> service.atualizarTransacao(
                    999L,
                    dados
                )
            );

        assertEquals(
            "Transação não encontrada para o ID 999.",
            excecao.getMessage()
        );

        verifyNoInteractions(httpService);

        verify(transacaoRepo, never())
            .findAll(any(Sort.class));

        verify(transacaoRepo, never())
            .save(any(Transacao.class));
    }

    @Test
    public void deveExcluirTransacaoQuandoHistoricoContinuarValido() {
        Transacao compra = criarTransacao(
            1L,
            LocalDateTime.of(2026, 1, 1, 10, 0),
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        Transacao venda = criarTransacao(
            2L,
            LocalDateTime.of(2026, 1, 1, 11, 0),
            "BTC",
            BigDecimal.valueOf(0.25),
            BigDecimal.valueOf(60000),
            TipoTransacao.VENDA
        );

        when(transacaoRepo.findById(2L))
            .thenReturn(Optional.of(venda));

        when(transacaoRepo.findAll(any(Sort.class)))
            .thenReturn(List.of(compra, venda));

        service.excluirTransacao(2L);

        verify(transacaoRepo).delete(same(venda));
    }

    @Test
    public void deveRejeitarExclusaoQueTornaHistoricoInconsistente() {
        Transacao compra = criarTransacao(
            1L,
            LocalDateTime.of(2026, 1, 1, 10, 0),
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        Transacao venda = criarTransacao(
            2L,
            LocalDateTime.of(2026, 1, 1, 11, 0),
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(60000),
            TipoTransacao.VENDA
        );

        when(transacaoRepo.findById(1L))
            .thenReturn(Optional.of(compra));

        when(transacaoRepo.findAll(any(Sort.class)))
            .thenReturn(List.of(compra, venda));

        AlteracaoHistoricoInvalidaException excecao =
            assertThrows(
                AlteracaoHistoricoInvalidaException.class,
                () -> service.excluirTransacao(1L)
            );

        assertEquals(
            "A exclusão da transação de ID 1 "
                + "tornaria o histórico inconsistente.",
            excecao.getMessage()
        );

        verify(transacaoRepo, never())
            .delete(any(Transacao.class));
    }

    @Test
    public void deveRejeitarExclusaoDeTransacaoInexistente() {
        when(transacaoRepo.findById(999L))
            .thenReturn(Optional.empty());

        TransacaoNaoEncontradaException excecao =
            assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> service.excluirTransacao(999L)
            );

        assertEquals(
            "Transação não encontrada para o ID 999.",
            excecao.getMessage()
        );

        verify(transacaoRepo, never())
            .findAll(any(Sort.class));

        verify(transacaoRepo, never())
            .delete(any(Transacao.class));
    }

    private Transacao criarTransacao(
        Long id,
        LocalDateTime data,
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

        transacao.setId(id);
        transacao.setData(data);

        return transacao;
    }
}
