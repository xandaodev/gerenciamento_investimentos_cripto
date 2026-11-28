package br.com.criptovision.observabilidade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os testes HTTP rodam com a porta de gerenciamento aleatória (perfil test), então
 * não enxergam os padrões do application.properties. Este teste os lê direto,
 * como a produção os usa sem variáveis de ambiente: se o Actuator fosse parar na
 * porta da API, o Caddy o publicaria na internet.
 */
class ConfiguracaoDeProducaoTest {

    private static final Pattern VARIAVEL_COM_PADRAO =
        Pattern.compile("^\\$\\{[A-Z_]+:(.*)}$");

    private static Properties propriedades;

    @BeforeAll
    static void carregar() throws IOException {
        propriedades = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("application.properties")
        );
    }

    @Test
    void actuatorDeveFicarNumaPortaSeparadaSoNoLoopback() {
        assertThat(padrao("management.server.port")).isEqualTo("8081");
        assertThat(padrao("server.port", "8080")).isNotEqualTo(padrao("management.server.port"));
        assertThat(padrao("management.server.address")).isEqualTo("127.0.0.1");
    }

    @Test
    void soHealthEMetricasDevemSerExpostos() {
        // env, configprops, heapdump etc. revelariam segredos ou a memória da JVM
        assertThat(Arrays.asList(padrao("management.endpoints.web.exposure.include").split(",")))
            .containsExactlyInAnyOrder("health", "metrics", "prometheus");
    }

    @Test
    void saudePublicaNaoDeveMostrarDetalhes() {
        assertThat(padrao("management.endpoint.health.group.publico.additional-path"))
            .isEqualTo("server:/saude");
        assertThat(padrao("management.endpoint.health.group.publico.show-components"))
            .isEqualTo("never");
        assertThat(padrao("management.endpoint.health.group.publico.show-details"))
            .isEqualTo("never");
    }

    @Test
    void sqlNaoDeveIrParaOLogPorPadrao() {
        assertThat(padrao("spring.jpa.show-sql")).isEqualTo("false");
    }

    private static String padrao(String chave) {
        String valor = propriedades.getProperty(chave);
        assertThat(valor).as(chave).isNotNull();

        Matcher variavel = VARIAVEL_COM_PADRAO.matcher(valor);
        return variavel.matches() ? variavel.group(1) : valor;
    }

    private static String padrao(String chave, String seAusente) {
        return propriedades.containsKey(chave) ? padrao(chave) : seAusente;
    }
}
