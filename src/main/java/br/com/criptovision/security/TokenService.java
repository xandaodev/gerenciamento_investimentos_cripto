package br.com.criptovision.security;

import br.com.criptovision.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Emite e valida os tokens JWT (HMAC-SHA256).
 *
 * <p>Cada token tem emissor, audiência, o login como {@code subject}, a versão dos
 * tokens do usuário e expira em 2 horas. A versão é comparada com a do banco a
 * cada requisição ({@code SecurityFilter}): logout e troca de senha a incrementam
 * e revogam todos os tokens anteriores.
 */
@Service
public class TokenService {

    static final String EMISSOR = "CriptoVision API";
    static final String AUDIENCIA = "criptovision-api";
    static final String CLAIM_VERSAO = "versao";
    static final Duration VALIDADE = Duration.ofHours(2);

    // o HMAC-SHA256 exige uma chave de ao menos 256 bits (RFC 7518, seção 3.2)
    static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    // valor de exemplo do .env.example: nunca pode ir para produção
    static final String SEGREDO_DE_EXEMPLO =
        "replace-with-a-long-random-secret-at-least-32-characters";

    private final Algorithm algoritmo;
    private final JWTVerifier verificador;
    private final Clock clock;

    public TokenService(
        @Value("${api.security.token.secret}") String segredo,
        Clock clock
    ) {
        validarSegredo(segredo);
        this.algoritmo = Algorithm.HMAC256(segredo);
        this.clock = clock;
        this.verificador = ((JWTVerifier.BaseVerification) JWT.require(algoritmo)
            .withIssuer(EMISSOR)
            .withAudience(AUDIENCIA)
            .withClaimPresence(CLAIM_VERSAO))
            .build(clock);
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = clock.instant();

        try {
            return JWT.create()
                .withIssuer(EMISSOR)
                .withAudience(AUDIENCIA)
                .withSubject(usuario.getLogin())
                .withClaim(CLAIM_VERSAO, usuario.getVersaoToken())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plus(VALIDADE))
                .sign(algoritmo);
        } catch (JWTCreationException ex) {
            throw new IllegalStateException("Erro ao gerar o token JWT.", ex);
        }
    }

    /**
     * Confere assinatura, emissor, audiência, expiração e a presença da versão.
     * A comparação da versão com a do banco fica no {@code SecurityFilter}.
     */
    public Optional<TokenValido> validarToken(String token) {
        try {
            DecodedJWT decodificado = verificador.verify(token);
            String login = decodificado.getSubject();
            Claim versao = decodificado.getClaim(CLAIM_VERSAO);

            if (login == null || login.isBlank() || versao.asLong() == null) {
                return Optional.empty();
            }

            return Optional.of(new TokenValido(login, versao.asLong()));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

    private static void validarSegredo(String segredo) {
        if (segredo == null
            || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                "JWT_SECRET precisa ter ao menos " + TAMANHO_MINIMO_DO_SEGREDO
                    + " bytes para assinar os tokens com HMAC-SHA256. "
                    + "Gere um com: openssl rand -base64 48"
            );
        }

        if (segredo.equals(SEGREDO_DE_EXEMPLO)) {
            throw new IllegalStateException(
                "JWT_SECRET ainda é o valor de exemplo do .env.example. "
                    + "Gere um segredo próprio com: openssl rand -base64 48"
            );
        }
    }

    /** Login e versão de um token com assinatura e prazo válidos. */
    public record TokenValido(String login, long versao) {
    }
}
