package br.com.criptovision.exception;

public class BancoDeDadosException extends CriptoException {
    public BancoDeDadosException(String mensagem){
        super(mensagem);
    }

    public BancoDeDadosException(String mensagem, Throwable causa){
        super(mensagem, causa);
    }
}