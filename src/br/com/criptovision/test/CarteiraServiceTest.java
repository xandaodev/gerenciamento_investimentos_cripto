package br.com.criptovision.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.repository.LucroDAO;
import br.com.criptovision.exception.SaldoInsuficienteException;
import br.com.criptovision.dto.SimulacaoDCADTO;

public class CarteiraServiceTest {

    @Test
    public void deveCalcularPrecoMedioCorretamenteAposDuasCompras(){
        CarteiraService service = new CarteiraService();
        Moeda btc = new Moeda("BTC", "Bitcoin");

        Transacao t1 = new Transacao("BTC", 1.0, 50000.0, "COMPRA");
        service.processarTransacao(btc, t1);

        Transacao t2 = new Transacao("BTC", 1.0, 60000.0, "COMPRA");
        service.processarTransacao(btc, t2);

        assertEquals(55000.0, btc.getPrecoMedio(), 0.001);
        assertEquals(2.0, btc.getSaldo(), 0.001);
    }

    @Test
    public void deveCalcularLucroCorretamenteAposVendaParcial(){
        CarteiraService service = new CarteiraService();
        
        // INJEÇÃO DE DEPENDÊNCIA: aqui tem um DAO Falso só para esswe teste, assim ele nao grava no banco de dados
        service.setLucroRepo(new LucroDAO(){
            @Override
            public void salvarLucroRealizado(String ticker, double valorLucro){
                System.out.println("   [TESTE] Fingindo salvar lucro de " + ticker + " no banco: $" + valorLucro);
            }
            @Override
            public double lerLucroTotal() { return 0; }
        });

        Moeda link = new Moeda("LINK", "Chainlink");

        service.processarTransacao(link, new Transacao("LINK", 10.0, 10.0, "COMPRA"));

        Transacao venda = new Transacao("LINK", 5.0, 20.0, "VENDA");
        
        double custoParteVendida = 5.0 * link.getPrecoMedio();
        double lucroEsperado = (5.0 * 20.0) - custoParteVendida;

        assertEquals(50.0, lucroEsperado, 0.001);
        
        service.processarTransacao(link, venda);
        assertEquals(5.0, link.getSaldo(), 0.001);
    }

    @Test(expected = SaldoInsuficienteException.class)
    public void naoDevePermitirVendaMaiorQueSaldo() throws SaldoInsuficienteException{
        CarteiraService service = new CarteiraService();
        Moeda sol = new Moeda("SOL", "Solana");

        // simula compra de 10 solanas
        service.processarTransacao(sol, new Transacao("SOL", 10.0, 100.0, "COMPRA"), false);

        // o usuário tenta vender 15 SOL.
        // o teste vai passar se o sistema estourar o SaldoInsuficienteException e parar tudo.
        Transacao vendaInvalida = new Transacao("SOL", 15.0, 150.0, "VENDA");
        service.processarTransacao(sol, vendaInvalida, false);
    }
}