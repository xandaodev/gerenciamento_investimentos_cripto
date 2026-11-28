package br.com.criptovision.exception;

/**
 * A senha pedida para confirmar a exclusão da conta não confere. Conta como uma
 * falha de login para a proteção contra força bruta: quem roubar um token não
 * consegue apagar a conta nem descobrir a senha testando-a aqui.
 */
public class SenhaDeConfirmacaoIncorretaException extends RuntimeException {

    public SenhaDeConfirmacaoIncorretaException() {
        super("A senha está incorreta. A conta não foi excluída.");
    }
}
