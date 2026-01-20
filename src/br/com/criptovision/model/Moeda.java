package br.com.criptovision.model;

public class Moeda {
    private String nome; // "bitcoin"
    private String ticker;// "BTC"
    private double saldo;
    private double precoMedio;

    public Moeda(String nome, String ticker){
        this.nome = nome.toLowerCase();
        this.ticker = ticker.toUpperCase();
        this.saldo = 0.0;
        this.precoMedio = 0.0;
    }
    
    //getter e setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTicker() { return ticker; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public double getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(double precoMedio) { this.precoMedio = precoMedio; }
}
