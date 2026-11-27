package br.com.criptovision.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesTest {

    // sobe só o binding de propriedades, como o Spring faz na inicialização
    private final ApplicationContextRunner contexto =
        new ApplicationContextRunner()
            .withUserConfiguration(HabilitarCorsProperties.class);

    @EnableConfigurationProperties(CorsProperties.class)
    static class HabilitarCorsProperties {
    }

    @Test
    void deveLerOrigensSeparadasPorVirgulaComoNaVariavelDeAmbiente() {
        contexto
            .withPropertyValues(
                "cors.allowed-origins=https://app.criptovision.com.br, http://localhost:5173"
            )
            .run(ctx -> assertThat(
                ctx.getBean(CorsProperties.class).allowedOrigins()
            ).containsExactly(
                "https://app.criptovision.com.br",
                "http://localhost:5173"
            ));
    }

    @Test
    void deveImpedirASubidaDaAplicacaoComOrigemInvalida() {
        contexto
            .withPropertyValues(
                "cors.allowed-origins=https://app.criptovision.com.br/painel"
            )
            .run(ctx -> assertThat(ctx)
                .hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("https://app.criptovision.com.br/painel")
            );
    }

    @Test
    void deveAceitarOrigensValidasRemovendoEspacosEBarraFinal() {
        CorsProperties propriedades = new CorsProperties(List.of(
            " https://app.criptovision.com.br/ ",
            "http://localhost:5173"
        ));

        assertThat(propriedades.allowedOrigins()).containsExactly(
            "https://app.criptovision.com.br",
            "http://localhost:5173"
        );
    }

    @Test
    void deveIgnorarEntradasVaziasERepetidas() {
        // "a,,a" ou uma vírgula sobrando no fim da variável
        CorsProperties propriedades = new CorsProperties(List.of(
            "http://localhost:5173",
            " ",
            "http://localhost:5173/"
        ));

        assertThat(propriedades.allowedOrigins())
            .containsExactly("http://localhost:5173");
    }

    @Test
    void deveAceitarPorta() {
        CorsProperties propriedades = new CorsProperties(
            List.of("https://app.criptovision.com.br:8443")
        );

        assertThat(propriedades.allowedOrigins())
            .containsExactly("https://app.criptovision.com.br:8443");
    }

    @Test
    void deveRecusarConfiguracaoSemNenhumaOrigem() {
        assertThatThrownBy(() -> new CorsProperties(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");

        assertThatThrownBy(() -> new CorsProperties(List.of(" ", "")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "*",                                      // libera qualquer site
        "https://*.criptovision.com.br",          // curinga
        "app.criptovision.com.br",                // sem esquema
        "https://app.criptovision.com.br/painel", // com caminho
        "https://app.criptovision.com.br?x=1",    // com query
        "ftp://app.criptovision.com.br",          // esquema que o navegador não usa
        "https://"                                // sem host
    })
    void deveRecusarOrigemInvalidaIndicandoQualFoi(String origem) {
        assertThatThrownBy(() -> new CorsProperties(List.of(origem)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("\"" + origem + "\"")
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }
}
