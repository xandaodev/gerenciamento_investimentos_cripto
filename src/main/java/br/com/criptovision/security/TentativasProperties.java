package br.com.criptovision.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Limites da proteção contra força bruta em {@code /auth/login} e
 * {@code /auth/register} (propriedades {@code seguranca.tentativas.*}).
 */
@ConfigurationProperties(prefix = "seguranca.tentativas")
public record TentativasProperties(
    Limite loginPorIp,
    Limite falhasPorLogin,
    Limite cadastroPorIp
) {

    public TentativasProperties {
        exigir(loginPorIp, "login-por-ip");
        exigir(falhasPorLogin, "falhas-por-login");
        exigir(cadastroPorIp, "cadastro-por-ip");
    }

    private static void exigir(Limite limite, String nome) {
        if (limite == null) {
            throw new IllegalArgumentException(
                "Defina seguranca.tentativas." + nome + ".maximo e .janela."
            );
        }
    }

    /** No máximo {@code maximo} eventos a cada {@code janela}. */
    public record Limite(int maximo, Duration janela) {

        public Limite {
            if (maximo < 1) {
                throw new IllegalArgumentException(
                    "O máximo de tentativas deve ser maior que zero: " + maximo
                );
            }

            if (janela == null || janela.isZero() || janela.isNegative()) {
                throw new IllegalArgumentException(
                    "A janela de tentativas deve ser positiva: " + janela
                );
            }
        }
    }
}
