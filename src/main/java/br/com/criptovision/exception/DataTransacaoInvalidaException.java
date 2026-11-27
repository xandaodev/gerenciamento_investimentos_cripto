package br.com.criptovision.exception;

/** A data informada para a transação está no futuro ou antes da criação do Bitcoin. */
public class DataTransacaoInvalidaException extends RuntimeException {

    public DataTransacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
