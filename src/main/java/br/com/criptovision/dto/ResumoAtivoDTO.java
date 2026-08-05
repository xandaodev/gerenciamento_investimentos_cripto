package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "ResumoAtivo",
    description = "Indicadores calculados para um ativo presente na carteira."
)
public class ResumoAtivoDTO {

    @Schema(description = "Ticker do ativo.", example = "BTC")
    private String ticker;

    @Schema(description = "Quantidade atual do ativo.", example = "0.025")
    private double saldo;

    @Schema(description = "Preço atual do ativo em USD.", example = "65000.00")
    private double precoAtual;

    @Schema(description = "Preço médio de aquisição em USD.", example = "59000.00")
    private double precoMedio;

    @Schema(description = "Valor atual da posição em USD.", example = "1625.00")
    private double valorTotalUSD;

    @Schema(description = "Percentual de lucro ou prejuízo não realizado.", example = "10.17")
    private double porcentagemPNL;

    @Schema(description = "Variação do ativo nas últimas 24 horas, em percentual.", example = "2.35")
    private double variacao24h;

    public ResumoAtivoDTO(
        String ticker,
        double saldo,
        double precoAtual,
        double precoMedio,
        double valorTotalUSD,
        double porcentagemPNL,
        double variacao24h
    ) {
        this.ticker = ticker;
        this.saldo = saldo;
        this.precoAtual = precoAtual;
        this.precoMedio = precoMedio;
        this.valorTotalUSD = valorTotalUSD;
        this.porcentagemPNL = porcentagemPNL;
        this.variacao24h = variacao24h;
    }

    public String getTicker() {
        return ticker;
    }

    public double getSaldo() {
        return saldo;
    }

    public double getPrecoAtual() {
        return precoAtual;
    }

    public double getPrecoMedio() {
        return precoMedio;
    }

    public double getValorTotalUSD() {
        return valorTotalUSD;
    }

    public double getPorcentagemPNL() {
        return porcentagemPNL;
    }

    public double getVariacao24h() {
        return variacao24h;
    }
}
