package br.com.criptovision.model;


//essa clase representa uma criptomoeda dentro do sistema, ela armazena os dados de todas as compras e vendas

import java.math.BigDecimal;

public class Moeda {
    private String nome; // "bitcoin"
    private String ticker;// "BTC"
    private BigDecimal saldo;
    private BigDecimal precoMedio;

    private BigDecimal variacao24h;

    public Moeda(String nome, String ticker){
        this.nome = nome.toLowerCase();
        this.ticker = ticker.toUpperCase();
        this.saldo = BigDecimal.ZERO;
        this.precoMedio = BigDecimal.ZERO;
        this.variacao24h = BigDecimal.ZERO;
    }
    
    //getter e setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTicker() { return ticker; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }

    public BigDecimal getVariacao24h(){ return variacao24h; }
    public void setVariacao24h(BigDecimal variacao24h){ this.variacao24h = variacao24h; }
}
