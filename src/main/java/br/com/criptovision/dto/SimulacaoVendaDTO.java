package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(
    name = "SimulacaoVenda",
    description = "Resultado de uma simulação de venda de toda a posição."
)
public class SimulacaoVendaDTO {

    @Schema(description = "Lucro ou prejuízo estimado em USD.", example = "325.00")
    private final BigDecimal lucroEstimado;

    @Schema(description = "Lucro ou prejuízo estimado em percentual do custo da posição.", example = "22.03389831")
    private final BigDecimal porcentagemLucro;

    @Schema(description = "Valor da posição no preço-alvo.", example = "1800.00")
    private final BigDecimal valorTotalFicticio;

    @Schema(description = "Valor atual da posição.", example = "1625.00")
    private final BigDecimal valorTotalAtual;

    public SimulacaoVendaDTO(
        BigDecimal lucroEstimado,
        BigDecimal porcentagemLucro,
        BigDecimal valorTotalFicticio,
        BigDecimal valorTotalAtual
    ) {
        this.lucroEstimado = lucroEstimado;
        this.porcentagemLucro = porcentagemLucro;
        this.valorTotalFicticio = valorTotalFicticio;
        this.valorTotalAtual = valorTotalAtual;
    }

    public BigDecimal getLucroEstimado() {
        return lucroEstimado;
    }

    public BigDecimal getPorcentagemLucro() {
        return porcentagemLucro;
    }

    public BigDecimal getValorTotalFicticio() {
        return valorTotalFicticio;
    }

    public BigDecimal getValorTotalAtual() {
        return valorTotalAtual;
    }
}
