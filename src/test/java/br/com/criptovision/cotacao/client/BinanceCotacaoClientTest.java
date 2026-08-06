package br.com.criptovision.cotacao.client;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.exception.ServicoCotacaoIndisponivelException;
import br.com.criptovision.exception.TickerInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BinanceCotacaoClientTest {

    private MockRestServiceServer server;
    private BinanceCotacaoClient client;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        client = new BinanceCotacaoClient(
            builder
                .baseUrl("https://api.binance.test/api/v3")
                .build(),
            Clock.fixed(
                Instant.parse("2026-08-06T14:30:00Z"),
                ZoneOffset.UTC
            )
        );
    }

    @Test
    void deveDesserializarCotacaoTipada() {
        server.expect(requestTo(containsString("/ticker/24hr")))
            .andExpect(requestTo(containsString("type=FULL")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                """
                {
                  "symbol": "BTCUSDT",
                  "lastPrice": "65000.125",
                  "priceChangePercent": "2.35",
                  "closeTime": 1786026600000
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        CotacaoMercado cotacao =
            client.buscarCotacao("BTC");

        assertEquals("BTC", cotacao.ticker());
        assertEquals(
            0,
            new BigDecimal("65000.125")
                .compareTo(cotacao.preco())
        );
        assertEquals(
            0,
            new BigDecimal("2.35")
                .compareTo(cotacao.variacao24h())
        );
        assertEquals(
            Instant.ofEpochMilli(1786026600000L),
            cotacao.atualizadaEm()
        );

        server.verify();
    }

    @Test
    void deveConsultarVariosTickersEmUmaUnicaRequisicao() {
        server.expect(requestTo(containsString("symbols=")))
            .andExpect(requestTo(containsString("type=FULL")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                """
                [
                  {
                    "symbol": "BTCUSDT",
                    "lastPrice": "65000",
                    "priceChangePercent": "2.35",
                    "closeTime": 1786026600000
                  },
                  {
                    "symbol": "ETHUSDT",
                    "lastPrice": "3500",
                    "priceChangePercent": "-1.20",
                    "closeTime": 1786026600000
                  }
                ]
                """,
                MediaType.APPLICATION_JSON
            ));

        Map<String, CotacaoMercado> cotacoes =
            client.buscarCotacoes(Set.of("BTC", "ETH"));

        assertEquals(Set.of("BTC", "ETH"), cotacoes.keySet());
        assertEquals(
            0,
            new BigDecimal("3500")
                .compareTo(cotacoes.get("ETH").preco())
        );

        server.verify();
    }

    @Test
    void deveDiferenciarTickerInvalidoDeFalhaDoServidor() {
        server.expect(requestTo(containsString("symbol=XYZUSDT")))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                    {"code":-1121,"msg":"Invalid symbol."}
                    """));

        assertThrows(
            TickerInvalidoException.class,
            () -> client.buscarCotacao("XYZ")
        );

        server.verify();
    }

    @Test
    void deveMapearErroDoProvedorComoIndisponibilidade() {
        server.expect(requestTo(containsString("symbol=BTCUSDT")))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(
            ServicoCotacaoIndisponivelException.class,
            () -> client.buscarCotacao("BTC")
        );

        server.verify();
    }
}
