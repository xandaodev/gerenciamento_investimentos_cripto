package br.com.criptovision.service;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;

import br.com.criptovision.repository.LucroRepository;

public class CarteiraService {

    private LucroRepository lucroRepo = new LucroRepository();

    public void processarTransacao(Moeda moeda, Transacao transacao){
        if(transacao.getTipo().equals("COMPRA")){
            double custoTotalAntigo = moeda.getSaldo() * moeda.getPrecoMedio();
            double custoNovaCompra = transacao.getQuantidade() * transacao.getPrecoUnitario();

            double novoSaldo = moeda.getSaldo() + transacao.getQuantidade();
            double novoPrecoMedio = (custoTotalAntigo + custoNovaCompra) / novoSaldo;

            //atualizando os valores de saldo e preco medio
            moeda.setSaldo(novoSaldo);
            moeda.setPrecoMedio(novoPrecoMedio);

        }else if(transacao.getTipo().equals("VENDA")){
            // verificação de segurança: a quantidade deve ser positiva
            if (transacao.getQuantidade() <= 0){
                System.out.println("Erro: A quantidade de venda deve ser maior que zero.");
                return;
            }

            // verifica saldo
            if (transacao.getQuantidade() > moeda.getSaldo()){
                System.out.println("Erro: Saldo insuficiente para realizar a venda.");
                return;
            }

            // calculo de pnl
            double custoParteVendida = transacao.getQuantidade() * moeda.getPrecoMedio();
            double valorRecebidoNaVenda = transacao.getQuantidade() * transacao.getPrecoUnitario();
            double lucroOperacao = valorRecebidoNaVenda - custoParteVendida;

            System.out.printf("PNL dessa venda: $ %.2f\n", lucroOperacao);

            lucroRepo.salvarLucroRealizado(moeda.getTicker(), lucroOperacao);

            // atualização do saldo 
            moeda.setSaldo(moeda.getSaldo() - transacao.getQuantidade());
            
        }
    }
    
    public double calcularLucroPotencial(Moeda moeda, double precoAtual){
        if(moeda.getSaldo() <= 0){
            return 0;
        }
        double valorInvestido = moeda.getSaldo() * moeda.getPrecoMedio();
        double valorAtual = moeda.getSaldo() * precoAtual;
        return valorAtual - valorInvestido;
    }

    //metodo para calcular lucro total
    public double calcularValorTotalCarteira(java.util.Map<String, br.com.criptovision.model.Moeda> moedas, HttpService http){
        double valorTotal = 0;
        for(br.com.criptovision.model.Moeda m : moedas.values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                valorTotal += (m.getSaldo() * precoAtual);
            }
        }
        return valorTotal;
    }

    //metodo para calcular pnl total da carteira
    public double calcularPnlTotal(java.util.Map<String, Moeda> moedas, HttpService http){
        double pnlTotal = 0;
        for(Moeda m : moedas.values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                pnlTotal += calcularLucroPotencial(m, precoAtual);
            }
        }
        return pnlTotal;
    }
    
}
