package br.com.criptovision.dto;

import java.util.List;

public class ResumoCarteiraDTO {
    private double valorTotalCarteira;
    private double pnlGeral;
    private double variacao24hCarteira;
    private List<ResumoAtivoDTO> ativos;

    public ResumoCarteiraDTO(double valorTotalCarteira, double pnlGeral, double variacao24hCarteira, List<ResumoAtivoDTO> ativos){
        this.valorTotalCarteira = valorTotalCarteira;
        this.pnlGeral = pnlGeral;
        this.variacao24hCarteira = variacao24hCarteira;
        this.ativos = ativos;
    }

    public double getValorTotalCarteira(){ return valorTotalCarteira; }
    public double getPnlGeral(){ return pnlGeral; }
    public double getVariacao24hCarteira(){ return variacao24hCarteira; }
    public List<ResumoAtivoDTO> getAtivos(){ return ativos; }
}