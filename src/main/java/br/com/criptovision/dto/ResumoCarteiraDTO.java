package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
    name = "ResumoCarteira",
    description = "Resumo consolidado da carteira do usuário autenticado."
)
public class ResumoCarteiraDTO {

    @Schema(description = "Valor atual total da carteira em USD.", example = "3850.75")
    private double valorTotalCarteira;

    @Schema(description = "Lucro ou prejuízo não realizado total em USD.", example = "425.30")
    private double pnlGeral;

    @Schema(description = "Variação estimada da carteira nas últimas 24 horas.", example = "1.82")
    private double variacao24hCarteira;

    @Schema(description = "Detalhamento dos ativos com saldo positivo.")
    private List<ResumoAtivoDTO> ativos;

    public ResumoCarteiraDTO(
        double valorTotalCarteira,
        double pnlGeral,
        double variacao24hCarteira,
        List<ResumoAtivoDTO> ativos
    ) {
        this.valorTotalCarteira = valorTotalCarteira;
        this.pnlGeral = pnlGeral;
        this.variacao24hCarteira = variacao24hCarteira;
        this.ativos = ativos;
    }

    public double getValorTotalCarteira() {
        return valorTotalCarteira;
    }

    public double getPnlGeral() {
        return pnlGeral;
    }

    public double getVariacao24hCarteira() {
        return variacao24hCarteira;
    }

    public List<ResumoAtivoDTO> getAtivos() {
        return ativos;
    }
}
