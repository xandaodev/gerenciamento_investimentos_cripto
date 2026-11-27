package br.com.criptovision.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteDeTentativasTest {

    private RelogioAjustavel relogio;
    private LimiteDeTentativas limite;

    @BeforeEach
    void prepararLimiteDeTresPorMinuto() {
        relogio = new RelogioAjustavel(Instant.parse("2026-11-27T12:00:00Z"));
        limite = new LimiteDeTentativas("teste", 3, Duration.ofMinutes(1), relogio);
    }

    @Test
    void devePermitirAteOMaximoERecusarASeguinte() {
        assertThat(limite.tentar("1.1.1.1")).isEmpty();
        assertThat(limite.tentar("1.1.1.1")).isEmpty();
        assertThat(limite.tentar("1.1.1.1")).isEmpty();

        assertThat(limite.tentar("1.1.1.1")).contains(Duration.ofMinutes(1));
    }

    @Test
    void deveInformarQuantoFaltaParaAJanelaFechar() {
        tentarVezes("1.1.1.1", 3);

        relogio.avancar(Duration.ofSeconds(20));

        assertThat(limite.tentar("1.1.1.1")).contains(Duration.ofSeconds(40));
    }

    @Test
    void naoDeveEstenderOBloqueioComTentativasRecusadas() {
        tentarVezes("1.1.1.1", 3);

        relogio.avancar(Duration.ofSeconds(30));
        limite.tentar("1.1.1.1"); // recusada
        relogio.avancar(Duration.ofSeconds(20));

        // a janela continua fechando 60s depois da primeira tentativa
        assertThat(limite.tentar("1.1.1.1")).contains(Duration.ofSeconds(10));
    }

    @Test
    void deveLiberarQuandoAJanelaFecha() {
        tentarVezes("1.1.1.1", 3);
        assertThat(limite.tentar("1.1.1.1")).isPresent();

        relogio.avancar(Duration.ofMinutes(1));

        assertThat(limite.tentar("1.1.1.1")).isEmpty();
        assertThat(limite.tentar("1.1.1.1")).isEmpty();
        assertThat(limite.tentar("1.1.1.1")).isEmpty();
        assertThat(limite.tentar("1.1.1.1")).isPresent();
    }

    @Test
    void deveContarCadaChaveSeparadamente() {
        tentarVezes("1.1.1.1", 3);

        assertThat(limite.tentar("1.1.1.1")).isPresent();
        assertThat(limite.tentar("2.2.2.2")).isEmpty();
    }

    @Test
    void bloqueioDeveApenasConsultarSemContar() {
        for (int i = 0; i < 10; i++) {
            assertThat(limite.bloqueio("alexandre")).isEmpty();
        }

        assertThat(limite.tentar("alexandre")).isEmpty();
    }

    @Test
    void deveBloquearDepoisDeRegistrarOMaximoDeEventos() {
        limite.registrar("alexandre");
        limite.registrar("alexandre");
        assertThat(limite.bloqueio("alexandre")).isEmpty();

        limite.registrar("alexandre");

        assertThat(limite.bloqueio("alexandre")).contains(Duration.ofMinutes(1));
    }

    @Test
    void limparDeveZerarAContagem() {
        limite.registrar("alexandre");
        limite.registrar("alexandre");
        limite.registrar("alexandre");

        limite.limpar("alexandre");

        assertThat(limite.bloqueio("alexandre")).isEmpty();
    }

    @Test
    void naoDevePermitirMaisQueOMaximoComTentativasSimultaneas() throws Exception {
        LimiteDeTentativas limiteDe50 =
            new LimiteDeTentativas("teste", 50, Duration.ofMinutes(1), relogio);

        int threads = 16;
        int tentativasPorThread = 100;
        AtomicInteger aceitas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    largada.await();
                    for (int i = 0; i < tentativasPorThread; i++) {
                        if (limiteDe50.tentar("1.1.1.1").isEmpty()) {
                            aceitas.incrementAndGet();
                        }
                    }
                    return null;
                });
            }

            largada.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        // 1.600 tentativas ao mesmo tempo, exatamente 50 aceitas
        assertThat(aceitas).hasValue(50);
    }

    private void tentarVezes(String chave, int vezes) {
        for (int i = 0; i < vezes; i++) {
            assertThat(limite.tentar(chave)).isEmpty();
        }
    }

    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        private RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
