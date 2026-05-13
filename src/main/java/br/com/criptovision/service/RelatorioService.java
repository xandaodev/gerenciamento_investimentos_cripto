package br.com.criptovision.service;

import br.com.criptovision.model.Moeda;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioService {

    public void gerarRelatorio(List<Moeda> moedas) {
        String nomeArquivo = "relatorio_investimentos.txt";

        HttpService httpTradutor = new HttpService(); // busca o preço do dolar
        
        // java.time para capturar a data e hora exata da geração do relatório
        LocalDateTime agora = LocalDateTime.now();
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivo))) {
            writer.println("===========================================================");
            writer.println("              CRIPTOVISION - EXTRATO DE ATIVOS            ");
            writer.println("===========================================================");
            writer.println("Gerado em: " + agora.format(formatador));
            writer.println("-----------------------------------------------------------");
            
            writer.printf("%-10s | %-18s | %-15s\n", "ATIVO", "SALDO TOTAL", "PREÇO MÉDIO");
            writer.println("-----------|--------------------|-----------------------------");

            double custoTotalCarteira = 0;
            for (Moeda m : moedas) {
                if (m.getSaldo() > 0) {
                    writer.printf("%-10s | %-18.8f | $ %-14.2f\n", m.getTicker(), m.getSaldo(), m.getPrecoMedio());
                    custoTotalCarteira += (m.getSaldo() * m.getPrecoMedio());
                }
            }

            double precoDolar = httpTradutor.buscarCotacaoDolar(); 
            double custoTotalCarteiraReais = custoTotalCarteira * precoDolar;        
            
            writer.println("--------------------------------------------------------------");
            writer.printf("INVESTIMENTO TOTAL (CUSTO): $ %.2f (R$ %.2f)\n", custoTotalCarteira, custoTotalCarteiraReais);
            writer.println("==============================================================");
            System.out.println("\n Relatório gerado com sucesso em: " + nomeArquivo);
            
        }catch(IOException e){
            System.out.println("\n Erro ao gerar relatório: " + e.getMessage());
        }
    }
}