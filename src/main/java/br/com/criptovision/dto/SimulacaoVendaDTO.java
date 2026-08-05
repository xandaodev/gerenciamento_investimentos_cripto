package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "SimulacaoVenda",
    description = "Resultado de uma simulação de venda de toda a posição."
)
public class SimulacaoVendaDTO {

    @Schema(description = "Lucro ou prejuízo estimado em USD.", example = "325.00")
    private double lucroEstimado;

    @Schema(description = "Lucro ou prejuízo estimado em percentual.", example = "22.03")
    private double porcentagemLucro;

    @Schema(description = "Valor da posição no preço-alvo.", example = "1800.00")
    private double valorTotalFicticio;

    @Schema(description = "Valor atual da posição.", example = "1625.00")
    private double valorTotalAtual;

    public SimulacaoVendaDTO(
        double lucroEstimado,
        double porcentagemLucro,
        double valorTotalFicticio,
        double valorTotalAtual
    ) {
        this.lucroEstimado = lucroEstimado;
        this.porcentagemLucro = porcentagemLucro;
        this.valorTotalFicticio = valorTotalFicticio;
        this.valorTotalAtual = valorTotalAtual;
    }

    public double getLucroEstimado() {
        return lucroEstimado;
    }

    public double getPorcentagemLucro() {
        return porcentagemLucro;
    }

    public double getValorTotalFicticio() {
        return valorTotalFicticio;
    }

    public double getValorTotalAtual() {
        return valorTotalAtual;
    }
}
