package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;

import br.com.criptovision.model.Moeda;

public class HttpService {

    public double buscarPrecoAtual(Moeda moeda) {
        // convertendo o sticker pro nome correto 
        //String idMoeda = converterTickerParaId(ticker);
        
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + moeda.getNome() + "&vs_currencies=usd";

        try {
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
        return 0;
    }
}

    //funcao para converter o ticket para o nome que a api usa
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

    //funcao para verificar se existe o token antes de registrar a venda 
    public boolean validarTicker(String ticker){
        String idMoeda = converterTickerParaId(ticker);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idMoeda + "&vs_currencies=usd";
        try{
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // se o status for 200 e o corpo não for "{}", o ticker é válido
            return response.statusCode() == 200 && !response.body().equals("{}");
        }catch(Exception e){
            return false;
        }
    }
    
}
