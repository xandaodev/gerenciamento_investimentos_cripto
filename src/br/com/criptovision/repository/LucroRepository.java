package br.com.criptovision.repository;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class LucroRepository {
    private String nomeArquivo = "lucros.csv";
    public void salvarLucroRealizado(String ticker, double valorLucro){
        try(FileWriter fw = new FileWriter(nomeArquivo, true);PrintWriter pw = new PrintWriter(fw)){
            // salva valor, ticker e data
            pw.println(ticker + "," + valorLucro + "," + LocalDateTime.now());
        }catch(IOException e){
            System.out.println("Erro ao registrar lucro: " + e.getMessage());
        }
    }
}
