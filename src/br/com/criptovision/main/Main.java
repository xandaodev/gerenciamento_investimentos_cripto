package br.com.criptovision.main;

import br.com.criptovision.model.Transacao;

public class Main {
    public static void main(String[] args){
        Transacao teste = new Transacao("BTC", 0.01, 350000.00, "COMPRA");

        System.out.println("Transação registrada: " + teste.getTicker() + " a R$" + teste.getPrecoUnitario());
    }
    
}
