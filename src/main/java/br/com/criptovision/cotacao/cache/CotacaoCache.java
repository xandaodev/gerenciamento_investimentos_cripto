package br.com.criptovision.cotacao.cache;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CotacaoCache {

    private final Map<String, EntradaCache> entradas =
        new ConcurrentHashMap<>();

    private final Duration ttl;
    private final Duration tempoMaximoFallback;
    private final Clock clock;

    public CotacaoCache(
        @Value("${cotacoes.cache.ttl:60s}") Duration ttl,
        @Value("${cotacoes.cache.max-stale:15m}")
        Duration tempoMaximoFallback,
        Clock clock
    ) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException(
                "O TTL das cotações deve ser maior que zero."
            );
        }

        if (tempoMaximoFallback.compareTo(ttl) < 0) {
            throw new IllegalArgumentException(
                "O período de fallback não pode ser menor que o TTL."
            );
        }

        this.ttl = ttl;
        this.tempoMaximoFallback = tempoMaximoFallback;
        this.clock = clock;
    }

    public Optional<CotacaoMercado> buscarAtual(String ticker) {
        EntradaCache entrada = entradas.get(ticker);

        if (entrada == null || idade(entrada).compareTo(ttl) > 0) {
            return Optional.empty();
        }

        return Optional.of(entrada.cotacao());
    }

    public Optional<CotacaoMercado> buscarParaFallback(
        String ticker
    ) {
        EntradaCache entrada = entradas.get(ticker);

        if (entrada == null) {
            return Optional.empty();
        }

        if (idade(entrada).compareTo(tempoMaximoFallback) <= 0) {
            return Optional.of(
                entrada.cotacao().comoDesatualizada()
            );
        }

        entradas.remove(ticker, entrada);
        return Optional.empty();
    }

    public void armazenar(CotacaoMercado cotacao) {
        entradas.put(
            cotacao.ticker(),
            new EntradaCache(cotacao, Instant.now(clock))
        );
    }

    private Duration idade(EntradaCache entrada) {
        return Duration.between(
            entrada.armazenadaEm(),
            Instant.now(clock)
        );
    }

    private record EntradaCache(
        CotacaoMercado cotacao,
        Instant armazenadaEm
    ) {
    }
}
