package br.com.criptovision.cotacao.service;

import br.com.criptovision.cotacao.cache.CotacaoCache;
import br.com.criptovision.cotacao.client.CotacaoClient;
import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.model.ResultadoCotacoes;
import br.com.criptovision.cotacao.util.TickerNormalizer;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
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

@Service
public class CotacaoService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CotacaoService.class);

    private final CotacaoClient cotacaoClient;
    private final CotacaoCache cotacaoCache;
    private final Clock clock;

    public CotacaoService(
        CotacaoClient cotacaoClient,
        CotacaoCache cotacaoCache,
        Clock clock
    ) {
        this.cotacaoClient = cotacaoClient;
        this.cotacaoCache = cotacaoCache;
        this.clock = clock;
    }

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
            buscarPendentes(
                pendentes,
                encontrados,
                indisponiveis
            );
        }

        return new ResultadoCotacoes(
            encontrados,
            indisponiveis
        );
    }

    public CotacaoMercado buscarCotacao(String ticker) {
        String normalizado = normalizarTicker(ticker);

        if (normalizado.equals("USDT")) {
            return cotacaoEstavel();
        }

        return cotacaoCache.buscarAtual(normalizado)
            .orElseGet(() -> consultarComFallback(normalizado));
    }

    public void validarTicker(String ticker) {
        String normalizado = normalizarTicker(ticker);

        if (normalizado.equals("USDT")
            || cotacaoCache.buscarAtual(normalizado).isPresent()) {
            return;
        }

        try {
            CotacaoMercado cotacao = cotacaoClient.buscarCotacao(
                normalizado
            );

            cotacaoCache.armazenar(cotacao);
        } catch (ServicoCotacaoIndisponivelException ex) {
            if (cotacaoCache.buscarParaFallback(normalizado).isPresent()) {
                LOGGER.warn(
                    "Validação do ticker {} utilizou cotação anterior por indisponibilidade do provedor.",
                    normalizado
                );
                return;
            }

            throw ex;
        }
    }

    public String normalizarTicker(String ticker) {
        return TickerNormalizer.normalizar(ticker);
    }

    private void buscarPendentes(
        Set<String> pendentes,
        Map<String, CotacaoMercado> encontrados,
        Set<String> indisponiveis
    ) {
        try {
            Map<String, CotacaoMercado> novasCotacoes =
                cotacaoClient.buscarCotacoes(pendentes);

            novasCotacoes.values().forEach(cotacao -> {
                cotacaoCache.armazenar(cotacao);
                encontrados.put(cotacao.ticker(), cotacao);
            });

            Set<String> ausentesNaResposta =
                new LinkedHashSet<>(pendentes);
            ausentesNaResposta.removeAll(novasCotacoes.keySet());

            if (!ausentesNaResposta.isEmpty()) {
                buscarIndividualmente(
                    ausentesNaResposta,
                    encontrados,
                    indisponiveis
                );
            }
        } catch (TickerInvalidoException ex) {
            buscarIndividualmente(
                pendentes,
                encontrados,
                indisponiveis
            );
        } catch (ServicoCotacaoIndisponivelException ex) {
            LOGGER.warn(
                "Consulta em lote indisponível para os tickers {}. Tentando cotações anteriores.",
                pendentes
            );
            aplicarFallback(
                pendentes,
                encontrados,
                indisponiveis
            );
        }
    }

    private void buscarIndividualmente(
        Set<String> tickers,
        Map<String, CotacaoMercado> encontrados,
        Set<String> indisponiveis
    ) {
        for (String ticker : tickers) {
            try {
                CotacaoMercado cotacao =
                    cotacaoClient.buscarCotacao(ticker);

                cotacaoCache.armazenar(cotacao);
                encontrados.put(ticker, cotacao);
            } catch (TickerInvalidoException ex) {
                indisponiveis.add(ticker);
            } catch (ServicoCotacaoIndisponivelException ex) {
                aplicarFallback(
                    Set.of(ticker),
                    encontrados,
                    indisponiveis
                );
            }
        }
    }

    private void aplicarFallback(
        Set<String> tickers,
        Map<String, CotacaoMercado> encontrados,
        Set<String> indisponiveis
    ) {
        for (String ticker : tickers) {
            cotacaoCache.buscarParaFallback(ticker)
                .ifPresentOrElse(
                    cotacao -> {
                        encontrados.put(ticker, cotacao);
                        LOGGER.warn(
                            "Cotação anterior utilizada como fallback para {}.",
                            ticker
                        );
                    },
                    () -> indisponiveis.add(ticker)
                );
        }
    }

    private CotacaoMercado consultarComFallback(String ticker) {
        try {
            CotacaoMercado cotacao =
                cotacaoClient.buscarCotacao(ticker);
            cotacaoCache.armazenar(cotacao);
            return cotacao;
        } catch (ServicoCotacaoIndisponivelException ex) {
            return cotacaoCache.buscarParaFallback(ticker)
                .map(cotacao -> {
                    LOGGER.warn(
                        "Cotação anterior utilizada como fallback para {}.",
                        ticker
                    );
                    return cotacao;
                })
                .orElseThrow(() -> ex);
        }
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
