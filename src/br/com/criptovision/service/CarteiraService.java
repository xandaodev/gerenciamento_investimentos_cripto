package br.com.criptovision.service;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;

public class CarteiraService {

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
            moeda.setSaldo(moeda.getSaldo() - transacao.getQuantidade());
            //na venda o preço medio nao muda
            double custoParteVendida = transacao.getQuantidade() * moeda.getPrecoMedio();
            double valorRecebidoNaVenda = transacao.getQuantidade() * transacao.getPrecoUnitario();
            double lucroOperacao =valorRecebidoNaVenda - custoParteVendida;

            System.out.printf("PNL dessa venda: $ %.2f\n", lucroOperacao);

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
    
}
