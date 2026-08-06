package br.com.criptovision.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.TipoTransacao;

import java.time.LocalDateTime;
import java.util.List;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.exception.SaldoInsuficienteException;
import br.com.criptovision.dto.SimulacaoDCADTO;

import br.com.criptovision.exception.HistoricoInconsistenteException;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

public class CarteiraServiceTest {

    @Test
    public void deveCalcularPrecoMedioCorretamenteAposDuasCompras(){
        CarteiraService service = new CarteiraService(null, null);
        Moeda btc = new Moeda("BTC", "Bitcoin");

        Transacao t1 = new Transacao("BTC", BigDecimal.valueOf(1.0), BigDecimal.valueOf(50000.0), TipoTransacao.COMPRA);
        service.processarTransacao(btc, t1, false);

        Transacao t2 = new Transacao("BTC", BigDecimal.valueOf(1.0), BigDecimal.valueOf(60000.0), TipoTransacao.COMPRA);
        service.processarTransacao(btc, t2, false);

        assertEquals(55000.0, btc.getPrecoMedio().doubleValue(), 0.001);
        assertEquals(2.0, btc.getSaldo().doubleValue(), 0.001);
    }

    @Test
    public void deveCalcularLucroCorretamenteAposVendaParcial(){
        CarteiraService service = new CarteiraService(null, null);
        Moeda link = new Moeda("LINK", "Chainlink");

        service.processarTransacao(link, new Transacao("LINK", BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), TipoTransacao.COMPRA), false);

        Transacao venda = new Transacao("LINK", BigDecimal.valueOf(5.0), BigDecimal.valueOf(20.0), TipoTransacao.VENDA);

        double custoParteVendida = 5.0 * link.getPrecoMedio().doubleValue();
        double lucroEsperado = (5.0 * 20.0) - custoParteVendida;

        assertEquals(50.0, lucroEsperado, 0.001);

        service.processarTransacao(link, venda, false);
        assertEquals(5.0, link.getSaldo().doubleValue(), 0.001);
    }

    @Test
    public void naoDevePermitirVendaMaiorQueSaldo() {
        CarteiraService service = new CarteiraService(null, null);
        Moeda sol = new Moeda("SOL", "Solana");

        service.processarTransacao(sol, new Transacao("SOL", BigDecimal.valueOf(10.0), BigDecimal.valueOf(100.0), TipoTransacao.COMPRA), false);

        Transacao vendaInvalida = new Transacao("SOL", BigDecimal.valueOf(15.0), BigDecimal.valueOf(150.0), TipoTransacao.VENDA);

        assertThrows(SaldoInsuficienteException.class, () -> {
            service.processarTransacao(sol, vendaInvalida, false);
        });
    }

    @Test
    public void deveCalcularSimulacaoDCACorretamente(){
        CarteiraService service = new CarteiraService(null, null);
        Moeda eth = new Moeda("ETH", "Ethereum");

        eth.setSaldo(BigDecimal.valueOf(2.0));
        eth.setPrecoMedio(BigDecimal.valueOf(3000.0));

        SimulacaoDCADTO simulacao = service.simularDCA(eth, 3000.0, 1500.0);

        assertEquals(2.0, simulacao.getQtdComprada(), 0.001);
        assertEquals(4.0, simulacao.getNovoSaldoTotal(), 0.001);
        assertEquals(2250.0, simulacao.getNovoPM(), 0.001);
    }

    @Test
    public void deveCalcularLucroPotencialCorretamente(){
        CarteiraService service = new CarteiraService(null, null);
        Moeda btc = new Moeda("BTC", "Bitcoin");

        btc.setSaldo(BigDecimal.valueOf(0.5));
        btc.setPrecoMedio(BigDecimal.valueOf(50000.0));

        double lucroPotencial = service.calcularLucroPotencial(btc, 70000.0);

        assertEquals(10000.0, lucroPotencial, 0.001);
    }

    @Test
    public void deveReconstruirCarteiraEmOrdemCronologicaMesmoComHistoricoDesordenado() {
        CarteiraService service = new CarteiraService(null, null);
        Carteira carteira = new Carteira();

        Transacao compra = new Transacao(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );
        compra.setId(1L);
        compra.setData(LocalDateTime.of(2026, 1, 1, 10, 0));

        Transacao venda = new Transacao(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(60000),
            TipoTransacao.VENDA
        );
        venda.setId(2L);
        venda.setData(LocalDateTime.of(2026, 1, 1, 11, 0));

        List<Transacao> historicoDesordenado = List.of(venda, compra);

        service.reconstruirCarteira(carteira, historicoDesordenado);

        Moeda btc = carteira.obterMoeda("BTC", "Bitcoin");

        assertEquals(0.0, btc.getSaldo().doubleValue(), 0.001);
    }

    @Test
    public void deveInterromperReconstrucaoQuandoHistoricoForInconsistente() {
        CarteiraService service = new CarteiraService(null, null);
        Carteira carteira = new Carteira();

        Transacao vendaSemCompra = new Transacao(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(60000),
            TipoTransacao.VENDA
        );

        vendaSemCompra.setId(99L);
        vendaSemCompra.setData(
            LocalDateTime.of(2026, 1, 1, 10, 0)
        );

        HistoricoInconsistenteException excecao = assertThrows(
            HistoricoInconsistenteException.class,
            () -> service.reconstruirCarteira(
                carteira,
                List.of(vendaSemCompra)
            )
        );

        assertTrue(excecao.getMessage().contains("id=99"));
        assertTrue(excecao.getMessage().contains("ticker=BTC"));
        assertTrue(excecao.getMessage().contains("tipo=VENDA"));

        assertEquals(
            SaldoInsuficienteException.class,
            excecao.getCause().getClass()
        );
    }
}
