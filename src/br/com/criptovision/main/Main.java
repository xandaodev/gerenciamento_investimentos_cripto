package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
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
        List<Transacao> historico = new ArrayList<>();

        //focando apenas no bitcoin por enquanto
        Carteira minhaCarteira = new Carteira();

        int opcao = 0;
        while (opcao != 5){
            System.out.println("\n--- GERENCIAMNETO DE INVESTIMENTOS CRIPTO ---");
            System.out.println("1. Nova compra");
            System.out.println("2. Ver Saldo e Preço Médio");
            System.out.println("3. Ver Histórico de Transações");
            System.out.println("4. Nova venda");
            System.out.println("5. Sair do sistema");
            System.out.print("Escolha uma opção: ");
            
            opcao = leitor.nextInt();

            switch (opcao){
                case 1:
                    System.out.print("Qual o Ticker da moeda ? ");
                    String ticker = leitor.next().toUpperCase();

                    // se não existir, é criada a moeda na hora
                    Moeda moedaSelecionada = minhaCarteira.obterMoeda(ticker, ticker); 

                    System.out.print("Quantidade comprada: ");
                    double qtd = leitor.nextDouble();
                    System.out.print("Preço unitário pago: ");
                    double preco = leitor.nextDouble();
                    
                    Transacao t = new Transacao(ticker, qtd, preco, "COMPRA");
                    carteira.processarTransacao(moedaSelecionada, t);
                    historico.add(t); // guarda no histórico
                    
                    repositorio.salvar(t);//salvando no repository
                    System.out.println(" Compra registrada com sucesso!");
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
                    System.out.println("\n--- HISTÓRICO ---");
                    for(Transacao tr : historico){
                        System.out.println(tr.getTipo() + " | " + tr.getTicker() + " | " + tr.getQuantidade() + " | $ " + tr.getPrecoUnitario());
                    }
                    break;

                case 4:
                    System.out.print("Qual o Ticker da moeda para venda? ");
                    String tickerVenda = leitor.next().toUpperCase();
                    Moeda moedaVenda = minhaCarteira.obterMoeda(tickerVenda, tickerVenda);

                    System.out.print("Quantidade vendida: ");
                    double qtdVenda = leitor.nextDouble();
                    if(qtdVenda > moedaVenda.getSaldo()){
                        System.out.println("Saldo insuficiente!");
                    }else{
                    System.out.print("Preço unitário de venda: ");
                    double precoVenda = leitor.nextDouble();
                    
                    Transacao tVenda = new Transacao(tickerVenda, qtdVenda, precoVenda, "VENDA");
                    carteira.processarTransacao(moedaVenda, tVenda);
                    historico.add(tVenda); // guarda no histórico
                    
                    repositorio.salvar(tVenda);//salvando no repository
                    System.out.println(" Venda registrada com sucesso!");
                    }
                    break;
                case 5:
                    System.out.println("Saindo...");
                    break;

                default:
                    System.out.println("Opção inválida!");
            }
        }
        leitor.close();
    }
}