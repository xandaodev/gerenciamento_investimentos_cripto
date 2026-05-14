package br.com.criptovision.model;

import java.util.HashMap;
import java.util.Map;

public class Carteira {
    // a chave é o ticker da moeda e o valor é o objeto Moeda completo
    private Map<String, Moeda> moedas;

    public Carteira(){
        this.moedas = new HashMap<>();
    }

    // busca uma moeda ou cria uma nova se ela não existir
    public Moeda obterMoeda(String ticker, String nome){
        ticker = ticker.toUpperCase();
        if(!moedas.containsKey(ticker)){
            moedas.put(ticker, new Moeda(nome, ticker));
        }
        return moedas.get(ticker);
    }

    public Map<String, Moeda> getMoedas(){
        return moedas;
    }
}