package br.com.criptovision.test;

import br.com.criptovision.dto.AportePorMoedaProjection;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class TransacaoRepositoryUsuarioTest {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario alexandre;
    private Usuario outroUsuario;

    @BeforeEach
    public void prepararUsuarios() {
        alexandre = usuarioRepository.save(
            new Usuario(
                "alexandre",
                "senha-criptografada-alexandre"
            )
        );

        outroUsuario = usuarioRepository.save(
            new Usuario(
                "outro-usuario",
                "senha-criptografada-outro"
            )
        );
    }

    @Test
    public void deveListarSomenteTransacoesDoUsuarioInformado() {
        salvarTransacao(
            alexandre,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        salvarTransacao(
            alexandre,
            "ETH",
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(3000),
            TipoTransacao.COMPRA
        );

        salvarTransacao(
            outroUsuario,
            "SOL",
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(150),
            TipoTransacao.COMPRA
        );

        List<Transacao> resultado =
            transacaoRepository
                .findAllByUsuarioOrderByDataAscIdAsc(
                    alexandre
                );

        Set<String> tickersEncontrados = resultado
            .stream()
            .map(Transacao::getTicker)
            .collect(Collectors.toSet());

        assertEquals(2, resultado.size());

        assertEquals(
            Set.of("BTC", "ETH"),
            tickersEncontrados
        );
    }

    @Test
    public void naoDeveEncontrarTransacaoDeOutroUsuarioPeloId() {
        Transacao transacaoDoOutroUsuario = salvarTransacao(
            outroUsuario,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        boolean encontradaParaAlexandre =
            transacaoRepository
                .findByIdAndUsuario(
                    transacaoDoOutroUsuario.getId(),
                    alexandre
                )
                .isPresent();

        boolean encontradaParaProprietario =
            transacaoRepository
                .findByIdAndUsuario(
                    transacaoDoOutroUsuario.getId(),
                    outroUsuario
                )
                .isPresent();

        assertFalse(encontradaParaAlexandre);
        assertTrue(encontradaParaProprietario);
    }

    @Test
    public void deveCalcularAportesSomenteDoUsuarioInformado() {
        salvarTransacao(
            alexandre,
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            TipoTransacao.COMPRA
        );

        salvarTransacao(
            alexandre,
            "BTC",
            BigDecimal.valueOf(0.25),
            BigDecimal.valueOf(60000),
            TipoTransacao.VENDA
        );

        salvarTransacao(
            outroUsuario,
            "ETH",
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(3000),
            TipoTransacao.COMPRA
        );

        List<AportePorMoedaProjection> resultado =
            transacaoRepository
                .calcularTotalAportadoPorMoeda(
                    TipoTransacao.COMPRA,
                    alexandre
                );

        assertEquals(1, resultado.size());
        assertEquals("BTC", resultado.get(0).getTicker());
    }

    private Transacao salvarTransacao(
        Usuario usuario,
        String ticker,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        TipoTransacao tipo
    ) {
        Transacao transacao = new Transacao(
            ticker,
            quantidade,
            precoUnitario,
            tipo
        );

        transacao.setUsuario(usuario);

        return transacaoRepository.saveAndFlush(transacao);
    }
}
