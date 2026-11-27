package br.com.criptovision.security;

import br.com.criptovision.exception.TentativasExcedidasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtecaoForcaBrutaTest {

    @Test
    void deveNormalizarOLoginComoOCadastroFaz() {
        assertThat(ProtecaoForcaBruta.chaveDoLogin(" Alexandre.Dev "))
            .isEqualTo("alexandre.dev");
    }

    @Test
    void deveLimitarOTamanhoDaChaveDoLogin() {
        // o corpo da requisição é controlado pelo cliente: sem o corte,
        // logins gigantes ocupariam memória à vontade nos contadores
        String loginGigante = "a".repeat(10_000);

        assertThat(ProtecaoForcaBruta.chaveDoLogin(loginGigante))
            .hasSize(ProtecaoForcaBruta.TAMANHO_MAXIMO_DA_CHAVE);
    }

    @ParameterizedTest
    @CsvSource({
        "0,       1,  1 segundo",   // nunca pede para esperar zero
        "500,     1,  1 segundo",   // arredonda para cima
        "1000,    1,  1 segundo",
        "1001,    2,  2 segundos",
        "59000,   59, 59 segundos",
        "60000,   60, 1 minuto",
        "61000,   61, 2 minutos",
        "900000,  900, 15 minutos"
    })
    void deveInformarTempoDeEsperaArredondadoParaCima(
        long milissegundos,
        long segundosEsperados,
        String descricao
    ) {
        TentativasExcedidasException excecao =
            new TentativasExcedidasException(Duration.ofMillis(milissegundos));

        assertThat(excecao.getSegundosParaNovaTentativa())
            .isEqualTo(segundosEsperados);
        assertThat(excecao.getMessage())
            .isEqualTo("Muitas tentativas. Tente novamente em " + descricao + ".");
    }

    @Test
    void deveImpedirASubidaComLimiteInvalido() {
        new ApplicationContextRunner()
            .withUserConfiguration(HabilitarTentativasProperties.class)
            .withPropertyValues(
                "seguranca.tentativas.login-por-ip.maximo=0",
                "seguranca.tentativas.login-por-ip.janela=1m",
                "seguranca.tentativas.falhas-por-login.maximo=5",
                "seguranca.tentativas.falhas-por-login.janela=15m",
                "seguranca.tentativas.cadastro-por-ip.maximo=10",
                "seguranca.tentativas.cadastro-por-ip.janela=1h"
            )
            .run(ctx -> assertThat(ctx)
                .hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("maior que zero")
            );
    }

    @Test
    void deveImpedirASubidaSemOsLimitesConfigurados() {
        new ApplicationContextRunner()
            .withUserConfiguration(HabilitarTentativasProperties.class)
            .run(ctx -> assertThat(ctx)
                .hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("seguranca.tentativas.login-por-ip")
            );
    }

    @Test
    void deveRecusarJanelaNaoPositiva() {
        assertThatThrownBy(() ->
            new TentativasProperties.Limite(5, Duration.ZERO)
        ).hasMessageContaining("positiva");
    }

    @EnableConfigurationProperties(TentativasProperties.class)
    static class HabilitarTentativasProperties {
    }
}
