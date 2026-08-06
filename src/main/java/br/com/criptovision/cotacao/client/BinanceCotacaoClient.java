package br.com.criptovision.cotacao.client;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BinanceCotacaoClient implements CotacaoClient {

    private static final String MOEDA_COTACAO = "USDT";
    private static final int TAMANHO_MAXIMO_LOTE = 20;

    private final RestClient restClient;
    private final Clock clock;

    @Autowired
    public BinanceCotacaoClient(
        RestClient.Builder restClientBuilder,
        @Value("${binance.api.base-url}") String baseUrl,
        @Value("${cotacoes.http.connect-timeout:3s}")
        Duration connectTimeout,
        @Value("${cotacoes.http.read-timeout:5s}")
        Duration readTimeout,
        Clock clock
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        this.restClient = restClientBuilder
            .requestFactory(requestFactory)
            .baseUrl(baseUrl)
            .build();
        this.clock = clock;
    }

    BinanceCotacaoClient(RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    @Override
    public Map<String, CotacaoMercado> buscarCotacoes(
        Set<String> tickers
    ) {
        if (tickers.isEmpty()) {
            return Map.of();
        }

        List<String> listaTickers = new ArrayList<>(tickers);
        Map<String, CotacaoMercado> cotacoes =
            new LinkedHashMap<>();

        for (
            int inicio = 0;
            inicio < listaTickers.size();
            inicio += TAMANHO_MAXIMO_LOTE
        ) {
            int fim = Math.min(
                inicio + TAMANHO_MAXIMO_LOTE,
                listaTickers.size()
            );

            Set<String> lote = new LinkedHashSet<>(
                listaTickers.subList(inicio, fim)
            );

            cotacoes.putAll(buscarLote(lote));
        }

        return cotacoes;
    }

    @Override
    public CotacaoMercado buscarCotacao(String ticker) {
        String symbol = paraParBinance(ticker);

        try {
            BinanceTicker24hResponse resposta = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/ticker/24hr")
                    .queryParam("symbol", symbol)
                    .queryParam("type", "FULL")
                    .build())
                .retrieve()
                .onStatus(
                    status -> status.value() == 400,
                    (request, response) -> {
                        throw new TickerInvalidoException(ticker);
                    }
                )
                .onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> {
                        throw new ServicoCotacaoIndisponivelException();
                    }
                )
                .body(BinanceTicker24hResponse.class);

            if (resposta == null) {
                throw new ServicoCotacaoIndisponivelException();
            }

            return converterResposta(resposta);
        } catch (TickerInvalidoException
                 | ServicoCotacaoIndisponivelException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicoCotacaoIndisponivelException(ex);
        }
    }

    private Map<String, CotacaoMercado> buscarLote(
        Set<String> tickers
    ) {
        String symbols = tickers.stream()
            .map(this::paraParBinance)
            .map(symbol -> "\"" + symbol + "\"")
            .collect(Collectors.joining(",", "[", "]"));

        try {
            BinanceTicker24hResponse[] respostas = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/ticker/24hr")
                    .queryParam("symbols", symbols)
                    .queryParam("type", "FULL")
                    .build())
                .retrieve()
                .onStatus(
                    status -> status.value() == 400,
                    (request, response) -> {
                        throw new TickerInvalidoException(
                            String.join(", ", tickers)
                        );
                    }
                )
                .onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> {
                        throw new ServicoCotacaoIndisponivelException();
                    }
                )
                .body(BinanceTicker24hResponse[].class);

            if (respostas == null) {
                throw new ServicoCotacaoIndisponivelException();
            }

            Map<String, CotacaoMercado> cotacoes =
                new LinkedHashMap<>();

            Arrays.stream(respostas)
                .map(this::converterResposta)
                .forEach(cotacao -> cotacoes.put(
                    cotacao.ticker(),
                    cotacao
                ));

            return cotacoes;
        } catch (TickerInvalidoException
                 | ServicoCotacaoIndisponivelException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicoCotacaoIndisponivelException(ex);
        }
    }

    private CotacaoMercado converterResposta(
        BinanceTicker24hResponse resposta
    ) {
        try {
            String ticker = resposta.symbol();

            if (ticker == null
                || !ticker.endsWith(MOEDA_COTACAO)) {
                throw new IllegalArgumentException(
                    "Símbolo inesperado na resposta."
                );
            }

            ticker = ticker.substring(
                0,
                ticker.length() - MOEDA_COTACAO.length()
            );

            BigDecimal preco = new BigDecimal(
                resposta.lastPrice()
            );

            BigDecimal variacao24h = new BigDecimal(
                resposta.priceChangePercent()
            );

            Instant atualizadaEm = resposta.closeTime() == null
                ? Instant.now(clock)
                : Instant.ofEpochMilli(resposta.closeTime());

            return new CotacaoMercado(
                ticker,
                preco,
                variacao24h,
                atualizadaEm,
                false
            );
        } catch (RuntimeException ex) {
            throw new ServicoCotacaoIndisponivelException(ex);
        }
    }

    private String paraParBinance(String ticker) {
        return ticker + MOEDA_COTACAO;
    }
}
