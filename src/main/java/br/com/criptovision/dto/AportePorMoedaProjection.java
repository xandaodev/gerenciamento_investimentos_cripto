package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
    name = "AportePorMoeda",
    description = "Valor total aportado em compras agrupado por ativo."
)
public interface AportePorMoedaProjection {

    @Schema(description = "Ticker do ativo.", example = "BTC")
    String getTicker();

    @Schema(description = "Total aportado no ativo.", example = "12500.00")
    BigDecimal getTotalAportado();
}
