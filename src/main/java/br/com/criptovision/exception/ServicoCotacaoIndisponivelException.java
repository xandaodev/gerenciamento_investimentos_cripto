package br.com.criptovision.exception;

public class ServicoCotacaoIndisponivelException
    extends RuntimeException {

    public ServicoCotacaoIndisponivelException() {
        super(
            "O serviço de cotações está temporariamente indisponível."
        );
    }

    public ServicoCotacaoIndisponivelException(Throwable cause) {
        super(
            "O serviço de cotações está temporariamente indisponível.",
            cause
        );
    }
}
