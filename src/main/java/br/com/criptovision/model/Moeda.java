package br.com.criptovision.model;


//essa clase representa uma criptomoeda dentro do sistema, ela armazena os dados de todas as compras e vendas

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Moeda {

    // mesma escala do preço unitário no banco (DECIMAL(38, 18)),
    // para que moedas de preço muito baixo não percam casas na exibição
    public static final int ESCALA_PRECO_MEDIO = 18;

    private String nome; // "bitcoin"
    private String ticker;// "BTC"
    private BigDecimal saldo;

    // custo de aquisição das unidades em carteira, acumulado sem arredondar.
    // o preço médio é derivado dele só na leitura, para que o arredondamento
    // da exibição nunca volte para o cálculo da transação seguinte
    private BigDecimal custoTotal;

    private BigDecimal variacao24h;

    public Moeda(String nome, String ticker){
        this.nome = nome.toLowerCase();
        this.ticker = ticker.toUpperCase();
        this.saldo = BigDecimal.ZERO;
        this.custoTotal = BigDecimal.ZERO;
        this.variacao24h = BigDecimal.ZERO;
    }

    //getter e setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTicker() { return ticker; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public BigDecimal getCustoTotal() { return custoTotal; }
    public void setCustoTotal(BigDecimal custoTotal) { this.custoTotal = custoTotal; }

    // preço médio = custo total ÷ saldo; sem saldo não há posição, então é zero
    public BigDecimal getPrecoMedio() {
        if (saldo.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return custoTotal.divide(saldo, ESCALA_PRECO_MEDIO, RoundingMode.HALF_UP);
    }

    public BigDecimal getVariacao24h(){ return variacao24h; }
    public void setVariacao24h(BigDecimal variacao24h){ this.variacao24h = variacao24h; }
}
