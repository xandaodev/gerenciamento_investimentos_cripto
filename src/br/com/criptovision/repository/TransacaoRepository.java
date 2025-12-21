package br.com.criptovision.repository;

import br.com.criptovision.model.Transacao;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TransacaoRepository {
    private String nomeArquivo = "transacoes.csv";

    public void salvar(Transacao transacao){
        try(FileWriter fw = new FileWriter(nomeArquivo, true);
            PrintWriter pw = new PrintWriter(fw)){
            pw.println(transacao.getTicker() + "," +
                       transacao.getQuantidade() + "," +
                       transacao.getPrecoUnitario() + "," +
                       transacao.getTipo() + "," +
                       transacao.getData());
        }catch (IOException e){
            System.out.println("Erro ao salvar transação: " + e.getMessage());
        }
    }
}