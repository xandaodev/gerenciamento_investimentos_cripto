package br.com.criptovision.cotacao;

import br.com.criptovision.config.CotacaoConfiguration;
import br.com.criptovision.cotacao.client.CotacaoClient;
import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.service.ConsultaDeCotacoes;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * As garantias da consulta ao provedor. A simultaneidade é verificada com
 * barreiras e travas, e não com tempos medidos, para o teste não depender da
 * velocidade da máquina.
 */
class ConsultaDeCotacoesTest {

    private ProvedorFalso provedor;
    private RelogioAjustavel relogio;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void preparar() {
        provedor = new ProvedorFalso();
        relogio = new RelogioAjustavel(Instant.parse("2026-11-27T12:00:00Z"));
        circuitBreaker = CotacaoConfiguration.criarCircuitBreakerDaBinance(
            Duration.ofSeconds(30),
            Duration.ofSeconds(3),
            relogio,
            new SimpleMeterRegistry()
        );
    }

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    private ConsultaDeCotacoes consulta(int paralelismo, Duration tempoMaximo) {
        return new ConsultaDeCotacoes(provedor, circuitBreaker, paralelismo, tempoMaximo);
    }

    private ConsultaDeCotacoes consulta() {
        return consulta(4, Duration.ofSeconds(5));
    }

    // --- uma consulta por ticker ------------------------------------------------

    @Test
    void requisicoesSimultaneasDoMesmoTickerDevemFazerUmaConsultaSo() throws Exception {
        ConsultaDeCotacoes consulta = consulta();
        CountDownLatch liberar = new CountDownLatch(1);
        provedor.individual.put("BTC", ticker -> {
            aguardar(liberar);
            return cotacao(ticker);
        });

        Execucao primeira = executar(() -> consulta.consultar(Set.of("BTC")));
        esperarAte(() -> provedor.individuais.size() == 1);

        Execucao segunda = executar(() -> consulta.consultar(Set.of("BTC")));
        esperarAte(segunda::esperando);

        liberar.countDown();

        assertThat(primeira.resposta().encontradas()).containsKey("BTC");
        assertThat(segunda.resposta().encontradas()).containsKey("BTC");
        // sem a coalescência, cada requisição iria à Binance quando o cache expirasse
        assertThat(provedor.individuais).containsExactly("BTC");
    }

    @Test
    void loteQueSeSobrepoeAOutroEmAndamentoDeveBuscarSoOQueFalta() throws Exception {
        ConsultaDeCotacoes consulta = consulta();
        CountDownLatch liberar = new CountDownLatch(1);
        provedor.lote = tickers -> {
            aguardar(liberar);
            return cotacoes(tickers);
        };

        Execucao primeira = executar(() -> consulta.consultar(ordenado("BTC", "ETH")));
        esperarAte(() -> provedor.lotes.size() == 1);

        Execucao segunda = executar(() -> consulta.consultar(ordenado("BTC", "SOL")));
        esperarAte(segunda::esperando);

        liberar.countDown();

        assertThat(primeira.resposta().encontradas()).containsOnlyKeys("BTC", "ETH");
        assertThat(segunda.resposta().encontradas()).containsOnlyKeys("BTC", "SOL");
        assertThat(provedor.lotes).containsExactly(Set.of("BTC", "ETH"));
        // o BTC veio do lote da primeira requisição; só o SOL foi buscado de novo
        assertThat(provedor.individuais).containsExactly("SOL");
    }

    @Test
    void umaFalhaInesperadaNaoDeveDeixarQuemEsperaTravado() throws Exception {
        ConsultaDeCotacoes consulta = consulta();
        CountDownLatch liberar = new CountDownLatch(1);
        provedor.individual.put("BTC", ticker -> {
            aguardar(liberar);
            throw new IllegalStateException("bug simulado");
        });

        Execucao primeira = executar(() -> consulta.consultar(Set.of("BTC")));
        esperarAte(() -> provedor.individuais.size() == 1);
        Execucao segunda = executar(() -> consulta.consultar(Set.of("BTC")));
        esperarAte(segunda::esperando);

        liberar.countDown();

        // o erro inesperado sobe para as duas (vira um 500), e ninguém fica preso
        assertThat(primeira.erro()).isInstanceOf(IllegalStateException.class);
        assertThat(segunda.erro()).isInstanceOf(IllegalStateException.class);

        // e o ticker não fica marcado como "em andamento" para sempre
        provedor.individual.remove("BTC");
        assertThat(consulta.consultar(Set.of("BTC")).encontradas()).containsKey("BTC");
    }

    // --- consulta individual em paralelo e com prazo ----------------------------

