package br.com.criptovision.cotacao.service;

import br.com.criptovision.cotacao.client.CotacaoClient;
import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Consulta as cotações no provedor (a Binance), protegendo a API dele e dela mesma:
 *
 * <ul>
 *   <li><b>uma consulta por ticker:</b> requisições simultâneas do mesmo ticker
 *       esperam a mesma chamada, em vez de cada uma ir ao provedor quando o cache
 *       expira. Um lote que se sobrepõe a outro em andamento só busca o que falta;</li>
 *   <li><b>consulta individual em paralelo e com prazo:</b> a Binance recusa o lote
 *       inteiro por um ticker inválido, sem dizer qual. Os tickers são então
 *       consultados um a um, até {@code paralelismo} por vez, e os que não
 *       respondem em {@code tempoMaximoIndividual} ficam como indisponíveis;</li>
 *   <li><b>circuit breaker:</b> depois de falhas seguidas, as chamadas falham na
 *       hora, sem esperar timeouts, até o provedor voltar (veja CotacaoConfiguration).</li>
 * </ul>
 *
 * <p>Não guarda cotações nem decide o fallback: isso é do {@link CotacaoService}.
 */
@Component
public class ConsultaDeCotacoes {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ConsultaDeCotacoes.class);

    // quem espera a consulta de outra requisição desiste depois disso. A consulta
    // em si termina antes (timeouts HTTP + prazo individual); isto só evita uma
    // espera infinita se algo travar
    private static final Duration ESPERA_MAXIMA_POR_OUTRA_CONSULTA =
        Duration.ofSeconds(30);

    private final CotacaoClient cotacaoClient;
    private final CircuitBreaker circuitBreaker;
    private final int paralelismo;
    private final Duration tempoMaximoIndividual;

    private final ConcurrentMap<String, CompletableFuture<CotacaoMercado>> emAndamento =
        new ConcurrentHashMap<>();

    public ConsultaDeCotacoes(
        CotacaoClient cotacaoClient,
        CircuitBreaker circuitBreaker,
        @Value("${cotacoes.individual.paralelismo:4}") int paralelismo,
        @Value("${cotacoes.individual.tempo-maximo:5s}") Duration tempoMaximoIndividual
    ) {
        if (paralelismo < 1) {
            throw new IllegalArgumentException(
                "O paralelismo das consultas individuais deve ser ao menos 1."
            );
        }

        if (tempoMaximoIndividual.isNegative() || tempoMaximoIndividual.isZero()) {
            throw new IllegalArgumentException(
                "O tempo máximo das consultas individuais deve ser maior que zero."
            );
        }

        this.cotacaoClient = cotacaoClient;
        this.circuitBreaker = circuitBreaker;
        this.paralelismo = paralelismo;
        this.tempoMaximoIndividual = tempoMaximoIndividual;
    }

    /** O que o provedor respondeu para cada ticker pedido. */
    public record Resposta(
        Map<String, CotacaoMercado> encontradas,
        Set<String> invalidos,
        Set<String> indisponiveis
    ) {
    }

    public Resposta consultar(Set<String> tickers) {
        Map<String, CompletableFuture<CotacaoMercado>> todas = new LinkedHashMap<>();
        Map<String, CompletableFuture<CotacaoMercado>> minhas = new LinkedHashMap<>();

        for (String ticker : tickers) {
            CompletableFuture<CotacaoMercado> nova = new CompletableFuture<>();
            CompletableFuture<CotacaoMercado> existente = emAndamento.putIfAbsent(ticker, nova);

            if (existente == null) {
                minhas.put(ticker, nova);
                todas.put(ticker, nova);
            } else {
                todas.put(ticker, existente);
            }
        }

        try {
            if (!minhas.isEmpty()) {
                buscar(minhas);
            }
        } finally {
            // ninguém fica esperando para sempre, nem se a busca lançar algo inesperado.
            // Numa futura já concluída, completeExceptionally não faz nada
            minhas.forEach((ticker, futura) -> {
                futura.completeExceptionally(new ServicoCotacaoIndisponivelException());
                emAndamento.remove(ticker, futura);
            });
        }

        return coletar(todas);
    }

    private void buscar(Map<String, CompletableFuture<CotacaoMercado>> minhas) {
        if (minhas.size() == 1) {
            // um ticker só: a consulta individual já diferencia ticker inválido de falha
            Map.Entry<String, CompletableFuture<CotacaoMercado>> unica =
                minhas.entrySet().iterator().next();
            completar(unica.getValue(), () -> chamar(() -> cotacaoClient.buscarCotacao(unica.getKey())));
            return;
        }

        Set<String> pendentes = new LinkedHashSet<>(minhas.keySet());

        try {
            Map<String, CotacaoMercado> lote = chamar(() -> cotacaoClient.buscarCotacoes(pendentes));

            lote.forEach((ticker, cotacao) -> {
                CompletableFuture<CotacaoMercado> futura = minhas.get(ticker);
                if (futura != null) {
                    futura.complete(cotacao);
                }
            });

            Map<String, CompletableFuture<CotacaoMercado>> ausentes = new LinkedHashMap<>(minhas);
            ausentes.keySet().removeAll(lote.keySet());

            if (!ausentes.isEmpty()) {
                buscarIndividualmente(ausentes);
            }
        } catch (TickerInvalidoException ex) {
            // a Binance recusa o lote inteiro por um ticker inválido, sem dizer qual
            buscarIndividualmente(minhas);
        } catch (ServicoCotacaoIndisponivelException ex) {
            LOGGER.warn("Consulta em lote indisponível para os tickers {}.", pendentes);
            minhas.values().forEach(futura -> futura.completeExceptionally(ex));
        }
    }

    private void buscarIndividualmente(Map<String, CompletableFuture<CotacaoMercado>> tickers) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Semaphore vagas = new Semaphore(paralelismo);
        // os logs das consultas paralelas continuam com o ID da requisição
        Map<String, String> contextoDoLog = MDC.getCopyOfContextMap();

        try {
            tickers.forEach((ticker, futura) -> executor.execute(() -> {
                if (contextoDoLog != null) {
                    MDC.setContextMap(contextoDoLog);
                }

                try {
                    vagas.acquire();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    completar(futura, () -> chamar(() -> cotacaoClient.buscarCotacao(ticker)));
                } finally {
                    vagas.release();
                }
            }));

            CompletableFuture
                .allOf(tickers.values().toArray(CompletableFuture[]::new))
                .get(tempoMaximoIndividual.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            LOGGER.warn(
                "Consulta individual dos tickers {} passou de {}: os que não responderam ficam sem cotação nova.",
                tickers.keySet(),
                tempoMaximoIndividual
            );
        } catch (ExecutionException ex) {
            // todas terminaram, algumas com erro: o erro de cada uma está na própria futura
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            // interrompe as consultas que passaram do prazo, que ficam como indisponíveis
            executor.shutdownNow();
            tickers.values().forEach(futura ->
                futura.completeExceptionally(new ServicoCotacaoIndisponivelException())
            );
        }
    }

    private <T> T chamar(Supplier<T> chamada) {
        try {
            return circuitBreaker.executeSupplier(chamada);
        } catch (CallNotPermittedException ex) {
            // circuito aberto: o provedor falhou demais há pouco, nem tenta
            throw new ServicoCotacaoIndisponivelException(ex);
        }
    }

    private static void completar(
        CompletableFuture<CotacaoMercado> futura,
        Supplier<CotacaoMercado> busca
    ) {
        try {
            futura.complete(busca.get());
        } catch (RuntimeException ex) {
            futura.completeExceptionally(ex);
        }
    }

    private Resposta coletar(Map<String, CompletableFuture<CotacaoMercado>> todas) {
        Map<String, CotacaoMercado> encontradas = new LinkedHashMap<>();
        Set<String> invalidos = new LinkedHashSet<>();
        Set<String> indisponiveis = new LinkedHashSet<>();
        long limite = System.nanoTime() + ESPERA_MAXIMA_POR_OUTRA_CONSULTA.toNanos();

        todas.forEach((ticker, futura) -> {
            try {
                long restante = Math.max(0, limite - System.nanoTime());
                encontradas.put(ticker, futura.get(restante, TimeUnit.NANOSECONDS));
            } catch (ExecutionException ex) {
                switch (ex.getCause()) {
                    case TickerInvalidoException invalido -> invalidos.add(ticker);
                    case ServicoCotacaoIndisponivelException indisponivel -> indisponiveis.add(ticker);
                    // erro inesperado (um bug): sobe, como antes, e vira um 500 com métrica
                    case RuntimeException outro -> throw outro;
                    default -> throw new IllegalStateException(ex.getCause());
                }
            } catch (TimeoutException ex) {
                LOGGER.warn("A consulta de {} feita por outra requisição não terminou a tempo.", ticker);
                indisponiveis.add(ticker);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                indisponiveis.add(ticker);
            }
        });

        return new Resposta(encontradas, invalidos, indisponiveis);
    }
}
