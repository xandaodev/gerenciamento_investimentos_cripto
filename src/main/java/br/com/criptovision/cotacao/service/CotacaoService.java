package br.com.criptovision.cotacao.service;

import br.com.criptovision.cotacao.cache.CotacaoCache;
import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.model.ResultadoCotacoes;
import br.com.criptovision.cotacao.util.TickerNormalizer;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import br.com.criptovision.observabilidade.MetricasCriptoVision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A política das cotações: cache fresco, USDT fixo em 1 e, com o provedor fora do
 * ar, a última cotação conhecida (marcada como desatualizada) em vez de um preço
 * zerado. A consulta ao provedor, com o circuit breaker, fica na
 * {@link ConsultaDeCotacoes}.
 */
@Service
public class CotacaoService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CotacaoService.class);

    private final ConsultaDeCotacoes consulta;
    private final CotacaoCache cotacaoCache;
    private final Clock clock;
    private final MetricasCriptoVision metricas;

    public CotacaoService(
        ConsultaDeCotacoes consulta,
        CotacaoCache cotacaoCache,
        Clock clock,
        MetricasCriptoVision metricas
    ) {
        this.consulta = consulta;
        this.cotacaoCache = cotacaoCache;
        this.clock = clock;
        this.metricas = metricas;
    }

    /**
     * Cotações para o resumo: um ticker sem cotação não derruba a resposta, só
     * entra em {@code indisponiveis}.
     */
    public ResultadoCotacoes buscarCotacoes(
        Collection<String> tickers
    ) {
        Set<String> normalizados = normalizarTickers(tickers);
        Map<String, CotacaoMercado> encontrados =
            new LinkedHashMap<>();
        Set<String> pendentes = new LinkedHashSet<>();
        Set<String> indisponiveis = new LinkedHashSet<>();

        for (String ticker : normalizados) {
            if (ticker.equals("USDT")) {
                encontrados.put(ticker, cotacaoEstavel());
                continue;
            }

            cotacaoCache.buscarAtual(ticker)
                .ifPresentOrElse(
                    cotacao -> encontrados.put(ticker, cotacao),
                    () -> pendentes.add(ticker)
                );
        }

        if (!pendentes.isEmpty()) {
            ConsultaDeCotacoes.Resposta resposta =
                consulta.consultar(pendentes);

            resposta.encontradas().values().forEach(cotacao -> {
                cotacaoCache.armazenar(cotacao);
                encontrados.put(cotacao.ticker(), cotacao);
            });

            indisponiveis.addAll(resposta.invalidos());

            for (String ticker : resposta.indisponiveis()) {
                cotacaoCache.buscarParaFallback(ticker)
                    .ifPresentOrElse(
                        cotacao -> {
                            encontrados.put(ticker, cotacao);
                            LOGGER.warn(
                                "Cotação anterior utilizada como fallback para {}.",
                                ticker
                            );
                            metricas.cotacaoAnteriorUsada();
                        },
                        () -> {
                            indisponiveis.add(ticker);
                            metricas.semCotacao();
                        }
                    );
            }
        }

        return new ResultadoCotacoes(
            encontrados,
            indisponiveis
        );
    }

    /**
     * A cotação de um ticker. Lança {@link TickerInvalidoException} (400) se ele não
     * existir no provedor e {@link ServicoCotacaoIndisponivelException} (503) se o
     * provedor estiver fora do ar e não houver cotação anterior utilizável.
     */
    public CotacaoMercado buscarCotacao(String ticker) {
        String normalizado = normalizarTicker(ticker);

        if (normalizado.equals("USDT")) {
            return cotacaoEstavel();
        }

        return cotacaoCache.buscarAtual(normalizado)
            .orElseGet(() -> consultarComFallback(normalizado));
    }

    /**
     * Confirma que o ticker existe antes de gravar uma transação, com as mesmas
     * regras de {@link #buscarCotacao}: com o provedor fora do ar, um ticker que
     * já teve cotação recente é aceito.
     */
    public void validarTicker(String ticker) {
        buscarCotacao(ticker);
    }

    public String normalizarTicker(String ticker) {
        return TickerNormalizer.normalizar(ticker);
    }

    private CotacaoMercado consultarComFallback(String ticker) {
        ConsultaDeCotacoes.Resposta resposta =
            consulta.consultar(Set.of(ticker));

        CotacaoMercado encontrada = resposta.encontradas().get(ticker);
        if (encontrada != null) {
            cotacaoCache.armazenar(encontrada);
            return encontrada;
        }

        if (resposta.invalidos().contains(ticker)) {
            throw new TickerInvalidoException(ticker);
        }

        return cotacaoCache.buscarParaFallback(ticker)
            .map(cotacao -> {
                LOGGER.warn(
                    "Cotação anterior utilizada como fallback para {}.",
                    ticker
                );
                metricas.cotacaoAnteriorUsada();
                return cotacao;
            })
            .orElseThrow(() -> {
                metricas.semCotacao();
                return new ServicoCotacaoIndisponivelException();
            });
    }

    private Set<String> normalizarTickers(
        Collection<String> tickers
    ) {
        Set<String> normalizados = new LinkedHashSet<>();

        for (String ticker : tickers) {
            normalizados.add(normalizarTicker(ticker));
        }

        return normalizados;
    }

    private CotacaoMercado cotacaoEstavel() {
        return new CotacaoMercado(
            "USDT",
            BigDecimal.ONE,
            BigDecimal.ZERO,
            Instant.now(clock),
            false
        );
    }
}
