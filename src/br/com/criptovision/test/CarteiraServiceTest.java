package br.com.criptovision.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;

public class CarteiraServiceTest {

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
}
