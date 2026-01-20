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

        //focando apenas no bitcoin por enquanto
        Carteira minhaCarteira = new Carteira();

        for (Transacao tAntiga : historico) {
            Moeda m = minhaCarteira.obterMoeda(tAntiga.getTicker(), tAntiga.getTicker());
            carteira.processarTransacao(m, tAntiga);
        }

        int opcao = 0;
        while (opcao != 6){
            try{
                System.out.println("\n--- GERENCIAMNETO DE INVESTIMENTOS CRIPTO ---");
                System.out.println("1. Nova compra");
                System.out.println("2. Ver Saldo e Preço Médio");
                System.out.println("3. Ver Histórico de Transações");
                System.out.println("4. Nova venda");
                System.out.println("5. Simular lucro");
                System.out.println("6. Sair do sistema");
                System.out.print("Escolha uma opção: ");
                
                opcao = leitor.nextInt();

                switch (opcao){
                    case 1:
                        try{
                            System.out.print("Qual o Ticker da moeda ? ");
                            String ticker = leitor.next().toUpperCase();

                            // se não existir, é criada a moeda na hora
                            Moeda moedaSelecionada = minhaCarteira.obterMoeda(ticker, ticker); 

                            System.out.print("Quantidade comprada: ");
                            double qtd = Double.parseDouble(leitor.next().replace(",", "."));
        
                            System.out.print("Preço unitário pago: ");
                            double preco = Double.parseDouble(leitor.next().replace(",", "."));
                            
                            Transacao t = new Transacao(ticker, qtd, preco, "COMPRA");
                            carteira.processarTransacao(moedaSelecionada, t);
                            historico.add(t); // guarda no histórico
                            repositorio.salvar(t);
                            System.out.println(" Compra registrada com sucesso!");
                            break;
                        }catch(NumberFormatException e){
                            System.out.println("ERRO: Valor inválido! Use apenas números e pontos (ex: 10.50).");
                            leitor.nextLine(); // limpa qualquer resquício do erro
                        }
                        
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
                        System.out.println("\n--- HISTÓRICO ---");
                        for(Transacao tr : historico){
                            System.out.println(tr.getTipo() + " | " + tr.getTicker() + " | " + tr.getQuantidade() + " | $ " + tr.getPrecoUnitario());
                        }
                        break;

                    case 4:
                        try{
                            System.out.print("Qual o Ticker da moeda para venda? ");
                            String tickerVenda = leitor.next().toUpperCase();
                            Moeda moedaVenda = minhaCarteira.obterMoeda(tickerVenda, tickerVenda);

                            System.out.print("Quantidade vendida: ");
                            double qtdVenda = Double.parseDouble(leitor.next().replace(",", "."));
                            
                            if(qtdVenda > moedaVenda.getSaldo()){
                                System.out.println("Saldo insuficiente!");
                            }else{
                                System.out.print("Preço unitário de venda: ");
                                double precoVenda = Double.parseDouble(leitor.next().replace(",", "."));
                                
                                Transacao tVenda = new Transacao(tickerVenda, qtdVenda, precoVenda, "VENDA");
                                carteira.processarTransacao(moedaVenda, tVenda);
                                historico.add(tVenda); 
                                
                                repositorio.salvar(tVenda);
                                System.out.println(" Venda registrada com sucesso!");
                            }
                        }catch(NumberFormatException e){
                            System.out.println("ERRO: Valor inválido! Use apenas números e pontos (ex: 10.50).");
                            leitor.nextLine(); 
                        }
                        break;
                    case 5:
                        System.out.println("\n--- SIMULADOR DE LUCRO ---");

                        HttpService http = new HttpService();

                        for (Moeda m : minhaCarteira.getMoedas().values()) {
                            if (m.getSaldo() > 0) {

                                double precoMercado = http.buscarPrecoAtual(m.getTicker());
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