package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
    name = "SimulacaoDCA",
    description = "Resultado de uma simulação de novo aporte e preço médio."
)
public class SimulacaoDCADTO {

    @Schema(description = "Quantidade que o aporte compraria.", example = "0.007692307692307692")
    private final BigDecimal qtdComprada;

    @Schema(description = "Saldo atual do ativo.", example = "0.025")
    private final BigDecimal saldoAtual;

    @Schema(description = "Saldo após o aporte simulado.", example = "0.032692307692307692")
    private final BigDecimal novoSaldoTotal;

    @Schema(
        description = "Preço médio atual. Ausente quando ainda não há posição no ativo.",
        example = "59000.00",
        nullable = true
    )
    private final BigDecimal pmAtual;

    @Schema(description = "Novo preço médio estimado.", example = "60411.764705882352941176")
    private final BigDecimal novoPM;

    @Schema(
        description = "Variação percentual do preço médio. Ausente quando ainda não há posição no ativo.",
        example = "2.39182114",
        nullable = true
    )
    private final BigDecimal diferencaPM;

    @Schema(description = "Valorização necessária para atingir o ponto de equilíbrio, em percentual.", example = "-7.05882353")
    private final BigDecimal valorizacaoNecessaria;

    public SimulacaoDCADTO(
        BigDecimal qtdComprada,
        BigDecimal saldoAtual,
        BigDecimal novoSaldoTotal,
        BigDecimal pmAtual,
        BigDecimal novoPM,
        BigDecimal diferencaPM,
        BigDecimal valorizacaoNecessaria
    ) {
        this.qtdComprada = qtdComprada;
        this.saldoAtual = saldoAtual;
        this.novoSaldoTotal = novoSaldoTotal;
        this.pmAtual = pmAtual;
        this.novoPM = novoPM;
        this.diferencaPM = diferencaPM;
        this.valorizacaoNecessaria = valorizacaoNecessaria;
    }

    public BigDecimal getQtdComprada() {
        return qtdComprada;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public BigDecimal getNovoSaldoTotal() {
        return novoSaldoTotal;
    }

    public BigDecimal getPmAtual() {
        return pmAtual;
    }

    public BigDecimal getNovoPM() {
        return novoPM;
    }

    public BigDecimal getDiferencaPM() {
        return diferencaPM;
    }

    public BigDecimal getValorizacaoNecessaria() {
        return valorizacaoNecessaria;
    }
}
