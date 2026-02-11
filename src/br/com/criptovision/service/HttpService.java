package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import br.com.criptovision.model.Moeda;

public class HttpService {

    public double buscarPrecoAtual(Moeda moeda){
        try{
            String t = moeda.getTicker().toUpperCase().trim();
                    if (t.equals("USDT")) {
                return 1.0;
            }
            if (t.equals("BITCOIN")) t = "BTC";
            if (t.equals("SOLANA")) t = "SOL";
            if (t.equals("CHAINLINK")) t = "LINK";
            if (t.equals("ETHEREUM")) t = "ETH";

            String simboloBinance = t + "USDT";
            
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + simboloBinance;
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return 0;

            String json = response.body();
            
            String[] partes = json.split("\"price\":\"");
            if (partes.length < 2) return 0;
            
            String valorString = partes[1].split("\"")[0];
            return Double.parseDouble(valorString);

        }catch(Exception e){
            return 0;
        }
    }

    public String converterTickerParaId(String ticker){
    if (ticker == null) return "";
    String t = ticker.toUpperCase().trim();
        return switch (t){
        case "BITCOIN", "BTC" -> "BTC";
        case "ETHEREUM", "ETH" -> "ETH";
        case "SOLANA", "SOL" -> "SOL";
        case "CHAINLINK", "LINK", "LNK" -> "LINK";
        default -> t; 
    };
    }

    public boolean validarTicker(String idMoeda){
        if(idMoeda.trim().toUpperCase().equals("USDT")){
            return true;
        }
        try {
            String tickerParaValidar = idMoeda.trim().toUpperCase();
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + tickerParaValidar + "USDT";
            
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch(Exception e) {
            return false;
        }
    }

    public double consultarPrecoPorTicker(String ticker){
        Moeda moedaTemporaria = new Moeda(ticker, ticker);
        return buscarPrecoAtual(moedaTemporaria);
    }

    public double buscarCotacaoDolar(){
        try{
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=USDTBRL";
            
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return 5.0; 

            String json = response.body();
            String[] partes = json.split("\"price\":\"");
            if (partes.length < 2) return 5.0;
            
            String valorString = partes[1].split("\"")[0];
            return Double.parseDouble(valorString);
        } catch (Exception e) {
            return 5.50; // valor médio de segurança caso esteja sem internet
        }
    }
}