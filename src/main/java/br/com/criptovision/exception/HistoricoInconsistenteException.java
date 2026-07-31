package br.com.criptovision.exception;

public class HistoricoInconsistenteException extends br.com.criptovision.exception.CriptoException {

    public HistoricoInconsistenteException(
        Long transacaoId,
        String ticker,
        String tipo,
        Throwable causa
    ) {
        super(criarMensagem(transacaoId, ticker, tipo, causa), causa);
    }

    private static String criarMensagem(
        Long transacaoId,
        String ticker,
        String tipo,
        Throwable causa
    ) {
        String idFormatado = transacaoId == null
            ? "sem-id"
            : transacaoId.toString();

        return "Não foi possível reconstruir a carteira. "
            + "Transação inconsistente "
            + "[id=" + idFormatado
            + ", ticker=" + ticker
            + ", tipo=" + tipo
            + "]. Motivo: " + causa.getMessage();
    }
}