    @Test
    void loteComTickerInvalidoDeveConsultarOsTickersEmParalelo() {
        // só passa se as 4 consultas estiverem em andamento ao mesmo tempo
        CyclicBarrier todasJuntas = new CyclicBarrier(4);
        provedor.lote = tickers -> {
            throw new TickerInvalidoException("XYZ");
        };
        provedor.padrao = ticker -> {
            try {
                todasJuntas.await(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("as consultas não rodaram em paralelo", ex);
            }
            if (ticker.equals("XYZ")) {
                throw new TickerInvalidoException(ticker);
            }
            return cotacao(ticker);
        };

        ConsultaDeCotacoes.Resposta resposta =
            consulta(4, Duration.ofSeconds(10)).consultar(ordenado("BTC", "ETH", "SOL", "XYZ"));

        assertThat(resposta.encontradas()).containsOnlyKeys("BTC", "ETH", "SOL");
        assertThat(resposta.invalidos()).containsExactly("XYZ");
        assertThat(resposta.indisponiveis()).isEmpty();
    }

    @Test
    void consultaIndividualDeveRespeitarOLimiteDeParalelismo() {
        AtomicInteger simultaneas = new AtomicInteger();
        AtomicInteger maximo = new AtomicInteger();
        provedor.lote = tickers -> {
            throw new TickerInvalidoException("XYZ");
        };
        provedor.padrao = ticker -> {
            maximo.accumulateAndGet(simultaneas.incrementAndGet(), Math::max);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                simultaneas.decrementAndGet();
            }
            return cotacao(ticker);
        };

        ConsultaDeCotacoes.Resposta resposta =
            consulta(2, Duration.ofSeconds(10)).consultar(ordenado("A", "B", "C", "D", "E", "F"));

        assertThat(resposta.encontradas()).hasSize(6);
        // a Binance limita as requisições por IP: nada de disparar todas de uma vez
        assertThat(maximo.get()).isLessThanOrEqualTo(2);
    }

    @Test
    void tickerQueNaoRespondeNoPrazoDeveFicarIndisponivelSemSegurarOsOutros() {
        provedor.lote = tickers -> {
            throw new TickerInvalidoException("XYZ");
        };
        provedor.individual.put("LENTO", ticker -> {
            try {
                Thread.sleep(Duration.ofMinutes(1));
            } catch (InterruptedException ex) {
                // a consulta é interrompida quando o prazo acaba
                Thread.currentThread().interrupt();
            }
            throw new ServicoCotacaoIndisponivelException();
        });

        long inicio = System.nanoTime();
        ConsultaDeCotacoes.Resposta resposta =
            consulta(4, Duration.ofMillis(300)).consultar(ordenado("BTC", "LENTO", "ETH"));
        Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(resposta.encontradas()).containsOnlyKeys("BTC", "ETH");
        assertThat(resposta.indisponiveis()).containsExactly("LENTO");
        assertThat(duracao).isLessThan(Duration.ofSeconds(10));
    }

    @Test
    void consultasParalelasDevemManterOIdDaRequisicaoNosLogs() {
        Map<String, String> idPorTicker = new ConcurrentHashMap<>();
        provedor.lote = tickers -> {
            throw new TickerInvalidoException("XYZ");
        };
        provedor.padrao = ticker -> {
            idPorTicker.put(ticker, String.valueOf(MDC.get("requestId")));
            return cotacao(ticker);
        };

        MDC.put("requestId", "req-123");
        consulta().consultar(ordenado("BTC", "ETH"));

        assertThat(idPorTicker).containsOnly(
            Map.entry("BTC", "req-123"),
            Map.entry("ETH", "req-123")
        );
    }

    // --- circuit breaker --------------------------------------------------------

