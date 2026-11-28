package br.com.criptovision.config;

import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class CotacaoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CotacaoConfiguration.class);

    /** Nome do circuit breaker, que aparece nas métricas (tag {@code name}). */
    public static final String CIRCUIT_BREAKER_BINANCE = "binance";

    // as últimas 10 chamadas decidem, a partir de 5: metade com falha (ou lenta)
    // abre o circuito. Fechado de novo depois de 2 chamadas de teste bem-sucedidas
    private static final int JANELA_DE_CHAMADAS = 10;
    private static final int MINIMO_DE_CHAMADAS = 5;
    private static final float PERCENTUAL_PARA_ABRIR = 50;
    private static final int CHAMADAS_DE_TESTE = 2;

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CircuitBreaker circuitBreakerDaBinance(
        @Value("${cotacoes.circuit-breaker.espera-aberto:30s}") Duration esperaAberto,
        @Value("${cotacoes.circuit-breaker.chamada-lenta:3s}") Duration chamadaLenta,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        return criarCircuitBreakerDaBinance(esperaAberto, chamadaLenta, clock, meterRegistry);
    }

    /**
     * Com o circuito aberto, as consultas à Binance falham na hora, sem esperar os
     * timeouts, e o resumo usa as cotações anteriores. Depois de {@code esperaAberto},
     * algumas chamadas de teste decidem se ele fecha. Só a indisponibilidade conta
     * como falha: um ticker inválido (400) é uma resposta normal do provedor.
     *
     * <p>Público e estático para que os testes usem exatamente esta configuração.
     */
    public static CircuitBreaker criarCircuitBreakerDaBinance(
        Duration esperaAberto,
        Duration chamadaLenta,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        CircuitBreakerConfig configuracao = CircuitBreakerConfig.custom()
            .slidingWindow(JANELA_DE_CHAMADAS, MINIMO_DE_CHAMADAS, SlidingWindowType.COUNT_BASED)
            .failureRateThreshold(PERCENTUAL_PARA_ABRIR)
            .slowCallDurationThreshold(chamadaLenta)
            .slowCallRateThreshold(PERCENTUAL_PARA_ABRIR)
            .waitDurationInOpenState(esperaAberto)
            .permittedNumberOfCallsInHalfOpenState(CHAMADAS_DE_TESTE)
            .recordExceptions(ServicoCotacaoIndisponivelException.class)
            .ignoreExceptions(TickerInvalidoException.class)
            .clock(clock)
            .build();

        CircuitBreakerRegistry registro = CircuitBreakerRegistry.of(configuracao);
        CircuitBreaker circuitBreaker = registro.circuitBreaker(CIRCUIT_BREAKER_BINANCE);

        circuitBreaker.getEventPublisher().onStateTransition(evento -> {
            switch (evento.getStateTransition().getToState()) {
                case OPEN -> LOGGER.warn(
                    "Circuit breaker da Binance aberto: as consultas falham na hora por {} e o resumo usa as cotações anteriores.",
                    esperaAberto
                );
                case HALF_OPEN -> LOGGER.info("Circuit breaker da Binance testando o provedor de novo.");
                case CLOSED -> LOGGER.info("Circuit breaker da Binance fechado: o provedor voltou.");
                default -> LOGGER.info("Circuit breaker da Binance: {}.", evento.getStateTransition());
            }
        });

        // resilience4j.circuitbreaker.state, .calls, .failure.rate etc. no Actuator
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registro).bindTo(meterRegistry);

        return circuitBreaker;
    }
}
