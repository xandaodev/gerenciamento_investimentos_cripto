package br.com.criptovision.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// toda vez que voce executa uma operação no sistema, esse objeto é criado, salvo no arquivo csv e processado pela carteira

//se voce comprar bitcoin 10 vezes, serão 10 objetos transaçao criados, pra somente um objeto Moeda que é o bitcoin

@Entity
@Table(name = "transacoes")
@Schema(
    name = "Transacao",
    description = "Operação de compra ou venda registrada pelo usuário autenticado."
)
public class Transacao {

    @Schema(description = "Ticker do ativo.", example = "BTC")
    private String ticker; // moeda (btc, eth, sol ....)
    @Schema(description = "Quantidade negociada.", example = "0.015")
    private BigDecimal quantidade;
    @Schema(description = "Preço unitário informado na operação.", example = "64000.00")
    private BigDecimal precoUnitario;
    @CreationTimestamp
    @Schema(
        description = "Data e hora de criação da transação.",
        example = "2026-08-05T17:30:00",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime data;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Tipo da operação.", example = "COMPRA")
    private TipoTransacao tipo;

    @JsonIgnore
    @Schema(hidden = true)
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "usuario_id",
        nullable = false
    )
    private Usuario usuario;

    //private double taxa;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    @Schema(
        description = "Identificador da transação.",
        example = "42",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id; // chave primaria

    public Transacao() {
    }

    //construtor
    public Transacao(String ticker, BigDecimal quantidade, BigDecimal precoUnitario, TipoTransacao tipo) {
        this.ticker = ticker.toUpperCase();
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.tipo = tipo;
        this.data = LocalDateTime.now();
    }

    //getters e setters
    public String getTicker(){ return ticker; }
    public BigDecimal getQuantidade(){ return quantidade; }
    public BigDecimal getPrecoUnitario(){ return precoUnitario; }
    public TipoTransacao getTipo() {
        return tipo;
    }
    public LocalDateTime getData(){ return data; }

    public void setData(LocalDateTime data){ this.data = data; }

    //public double getTaxa(){ return taxa; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public void setPrecoUnitario(BigDecimal  precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