    @Test
    void falhasSeguidasDevemAbrirOCircuitoEPararDeChamarOProvedor() {
        ConsultaDeCotacoes consulta = consulta();
        provedor.padrao = ticker -> {
            throw new ServicoCotacaoIndisponivelException();
        };

        for (int i = 0; i < 5; i++) {
            assertThat(consulta.consultar(Set.of("BTC")).indisponiveis()).containsExactly("BTC");
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // com o circuito aberto, a resposta vem na hora, sem chamar a Binance
        assertThat(consulta.consultar(Set.of("ETH")).indisponiveis()).containsExactly("ETH");
        assertThat(provedor.individuais).hasSize(5).doesNotContain("ETH");
    }

    @Test
    void tickerInvalidoNaoDeveAbrirOCircuito() {
        ConsultaDeCotacoes consulta = consulta();
        provedor.padrao = ticker -> {
            throw new TickerInvalidoException(ticker);
        };

        for (int i = 0; i < 10; i++) {
            assertThat(consulta.consultar(Set.of("XYZ" + i)).invalidos()).hasSize(1);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(provedor.individuais).hasSize(10);
    }

    @Test
    void circuitoDeveFecharDepoisDaEsperaQuandoOProvedorVolta() {
        ConsultaDeCotacoes consulta = consulta();
        AtomicReference<Boolean> foraDoAr = new AtomicReference<>(true);
        provedor.padrao = ticker -> {
            if (foraDoAr.get()) {
                throw new ServicoCotacaoIndisponivelException();
            }
            return cotacao(ticker);
        };

        for (int i = 0; i < 5; i++) {
            consulta.consultar(Set.of("BTC"));
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        foraDoAr.set(false);
        relogio.avancar(Duration.ofSeconds(29));
        assertThat(consulta.consultar(Set.of("BTC")).indisponiveis()).containsExactly("BTC");

        // passados os 30s, duas chamadas de teste bem-sucedidas fecham o circuito
        relogio.avancar(Duration.ofSeconds(2));
        assertThat(consulta.consultar(Set.of("BTC")).encontradas()).containsKey("BTC");
        assertThat(consulta.consultar(Set.of("ETH")).encontradas()).containsKey("ETH");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void loteIndisponivelDeveDeixarTodosOsTickersIndisponiveis() {
        provedor.lote = tickers -> {
            throw new ServicoCotacaoIndisponivelException();
        };

        ConsultaDeCotacoes.Resposta resposta = consulta().consultar(ordenado("BTC", "ETH"));

        assertThat(resposta.indisponiveis()).containsExactly("BTC", "ETH");
        // sem cair para N consultas individuais, que também falhariam
        assertThat(provedor.individuais).isEmpty();
    }

    @Test
    void deveRecusarConfiguracaoInvalida() {
        assertThatThrownBy(() -> consulta(0, Duration.ofSeconds(5)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consulta(4, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- apoio ------------------------------------------------------------------

    private static Set<String> ordenado(String... tickers) {
        return new LinkedHashSet<>(List.of(tickers));
    }

    private static CotacaoMercado cotacao(String ticker) {
        return new CotacaoMercado(
            ticker,
            new BigDecimal("100"),
            BigDecimal.ONE,
            Instant.parse("2026-11-27T12:00:00Z"),
            false
        );
    }

    private static Map<String, CotacaoMercado> cotacoes(Set<String> tickers) {
        Map<String, CotacaoMercado> cotacoes = new LinkedHashMap<>();
        tickers.forEach(ticker -> cotacoes.put(ticker, cotacao(ticker)));
        return cotacoes;
    }

    private static void aguardar(CountDownLatch trava) {
        try {
            if (!trava.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("o teste não liberou a consulta");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static void esperarAte(BooleanSupplier condicao) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condicao.getAsBoolean()) {
            if (System.nanoTime() > limite) {
                fail("a condição esperada não aconteceu em 10 segundos");
            }
            Thread.sleep(5);
        }
    }

    private static Execucao executar(java.util.function.Supplier<ConsultaDeCotacoes.Resposta> acao) {
        Execucao execucao = new Execucao(acao);
        execucao.thread.start();
        return execucao;
    }

    /** Uma consulta rodando em outra thread, para simular requisições simultâneas. */
    private static final class Execucao {

        private final AtomicReference<ConsultaDeCotacoes.Resposta> resposta = new AtomicReference<>();
        private final AtomicReference<Throwable> erro = new AtomicReference<>();
        private final Thread thread;

        private Execucao(java.util.function.Supplier<ConsultaDeCotacoes.Resposta> acao) {
            this.thread = new Thread(() -> {
                try {
                    resposta.set(acao.get());
                } catch (Throwable ex) {
                    erro.set(ex);
                }
            });
        }

        /** Parada esperando a consulta de outra requisição terminar. */
        boolean esperando() {
            Thread.State estado = thread.getState();
            return estado == Thread.State.WAITING || estado == Thread.State.TIMED_WAITING;
        }

        ConsultaDeCotacoes.Resposta resposta() throws InterruptedException {
            terminar();
            if (erro.get() != null) {
                throw new AssertionError("a consulta falhou", erro.get());
            }
            return resposta.get();
        }

        Throwable erro() throws InterruptedException {
            terminar();
            return erro.get();
        }

        private void terminar() throws InterruptedException {
            thread.join(Duration.ofSeconds(15));
            if (thread.isAlive()) {
                fail("a consulta ficou travada");
            }
        }
    }

    /** Provedor falso: cada ticker responde como o teste mandar, e as chamadas ficam registradas. */
    private static final class ProvedorFalso implements CotacaoClient {

        final Map<String, Function<String, CotacaoMercado>> individual = new ConcurrentHashMap<>();
        volatile Function<String, CotacaoMercado> padrao = ConsultaDeCotacoesTest::cotacao;
        volatile Function<Set<String>, Map<String, CotacaoMercado>> lote = ConsultaDeCotacoesTest::cotacoes;

        final List<Set<String>> lotes = Collections.synchronizedList(new ArrayList<>());
        final List<String> individuais = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Map<String, CotacaoMercado> buscarCotacoes(Set<String> tickers) {
            lotes.add(Set.copyOf(tickers));
            return lote.apply(tickers);
        }

        @Override
        public CotacaoMercado buscarCotacao(String ticker) {
            individuais.add(ticker);
            return individual.getOrDefault(ticker, padrao).apply(ticker);
        }
    }

    private static final class RelogioAjustavel extends Clock {

        private volatile Instant agora;

        private RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
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
            return agora;
        }
    }
}
