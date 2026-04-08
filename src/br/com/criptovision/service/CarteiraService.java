package br.com.criptovision.service;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.repository.LucroDAO;
import br.com.criptovision.repository.LucroDAOMySQL;

// uma das classes mais importantes, aqui são feitos todos os calculos usando os dados que as outras classes fornecem

public class CarteiraService {

    // repositorio pra gravar os lucros toda vez que uma venda ocorre
    //private LucroRepository lucroRepo = new LucroRepository();

    private LucroDAO lucroRepo = new LucroDAOMySQL();// agr usando a interface apontando para o MySQL


    // nesse metodo é atualizado o estado de uma moeda baseado numa transacao
    // ele é chamado e rechamado varias vezes quando o programa é iniciado para reconstruir seu saldo
    public void processarTransacao(Moeda moeda, Transacao transacao) throws br.com.criptovision.exception.SaldoInsuficienteException{

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
            
            if (transacao.getQuantidade() <= 0){
                throw new IllegalArgumentException("A quantidade de venda deve ser maior que zero.");
            }

            if (transacao.getQuantidade() > moeda.getSaldo()){
                throw new br.com.criptovision.exception.SaldoInsuficienteException(
                    "Saldo insuficiente para a venda! Você tentou vender " + transacao.getQuantidade() + 
                    ", mas possui apenas " + moeda.getSaldo() + " de " + moeda.getTicker()
                );
            }

            // CALCULO DE PNL (profit and loss):
            double custoParteVendida = transacao.getQuantidade() * moeda.getPrecoMedio();
            double valorRecebidoNaVenda = transacao.getQuantidade() * transacao.getPrecoUnitario();
            double lucroOperacao = valorRecebidoNaVenda - custoParteVendida;

            lucroRepo.salvarLucroRealizado(moeda.getTicker(), lucroOperacao);

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

    // agr o metodo puro, sem prints, apenas regra de negócio
    public br.com.criptovision.dto.SimulacaoDCADTO simularDCA(br.com.criptovision.model.Moeda moeda, double valorAporteUSD, double precoMercado){
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

        //mostra quanto a moeda precisa valorizar para começar a ter lucro
        double valorizacaoNecessaria = ((novoPM / precoMercado) - 1) * 100;

        // empacota tudo na caixa e devolve
        return new br.com.criptovision.dto.SimulacaoDCADTO(
            qtdComprada, saldoAtual, novoSaldoTotal, pmAtual, novoPM, diferencaPM, valorizacaoNecessaria
        );
    }

    // método criado para permitir a injeção de um DAO falso durante os testes
    public void setLucroRepo(br.com.criptovision.repository.LucroDAO lucroRepo){
        this.lucroRepo = lucroRepo;
    }

    // METODOS DTO

    public br.com.criptovision.dto.SimulacaoVendaDTO simularVendaFutura(br.com.criptovision.model.Moeda moeda, double precoFicticio, double precoAtualMercado){
        
        // calcula o lucro e a porcentagem
        double lucroSimulado = calcularLucroPotencial(moeda, precoFicticio);
        double porcSimulada = (lucroSimulado / (moeda.getSaldo() * moeda.getPrecoMedio())) * 100;

        // calcula os totais
        double valorTotalFicticio = moeda.getSaldo() * precoFicticio;
        double valorTotalAtual = moeda.getSaldo() * precoAtualMercado;

        //empacota tudo na caixa (DTO) e devolve para quem chamou
        return new br.com.criptovision.dto.SimulacaoVendaDTO(
            lucroSimulado, 
            porcSimulada, 
            valorTotalFicticio, 
            valorTotalAtual
        );
    }

    // gera o resumo completo da carteira e empacota tudo num DTO
    public br.com.criptovision.dto.ResumoCarteiraDTO gerarResumoCompleto(br.com.criptovision.model.Carteira carteira, br.com.criptovision.service.HttpService httpService){
        
        double totalCalculado = 0;
        double pnlTotalGeral = 0;
        double totalPatrimonioOntem = 0;
        java.util.List<br.com.criptovision.dto.ResumoAtivoDTO> listaAtivos = new java.util.ArrayList<>();

        for(br.com.criptovision.model.Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo() > 0){
                double[] dadosApi = httpService.buscarPrecoEVariacao(m); 
                double preco = dadosApi[0];
                double variacao24h = dadosApi[1];

                double valorNoAtivo = m.getSaldo() * preco;
                double lucroDestaMoeda = calcularLucroPotencial(m, preco);
                double porcentagemLucro = (lucroDestaMoeda / (m.getSaldo() * m.getPrecoMedio())) * 100;
                
                pnlTotalGeral += lucroDestaMoeda;
                totalCalculado += valorNoAtivo;
                totalPatrimonioOntem += valorNoAtivo / (1 + (variacao24h / 100)); 

                listaAtivos.add(new br.com.criptovision.dto.ResumoAtivoDTO(
                    m.getTicker(), m.getSaldo(), preco, valorNoAtivo, porcentagemLucro, variacao24h
                ));
            }
        }
        double varTotalCarteira = (totalPatrimonioOntem > 0) ? ((totalCalculado - totalPatrimonioOntem) / totalPatrimonioOntem) * 100 : 0;

        return new br.com.criptovision.dto.ResumoCarteiraDTO(totalCalculado, pnlTotalGeral, varTotalCarteira, listaAtivos);
    }


}
