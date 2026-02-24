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
        String tickerParaValidar = idMoeda.trim().toUpperCase();
        // USDT é sempre válido no nosso sistema
        if (tickerParaValidar.equals("USDT")) return true;
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + tickerParaValidar + "USDT";
        
        // se o realizarChamada retornar algo diferente de null, é porque o status foi 200
        return realizarChamada(url) != null;
    }

    public double consultarPrecoPorTicker(String ticker){
        Moeda moedaTemporaria = new Moeda(ticker, ticker);
        return buscarPrecoAtual(moedaTemporaria);
    }

    //iniciando refatoraçao do httpservice
    public double buscarCotacaoDolar() {
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=USDTBRL";
        
        //u samos o ajudante para pegar o texto bruto (JSON)
        String json = realizarChamada(url);
        
        if (json == null) return 5.50; // valor de segurança caso a rede falhe

        // logica de extração do preço (que vamos limpar no próximo passo)
        try{
            String[] partes = json.split("\"price\":\"");
            if (partes.length < 2) return 5.50;
            
            String valorString = partes[1].split("\"")[0];
            return Double.parseDouble(valorString);
        }catch(Exception e){
            return 5.50;
        }   
    }

    // se precisar mudar o timeout ou a biblioteca no futuro, muda só aqui.
    private String realizarChamada(String url) {
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

            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}