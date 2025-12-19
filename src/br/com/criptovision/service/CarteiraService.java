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
            
        }
    }
    
}
