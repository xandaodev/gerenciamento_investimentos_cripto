package br.com.criptovision.service;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.model.ResultadoCotacoes;
import br.com.criptovision.dto.ResumoAtivoDTO;
import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CarteiraServicePrecoMedioTest {

    private final CarteiraService service =
        new CarteiraService(null, null, null, null, null);

    @Test
    void deveManterPrecoMedioExatoDeMoedaComPrecoMuitoBaixo() {
        Moeda pepe = new Moeda("PEPE", "PEPE");

        comprar(pepe, "1000000", "0.0000012345");

        assertThat(pepe.getPrecoMedio())
            .isEqualByComparingTo("0.0000012345");
    }

    @Test
    void naoDeveAcumularErroDeArredondamentoEmComprasSeguidas() {
        Moeda pepe = new Moeda("PEPE", "PEPE");

        comprar(pepe, "1000000", "0.0000012345"); // custo 1.2345
        comprar(pepe, "2500000", "0.0000011111"); // custo 2.77775
        comprar(pepe, "700000", "0.0000013579");  // custo 0.95053

        // 4.96278 ÷ 4.200.000 = 0.00000118161428571428...
        assertThat(pepe.getSaldo()).isEqualByComparingTo("4200000");
        assertThat(pepe.getCustoTotal()).isEqualByComparingTo("4.96278");
        assertThat(pepe.getPrecoMedio())
            .isEqualByComparingTo("0.000001181614285714");
    }

    @Test
    void deveManterPrecoMedioAposVendaParcial() {
        Moeda sol = new Moeda("SOL", "SOL");

        comprar(sol, "3", "100");
        vender(sol, "1", "150");

        assertThat(sol.getSaldo()).isEqualByComparingTo("2");
        assertThat(sol.getCustoTotal()).isEqualByComparingTo("200");
        assertThat(sol.getPrecoMedio()).isEqualByComparingTo("100");
    }

    @Test
    void deveManterPrecoMedioExatoAposVendaParcialEComprasSeguintes() {
        Moeda eth = new Moeda("ETH", "ETH");

        comprar(eth, "1", "10");
        comprar(eth, "2", "20"); // custo 50, saldo 3
        vender(eth, "1", "30");  // custo restante 50 × 2/3 = 33.333...
        comprar(eth, "3", "40"); // custo 153.333..., saldo 5

        assertThat(eth.getSaldo()).isEqualByComparingTo("5");
        assertThat(eth.getPrecoMedio())
            .isEqualByComparingTo("30.666666666666666667");
    }

    @Test
    void deveZerarPrecoMedioQuandoPosicaoForTotalmenteVendida() {
        Moeda btc = new Moeda("BTC", "BTC");

        comprar(btc, "2", "100");
        vender(btc, "2", "150");

        assertThat(btc.getSaldo()).isEqualByComparingTo("0");
        assertThat(btc.getCustoTotal()).isEqualByComparingTo("0");
        assertThat(btc.getPrecoMedio()).isEqualByComparingTo("0");

        comprar(btc, "1", "200");

        assertThat(btc.getPrecoMedio()).isEqualByComparingTo("200");
    }

    @Test
    void deveCalcularPnlSemErroEmMoedaComPrecoMuitoBaixo() {
        Carteira carteira = new Carteira();
        Moeda pepe = carteira.obterMoeda("PEPE", "PEPE");

        comprar(pepe, "1000000", "0.0000012345"); // custo 1.2345

        ResumoCarteiraDTO resumo = service.gerarResumoCompleto(
            carteira,
            new ResultadoCotacoes(
                Map.of(
                    "PEPE",
                    new CotacaoMercado(
                        "PEPE",
                        new BigDecimal("0.0000015"),
                        BigDecimal.ZERO,
                        Instant.parse("2026-11-26T12:00:00Z"),
                        false
                    )
                ),
                Set.of()
            )
        );

        ResumoAtivoDTO ativo = resumo.getAtivos().getFirst();

        // valor atual 1.5 − custo 1.2345 = 0.2655, ou 21.50668287% do custo
        assertThat(resumo.getValorTotalCarteira()).isEqualByComparingTo("1.5");
        assertThat(resumo.getPnlGeral()).isEqualByComparingTo("0.2655");
        assertThat(ativo.getPorcentagemPNL())
            .isEqualByComparingTo("21.50668287");
        assertThat(ativo.getPrecoMedio())
            .isEqualByComparingTo("0.0000012345");
    }

    private void comprar(Moeda moeda, String quantidade, String preco) {
        processar(moeda, quantidade, preco, TipoTransacao.COMPRA);
    }

    private void vender(Moeda moeda, String quantidade, String preco) {
        processar(moeda, quantidade, preco, TipoTransacao.VENDA);
    }

    private void processar(
        Moeda moeda,
        String quantidade,
        String preco,
        TipoTransacao tipo
    ) {
        service.processarTransacao(
            moeda,
            new Transacao(
                moeda.getTicker(),
                new BigDecimal(quantidade),
                new BigDecimal(preco),
                tipo
            ),
            false
        );
    }
}
