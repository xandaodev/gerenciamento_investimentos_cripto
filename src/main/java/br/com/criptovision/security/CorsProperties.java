package br.com.criptovision.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Origens do frontend autorizadas a chamar a API pelo navegador (CORS).
 *
 * <p>Vem de {@code cors.allowed-origins}, alimentada pela variável de ambiente
 * {@code CORS_ALLOWED_ORIGINS} (origens separadas por vírgula). Uma configuração
 * inválida impede a aplicação de subir: uma origem mal escrita nunca casaria com
 * o cabeçalho {@code Origin} do navegador, e o frontend falharia sem explicação.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    // esquema http ou https, host e porta opcional: o formato do cabeçalho Origin.
    // não aceita caminho, query, fragmento nem curinga
    private static final Pattern ORIGEM =
        Pattern.compile("^https?://[^/\\s*?#]+$");

    private static final String EXEMPLO = "https://app.exemplo.com";

    public CorsProperties {
        if (allowedOrigins == null) {
            throw new IllegalArgumentException(mensagemListaVazia());
        }

        allowedOrigins = allowedOrigins.stream()
            .map(String::trim)
            .filter(origem -> !origem.isEmpty())
            .map(CorsProperties::normalizar)
            .distinct()
            .toList();

        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(mensagemListaVazia());
        }
    }

    private static String normalizar(String origem) {
        // o navegador nunca envia a barra final no Origin, então ela é só removida
        String semBarraFinal = origem.endsWith("/")
            ? origem.substring(0, origem.length() - 1)
            : origem;

        // a mensagem mostra o valor como foi digitado, para ser achado na configuração
        if (!ORIGEM.matcher(semBarraFinal).matches()) {
            throw new IllegalArgumentException(
                "Origem de CORS inválida em cors.allowed-origins "
                    + "(variável CORS_ALLOWED_ORIGINS): \"" + origem + "\". "
                    + "Informe esquema, host e porta opcional, sem caminho nem curinga, "
                    + "por exemplo " + EXEMPLO + "."
            );
        }

        return semBarraFinal;
    }

    private static String mensagemListaVazia() {
        return "Defina ao menos uma origem em cors.allowed-origins "
            + "(variável CORS_ALLOWED_ORIGINS), por exemplo " + EXEMPLO + ".";
    }
}
