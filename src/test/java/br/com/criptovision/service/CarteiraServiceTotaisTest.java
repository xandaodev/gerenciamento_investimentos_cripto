package br.com.criptovision.service;

import br.com.criptovision.dto.TotaisCarteiraDTO;
import br.com.criptovision.exception.HistoricoInconsistenteException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteiraServiceTotaisTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    private final Usuario usuario = new Usuario("alexandre", "hash");
    private final List<Transacao> historico = new ArrayList<>();

    @Test
    void deveSepararCustoDasPosicoesAportesEVendas() {
        operar("BTC", "1", "50000", TipoTransacao.COMPRA);
        operar("BTC", "0.5", "40000", TipoTransacao.VENDA);

        TotaisCarteiraDTO totais = calcular();

        // o cálculo antigo devolvia 50.000 - 20.000 = 30.000
        assertThat(totais.custoDasPosicoes()).isEqualByComparingTo("25000");
        assertThat(totais.totalAportado()).isEqualByComparingTo("50000");
        assertThat(totais.totalRecebidoEmVendas()).isEqualByComparingTo("20000");
        assertThat(totais.moeda()).isEqualTo("USDT");
    }

    @Test
    void deveSomarOCustoDeVariosAtivos() {
        operar("BTC", "3", "100", TipoTransacao.COMPRA);  // custo 300
        operar("BTC", "1", "200", TipoTransacao.VENDA);   // custo restante 200
        operar("ETH", "2", "1500", TipoTransacao.COMPRA); // custo 3.000

        TotaisCarteiraDTO totais = calcular();

        assertThat(totais.custoDasPosicoes()).isEqualByComparingTo("3200");
        assertThat(totais.totalAportado()).isEqualByComparingTo("3300");
        assertThat(totais.totalRecebidoEmVendas()).isEqualByComparingTo("200");
    }

    @Test
    void deveZerarOCustoDeUmaPosicaoTotalmenteVendida() {
        operar("ETH", "1", "1000", TipoTransacao.COMPRA);
        operar("ETH", "1", "3000", TipoTransacao.VENDA);

        TotaisCarteiraDTO totais = calcular();

        // o cálculo antigo devolvia 1.000 - 3.000 = -2.000
        assertThat(totais.custoDasPosicoes()).isEqualByComparingTo("0");
        assertThat(totais.totalAportado()).isEqualByComparingTo("1000");
        assertThat(totais.totalRecebidoEmVendas()).isEqualByComparingTo("3000");
    }

    @Test
    void deveSomarSemPerderCasasDecimais() {
        operar("PEPE", "1000000", "0.000001234500000001", TipoTransacao.COMPRA);

        TotaisCarteiraDTO totais = calcular();

        assertThat(totais.totalAportado()).isEqualByComparingTo("1.234500000001");
        assertThat(totais.custoDasPosicoes()).isEqualByComparingTo("1.234500000001");
    }

    @Test
    void deveDevolverZerosParaCarteiraVazia() {
        TotaisCarteiraDTO totais = calcular();

        assertThat(totais.custoDasPosicoes()).isEqualByComparingTo("0");
        assertThat(totais.totalAportado()).isEqualByComparingTo("0");
        assertThat(totais.totalRecebidoEmVendas()).isEqualByComparingTo("0");
    }

    @Test
    void deveRecusarHistoricoInconsistente() {
        // uma venda sem compra antes: os totais não são calculados sobre dados inválidos
        operar("BTC", "1", "50000", TipoTransacao.VENDA);

        assertThatThrownBy(this::calcular)
            .isInstanceOf(HistoricoInconsistenteException.class);
    }

    private TotaisCarteiraDTO calcular() {
        when(transacaoRepository.findAllByUsuarioOrderByDataAscIdAsc(usuario))
            .thenReturn(historico);

        return new CarteiraService(transacaoRepository, null, null, null, null)
            .calcularTotais(usuario);
    }

    private void operar(String ticker, String quantidade, String preco, TipoTransacao tipo) {
        Transacao transacao = new Transacao(
            ticker, new BigDecimal(quantidade), new BigDecimal(preco), tipo
        );
        transacao.setId((long) historico.size() + 1);
        transacao.setData(LocalDateTime.of(2026, 1, 1, 10, 0).plusMinutes(historico.size()));
        historico.add(transacao);
    }
}
