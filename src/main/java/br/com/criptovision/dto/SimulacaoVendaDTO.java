package br.com.criptovision.dto;

public class SimulacaoVendaDTO {
    private double lucroEstimado;
    private double porcentagemLucro;
    private double valorTotalFicticio;
    private double valorTotalAtual;

    public SimulacaoVendaDTO(double lucroEstimado, double porcentagemLucro, double valorTotalFicticio, double valorTotalAtual){
        this.lucroEstimado = lucroEstimado;
        this.porcentagemLucro = porcentagemLucro;
        this.valorTotalFicticio = valorTotalFicticio;
        this.valorTotalAtual = valorTotalAtual;
    }

    public double getLucroEstimado(){ return lucroEstimado; }
    public double getPorcentagemLucro(){ return porcentagemLucro; }
    public double getValorTotalFicticio(){ return valorTotalFicticio; }
    public double getValorTotalAtual(){ return valorTotalAtual; }
}