package br.com.criptovision.exception;

/**
 * A senha atual informada na troca de senha não confere. Conta como uma falha
 * de login para a proteção contra força bruta.
 */
public class SenhaAtualIncorretaException extends RuntimeException {

    public SenhaAtualIncorretaException() {
        super("A senha atual está incorreta.");
    }
}
