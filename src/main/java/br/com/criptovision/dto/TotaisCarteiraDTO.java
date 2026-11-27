package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
    name = "TotaisCarteira",
    description = "Totais financeiros da carteira, calculados pelos preços das operações "
        + "(não pelo preço atual de mercado, que está no resumo)."
)
public record TotaisCarteiraDTO(
    @Schema(
        description = "Custo das posições abertas: quanto do dinheiro está aplicado hoje, "
            + "pelo preço pago. Nunca é negativo, e é zero depois de vender tudo.",
        example = "25000.00"
    )
    BigDecimal custoDasPosicoes,

    @Schema(
        description = "Soma de todas as compras desde o início (quantidade × preço). "
            + "Não diminui com as vendas.",
        example = "50000.00"
    )
    BigDecimal totalAportado,

    @Schema(
        description = "Soma de todas as vendas (quantidade × preço de venda).",
        example = "20000.00"
    )
    BigDecimal totalRecebidoEmVendas,

    @Schema(
        description = "Moeda dos valores. Os preços das operações estão em USDT; "
            + "não há conversão para reais.",
        example = "USDT"
    )
    String moeda
) {
}
