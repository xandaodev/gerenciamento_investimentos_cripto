package br.com.criptovision.main;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);
        CarteiraService carteira = new CarteiraService();
        //criando um array pro historico
        List<Transacao> historico = new ArrayList<>();

        //focando apenas no bitcoin por enquango
        Moeda btc = new Moeda("Bitcoin", "BTC");

        int opcao = 0;
        while (opcao != 4){
            System.out.println("\n--- GERENCIAMNETO DE INVESTIMENTOS CRIPTO ---");
            System.out.println("1. Nova compra");
            System.out.println("2. Ver Saldo e Preço Médio");
            System.out.println("3. Ver Histórico de Transações");
            System.out.println("4. Sair");
            System.out.print("Escolha uma opção: ");
            
            opcao = leitor.nextInt();

            switch (opcao){
                case 1:
                    System.out.print("Quantidade comprada: ");
                    double qtd = leitor.nextDouble();
                    System.out.print("Preço unitário pago: ");
                    double preco = leitor.nextDouble();
                    
                    Transacao t = new Transacao("BTC", qtd, preco, "COMPRA");
                    carteira.processarTransacao(btc, t);
                    historico.add(t); // guarda no histórico
                    
                    System.out.println(" Compra registrada com sucesso!");
                    break;
                    
                case 2:
                    System.out.println("\n--- MEU SALDO ---");
                    System.out.println("Ativo: " + btc.getTicker());
                    System.out.println("Quantidade: " + btc.getSaldo());
                    System.out.printf("Preço Médio: R$ %.2f\n", btc.getPrecoMedio());
                    break;

                case 3:
                    System.out.println("\n--- HISTÓRICO ---");
                    for(Transacao tr : historico){
                        System.out.println(tr.getTipo() + " | " + tr.getQuantidade() + " BTC | R$ " + tr.getPrecoUnitario());
                    }
                    break;

                case 4:
                    System.out.println("Saindo...");
                    break;

                default:
                    System.out.println("Opção inválida!");
            }
        }
        leitor.close();
    }
}