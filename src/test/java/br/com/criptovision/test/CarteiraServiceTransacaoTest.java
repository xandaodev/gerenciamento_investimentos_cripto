package br.com.criptovision.test;

import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.exception.AlteracaoHistoricoInvalidaException;
import br.com.criptovision.exception.TransacaoNaoEncontradaException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.cotacao.service.CotacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CarteiraServiceTransacaoTest {

    private Usuario usuario;

    @Mock
    private TransacaoRepository transacaoRepo;

    @Mock
    private CotacaoService cotacaoService;

    @InjectMocks
    private CarteiraService service;

    @BeforeEach
    public void criarUsuario() {
        usuario = new Usuario(
            "alexandre",
            "senha-criptografada"
        );

        lenient()
            .when(cotacaoService.normalizarTicker(any(String.class)))
            .thenAnswer(invocacao ->
                invocacao.<String>getArgument(0)
                    .trim()
                    .toUpperCase()
            );
    }

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

        when(transacaoRepo.findByIdAndUsuario(1L, usuario))
            .thenReturn(Optional.of(existente));

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of(existente));

        when(transacaoRepo.save(same(existente)))
            .thenReturn(existente);

        Transacao resultado =
            service.atualizarTransacao(1L, dados, usuario);

        assertSame(existente, resultado);

        assertSame(
            usuario,
            resultado.getUsuario()
        );

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

        when(transacaoRepo.findByIdAndUsuario(1L, usuario))
            .thenReturn(Optional.of(compra));

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of(compra, venda));

        AlteracaoHistoricoInvalidaException excecao =
            assertThrows(
                AlteracaoHistoricoInvalidaException.class,
                () -> service.atualizarTransacao(
                    1L,
                    dadosInvalidos, usuario
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

        when(transacaoRepo.findByIdAndUsuario(999L, usuario))
            .thenReturn(Optional.empty());

        TransacaoNaoEncontradaException excecao =
            assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> service.atualizarTransacao(
                    999L,
                    dados, usuario
                )
            );

        assertEquals(
            "Transação não encontrada para o ID 999.",
            excecao.getMessage()
        );

        verifyNoInteractions(cotacaoService);

        verify(transacaoRepo, never())
            .findAllByUsuarioOrderByDataAscIdAsc(
                any(Usuario.class)
            );

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

        when(transacaoRepo.findByIdAndUsuario(2L, usuario))
            .thenReturn(Optional.of(venda));

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of(compra, venda));

        service.excluirTransacao(2L, usuario);

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

        when(transacaoRepo.findByIdAndUsuario(1L, usuario))
            .thenReturn(Optional.of(compra));

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of(compra, venda));

        AlteracaoHistoricoInvalidaException excecao =
            assertThrows(
                AlteracaoHistoricoInvalidaException.class,
                () -> service.excluirTransacao(1L, usuario)
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
        when(transacaoRepo.findByIdAndUsuario(999L, usuario))
            .thenReturn(Optional.empty());

        TransacaoNaoEncontradaException excecao =
            assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> service.excluirTransacao(999L, usuario)
            );

        assertEquals(
            "Transação não encontrada para o ID 999.",
            excecao.getMessage()
        );

        verify(transacaoRepo, never())
            .findAllByUsuarioOrderByDataAscIdAsc(
                any(Usuario.class)
            );

        verify(transacaoRepo, never())
            .delete(any(Transacao.class));
    }

    @Test
    public void deveAssociarUsuarioAutenticadoAoRegistrarTransacao() {
        Transacao novaTransacao = new Transacao(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of());

        Transacao resultado =
            service.registrarNovaTransacao(
                novaTransacao,
                usuario
            );

        assertSame(novaTransacao, resultado);
        assertSame(usuario, resultado.getUsuario());

        verify(transacaoRepo)
            .findAllByUsuarioOrderByDataAscIdAsc(
                same(usuario)
            );

        verify(transacaoRepo)
            .save(same(novaTransacao));
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
        transacao.setUsuario(usuario);

        return transacao;
    }
}
