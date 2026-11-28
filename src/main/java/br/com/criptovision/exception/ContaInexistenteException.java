package br.com.criptovision.exception;

/**
 * O usuário do token não existe mais: a conta foi excluída enquanto a requisição
 * estava em andamento (por exemplo, uma transação esperando o bloqueio do usuário
 * que a exclusão segurava). Responde 401, como um token de conta inexistente.
 */
public class ContaInexistenteException extends RuntimeException {

    public ContaInexistenteException() {
        super("A conta deste token não existe mais.");
    }
}
