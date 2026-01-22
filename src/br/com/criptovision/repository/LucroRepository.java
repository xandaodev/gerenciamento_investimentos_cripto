package br.com.criptovision.repository;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.FileReader;

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

    public double lerLucroTotal(){
        double somaTotal = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(nomeArquivo))){
            String linha;
            while((linha = br.readLine()) != null){
                // no csv, salvamos o ticker, o valor e a data
                // o split dive a linha onde tiver ","
                String[] dados = linha.split(",");
                // a posicao 1 armazena o valor do lucro
                double valorDaLinha = Double.parseDouble(dados[1]);
                somaTotal += valorDaLinha;
            }
        }catch(Exception e){
            return 0;
        }
        return somaTotal;
    }
}
