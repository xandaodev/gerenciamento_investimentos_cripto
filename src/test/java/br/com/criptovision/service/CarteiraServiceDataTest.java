package br.com.criptovision.service;

import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.exception.DataTransacaoInvalidaException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/** As bordas da validação da data, com um relógio fixo em 2026-11-27T12:00:00Z. */
@ExtendWith(MockitoExtension.class)
class CarteiraServiceDataTest {

    private static final Instant AGORA = Instant.parse("2026-11-27T12:00:00Z");

    @Mock
    private TransacaoRepository transacaoRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private CotacaoService cotacaoService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final Usuario usuario = new Usuario("alexandre", "hash");
    private CarteiraService service;

    @BeforeEach
    void prepararServico() {
        service = new CarteiraService(
            transacaoRepo, usuarioRepo, cotacaoService, transactionManager,
            Clock.fixed(AGORA, ZoneOffset.UTC)
        );

        lenient().when(usuarioRepo.bloquearPorId(any())).thenReturn(Optional.of(usuario));
        lenient().when(cotacaoService.normalizarTicker(any(String.class)))
            .thenAnswer(chamada -> chamada.<String>getArgument(0));
        lenient().when(transacaoRepo.findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(List.of());
    }

    @Test
    void deveUsarOMomentoDoRelogioQuandoADataNaoForInformada() {
        Transacao registrada = service.registrarNovaTransacao(compraEm(null), usuario);

        assertThat(registrada.getData()).isEqualTo(LocalDateTime.of(2026, 11, 27, 12, 0));
    }

    @Test
    void deveAceitarAteUmMinutoNoFuturoPorDiferencaDeRelogio() {
        LocalDateTime trintaSegundosDepois = LocalDateTime.of(2026, 11, 27, 12, 0, 30);

        Transacao registrada = service.registrarNovaTransacao(compraEm(trintaSegundosDepois), usuario);

        assertThat(registrada.getData()).isEqualTo(trintaSegundosDepois);
    }

    @Test
    void deveRecusarMaisDeUmMinutoNoFuturo() {
        LocalDateTime doisMinutosDepois = LocalDateTime.of(2026, 11, 27, 12, 2);

        assertThatThrownBy(() -> service.registrarNovaTransacao(compraEm(doisMinutosDepois), usuario))
            .isInstanceOf(DataTransacaoInvalidaException.class)
            .hasMessageContaining("futuro");
    }

    @Test
    void deveAceitarODiaDoBlocoGeneseDoBitcoin() {
        LocalDateTime genese = LocalDateTime.of(2009, 1, 3, 0, 0);

        assertThat(service.registrarNovaTransacao(compraEm(genese), usuario).getData())
            .isEqualTo(genese);
    }

    @Test
    void deveRecusarDataAnteriorAoBitcoin() {
        LocalDateTime vespera = LocalDateTime.of(2009, 1, 2, 23, 59, 59);

        assertThatThrownBy(() -> service.registrarNovaTransacao(compraEm(vespera), usuario))
            .isInstanceOf(DataTransacaoInvalidaException.class)
            .hasMessageContaining("2009-01-03");
    }

    @Test
    void deveGuardarADataComPrecisaoDeMicrossegundos() {
        // a coluna DATETIME(6) guarda microssegundos: o valor em memória fica igual ao do banco
        LocalDateTime comNanossegundos = LocalDateTime.of(2025, 3, 10, 14, 0, 0, 123_456_789);

        assertThat(service.registrarNovaTransacao(compraEm(comNanossegundos), usuario).getData())
            .isEqualTo(LocalDateTime.of(2025, 3, 10, 14, 0, 0, 123_456_000));
    }

    private static Transacao compraEm(LocalDateTime data) {
        Transacao compra = new Transacao("BTC", BigDecimal.ONE, BigDecimal.valueOf(50000), TipoTransacao.COMPRA);
        compra.setData(data);
        return compra;
    }
}
