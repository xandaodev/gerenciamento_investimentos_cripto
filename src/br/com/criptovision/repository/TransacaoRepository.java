package br.com.criptovision.repository;

import br.com.criptovision.exception.ArquivoNaoEncontradoException;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.HttpService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransacaoRepository {

    // esse é o nome do arquivo onde os dados das transacoes serão salvos
    private String nomeArquivo = "transacoes.csv";

    // esse metodo pega um objeto Transacao e escreve uma linha no fim do arquivo CSV
    public void salvar(Transacao transacao){
        try(FileWriter fw = new FileWriter(nomeArquivo, true);// o true aqui acrescenta no final, evitando que ele apague o que ja existe no arquivo
        // formatacao doa dados:    
        PrintWriter pw = new PrintWriter(fw)){
            // concatenacao dos dados separados por virgula
            pw.println(transacao.getTicker() + "," +
                       transacao.getQuantidade() + "," +
                       transacao.getPrecoUnitario() + "," +
                       transacao.getTipo() + "," +
                       transacao.getData());
        }catch(IOException e){
            System.out.println("Erro ao salvar transação: " + e.getMessage());
        }
    }

    // esse metodo abre o arquivo e le linha por linha, transformando cada linha em um objeto transacao
    // basicamente reconstroi sua carteira lendo todo o historico, quando o programa é iniciado
    public List<Transacao> lerTudo(){
        List<Transacao> transacoes = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(nomeArquivo))){
            String linha;
            while((linha = br.readLine()) != null){
                // o split quebra a linha na virgula, e cria um array de strings
                String[] dados = linha.split(",");
                String ticker = dados[0];
                double qtd = Double.parseDouble(dados[1]);
                double preco = Double.parseDouble(dados[2]);
                String tipo = dados[3];
                
                //aqui cria o objeto na memoria
                Transacao t = new Transacao(ticker, qtd, preco, tipo);
                transacoes.add(t);
            }
        }catch(IOException e){
            throw new ArquivoNaoEncontradoException("Erro fatal: Não foi possível ler o arquivo " + nomeArquivo);
        }
        return transacoes;
    }

    // metodo antigo estava muito basico, removi e construi esse com mais dados e melhor formatado
    // metodo que cria um relatorio em txt, organizado e legivel
    public void gerarRelatorio(List<Moeda> moedas){
    // nome do arquivo
    String nomeArquivo = "relatorio_investimentos.txt";

    HttpService httpTradutor = new HttpService();// pra buscra o preço do dolar
    
    //  java.time para pegar a data e hora exata da geração do relatorio
    java.time.LocalDateTime agora = java.time.LocalDateTime.now();
    java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    try(PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivo))){
        writer.println("===========================================================");
        writer.println("              CRIPTOVISION - EXTRATO DE ATIVOS            ");
        writer.println("===========================================================");
        writer.println("Gerado em: " + agora.format(formatador));
        writer.println("-----------------------------------------------------------");
        
        // cabeçalho da tabela
        //  %-10s alinha à esquerda com 10 espaços, o %15s alinha à direita
        writer.printf("%-10s | %-18s | %-15s\n", "ATIVO", "SALDO TOTAL", "PREÇO MÉDIO");
        writer.println("-----------|--------------------|-----------------------------");

        double custoTotalCarteira = 0;
        for(Moeda m : moedas){
            if(m.getSaldo() > 0){
                writer.printf("%-10s | %-18.8f | $ %-14.2f\n", m.getTicker(), m.getSaldo(), m.getPrecoMedio());
                custoTotalCarteira += (m.getSaldo() * m.getPrecoMedio());
            }
        }

        // resumo no rodapé:
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

    //nova funcao para fazer um backup do relatorio todas as vezes que o programa for iniciado
    public void realizarBackup(){
        try{
            java.io.File pasta = new java.io.File("backups");// cria a pasta se ela nao existir
            if(!pasta.exists()) pasta.mkdir();
            String data = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));// carimba com a data/hora
            // o java nio copia o arquivo inteiro:
            java.nio.file.Files.copy(java.nio.file.Paths.get("transacoes.csv"),java.nio.file.Paths.get("backups/transacoes_backup_" + data + ".csv"),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            //System.out.println("Backup realizado");
        }catch(Exception e){
        }
    }
}