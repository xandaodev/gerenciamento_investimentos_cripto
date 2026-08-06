package br.com.criptovision.cotacao.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record CotacaoMercado(
    String ticker,
    BigDecimal preco,
    BigDecimal variacao24h,
    Instant atualizadaEm,
    boolean desatualizada
) {

    public CotacaoMercado {
        ticker = Objects.requireNonNull(ticker, "ticker");
        preco = Objects.requireNonNull(preco, "preco");
        variacao24h = Objects.requireNonNull(
            variacao24h,
            "variacao24h"
        );
        atualizadaEm = Objects.requireNonNull(
            atualizadaEm,
            "atualizadaEm"
        );

        if (preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "A cotação deve possuir preço maior que zero."
            );
        }
    }

    public CotacaoMercado comoDesatualizada() {
        if (desatualizada) {
            return this;
        }

        return new CotacaoMercado(
            ticker,
            preco,
            variacao24h,
            atualizadaEm,
            true
        );
    }
}
