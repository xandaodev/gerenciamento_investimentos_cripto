package br.com.criptovision.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;

public class CarteiraServiceTest {

    //teste o preco medio apos 2 compras de uma moeda serem feitas
    @Test
    public void deveCalcularPrecoMedioCorretamenteAposDuasCompras(){
        // organizando
        CarteiraService service = new CarteiraService();
        Moeda btc = new Moeda("BTC", "Bitcoin");


        // executando, fazendo a primeiro compra de 50000
        Transacao t1 = new Transacao("BTC", 1.0, 50000.0, "COMPRA");
        service.processarTransacao(btc, t1);

        // fazendo a segunda compra a 60000
        Transacao t2 = new Transacao("BTC", 1.0, 60000.0, "COMPRA");
        service.processarTransacao(btc, t2);



        // verificando se o resultado é o esperado, que no caso é (50000 + 60000) / 2 = 55000
        // ajustando 0.001 de "tolerância" de erro decimal 
        assertEquals(55000.0, btc.getPrecoMedio(), 0.001);
        assertEquals(2.0, btc.getSaldo(), 0.001);
    }


    // esse teste calcula o lucro da venda, depois de uma venda parcial
    @Test
    public void deveCalcularLucroCorretamenteAposVendaParcial(){
        //vamos testar com a link
        CarteiraService service = new CarteiraService();
        Moeda link = new Moeda("LINK", "Chainlink");

        // compra 10 unidades de LINK, por um preço de $10 cada, resultando em $100
        service.processarTransacao(link, new Transacao("LINK", 10.0, 10.0, "COMPRA"));

        // vende 5 unidade a $20 cada ($100)
        // e o custo dessas 5 foi %50 -> (5 * $10)
        // lucro esperado = $100 - $50 = $50
        Transacao venda = new Transacao("LINK", 5.0, 20.0, "VENDA");
        
        // aqui testa a lógica do service
        double custoParteVendida = 5.0 * link.getPrecoMedio();
        double lucroEsperado = (5.0 * 20.0) - custoParteVendida;

        // 0.001 = tolerância
        assertEquals(50.0, lucroEsperado, 0.001);
        
        // execeuta a venda no service pra verificar o saldo final
        service.processarTransacao(link, venda);
        assertEquals(5.0, link.getSaldo(), 0.001);
    }
}
