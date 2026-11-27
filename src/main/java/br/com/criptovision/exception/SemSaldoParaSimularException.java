package br.com.criptovision.exception;

/** A simulação de venda foi pedida para um ativo que o usuário não tem. */
public class SemSaldoParaSimularException extends RuntimeException {

    public SemSaldoParaSimularException(String ticker) {
        super("Não há saldo de " + ticker + " na carteira para simular a venda.");
    }
}
