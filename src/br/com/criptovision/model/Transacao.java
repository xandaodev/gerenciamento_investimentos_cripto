package br.com.criptovision.model;

import java.time.LocalDateTime;

public class Transacao {
    private String ticker; // moeda (btc, eth...)
    private double quantidade;
    private double precoUnitario;
    private LocalDateTime data;
    private String tipo;  // se vai ser compra ou venda
    //private double taxa;

    //construtor
public Transacao(String ticker, double quantidade, double precoUnitario, String tipo/*, double taxa*/){
        this.ticker = ticker.toUpperCase();
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.tipo = tipo.toUpperCase();
        this.data = LocalDateTime.now();
        //this.taxa = taxa;
    }
    
    //getters e setters
    public String getTicker(){ return ticker; }
    public double getQuantidade(){ return quantidade; }
    public double getPrecoUnitario(){ return precoUnitario; }
    public String getTipo(){ return tipo; }
    public LocalDateTime getData(){ return data; }
    //public double getTaxa(){ return taxa; }

}
