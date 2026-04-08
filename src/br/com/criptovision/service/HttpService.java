package br.com.criptovision.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import br.com.criptovision.model.Moeda;

// essa classe gerencia as chamadas externas
// ela que é a repsonsavael por conectar o java a API  da binance e extrair os preços das criptomoedas em tempo real

public class HttpService {

    // novo metodo que busca o preço e a variação nas ultimas 24h
    public double[] buscarPrecoEVariacao(Moeda moeda) {
        String t = moeda.getTicker().toUpperCase().trim();
        
        if (t.equals("USDT")) return new double[]{1.0, 0.0}; 
        if (t.equals("BITCOIN")) t = "BTC";
        if (t.equals("SOLANA")) t = "SOL";
        if (t.equals("CHAINLINK")) t = "LINK";
        if (t.equals("ETHEREUM")) t = "ETH";

        String url = "https://api.binance.com/api/v3/ticker/24hr?symbol=" + t + "USDT";
        String json = realizarChamada(url);
        
        if (json == null) return new double[]{0.0, 0.0};

        // extrai usando o novo metodo auxiliar generico
        double preco = extrairValorJsonGenerico(json, "\"lastPrice\":\"");
        double variacao = extrairValorJsonGenerico(json, "\"priceChangePercent\":\"");

        return new double[]{preco, variacao};
    }

    // busca o preço de uma moeda da carteira
    public double buscarPrecoAtual(Moeda moeda){
        String t = moeda.getTicker().toUpperCase().trim();
        
        // conversoes pra facilitar a vida do usuario
        if (t.equals("USDT")) return 1.0; // usdt é o dolar, ent 1 usdt sempre vale 1 dolar
        if (t.equals("BITCOIN")) t = "BTC";
        if (t.equals("SOLANA")) t = "SOL";
        if (t.equals("CHAINLINK")) t = "LINK";
        if (t.equals("ETHEREUM")) t = "ETH";

        // a binance usa pares de moedas( BTC/USDT ), entao sempre fazamos a consulta contra o dolar (USDT)
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + t + "USDT";
        
        // aqui uma linha faz a chamada e a outra extrai o preço
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

    // esse metodo verifica se a moeda informada pelo usuario realemnte existe na binance antes de registra-la na carteira
    public boolean validarTicker(String idMoeda){
        String tickerParaValidar = idMoeda.trim().toUpperCase();
        // USDT é sempre válido no sistema
        if (tickerParaValidar.equals("USDT")) return true;
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + tickerParaValidar + "USDT";
        
        // se o realizarChamada retornar algo diferente de null, é porque o status foi 200
        return realizarChamada(url) != null;
    }

    public double consultarPrecoPorTicker(String ticker){
        Moeda moedaTemporaria = new Moeda(ticker, ticker);
        return buscarPrecoAtual(moedaTemporaria);
    }

    // metodo que busca a cotação do dolar em relaçao ao real (USDT/BRL)
    public double buscarCotacaoDolar(){
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=USDTBRL";
        String json = realizarChamada(url);
        double preco = extrairPreco(json);
        
        return (preco > 0) ? preco : 5.50;// operador ternario, se ele nao conseguir o preço, assume um valor medio do dolar
    }

    // esse metodo ajuda na burocracia do http, apenas um metodo auxiliar
    // se precisar mudar o timeout ou a biblioteca no futuro, muda só aqui.
    private String realizarChamada(String url) {
        try{
            // aqui criamos o cliente e a requisição(endereço)
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

    // outro metodo auxiliar, ele limpa o texto JSON  e pega apenas o numero, que é o que precisamos
    // exemplo: {"symbol":"BTCUSDT","price":"95000.00"}
    private double extrairPreco(String json){
        if (json == null) return 0;
        try{
            // quebra o texto na parte onde diz "price" e pega o que vem depois
            String[] partes = json.split("\"price\":\"");
            if (partes.length < 2) return 0;
            
            // pega o valor até a proxima "" e converte para numero
            String valorString = partes[1].split("\"")[0];
            return Double.parseDouble(valorString);
        }catch(Exception e){
            return 0;
        }
    }
}