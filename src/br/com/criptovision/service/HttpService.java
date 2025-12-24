package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpService {

    public double buscarPrecoAtual(String ticker) {
        // convertendo o sticker pro nome correto 
        String idMoeda = converterTickerParaId(ticker);
        
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idMoeda + "&vs_currencies=usd";

        try{
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();
            String valorString = json.split(":")[2].replace("}}", "");
            
            return Double.parseDouble(valorString);

        }catch (Exception e){
            System.out.println("Erro ao buscar preço para " + ticker + ". Verifique sua conexão com a internet.");
            return 0;
        }
    }

    //funcao para converter o ticket para o nome que a api usa
    private String converterTickerParaId(String ticker) {
        return switch (ticker.toUpperCase()) {
            case "BTC" -> "bitcoin";
            case "ETH" -> "ethereum";
            case "SOL" -> "solana";
            case "LNK" -> "chainlink";
            default -> ticker.toLowerCase();
        };
    }
    
}
