package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "SimulacaoDCA",
    description = "Resultado de uma simulação de novo aporte e preço médio."
)
public class SimulacaoDCADTO {

    @Schema(description = "Quantidade que o aporte compraria.", example = "0.0076923")
    private double qtdComprada;

    @Schema(description = "Saldo atual do ativo.", example = "0.025")
    private double saldoAtual;

    @Schema(description = "Saldo após o aporte simulado.", example = "0.0326923")
    private double novoSaldoTotal;

    @Schema(description = "Preço médio atual.", example = "59000.00")
    private double pmAtual;

    @Schema(description = "Novo preço médio estimado.", example = "60411.76")
    private double novoPM;

    @Schema(description = "Variação percentual do preço médio.", example = "2.39")
    private double diferencaPM;

    @Schema(description = "Valorização necessária para atingir o ponto de equilíbrio.", example = "-7.06")
    private double valorizacaoNecessaria;

    public SimulacaoDCADTO(
        double qtdComprada,
        double saldoAtual,
        double novoSaldoTotal,
        double pmAtual,
        double novoPM,
        double diferencaPM,
        double valorizacaoNecessaria
    ) {
        this.qtdComprada = qtdComprada;
        this.saldoAtual = saldoAtual;
        this.novoSaldoTotal = novoSaldoTotal;
        this.pmAtual = pmAtual;
        this.novoPM = novoPM;
        this.diferencaPM = diferencaPM;
        this.valorizacaoNecessaria = valorizacaoNecessaria;
    }

    public double getQtdComprada() {
        return qtdComprada;
    }

    public double getSaldoAtual() {
        return saldoAtual;
    }

    public double getNovoSaldoTotal() {
        return novoSaldoTotal;
    }

    public double getPmAtual() {
        return pmAtual;
    }

    public double getNovoPM() {
        return novoPM;
    }

    public double getDiferencaPM() {
        return diferencaPM;
    }

    public double getValorizacaoNecessaria() {
        return valorizacaoNecessaria;
    }
}
