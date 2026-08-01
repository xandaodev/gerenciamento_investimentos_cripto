package br.com.criptovision.test;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransacaoTest {

    @Test
    public void deveCalcularOValorTotalDaCompraCorretamente() {
        String ticker = "BTC";
        BigDecimal quantidade = BigDecimal.valueOf(0.5);
        BigDecimal precoUnitario = BigDecimal.valueOf(200000.0);

        Transacao transacao = new Transacao(
            ticker,
            quantidade,
            precoUnitario,
            TipoTransacao.COMPRA
        );

        double valorTotalGasto = transacao
            .getQuantidade()
            .multiply(transacao.getPrecoUnitario())
            .doubleValue();

        assertEquals(100000.0, valorTotalGasto, 0.001);
    }

    @Test
    public void deveNormalizarTipoDaTransacao() {
        assertEquals(
            TipoTransacao.COMPRA,
            TipoTransacao.fromValue(" compra ")
        );

        assertEquals(
            TipoTransacao.VENDA,
            TipoTransacao.fromValue("venda")
        );
    }


    @Test
    public void deveRejeitarTipoDeTransacaoInvalido() {
        IllegalArgumentException excecao = assertThrows(
            IllegalArgumentException.class,
            () -> TipoTransacao.fromValue("TROCA")
        );

        assertEquals(
            "Tipo de transação inválido. Use COMPRA ou VENDA.",
            excecao.getMessage()
        );
    }
}
