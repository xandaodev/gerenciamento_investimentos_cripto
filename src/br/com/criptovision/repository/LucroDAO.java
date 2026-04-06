package br.com.criptovision.repository;

public interface LucroDAO {
    void salvarLucroRealizado(String ticker, double valorLucro);// guarda o lucro de uma venda
    
    double lerLucroTotal();//lê a soma de todos os lucros
}