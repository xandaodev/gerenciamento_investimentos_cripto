package br.com.criptovision.cotacao;

import br.com.criptovision.cotacao.cache.CotacaoCache;
import br.com.criptovision.cotacao.client.CotacaoClient;
import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.model.ResultadoCotacoes;
import br.com.criptovision.config.CotacaoConfiguration;
import br.com.criptovision.cotacao.service.ConsultaDeCotacoes;
import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import br.com.criptovision.observabilidade.MetricasCriptoVision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotacaoServiceTest {

    @Mock
    private CotacaoClient cotacaoClient;

    private MutableClock clock;
    private CotacaoService service;
    private SimpleMeterRegistry registro;

    @BeforeEach
    void preparar() {
        clock = new MutableClock(
            Instant.parse("2026-08-06T14:00:00Z")
        );

        CotacaoCache cache = new CotacaoCache(
            Duration.ofMinutes(1),
            Duration.ofMinutes(15),
            clock
        );

        registro = new SimpleMeterRegistry();

        ConsultaDeCotacoes consulta = new ConsultaDeCotacoes(
            cotacaoClient,
            CotacaoConfiguration.criarCircuitBreakerDaBinance(
                Duration.ofSeconds(30),
                Duration.ofSeconds(3),
                clock,
                registro
            ),
            4,
            Duration.ofSeconds(5)
        );

        service = new CotacaoService(
            consulta,
            cache,
            clock,
            new MetricasCriptoVision(registro)
        );
    }

    @Test
    void deveConsultarEmLoteEReutilizarCache() {
        Set<String> tickers = Set.of("BTC", "ETH");

        when(cotacaoClient.buscarCotacoes(tickers))
            .thenReturn(Map.of(
                "BTC", cotacao("BTC", "65000", "2.5"),
                "ETH", cotacao("ETH", "3500", "-1.2")
            ));

        ResultadoCotacoes primeira =
            service.buscarCotacoes(tickers);
        ResultadoCotacoes segunda =
            service.buscarCotacoes(tickers);

        assertEquals(2, primeira.cotacoes().size());
        assertEquals(2, segunda.cotacoes().size());
        assertFalse(segunda.possuiDadosParciais());

        verify(cotacaoClient, times(1))
            .buscarCotacoes(tickers);
    }

    @Test
    void deveUsarCotacaoAnteriorQuandoProvedorFalhar() {
        Set<String> tickers = Set.of("BTC");

        // um ticker só vai pela consulta individual
        when(cotacaoClient.buscarCotacao("BTC"))
            .thenReturn(cotacao("BTC", "65000", "2.5"))
            .thenThrow(
                new ServicoCotacaoIndisponivelException()
            );

        service.buscarCotacoes(tickers);
        clock.avancar(Duration.ofMinutes(2));

        ResultadoCotacoes resultado =
            service.buscarCotacoes(tickers);

        CotacaoMercado cotacao = resultado
            .obter("BTC")
            .orElseThrow();

        assertTrue(cotacao.desatualizada());
        assertTrue(resultado.possuiDadosParciais());
        assertTrue(resultado.indisponiveis().isEmpty());
        assertEquals(1, falhasDoProvedor("cotacao_anterior"));
        assertEquals(0, falhasDoProvedor("sem_cotacao"));
    }

    @Test
    void deveContarAtivoSemCotacaoQuandoProvedorFalharSemCotacaoAnterior() {
        Set<String> tickers = Set.of("BTC", "ETH");

        when(cotacaoClient.buscarCotacoes(tickers))
            .thenThrow(new ServicoCotacaoIndisponivelException());

        ResultadoCotacoes resultado =
            service.buscarCotacoes(tickers);

        assertEquals(Set.of("BTC", "ETH"), resultado.indisponiveis());
        assertEquals(2, falhasDoProvedor("sem_cotacao"));
        assertEquals(0, falhasDoProvedor("cotacao_anterior"));
    }

    @Test
    void naoDeveContarTickerInvalidoComoFalhaDoProvedor() {
        when(cotacaoClient.buscarCotacao("XYZ"))
            .thenThrow(new TickerInvalidoException("XYZ"));

        assertThrows(
            TickerInvalidoException.class,
            () -> service.buscarCotacao("XYZ")
        );

        assertEquals(0, falhasDoProvedor("sem_cotacao"));
        assertEquals(0, falhasDoProvedor("cotacao_anterior"));
    }

    @Test
    void deveContarFalhaDoProvedorNaConsultaDeUmTicker() {
        when(cotacaoClient.buscarCotacao("BTC"))
            .thenReturn(cotacao("BTC", "65000", "2.5"))
            .thenThrow(new ServicoCotacaoIndisponivelException());
        when(cotacaoClient.buscarCotacao("ETH"))
            .thenThrow(new ServicoCotacaoIndisponivelException());

        service.buscarCotacao("BTC");
        clock.avancar(Duration.ofMinutes(2));

        assertTrue(service.buscarCotacao("BTC").desatualizada());
        assertThrows(
            ServicoCotacaoIndisponivelException.class,
            () -> service.buscarCotacao("ETH")
        );

        assertEquals(1, falhasDoProvedor("cotacao_anterior"));
        assertEquals(1, falhasDoProvedor("sem_cotacao"));
    }

    @Test
    void devePreservarAtivosValidosQuandoUmTickerFalhar() {
        Set<String> tickers = Set.of("BTC", "XYZ");

        when(cotacaoClient.buscarCotacoes(tickers))
            .thenThrow(new TickerInvalidoException("XYZ"));

        when(cotacaoClient.buscarCotacao("BTC"))
            .thenReturn(cotacao("BTC", "65000", "2.5"));

        when(cotacaoClient.buscarCotacao("XYZ"))
            .thenThrow(new TickerInvalidoException("XYZ"));

        ResultadoCotacoes resultado =
            service.buscarCotacoes(tickers);

        assertTrue(resultado.obter("BTC").isPresent());
        assertTrue(resultado.obter("XYZ").isEmpty());
        assertEquals(Set.of("XYZ"), resultado.indisponiveis());
        assertTrue(resultado.possuiDadosParciais());
    }


    @Test
    void deveValidarTickerConhecidoComFallbackDuranteIndisponibilidade() {
        when(cotacaoClient.buscarCotacao("BTC"))
            .thenReturn(cotacao("BTC", "65000", "2.5"))
            .thenThrow(
                new ServicoCotacaoIndisponivelException()
            );

        service.validarTicker("BTC");
        clock.avancar(Duration.ofMinutes(2));

        assertDoesNotThrow(() -> service.validarTicker("BTC"));

        verify(cotacaoClient, times(2))
            .buscarCotacao("BTC");
        assertEquals(1, falhasDoProvedor("cotacao_anterior"));
    }

    @Test
    void deveDiferenciarIndisponibilidadeDeTickerInvalido() {
        when(cotacaoClient.buscarCotacao("BTC"))
            .thenThrow(
                new ServicoCotacaoIndisponivelException()
            );

        assertThrows(
            ServicoCotacaoIndisponivelException.class,
            () -> service.validarTicker("BTC")
        );
        assertEquals(1, falhasDoProvedor("sem_cotacao"));
    }

    private double falhasDoProvedor(String resultado) {
        Counter contador = registro
            .find(MetricasCriptoVision.PROVEDOR_DE_COTACOES_INDISPONIVEL)
            .tag("resultado", resultado)
            .counter();

        return contador == null ? 0 : contador.count();
    }

    private CotacaoMercado cotacao(
        String ticker,
        String preco,
        String variacao
    ) {
        return new CotacaoMercado(
            ticker,
            new BigDecimal(preco),
            new BigDecimal(variacao),
            clock.instant(),
            false
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void avancar(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
