package br.com.criptovision.service;

import br.com.criptovision.dto.SimulacaoDCADTO;
import br.com.criptovision.dto.SimulacaoVendaDTO;
import br.com.criptovision.exception.SemSaldoParaSimularException;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarteiraServiceSimulacaoTest {

    private final CarteiraService service = new CarteiraService(null, null, null, null);

    // --- DCA ----------------------------------------------------------------

    @Test
    void deveSimularAporteComValoresExatos() {
        Moeda eth = comprar("ETH", "2", "3000"); // custo 6.000, preço médio 3.000

        SimulacaoDCADTO simulacao = service.simularDCA(eth, valor("3000"), valor("1500"));

        assertThat(simulacao.getQtdComprada()).isEqualByComparingTo("2");
        assertThat(simulacao.getSaldoAtual()).isEqualByComparingTo("2");
        assertThat(simulacao.getNovoSaldoTotal()).isEqualByComparingTo("4");
        assertThat(simulacao.getPmAtual()).isEqualByComparingTo("3000");
        assertThat(simulacao.getNovoPM()).isEqualByComparingTo("2250");
        assertThat(simulacao.getDiferencaPM()).isEqualByComparingTo("-25");
        assertThat(simulacao.getValorizacaoNecessaria()).isEqualByComparingTo("50");
    }

    @Test
    void deveManterAPrecisaoEmMoedaDePrecoMuitoBaixo() {
        // com double, estes valores perdiam casas
        Moeda pepe = comprar("PEPE", "1000000", "0.0000012345"); // custo 1,2345

        SimulacaoDCADTO simulacao = service.simularDCA(pepe, valor("10"), valor("0.000001"));

        assertThat(simulacao.getQtdComprada()).isEqualByComparingTo("10000000");
        assertThat(simulacao.getNovoSaldoTotal()).isEqualByComparingTo("11000000");
        // 11,2345 ÷ 11.000.000
        assertThat(simulacao.getNovoPM()).isEqualByComparingTo("0.000001021318181818");
        assertThat(simulacao.getDiferencaPM()).isEqualByComparingTo("-17.26867705");
        assertThat(simulacao.getValorizacaoNecessaria()).isEqualByComparingTo("2.13181818");
    }

    @Test
    void deveSimularAPrimeiraCompraSemPosicaoAnterior() {
        Moeda sol = new Moeda("SOL", "SOL");

        SimulacaoDCADTO simulacao = service.simularDCA(sol, valor("100"), valor("50"));

        // sem posição não há preço médio atual: null, e não 0 nem NaN
        assertThat(simulacao.getPmAtual()).isNull();
        assertThat(simulacao.getDiferencaPM()).isNull();
        assertThat(simulacao.getSaldoAtual()).isEqualByComparingTo("0");
        assertThat(simulacao.getQtdComprada()).isEqualByComparingTo("2");
        assertThat(simulacao.getNovoPM()).isEqualByComparingTo("50");
        assertThat(simulacao.getValorizacaoNecessaria()).isEqualByComparingTo("0");
    }

    @Test
    void deveSimularAporteDepoisDeVenderTudo() {
        // vendida toda a posição, a simulação volta a ser a de uma primeira compra
        Moeda btc = comprar("BTC", "1", "50000");
        processar(btc, "1", "60000", TipoTransacao.VENDA);

        SimulacaoDCADTO simulacao = service.simularDCA(btc, valor("1000"), valor("40000"));

        assertThat(simulacao.getPmAtual()).isNull();
        assertThat(simulacao.getDiferencaPM()).isNull();
        assertThat(simulacao.getNovoPM()).isEqualByComparingTo("40000");
    }

    @Test
    void deveRecusarAporteOuPrecoQueNaoSejamPositivos() {
        Moeda eth = comprar("ETH", "1", "2000");

        assertThatThrownBy(() -> service.simularDCA(eth, BigDecimal.ZERO, valor("1500")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("aporte");
        assertThatThrownBy(() -> service.simularDCA(eth, valor("100"), valor("-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("preço de mercado");
        assertThatThrownBy(() -> service.simularDCA(eth, null, valor("1500")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- venda --------------------------------------------------------------

    @Test
    void deveSimularVendaComLucro() {
        Moeda eth = comprar("ETH", "1", "2000");

        SimulacaoVendaDTO simulacao = service.simularVendaFutura(eth, valor("3000"), valor("2500"));

        assertThat(simulacao.getLucroEstimado()).isEqualByComparingTo("1000");
        assertThat(simulacao.getPorcentagemLucro()).isEqualByComparingTo("50");
        assertThat(simulacao.getValorTotalFicticio()).isEqualByComparingTo("3000");
        assertThat(simulacao.getValorTotalAtual()).isEqualByComparingTo("2500");
    }

    @Test
    void deveSimularVendaComPrejuizo() {
        Moeda eth = comprar("ETH", "1", "2000");

        SimulacaoVendaDTO simulacao = service.simularVendaFutura(eth, valor("1500"), valor("2500"));

        assertThat(simulacao.getLucroEstimado()).isEqualByComparingTo("-500");
        assertThat(simulacao.getPorcentagemLucro()).isEqualByComparingTo("-25");
    }

    @Test
    void deveArredondarOPercentualComOitoCasas() {
        Moeda eth = comprar("ETH", "3", "100"); // custo 300

        // 3 × 133,33 = 399,99; lucro de 99,99, ou 33,33% do custo
        SimulacaoVendaDTO simulacao = service.simularVendaFutura(eth, valor("133.33"), valor("120"));

        assertThat(simulacao.getLucroEstimado()).isEqualByComparingTo("99.99");
        assertThat(simulacao.getPorcentagemLucro()).isEqualByComparingTo("33.33");
        assertThat(simulacao.getPorcentagemLucro().scale()).isEqualTo(8);
    }

    @Test
    void deveRecusarVendaSemSaldo() {
        Moeda sol = new Moeda("SOL", "SOL");

        assertThatThrownBy(() -> service.simularVendaFutura(sol, valor("200"), valor("150")))
            .isInstanceOf(SemSaldoParaSimularException.class)
            .hasMessageContaining("SOL");
    }

    @Test
    void deveRecusarPrecoAlvoQueNaoSejaPositivo() {
        Moeda eth = comprar("ETH", "1", "2000");

        assertThatThrownBy(() -> service.simularVendaFutura(eth, BigDecimal.ZERO, valor("2500")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("preço-alvo");
    }

    @Test
    void deveCalcularLucroPotencialZeroSemSaldo() {
        assertThat(service.calcularLucroPotencial(new Moeda("SOL", "SOL"), valor("150")))
            .isEqualByComparingTo("0");
    }

    // -------------------------------------------------------------------------

    private Moeda comprar(String ticker, String quantidade, String preco) {
        Moeda moeda = new Moeda(ticker, ticker);
        processar(moeda, quantidade, preco, TipoTransacao.COMPRA);
        return moeda;
    }

    private void processar(Moeda moeda, String quantidade, String preco, TipoTransacao tipo) {
        service.processarTransacao(
            moeda,
            new Transacao(moeda.getTicker(), valor(quantidade), valor(preco), tipo),
            false
        );
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
