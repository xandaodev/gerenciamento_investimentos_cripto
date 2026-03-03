package br.com.criptovision.service;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;

import br.com.criptovision.repository.LucroRepository;

// uma das classes mais importantes, aqui são feitos todos os calculos usando os dados que as outras classes fornecem

public class CarteiraService {

    // repositorio pra gravar os lucros toda vez que uma venda ocorre
    private LucroRepository lucroRepo = new LucroRepository();


    // nesse metodo é atualizado o estado de uma moeda baseado numa transacao
    // ele é chamado e rechamado varias vezes quando o programa é iniciado para reconstruir seu saldo
    public void processarTransacao(Moeda moeda, Transacao transacao){

        // LOGICA DA COMPRA:
        if(transacao.getTipo().equals("COMPRA")){

            // CALCULA O QUANTO SE TINHA INVESTIDO ANTES:
            double custoTotalAntigo = moeda.getSaldo() * moeda.getPrecoMedio();
            // CALCULA O CUSTO DA NOVA COMPRA:
            double custoNovaCompra = transacao.getQuantidade() * transacao.getPrecoUnitario();
            // O NOVO SALDO É A SOMA DAS QUANTIDADES:
            double novoSaldo = moeda.getSaldo() + transacao.getQuantidade();

            // CALCULO DO PREÇO MEDIO:
            double novoPrecoMedio = (custoTotalAntigo + custoNovaCompra) / novoSaldo;

            //atualizando os valores de saldo e preco medio:
            moeda.setSaldo(novoSaldo);
            moeda.setPrecoMedio(novoPrecoMedio);

        // LOGICA DA VENDA:
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

            // CALCULO DE PNL (profit and loss):

            
            double custoParteVendida = transacao.getQuantidade() * moeda.getPrecoMedio();// quanto custou a parte que esta sendo vendida agora
            double valorRecebidoNaVenda = transacao.getQuantidade() * transacao.getPrecoUnitario();// quanto estou recebendo por ela
            double lucroOperacao = valorRecebidoNaVenda - custoParteVendida;// lucro = recebido - gasto

            //System.out.printf("PNL dessa venda: $ %.2f\n", lucroOperacao);

            lucroRepo.salvarLucroRealizado(moeda.getTicker(), lucroOperacao);// salvamos esse lucro no lucroRepo

            // atualização do saldo 
            moeda.setSaldo(moeda.getSaldo() - transacao.getQuantidade());
            
        }
    }
    
    // metodo muito importante e funcional, ele calcula quanto voce ganharia se vendesse tudo agora
    // "Lucro nao realizado"
    public double calcularLucroPotencial(Moeda moeda, double precoAtual){
        if(moeda.getSaldo() <= 0){
            return 0;
        }
        double valorInvestido = moeda.getSaldo() * moeda.getPrecoMedio();
        double valorAtual = moeda.getSaldo() * precoAtual;
        return valorAtual - valorInvestido;
    }

    //metodo para calcular valor total da carteira, ele soma o valor de todas as suas moedas da carteira a preço atual de mercado
    public double calcularValorTotalCarteira(java.util.Map<String, Moeda> moedas, HttpService http){
        double valorTotal = 0;
        for(Moeda m : moedas.values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);// aqui ele pede ao HttpService o preço atual da Binance
                valorTotal += (m.getSaldo() * precoAtual);
            }
        }
        return valorTotal;
    }

    //esse metodo soma o lucro/prejuizo individual de cada moeda que voce tem na carteira e calcula o pnl total dela
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

    //novo metodo para calcular, antes era feito na propria main
    public void simularDCA(Moeda moeda, double valorAporteUSD, double precoMercado){

        double saldoAtual = moeda.getSaldo();
        double pmAtual = moeda.getPrecoMedio();
        double custoTotalAtual = saldoAtual * pmAtual;

        // quantidade que o novo aporte compraria
        double qtdComprada = valorAporteUSD / precoMercado;

        // novos valores totais simulados
        double novoSaldoTotal = saldoAtual + qtdComprada;
        double novoCustoTotal = custoTotalAtual + valorAporteUSD;
        double novoPM = novoCustoTotal / novoSaldoTotal;

        // diferenças em porcentagem do preço medio
        double diferencaPM = ((novoPM - pmAtual) / pmAtual) * 100;

        // exibião e formatação no terminal
        System.out.println("\n================ RESULTADO DA SIMULAÇÃO ================");
        System.out.printf(" Se você investir: $ %.2f com %s a $ %.2f\n", valorAporteUSD, moeda.getTicker(), precoMercado);
        System.out.printf(" Você irá comprar:    %.8f %s\n", qtdComprada, moeda.getTicker());
        System.out.println("--------------------------------------------------------");
        System.out.printf(" Saldo:   %.8f  ->  Novo Saldo: %.8f\n", saldoAtual, novoSaldoTotal);
        System.out.printf(" Preço Médio:      $ %.2f      ->  Novo Preço Médio:    $ %.2f\n", pmAtual, novoPM);

        if (novoPM < pmAtual){
            System.out.printf(" Seu Preço Médio cairia %.2f%%!\n", Math.abs(diferencaPM));
        }else{
            System.out.printf(" Seu Preço Médio subiria %.2f%%.\n", diferencaPM);
        }
        
        // PONTO DE EQUILIBRIO
        // Mostra quanto a moeda precisa valorizar para você começar a ter lucro após esse aporte
        double valorizacaoNecessaria = ((novoPM / precoMercado) - 1) * 100;
        if(valorizacaoNecessaria > 0){
            System.out.printf(" A moeda precisa subir %.2f%% para atingir o novo Preço Médio.\n", valorizacaoNecessaria);
        }
        System.out.println("===================================================================");
    }
    
}
