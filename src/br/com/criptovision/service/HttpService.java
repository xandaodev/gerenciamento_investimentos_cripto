package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import br.com.criptovision.model.Moeda;

public class HttpService {

    public double buscarPrecoAtual(Moeda moeda) {
        try {
            //iniciando a troca da api da coingecko para a api da binance
            // removido o Thread.sleep(2000) pq a Binance permite muitas requisições
            
            // a binance usa o par da moeda com o Dólar (USDT)
            String simboloBinance = moeda.getTicker().toUpperCase() + "USDT";
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + simboloBinance;

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return 0;

            String json = response.body();
            if (json.equals("{}")) return 0;

            // ajustado o split para o formato da binance: {"symbol":"BTCUSDT","price":"43500.00"}
            String valorString = json.split("\"price\":\"")[1].split("\"")[0];
            return Double.parseDouble(valorString);

        }catch(Exception e){
            // se der erro de limite ou interrupção, retorna 0
            return 0;
        }
    }

    public String converterTickerParaId(String ticker){
        if (ticker == null) return "";
        return switch (ticker.toUpperCase()) {
            case "BTC" -> "bitcoin";
            case "ETH" -> "ethereum";
            case "SOL" -> "solana";
            case "LNK" -> "chainlink";
            default -> ticker.toLowerCase(); 
        };
    }

    public boolean validarTicker(String idMoeda){
        try {
            // removido o sleep e ajustado para validar na binance usando o ticker
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + idMoeda.toUpperCase() + "USDT";
            
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200 && !response.body().equals("{}");
        } catch(Exception e) {
            return false;
        }
    }

    public double consultarPrecoPorTicker(String ticker){
        String idParaAPI = converterTickerParaId(ticker);
        Moeda moedaTemporaria = new Moeda(ticker, idParaAPI);
        return buscarPrecoAtual(moedaTemporaria);
    }
}