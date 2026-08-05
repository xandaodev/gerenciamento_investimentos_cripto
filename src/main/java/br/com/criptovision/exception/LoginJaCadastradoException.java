package br.com.criptovision.exception;

public class LoginJaCadastradoException extends RuntimeException {

    public LoginJaCadastradoException() {
        super("O login informado já está cadastrado.");
    }
}
