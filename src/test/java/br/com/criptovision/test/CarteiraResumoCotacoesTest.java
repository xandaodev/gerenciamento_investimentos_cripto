package br.com.criptovision.test;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.model.ResultadoCotacoes;
import br.com.criptovision.dto.ResumoAtivoDTO;
import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.service.CarteiraService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CarteiraResumoCotacoesTest {

    @Test
    void devePreservarAtivoSemCotacaoSemInventarValores() {
        Carteira carteira = new Carteira();

        Moeda btc = carteira.obterMoeda("BTC", "BTC");
        btc.setSaldo(new BigDecimal("0.001"));
        btc.setPrecoMedio(new BigDecimal("60000"));

        Moeda eth = carteira.obterMoeda("ETH", "ETH");
        eth.setSaldo(new BigDecimal("1.5"));
        eth.setPrecoMedio(new BigDecimal("3000"));

        Instant atualizadaEm =
            Instant.parse("2026-08-06T14:30:00Z");

        ResultadoCotacoes resultadoCotacoes =
            new ResultadoCotacoes(
                Map.of(
                    "BTC",
                    new CotacaoMercado(
                        "BTC",
                        new BigDecimal("65000"),
                        new BigDecimal("2.5"),
                        atualizadaEm,
                        false
                    )
                ),
                Set.of("ETH")
            );

        CarteiraService service =
            new CarteiraService(null, null);

        ResumoCarteiraDTO resumo =
            service.gerarResumoCompleto(
                carteira,
                resultadoCotacoes
            );

        assertEquals(
            0,
            new BigDecimal("65")
                .compareTo(resumo.getValorTotalCarteira())
        );
        assertTrue(resumo.isCotacoesParciais());
        assertEquals(
            Set.of("ETH"),
            Set.copyOf(resumo.getAtivosSemCotacao())
        );

        ResumoAtivoDTO ativoSemCotacao = resumo.getAtivos()
            .stream()
            .filter(ativo -> ativo.getTicker().equals("ETH"))
            .findFirst()
            .orElseThrow();

        assertFalse(ativoSemCotacao.isCotacaoDisponivel());
        assertNull(ativoSemCotacao.getPrecoAtual());
        assertNull(ativoSemCotacao.getValorTotalUSD());
        assertNull(ativoSemCotacao.getPorcentagemPNL());
    }
}
