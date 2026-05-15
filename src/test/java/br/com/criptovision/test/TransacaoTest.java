package br.com.criptovision.test;

import br.com.criptovision.model.Transacao;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransacaoTest {

    @Test
    public void deveCalcularOValorTotalDaCompraCorretamente() {
        String ticker = "BTC";
        BigDecimal quantidade = BigDecimal.valueOf(0.5);
        BigDecimal precoUnitario = BigDecimal.valueOf(200000.0);
        String tipo = "COMPRA";

        Transacao t = new Transacao(ticker, quantidade, precoUnitario, tipo);
        double valorTotalGasto = t.getQuantidade().multiply(t.getPrecoUnitario()).doubleValue();

        assertEquals(100000.0, valorTotalGasto, 0.001); // o 0.001 é a margem de erro
    }
}