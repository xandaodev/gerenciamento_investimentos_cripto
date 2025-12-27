package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;

public class HttpService {

    public double buscarPrecoAtual(String ticker) {
        // convertendo o sticker pro nome correto 
        String idMoeda = converterTickerParaId(ticker);
        
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idMoeda + "&vs_currencies=usd";

        try{
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200){
                System.out.println("API retornou algum erro: " + response.statusCode());
                return 0;
            }

            String json = response.body();
            // verificaçao:  se o JSON for {}, significa que a moeda nao foi encontrada
            if (json.equals("{}")){
                System.out.println("Moeda '" + ticker + "' nao encontrada na API");
                return 0;
            }
            String valorString = json.split(":")[2].replace("}}", "");
            
            return Double.parseDouble(valorString);

        }catch(java.net.http.HttpTimeoutException e){
            System.out.println("Erro: Tempo limite esgotado. Talvez a internet esteja lenta");
        }catch(java.net.ConnectException e){
            System.out.println("Erro: Falha ao conectar. Talvez voce esteja sem internet");
        }catch(Exception e){
            System.out.println("Erro inesperado na consulta: " + e.getMessage());
        }
        return 0;
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
