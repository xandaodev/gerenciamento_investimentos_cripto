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
        }
        return transacoes;
    }

    // metodo que cria um relatorio em txt, organizado e legivel
    public void gerarRelatorio(List<Moeda> moedas){
        try(PrintWriter writer = new PrintWriter(new FileWriter("relatorio.txt"))){
            writer.println("--- RELATÓRIO DE INVESTIMENTOS CRIPTO ---");
            writer.println("Data: " + LocalDateTime.now());
            writer.println("------------------------------------------");
            for(Moeda m : moedas){
                if(m.getSaldo() > 0){
                    writer.printf("Ativo: %s | Saldo: %.8f | Preço Médio: $ %.2f\n", m.getTicker(), m.getSaldo(), m.getPrecoMedio());
                }
            }
            System.out.println("Relatório gerado com sucesso em 'relatorio.txt'");
        }catch(IOException e){
            System.out.println("Erro ao gerar relatório: " + e.getMessage());
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