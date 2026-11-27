package br.com.criptovision.exception;

/** A nova senha informada na troca de senha é igual à atual. */
public class NovaSenhaInvalidaException extends RuntimeException {

    public NovaSenhaInvalidaException() {
        super("A nova senha deve ser diferente da atual.");
    }
}
