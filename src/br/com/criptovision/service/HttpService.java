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
            // aguarda 2 segundos para não estourar o limite da API gratuita
            Thread.sleep(2000); 

            String idParaBusca = converterTickerParaId(moeda.getTicker());
            if (idParaBusca.isEmpty()) idParaBusca = moeda.getNome();

            String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idParaBusca + "&vs_currencies=usd";

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

            String valorString = json.split(":")[2].replace("}}", "");
            return Double.parseDouble(valorString);

        } catch (Exception e) {
            // se der erro de limite (429) ou interrupção, retorna 0
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
            Thread.sleep(2000);

            String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idMoeda + "&vs_currencies=usd";
            
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