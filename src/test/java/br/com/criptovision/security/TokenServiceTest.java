package br.com.criptovision.security;

import br.com.criptovision.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-dos-testes-com-mais-de-32-bytes";
    private static final Instant AGORA = Instant.parse("2026-11-27T12:00:00Z");

    private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final TokenService tokenService = new TokenService(SEGREDO, relogio);

    @Test
    void deveImpedirASubidaComSegredoCurto() {
        // 31 bytes: um a menos que o mínimo do HMAC-SHA256
        assertThatThrownBy(() -> new TokenService("a".repeat(31), relogio))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET precisa ter ao menos 32 bytes");

        assertThatThrownBy(() -> new TokenService(null, relogio))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveImpedirASubidaComOSegredoDeExemplo() {
        assertThatThrownBy(() ->
            new TokenService(TokenService.SEGREDO_DE_EXEMPLO, relogio)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("valor de exemplo");
    }

    @Test
    void deveDevolverLoginEVersaoDeUmTokenValido() {
        Usuario usuario = new Usuario("alexandre", "hash");
        usuario.revogarTokens();
        usuario.revogarTokens(); // versão 2

        String token = tokenService.gerarToken(usuario);

        assertThat(tokenService.validarToken(token))
            .contains(new TokenService.TokenValido("alexandre", 2));
        assertThat(JWT.decode(token).getAudience())
            .containsExactly(TokenService.AUDIENCIA);
    }

    @Test
    void deveRecusarTokenDeOutraAudiencia() {
        String token = assinar(JWT.create()
            .withIssuer(TokenService.EMISSOR)
            .withAudience("outro-sistema")
            .withSubject("alexandre")
            .withClaim(TokenService.CLAIM_VERSAO, 0L)
            .withExpiresAt(AGORA.plusSeconds(60)));

        assertThat(tokenService.validarToken(token)).isEmpty();
    }

    @Test
    void deveRecusarTokenSemAVersao() {
        // como os tokens emitidos antes desta mudança
        String token = assinar(JWT.create()
            .withIssuer(TokenService.EMISSOR)
            .withAudience(TokenService.AUDIENCIA)
            .withSubject("alexandre")
            .withExpiresAt(AGORA.plusSeconds(60)));

        assertThat(tokenService.validarToken(token)).isEmpty();
    }

    @Test
    void deveRecusarTokenAssinadoComOutroSegredo() {
        TokenService outro = new TokenService(
            "outro-segredo-tambem-com-mais-de-32-bytes", relogio
        );

        String token = outro.gerarToken(new Usuario("alexandre", "hash"));

        assertThat(tokenService.validarToken(token)).isEmpty();
    }

    @Test
    void deveExpirarDepoisDeDuasHoras() {
        String token = tokenService.gerarToken(new Usuario("alexandre", "hash"));

        TokenService quaseDuasHorasDepois = new TokenService(SEGREDO, Clock.fixed(
            AGORA.plus(TokenService.VALIDADE).minusSeconds(1), ZoneOffset.UTC
        ));
        TokenService duasHorasDepois = new TokenService(SEGREDO, Clock.fixed(
            AGORA.plus(TokenService.VALIDADE).plus(Duration.ofSeconds(1)), ZoneOffset.UTC
        ));

        assertThat(quaseDuasHorasDepois.validarToken(token)).isPresent();
        assertThat(duasHorasDepois.validarToken(token)).isEmpty();
    }

    @Test
    void deveRecusarTextoQueNaoEUmToken() {
        assertThat(tokenService.validarToken("nao-e-um-jwt")).isEmpty();
    }

    private static String assinar(com.auth0.jwt.JWTCreator.Builder construtor) {
        return construtor.sign(Algorithm.HMAC256(SEGREDO));
    }
}
