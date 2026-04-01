package br.com.criptovision.model;

import java.time.LocalDateTime;

// toda vez que voce executa uma operação no sistema, esse objeto é criado, salvo no arquivo csv e processado pela carteira

//se voce comprar bitcoin 10 vezes, serão 10 objetos transaçao criados, pra somente um objeto Moeda que é o bitcoin

public class Transacao {
    private String ticker; // moeda (btc, eth, sol ....)
    private double quantidade;
    private double precoUnitario;
    private LocalDateTime data;
    private String tipo;  // se vai ser compra ou venda
    //private double taxa;

    private Long id; // chave primaria para a base de dados

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

}
