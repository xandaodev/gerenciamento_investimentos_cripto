package br.com.criptovision.repository;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public List<Transacao> lerTudo(){
        List<Transacao> transacoes = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(nomeArquivo))){
            String linha;
            while((linha = br.readLine()) != null){
                String[] dados = linha.split(",");
                String ticker = dados[0];
                double qtd = Double.parseDouble(dados[1]);
                double preco = Double.parseDouble(dados[2]);
                String tipo = dados[3];
                
                Transacao t = new Transacao(ticker, qtd, preco, tipo);
                transacoes.add(t);
            }
        }catch(IOException e){
        }
        return transacoes;
    }

    public void gerarRelatorio(List<Moeda> moedas){
    try(PrintWriter writer = new PrintWriter(new FileWriter("relatorio.txt"))){
        writer.println("--- RELATÓRIO DE INVESTIMENTOS CRIPTO ---");
        writer.println("Data: " + LocalDateTime.now());
        writer.println("------------------------------------------");
        for(Moeda m : moedas){
            if(m.getSaldo() > 0){
                writer.printf("Ativo: %s | Saldo: %.8f | Preço Médio: $ %.2f\n",
                        m.getTicker(), m.getSaldo(), m.getPrecoMedio());
            }
        }
        System.out.println("Relatório gerado com sucesso em 'relatorio.txt'!");
    }catch(IOException e){
        System.out.println("Erro ao gerar relatório: " + e.getMessage());
    }
}
}