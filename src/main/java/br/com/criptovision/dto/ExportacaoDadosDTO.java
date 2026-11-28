package br.com.criptovision.dto;

import br.com.criptovision.model.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Todos os dados pessoais que a API guarda sobre o usuário, num formato
 * estruturado (LGPD, art. 18, II e V: acesso e portabilidade). Ficam de fora só
 * o hash da senha e a versão interna dos tokens, que não são informação sobre o
 * titular e enfraqueceriam a segurança se circulassem num arquivo.
 */
@Schema(
    name = "ExportacaoDados",
    description = "Todos os dados guardados sobre o usuário: a conta e o histórico completo de transações."
)
public record ExportacaoDadosDTO(
    @Schema(description = "Momento da exportação, em UTC.", example = "2026-11-27T22:00:00Z")
    OffsetDateTime exportadoEm,

    ContaExportada conta,

    @Schema(description = "Todas as transações, em ordem cronológica.")
    List<TransacaoExportada> transacoes
) {

    @Schema(name = "ContaExportada", description = "Dados da conta.")
    public record ContaExportada(
        @Schema(description = "Login do usuário.", example = "alexandre")
        String login
    ) {
    }

    @Schema(name = "TransacaoExportada", description = "Uma compra ou venda registrada.")
    public record TransacaoExportada(
        @Schema(example = "1")
        Long id,

        @Schema(example = "BTC")
        String ticker,

        @Schema(example = "COMPRA")
        TipoTransacao tipo,

        @Schema(example = "0.5")
        BigDecimal quantidade,

        @Schema(description = "Preço unitário, em USDT.", example = "65000.00")
        BigDecimal precoUnitario,

        @Schema(description = "Data da operação, em UTC.", example = "2025-03-10T17:00:00Z")
        OffsetDateTime data
    ) {
    }
}
