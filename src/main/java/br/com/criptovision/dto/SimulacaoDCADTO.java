package br.com.criptovision.dto;

public class SimulacaoDCADTO {
    
    private double qtdComprada;
    private double saldoAtual;
    private double novoSaldoTotal;
    private double pmAtual;
    private double novoPM;
    private double diferencaPM;
    private double valorizacaoNecessaria;

    public SimulacaoDCADTO(double qtdComprada, double saldoAtual, double novoSaldoTotal, double pmAtual, double novoPM, double diferencaPM, double valorizacaoNecessaria){
        this.qtdComprada = qtdComprada;
        this.saldoAtual = saldoAtual;
        this.novoSaldoTotal = novoSaldoTotal;
        this.pmAtual = pmAtual;
        this.novoPM = novoPM;
        this.diferencaPM = diferencaPM;
        this.valorizacaoNecessaria = valorizacaoNecessaria;
    }

    public double getQtdComprada(){ return qtdComprada; }
    public double getSaldoAtual(){ return saldoAtual; }
    public double getNovoSaldoTotal(){ return novoSaldoTotal; }
    public double getPmAtual(){ return pmAtual; }
    public double getNovoPM(){ return novoPM; }
    public double getDiferencaPM(){ return diferencaPM; }
    public double getValorizacaoNecessaria(){ return valorizacaoNecessaria; }
}