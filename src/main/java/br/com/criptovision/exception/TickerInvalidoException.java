package br.com.criptovision.exception;

public class TickerInvalidoException extends RuntimeException {

    public TickerInvalidoException(String ticker) {
        super(
            "A criptomoeda '" + ticker
                + "' não possui cotação disponível no provedor."
        );
    }
}
