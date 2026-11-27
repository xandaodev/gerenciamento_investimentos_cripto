package br.com.criptovision.exception;

import java.time.Duration;

/**
 * Lançada quando um IP ou login passa do limite de tentativas em
 * {@code /auth/login} ou {@code /auth/register}. Vira {@code 429 Too Many Requests}.
 *
 * <p>A mensagem é a mesma para qualquer limite: ela não diz se o bloqueio
 * veio do IP ou do login, nem se a conta existe.
 */
public class TentativasExcedidasException extends RuntimeException {

    private final long segundosParaNovaTentativa;

    public TentativasExcedidasException(Duration tempoParaNovaTentativa) {
        this(arredondarParaCima(tempoParaNovaTentativa));
    }

    private TentativasExcedidasException(long segundos) {
        super("Muitas tentativas. Tente novamente em " + descrever(segundos) + ".");
        this.segundosParaNovaTentativa = segundos;
    }

    /** Valor do cabeçalho {@code Retry-After}, sempre ao menos 1 segundo. */
    public long getSegundosParaNovaTentativa() {
        return segundosParaNovaTentativa;
    }

    private static long arredondarParaCima(Duration tempo) {
        long milissegundos = Math.max(tempo.toMillis(), 0);
        return Math.max(1, (milissegundos + 999) / 1000);
    }

    private static String descrever(long segundos) {
        if (segundos < 60) {
            return segundos == 1 ? "1 segundo" : segundos + " segundos";
        }

        long minutos = (segundos + 59) / 60;
        return minutos == 1 ? "1 minuto" : minutos + " minutos";
    }
}
