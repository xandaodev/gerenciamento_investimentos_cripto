package br.com.criptovision.test;

import br.com.criptovision.model.Transacao;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TransacaoTest {

    @Test
    public void deveCalcularOValorTotalDaCompraCorretamente() {
        String ticker = "BTC";
        double quantidade = 0.5;
        double precoUnitario = 200000.0;
        String tipo = "COMPRA";

        Transacao t = new Transacao(ticker, quantidade, precoUnitario, tipo);
        double valorTotalGasto = t.getQuantidade() * t.getPrecoUnitario();

        assertEquals(100000.0, valorTotalGasto, 0.001); // o 0.001 é a margem de erro
    }
}