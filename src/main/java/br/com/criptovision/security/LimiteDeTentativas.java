package br.com.criptovision.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Conta eventos por chave (um IP, um login) em janelas fixas: no máximo
 * {@code maximo} eventos a cada {@code janela}. A janela começa no primeiro
 * evento e, quando o limite é atingido, a chave fica bloqueada até ela fechar.
 * Tentativas recusadas não estendem o bloqueio.
 *
 * <p>Os contadores ficam em memória, limitados a {@value #MAXIMO_DE_CHAVES}
 * chaves, para que um ataque com milhões de IPs ou logins diferentes não
 * esgote a memória. O tempo vem do {@link Clock}, para os testes o controlarem.
 */
final class LimiteDeTentativas {

    static final long MAXIMO_DE_CHAVES = 100_000;

    private static final Logger LOGGER =
        LoggerFactory.getLogger(LimiteDeTentativas.class);

    private final String nome;
    private final int maximo;
    private final Duration janela;
    private final Clock clock;
    private final Cache<String, Contagem> contagens;

    LimiteDeTentativas(String nome, int maximo, Duration janela, Clock clock) {
        this.nome = nome;
        this.maximo = maximo;
        this.janela = janela;
        this.clock = clock;
        // a expiração só libera memória; quem decide o bloqueio é o Clock
        this.contagens = Caffeine.newBuilder()
            .maximumSize(MAXIMO_DE_CHAVES)
            .expireAfterWrite(janela)
            .build();
    }

    /**
     * Registra uma tentativa, se a chave não estiver bloqueada.
     *
     * @return vazio se a tentativa foi aceita, ou quanto falta para a chave ser liberada
     */
    Optional<Duration> tentar(String chave) {
        Instant agora = clock.instant();
        boolean[] recusada = {false};

        // compute é atômico por chave: duas requisições simultâneas não
        // conseguem passar as duas pela última vaga
        Contagem depois = contagens.asMap().compute(chave, (k, atual) -> {
            Contagem vigente = vigente(atual, agora);

            if (vigente != null && vigente.quantidade() >= maximo) {
                recusada[0] = true;
                return vigente; // bloqueada: recusa sem contar
            }

            return contar(vigente, agora);
        });

        if (recusada[0]) {
            return Optional.of(tempoParaLiberar(depois, agora));
        }

        avisarSeAtingiuOLimite(chave, depois, agora);
        return Optional.empty();
    }

    /** Registra um evento sem consultar o bloqueio (por exemplo, uma falha de login). */
    void registrar(String chave) {
        Instant agora = clock.instant();

        Contagem depois = contagens.asMap().compute(chave, (k, atual) ->
            contar(vigente(atual, agora), agora)
        );

        avisarSeAtingiuOLimite(chave, depois, agora);
    }

    /** Informa se a chave está bloqueada, sem registrar nada. */
    Optional<Duration> bloqueio(String chave) {
        Instant agora = clock.instant();
        Contagem atual = contagens.getIfPresent(chave);

        return bloqueada(atual, agora)
            ? Optional.of(tempoParaLiberar(atual, agora))
            : Optional.empty();
    }

    void limpar(String chave) {
        contagens.invalidate(chave);
    }

    private Contagem vigente(Contagem contagem, Instant agora) {
        return contagem == null || agora.isBefore(contagem.inicio().plus(janela))
            ? contagem
            : null; // a janela fechou
    }

    private Contagem contar(Contagem vigente, Instant agora) {
        return vigente == null
            ? new Contagem(agora, 1)
            : new Contagem(vigente.inicio(), vigente.quantidade() + 1);
    }

    private boolean bloqueada(Contagem contagem, Instant agora) {
        Contagem vigente = vigente(contagem, agora);
        return vigente != null && vigente.quantidade() >= maximo;
    }

    private Duration tempoParaLiberar(Contagem contagem, Instant agora) {
        return Duration.between(agora, contagem.inicio().plus(janela));
    }

    // um aviso por chave e janela, e não um por tentativa recusada,
    // para que um ataque não lote o log
    private void avisarSeAtingiuOLimite(String chave, Contagem contagem, Instant agora) {
        if (contagem.quantidade() == maximo) {
            LOGGER.warn(
                "Limite de {} atingido: chave={}, bloqueada por {}s",
                nome,
                paraLog(chave),
                tempoParaLiberar(contagem, agora).toSeconds()
            );
        }
    }

    // a chave pode vir do corpo da requisição: só caracteres seguros vão para o log
    private static String paraLog(String chave) {
        return chave.replaceAll("[^A-Za-z0-9._@:-]", "?");
    }

    private record Contagem(Instant inicio, int quantidade) {
    }
}
