package br.com.criptovision.exception;

public class TransacaoNaoEncontradaException extends CriptoException {

    public TransacaoNaoEncontradaException(Long id) {
        super("Transação não encontrada para o ID " + id + ".");
    }
}
