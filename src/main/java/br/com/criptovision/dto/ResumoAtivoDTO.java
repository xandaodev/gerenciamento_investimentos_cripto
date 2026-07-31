package br.com.criptovision.dto;

public class ResumoAtivoDTO {
    private String ticker;
    private double saldo;
    private double precoAtual;
    private double precoMedio;
    private double valorTotalUSD;
    private double porcentagemPNL;
    private double variacao24h;

    public ResumoAtivoDTO(String ticker, double saldo, double precoAtual, double precoMedio, double valorTotalUSD, double porcentagemPNL, double variacao24h) {
        this.ticker = ticker;
        this.saldo = saldo;
        this.precoAtual = precoAtual;
        this.precoMedio = precoMedio;
        this.valorTotalUSD = valorTotalUSD;
        this.porcentagemPNL = porcentagemPNL;
        this.variacao24h = variacao24h;
    }

    public String getTicker() { return ticker; }
    public double getSaldo() { return saldo; }
    public double getPrecoAtual() { return precoAtual; }
    public double getPrecoMedio() { return precoMedio; }
    public double getValorTotalUSD() { return valorTotalUSD; }
    public double getPorcentagemPNL() { return porcentagemPNL; }
    public double getVariacao24h() { return variacao24h; }
}