package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import br.com.criptovision.model.Moeda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// essa classe gerencia as chamadas externas
// ela que é a responsavel por conectar o java a API  da binance e extrair os preços das criptomoedas em tempo real

@Service
public class HttpService {

    @Value("${binance.api.base-url}")
    private String baseUrl;

    public double[] buscarPrecoEVariacao(Moeda moeda) {
        String t = moeda.getTicker().toUpperCase().trim();

        if (t.equals("USDT")) return new double[]{1.0, 0.0};
        if (t.equals("BITCOIN")) t = "BTC";
        if (t.equals("SOLANA")) t = "SOL";
        if (t.equals("CHAINLINK")) t = "LINK";
        if (t.equals("ETHEREUM")) t = "ETH";

        // Usamos a variável baseUrl em vez da string fixa
        String url = baseUrl + "/ticker/24hr?symbol=" + t + "USDT";
        String json = realizarChamada(url);

        if (json == null) return new double[]{0.0, 0.0};

        double preco = extrairValorJsonGenerico(json, "\"lastPrice\":\"");
        double variacao = extrairValorJsonGenerico(json, "\"priceChangePercent\":\"");

        return new double[]{preco, variacao};
    }

    public double buscarPrecoAtual(Moeda moeda){
        String t = moeda.getTicker().toUpperCase().trim();

        if (t.equals("USDT")) return 1.0;
        if (t.equals("BITCOIN")) t = "BTC";
        if (t.equals("SOLANA")) t = "SOL";
        if (t.equals("CHAINLINK")) t = "LINK";
        if (t.equals("ETHEREUM")) t = "ETH";

        String url = baseUrl + "/ticker/price?symbol=" + t + "USDT";

        String json = realizarChamada(url);
        return extrairPreco(json);
    }

    // metodo pra formatar e normalizar os nomes das criptos no sistema
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
        // USDT é sempre válido no sistema
        if (tickerParaValidar.equals("USDT")) return true;

        String url = baseUrl + "/ticker/price?symbol=" + tickerParaValidar + "USDT";
        return realizarChamada(url) != null;
    }

    public double consultarPrecoPorTicker(String ticker){
        Moeda moedaTemporaria = new Moeda(ticker, ticker);
        return buscarPrecoAtual(moedaTemporaria);
    }

    public double buscarCotacaoDolar(){
        String url = baseUrl + "/ticker/price?symbol=USDTBRL";
        String json = realizarChamada(url);
        double preco = extrairPreco(json);

        return (preco > 0) ? preco : 5.50;
    }

    // esse metodo ajuda na burocracia do http, apenas um metodo auxiliar
    // se precisar mudar o timeout ou a biblioteca no futuro, muda só aqui.
    private String realizarChamada(String url) {
        try{
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();

            // aqui enviamos o pedido e aguardamos a resposta em String
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200){
                return response.body();
            }
            return null;
        }catch(Exception e){
            return null;
        }
    }

    // extrai qualquer valor da binance passando a chave
    private double extrairValorJsonGenerico(String json, String chaveDeBusca){
        if (json == null) return 0;
        try{
            String[] partes = json.split(chaveDeBusca);
            if (partes.length < 2) return 0;
            String valorString = partes[1].split("\"")[0];
            return Double.parseDouble(valorString);
        }catch(Exception e){
            return 0;
        }
    }

    private double extrairPreco(String json){
        if (json == null) return 0;
        try{
            // quebra o texto na parte onde diz "price" e pega o que vem depois
            String[] partes = json.split("\"price\":\"");
            if (partes.length < 2) return 0;
            String valorString = partes[1].split("\"")[0];
            return Double.parseDouble(valorString);
        }catch(Exception e){
            return 0;
        }
    }
}