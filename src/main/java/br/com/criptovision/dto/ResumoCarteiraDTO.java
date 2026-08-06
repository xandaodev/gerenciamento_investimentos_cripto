package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(
    name = "ResumoCarteira",
    description = "Resumo consolidado da carteira do usuário autenticado."
)
public class ResumoCarteiraDTO {

    @Schema(
        description = "Valor atual total das posições com cotação disponível, em USD.",
        example = "3850.75"
    )
    private BigDecimal valorTotalCarteira;

    @Schema(
        description = "Lucro ou prejuízo não realizado das posições com cotação disponível, em USD.",
        example = "425.30"
    )
    private BigDecimal pnlGeral;

    @Schema(
        description = "Variação estimada da carteira nas últimas 24 horas.",
        example = "1.82"
    )
    private BigDecimal variacao24hCarteira;

    @Schema(description = "Detalhamento dos ativos com saldo positivo.")
    private List<ResumoAtivoDTO> ativos;

    @Schema(
        description = "Horário da cotação mais antiga utilizada no resumo.",
        example = "2026-08-06T14:30:00Z",
        nullable = true
    )
    private Instant cotacoesAtualizadasEm;

    @Schema(
        description = "Indica se há cotações ausentes ou obtidas por fallback.",
        example = "false"
    )
    private boolean cotacoesParciais;

    @Schema(
        description = "Tickers para os quais não foi possível obter cotação nem fallback."
    )
    private List<String> ativosSemCotacao;

    public ResumoCarteiraDTO(
        BigDecimal valorTotalCarteira,
        BigDecimal pnlGeral,
        BigDecimal variacao24hCarteira,
        List<ResumoAtivoDTO> ativos,
        Instant cotacoesAtualizadasEm,
        boolean cotacoesParciais,
        List<String> ativosSemCotacao
    ) {
        this.valorTotalCarteira = valorTotalCarteira;
        this.pnlGeral = pnlGeral;
        this.variacao24hCarteira = variacao24hCarteira;
        this.ativos = ativos;
        this.cotacoesAtualizadasEm = cotacoesAtualizadasEm;
        this.cotacoesParciais = cotacoesParciais;
        this.ativosSemCotacao = ativosSemCotacao;
    }

    public BigDecimal getValorTotalCarteira() {
        return valorTotalCarteira;
    }

    public BigDecimal getPnlGeral() {
        return pnlGeral;
    }

    public BigDecimal getVariacao24hCarteira() {
        return variacao24hCarteira;
    }

    public List<ResumoAtivoDTO> getAtivos() {
        return ativos;
    }

    public Instant getCotacoesAtualizadasEm() {
        return cotacoesAtualizadasEm;
    }

    public boolean isCotacoesParciais() {
        return cotacoesParciais;
    }

    public List<String> getAtivosSemCotacao() {
        return ativosSemCotacao;
    }
}
