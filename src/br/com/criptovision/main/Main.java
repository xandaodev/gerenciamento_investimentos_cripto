package br.com.criptovision.main;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;

public class Main {
    public static void main(String[] args){
        CarteiraService service = new CarteiraService();
        
        // adicionando uma moeda na carteira
        Moeda btc = new Moeda("Bitcoin", "BTC");

        // primeira compra: 1 BTC a 86.000
        Transacao t1 = new Transacao("BTC", 1, 86000.00, "COMPRA");
        service.processarTransacao(btc, t1);
        exibirStatus(btc);

        // segunda compra: 1 BTC a 92.000
        Transacao t2 = new Transacao("BTC", 1, 92000.00, "COMPRA");
        service.processarTransacao(btc, t2);
        exibirStatus(btc);
    }

    public static void exibirStatus(Moeda moeda) {
        System.out.println("Saldo: " + moeda.getSaldo() + " " + moeda.getTicker());
        System.out.printf("Preço Médio: R$ %.2f\n", moeda.getPrecoMedio());
        System.out.print("\n");
    }
    
}
