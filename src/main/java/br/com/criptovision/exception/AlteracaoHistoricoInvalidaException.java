package br.com.criptovision.exception;

public class AlteracaoHistoricoInvalidaException
    extends CriptoException {

    public AlteracaoHistoricoInvalidaException(
        String mensagem,
        Throwable causa
    ) {
        super(mensagem, causa);
    }
}
