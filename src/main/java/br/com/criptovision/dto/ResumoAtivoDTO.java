package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(
    name = "ResumoAtivo",
    description = "Indicadores calculados para um ativo presente na carteira."
)
public class ResumoAtivoDTO {

    @Schema(description = "Ticker do ativo.", example = "BTC")
    private String ticker;

    @Schema(description = "Quantidade atual do ativo.", example = "0.025")
    private BigDecimal saldo;

    @Schema(
        description = "Preço atual do ativo em USD. Ausente quando não há cotação disponível.",
        example = "65000.00",
        nullable = true
    )
    private BigDecimal precoAtual;

    @Schema(description = "Preço médio de aquisição em USD.", example = "59000.00")
    private BigDecimal precoMedio;

    @Schema(
        description = "Valor atual da posição em USD. Ausente quando não há cotação disponível.",
        example = "1625.00",
        nullable = true
    )
    private BigDecimal valorTotalUSD;

    @Schema(
        description = "Percentual de lucro ou prejuízo não realizado. Ausente quando não há cotação disponível.",
        example = "10.17",
        nullable = true
    )
    private BigDecimal porcentagemPNL;

    @Schema(
        description = "Variação do ativo nas últimas 24 horas, em percentual. Ausente quando não há cotação disponível.",
        example = "2.35",
        nullable = true
    )
    private BigDecimal variacao24h;

    @Schema(
        description = "Indica se o ativo possui uma cotação utilizável.",
        example = "true"
    )
    private boolean cotacaoDisponivel;

    @Schema(
        description = "Indica se foi utilizada uma cotação anterior como fallback.",
        example = "false"
    )
    private boolean cotacaoDesatualizada;

    @Schema(
        description = "Horário da cotação utilizada. Ausente quando não há cotação disponível.",
        example = "2026-08-06T14:30:00Z",
        nullable = true
    )
    private Instant cotacaoAtualizadaEm;

    public ResumoAtivoDTO(
        String ticker,
        BigDecimal saldo,
        BigDecimal precoAtual,
        BigDecimal precoMedio,
        BigDecimal valorTotalUSD,
        BigDecimal porcentagemPNL,
        BigDecimal variacao24h,
        boolean cotacaoDisponivel,
        boolean cotacaoDesatualizada,
        Instant cotacaoAtualizadaEm
    ) {
        this.ticker = ticker;
        this.saldo = saldo;
        this.precoAtual = precoAtual;
        this.precoMedio = precoMedio;
        this.valorTotalUSD = valorTotalUSD;
        this.porcentagemPNL = porcentagemPNL;
        this.variacao24h = variacao24h;
        this.cotacaoDisponivel = cotacaoDisponivel;
        this.cotacaoDesatualizada = cotacaoDesatualizada;
        this.cotacaoAtualizadaEm = cotacaoAtualizadaEm;
    }

    public static ResumoAtivoDTO semCotacao(
        String ticker,
        BigDecimal saldo,
        BigDecimal precoMedio
    ) {
        return new ResumoAtivoDTO(
            ticker,
            saldo,
            null,
            precoMedio,
            null,
            null,
            null,
            false,
            false,
            null
        );
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getPrecoAtual() {
        return precoAtual;
    }

    public BigDecimal getPrecoMedio() {
        return precoMedio;
    }

    public BigDecimal getValorTotalUSD() {
        return valorTotalUSD;
    }

    public BigDecimal getPorcentagemPNL() {
        return porcentagemPNL;
    }

    public BigDecimal getVariacao24h() {
        return variacao24h;
    }

    public boolean isCotacaoDisponivel() {
        return cotacaoDisponivel;
    }

    public boolean isCotacaoDesatualizada() {
        return cotacaoDesatualizada;
    }

    public Instant getCotacaoAtualizadaEm() {
        return cotacaoAtualizadaEm;
    }
}
