package br.com.criptovision.test;

import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.exception.AlteracaoHistoricoInvalidaException;
import br.com.criptovision.exception.TransacaoNaoEncontradaException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.exception.TickerInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

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
    private UsuarioRepository usuarioRepo;

    @Mock
    private CotacaoService cotacaoService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private CarteiraService service;

    @BeforeEach
    public void criarUsuario() {
        usuario = new Usuario(
            "alexandre",
            "senha-criptografada"
        );

        lenient()
            .when(usuarioRepo.bloquearPorId(any()))
            .thenReturn(Optional.of(usuario));

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

        when(transacaoRepo.existsByIdAndUsuario(1L, usuario))
            .thenReturn(true);

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

        when(transacaoRepo.existsByIdAndUsuario(1L, usuario))
            .thenReturn(true);

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

        when(transacaoRepo.existsByIdAndUsuario(999L, usuario))
            .thenReturn(false);

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
        verifyNoInteractions(usuarioRepo);

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

    @Test
    public void deveBloquearHistoricoDepoisDeValidarTickerAoRegistrarTransacao() {
        Transacao novaTransacao = new Transacao(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of());

        service.registrarNovaTransacao(novaTransacao, usuario);

        InOrder ordem = inOrder(
            cotacaoService,
            usuarioRepo,
            transacaoRepo
        );

        ordem.verify(cotacaoService).validarTicker("BTC");
        ordem.verify(usuarioRepo).bloquearPorId(usuario.getId());
        ordem.verify(transacaoRepo)
            .findAllByUsuarioOrderByDataAscIdAsc(usuario);
        ordem.verify(transacaoRepo).save(same(novaTransacao));
    }

    @Test
    public void naoDeveBloquearHistoricoQuandoTickerForInvalido() {
        Transacao novaTransacao = new Transacao(
            "XYZ",
            BigDecimal.ONE,
            BigDecimal.TEN,
            TipoTransacao.COMPRA
        );

        doThrow(new TickerInvalidoException("XYZ"))
            .when(cotacaoService)
            .validarTicker("XYZ");

        assertThrows(
            TickerInvalidoException.class,
            () -> service.registrarNovaTransacao(
                novaTransacao,
                usuario
            )
        );

        verifyNoInteractions(usuarioRepo);
        verify(transacaoRepo, never())
            .save(any(Transacao.class));
    }

    @Test
    public void deveBloquearHistoricoAntesDeCarregarTransacoesAoExcluir() {
        Transacao compra = criarTransacao(
            1L,
            LocalDateTime.of(2026, 1, 1, 10, 0),
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        when(transacaoRepo.findByIdAndUsuario(1L, usuario))
            .thenReturn(Optional.of(compra));

        when(transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of(compra));

        service.excluirTransacao(1L, usuario);

        InOrder ordem = inOrder(usuarioRepo, transacaoRepo);

        ordem.verify(usuarioRepo).bloquearPorId(usuario.getId());
        ordem.verify(transacaoRepo).findByIdAndUsuario(1L, usuario);
        ordem.verify(transacaoRepo).delete(same(compra));
    }

    @Test
    public void deveRetornar404QuandoTransacaoForExcluidaAntesDoBloqueioNaAtualizacao() {
        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        // existia na verificação inicial, mas outra requisição a excluiu
        // antes de a atualização obter o bloqueio
        when(transacaoRepo.existsByIdAndUsuario(1L, usuario))
            .thenReturn(true);

        when(transacaoRepo.findByIdAndUsuario(1L, usuario))
            .thenReturn(Optional.empty());

        assertThrows(
            TransacaoNaoEncontradaException.class,
            () -> service.atualizarTransacao(1L, dados, usuario)
        );

        verify(usuarioRepo).bloquearPorId(usuario.getId());
        verify(transacaoRepo, never())
            .save(any(Transacao.class));
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
