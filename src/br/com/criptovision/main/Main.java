package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.repository.TransacaoRepository;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);
        CarteiraService carteira = new CarteiraService();
        TransacaoRepository repositorio = new TransacaoRepository();
        //criando um array pro historico

        List<Transacao> historico = repositorio.lerTudo();

        Carteira minhaCarteira = new Carteira();

        HttpService httpTradutor = new HttpService();
        for (Transacao tAntiga : historico) {
            String tickerOriginal = tAntiga.getTicker().toUpperCase();
            String nomeCorretoApi = httpTradutor.converterTickerParaId(tickerOriginal);
            
            // Certifique-se de que a moeda na carteira tenha o nome da API correto
            Moeda m = minhaCarteira.obterMoeda(tickerOriginal, nomeCorretoApi);
            carteira.processarTransacao(m, tAntiga);
        }

        int opcao = 0;
        while (opcao != 7){
            try{
                System.out.println("\n--- GERENCIAMNETO DE INVESTIMENTOS CRIPTO ---");
                System.out.println("1. Nova compra");
                System.out.println("2. Ver Saldo e Preço Médio");
                System.out.println("3. Ver Histórico de Transações");
                System.out.println("4. Nova venda");
                System.out.println("5. Simular lucro");
                System.out.println("6. Gerar Relatório");
                System.out.println("7. Sair do sistema");
                System.out.print("Escolha uma opção: ");
                
                opcao = leitor.nextInt();

                switch (opcao){
                    case 1:
                        try {
                            System.out.print("Qual o Ticker da moeda (ex: BTC)? ");
                            String ticker = leitor.next().toUpperCase();
                            leitor.nextLine(); 
                            
                            HttpService httpService = new HttpService();
                            
                            // PRIMEIRO: Converte o ticker para o ID que a API entende
                            String idMoeda = httpService.converterTickerParaId(ticker);
                            
                            System.out.println("Validando '" + idMoeda + "' na API...");
                            
                            // SEGUNDO: Valida usando o ID convertido
                            if (!httpService.validarTicker(idMoeda)) {
                                System.out.println("ERRO: A API não reconheceu o ativo '" + ticker + "' (ID: " + idMoeda + ").");
                                break; 
                            }

                            Moeda moedaSelecionada = minhaCarteira.obterMoeda(ticker, idMoeda); 

                            System.out.print("Quantidade comprada: ");
                            double qtd = Double.parseDouble(leitor.nextLine().replace(",", ".")); // Lendo linha cheia para evitar erro de buffer

                            System.out.print("Preço unitário pago: ");
                            double preco = Double.parseDouble(leitor.nextLine().replace(",", "."));
                            
                            Transacao t = new Transacao(ticker, qtd, preco, "COMPRA");
                            carteira.processarTransacao(moedaSelecionada, t);
                            historico.add(t);
                            repositorio.salvar(t);
                            System.out.println("Compra registrada com sucesso!");
                        } catch (Exception e) {
                            System.out.println("Erro na compra: " + e.getMessage());
                        }
                        break;
                        
                    case 2:
                        System.out.println("\n--- MINHAS MOEDAS ---");
                        for(Moeda m : minhaCarteira.getMoedas().values()){
                            if(m.getSaldo() > 0){
                                System.out.printf("Ativo: %s | Saldo: %.8f | Preço Médio: $ %.2f\n", 
                                    m.getTicker(), m.getSaldo(), m.getPrecoMedio());
                            }
                        }
                        break;

                    case 3:
                        System.out.println("\n--- DASHBOARD DE PATRIMÓNIO ---");
                        HttpService serviceHttp = new HttpService();
                        
                        double totalCalculado = 0;
                        // Criamos uma lista temporária para não ter que chamar a API de novo depois
                        java.util.Map<String, Double> valoresPorMoeda = new java.util.HashMap<>();

                        System.out.println("Atualizando preços (aguarde)...");

                        for (Moeda m : minhaCarteira.getMoedas().values()) {
                            if (m.getSaldo() > 0) {
                                double preco = serviceHttp.buscarPrecoAtual(m); // ÚNICA CHAMADA POR MOEDA
                                double valorNoAtivo = m.getSaldo() * preco;
                                
                                valoresPorMoeda.put(m.getTicker(), valorNoAtivo);
                                totalCalculado += valorNoAtivo;
                            }
                        }

                        if (totalCalculado == 0) {
                            System.out.println("Erro: Não foi possível obter preços da API ou carteira vazia.");
                        } else {
                            System.out.printf("VALOR TOTAL DO PATRIMÓNIO: $ %.2f\n", totalCalculado);
                            System.out.println("---------------------------------------");
                            System.out.println("Distribuição por Ativo:");

                            for (Moeda m : minhaCarteira.getMoedas().values()) {
                                if (m.getSaldo() > 0) {
                                    double valorAtivo = valoresPorMoeda.get(m.getTicker());
                                    double percentagem = (valorAtivo / totalCalculado) * 100;

                                    System.out.printf("   %s: $ %.2f (%.1f%%)\n", 
                                        m.getTicker(), valorAtivo, percentagem);
                                }
                            }
                        }
                        System.out.println("---------------------------------------");
                        break;

                    case 4:
                        try {
                            System.out.print("Qual o Ticker da moeda para venda? ");
                            String tickerVenda = leitor.next().toUpperCase();
                            leitor.nextLine(); // Limpa buffer

                            // BUSQUE O NOME REAL QUE A API USA
                            String nomeParaApi = httpTradutor.converterTickerParaId(tickerVenda);
                            Moeda moedaVenda = minhaCarteira.obterMoeda(tickerVenda, nomeParaApi);

                            if(moedaVenda.getSaldo() <= 0){
                                System.out.println("Não tem saldo de " + tickerVenda + " para vender.");
                                break;
                            }

                            System.out.printf("Saldo disponível: %.8f\n", moedaVenda.getSaldo());
                            System.out.print("Quantidade a vender: ");
                            double qtdVenda = Double.parseDouble(leitor.nextLine().replace(",", "."));
                            
                            if (qtdVenda > moedaVenda.getSaldo()) {
                                System.out.println("Erro: Saldo insuficiente!");
                                break; 
                            }

                            System.out.print("Preço unitário de venda: ");
                            double precoVenda = Double.parseDouble(leitor.nextLine().replace(",", "."));
                            
                            Transacao tVenda = new Transacao(tickerVenda, qtdVenda, precoVenda, "VENDA");
                            carteira.processarTransacao(moedaVenda, tVenda);
                            historico.add(tVenda); 
                            repositorio.salvar(tVenda);
                            
                            System.out.println("Venda registrada!");
                        } catch(Exception e) {
                            System.out.println("Erro na venda: " + e.getMessage());
                        }
                        break;
                    case 5:
                        System.out.println("\n--- SIMULADOR DE LUCRO ---");

                        HttpService http = new HttpService();

                        for (Moeda m : minhaCarteira.getMoedas().values()) {
                            if (m.getSaldo() > 0) {

                                double precoMercado = http.buscarPrecoAtual(m);
                                //double precoMercado = leitor.nextDouble();
                                
                                double lucro = carteira.calcularLucroPotencial(m, precoMercado);
                                double porcentagem = (lucro / (m.getSaldo() * m.getPrecoMedio())) * 100;

                                System.out.printf("Resultado para %s:\n", m.getTicker());
                                System.out.printf("  Saldo atual: %.8f\n", m.getSaldo());
                                System.out.printf("  Preço atual: %.8f\n", precoMercado);
                                System.out.printf("  PNL: $ %.2f (%.2f%%)\n", lucro, porcentagem);
                                System.out.println("---------------------------------------");
                            }
                        }
                        break;
                    case 6:
                        System.out.println("Gerando relatório...");
                        repositorio.gerarRelatorio(new ArrayList<>(minhaCarteira.getMoedas().values()));
                        break;
                    
                    case 7:
                        System.out.println("Saindo...");
                        break;

                    default:
                        System.out.println("Opção inválida!");
                }
            }catch(NumberFormatException e){ 
                System.out.println(" ERRO: Por favor, digite apenas números inteiros para as opções do menu.");
            }catch(Exception e){ 
                System.out.println(" Ocorreu um erro inesperado: " + e.getMessage());
            }
        }

        leitor.close();
    }
}