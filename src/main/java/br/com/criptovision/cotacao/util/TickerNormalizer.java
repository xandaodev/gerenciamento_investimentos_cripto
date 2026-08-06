package br.com.criptovision.cotacao.util;

import br.com.criptovision.exception.TickerInvalidoException;

import java.util.Locale;

public final class TickerNormalizer {

    private TickerNormalizer() {
    }

    public static String normalizar(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new TickerInvalidoException("");
        }

        String normalizado = ticker
            .trim()
            .toUpperCase(Locale.ROOT);

        return switch (normalizado) {
            case "BITCOIN", "BTC" -> "BTC";
            case "ETHEREUM", "ETH" -> "ETH";
            case "SOLANA", "SOL" -> "SOL";
            case "CHAINLINK", "LINK", "LNK" -> "LINK";
            default -> normalizado;
        };
    }
}
