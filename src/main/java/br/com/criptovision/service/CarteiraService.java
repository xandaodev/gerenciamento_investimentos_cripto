package br.com.criptovision.service;

import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Carteira;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.exception.SaldoInsuficienteException;
import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.dto.ResumoAtivoDTO;
import br.com.criptovision.dto.SimulacaoVendaDTO;
import br.com.criptovision.dto.SimulacaoDCADTO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// uma das classes mais importantes, aqui são feitos todos os calculos usando os dados que as outras classes fornecem

@Service
public class CarteiraService {

    @Autowired
    private TransacaoRepository transacaoRepo;

    // nesse metodo é atualizado o estado de uma moeda baseado numa transacao
    // ele é chamado e rechamado varias vezes quando o programa é iniciado para reconstruir seu saldo

    // metodo 1 que é usado na main
    public void processarTransacao(Moeda moeda, Transacao transacao) throws SaldoInsuficienteException {
        processarTransacao(moeda, transacao, true); // redireciona para o método de baixo
    }

    // metodo 2, o motor
    public void processarTransacao(Moeda moeda, Transacao transacao, boolean salvarNoBanco) throws SaldoInsuficienteException {

        if(transacao.getTipo().equals("COMPRA")){
            BigDecimal custoTotalAntigo = moeda.getSaldo().multiply(moeda.getPrecoMedio());

            // custoNovaCompra = quantidade * precoUnitario
            BigDecimal custoNovaCompra = transacao.getQuantidade().multiply(transacao.getPrecoUnitario());

            // novoSaldo = saldo + quantidade
            BigDecimal novoSaldo = moeda.getSaldo().add(transacao.getQuantidade());

            // novoPrecoMedio = (custoTotalAntigo + custoNovaCompra) / novoSaldo
            BigDecimal novoPrecoMedio = custoTotalAntigo.add(custoNovaCompra).divide(novoSaldo, 8, RoundingMode.HALF_UP);

            moeda.setSaldo(novoSaldo);
            moeda.setPrecoMedio(novoPrecoMedio);

        }else if(transacao.getTipo().equals("VENDA")){
            if (transacao.getQuantidade().compareTo(BigDecimal.ZERO) <= 0){
                throw new IllegalArgumentException("A quantidade de venda deve ser maior que zero.");
            }

            if (transacao.getQuantidade().compareTo(moeda.getSaldo()) > 0){
                throw new SaldoInsuficienteException(
                        "Saldo insuficiente para a venda! Você tentou vender " + transacao.getQuantidade() +
                                ", mas possui apenas " + moeda.getSaldo() + " de " + moeda.getTicker()
                );
            }

            BigDecimal custoParteVendida = transacao.getQuantidade().multiply(moeda.getPrecoMedio());
            BigDecimal valorRecebidoNaVenda = transacao.getQuantidade().multiply(transacao.getPrecoUnitario());
            BigDecimal lucroOperacao = valorRecebidoNaVenda.subtract(custoParteVendida);

            moeda.setSaldo(moeda.getSaldo().subtract(transacao.getQuantidade()));
        }

        if (salvarNoBanco){
            this.transacaoRepo.save(transacao);
        }
    }

    public List<Transacao> carregarHistoricoDeTransacoes(){
        return this.transacaoRepo.findAll();
    }

    // metodo muito importante e funcional, ele calcula quanto voce ganharia se vendesse tudo agora
    // "Lucro nao realizado"
    public double calcularLucroPotencial(Moeda moeda, double precoAtual){
        if(moeda.getSaldo().compareTo(BigDecimal.ZERO) <= 0){
            return 0;
        }
        double valorInvestido = moeda.getSaldo().multiply(moeda.getPrecoMedio()).doubleValue();
        double valorAtual = moeda.getSaldo().multiply(BigDecimal.valueOf(precoAtual)).doubleValue();
        return valorAtual - valorInvestido;
    }

    //metodo para calcular valor total da carteira, ele soma o valor de todas as suas moedas da carteira a preço atual de mercado
    public double calcularValorTotalCarteira(Map<String, Moeda> moedas, HttpService http){
        double valorTotal = 0;
        for(Moeda m : moedas.values()){
            if(m.getSaldo().compareTo(BigDecimal.ZERO) > 0){
                double precoAtual = http.buscarPrecoAtual(m);// aqui ele pede ao HttpService o preço atual da Binance
                double saldoDaMoeda = m.getSaldo().doubleValue();
                valorTotal += (saldoDaMoeda * precoAtual);
            }
        }
        return valorTotal;
    }

    //esse metodo soma o lucro/prejuizo individual de cada moeda que voce tem na carteira e calcula o pnl total dela
    public double calcularPnlTotal(Map<String, Moeda> moedas, HttpService http){
        double pnlTotal = 0;
        for(Moeda m : moedas.values()){
            if(m.getSaldo().compareTo(BigDecimal.ZERO) > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                pnlTotal += calcularLucroPotencial(m, precoAtual);
            }
        }
        return pnlTotal;
    }

    // agr o metodo puro, sem prints, apenas regra de negócio
    public SimulacaoDCADTO simularDCA(Moeda moeda, double valorAporteUSD, double precoMercado){
        double saldoAtual = moeda.getSaldo().doubleValue();
        double pmAtual = moeda.getPrecoMedio().doubleValue();
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
        return new SimulacaoDCADTO(
                qtdComprada, saldoAtual, novoSaldoTotal, pmAtual, novoPM, diferencaPM, valorizacaoNecessaria
        );
    }

    // agora a reconstrução da carteira nao fica mais na main, fica aqui no service
    public void reconstruirCarteira(Carteira carteira, List<Transacao> historico) {
        for(Transacao tAntiga : historico){
            String tickerOriginal = tAntiga.getTicker().toUpperCase();
            Moeda m = carteira.obterMoeda(tickerOriginal, tickerOriginal);
            try{
                processarTransacao(m, tAntiga, false);
            }catch(Exception e){
            }
        }
    }


    // METODOS DTO

    public SimulacaoVendaDTO simularVendaFutura(Moeda moeda, double precoFicticio, double precoAtualMercado){

        // calcula o lucro e a porcentagem
        double lucroSimulado = calcularLucroPotencial(moeda, precoFicticio);
        double custoBase = moeda.getSaldo().multiply(moeda.getPrecoMedio()).doubleValue();
        double porcSimulada = (lucroSimulado / custoBase) * 100;

        // calcula os totais
        double valorTotalFicticio = moeda.getSaldo().doubleValue() * precoFicticio;
        double valorTotalAtual = moeda.getSaldo().doubleValue() * precoAtualMercado;

        //empacota tudo na caixa (DTO) e devolve para quem chamou
        return new SimulacaoVendaDTO(lucroSimulado, porcSimulada, valorTotalFicticio, valorTotalAtual);
    }

    // gera o resumo completo da carteira e empacota tudo num DTO
    public ResumoCarteiraDTO gerarResumoCompleto(Carteira carteira, HttpService httpService){

        double totalCalculado = 0;
        double pnlTotalGeral = 0;
        double totalPatrimonioOntem = 0;
        List<ResumoAtivoDTO> listaAtivos = new ArrayList<>();

        for(Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo().compareTo(BigDecimal.ZERO) > 0){
                double[] dadosApi = httpService.buscarPrecoEVariacao(m);
                double preco = dadosApi[0];
                double variacao24h = dadosApi[1];

                double valorNoAtivo = m.getSaldo().doubleValue() * preco;
                double lucroDestaMoeda = calcularLucroPotencial(m, preco);
                double custoBase = m.getSaldo().multiply(m.getPrecoMedio()).doubleValue();

                double porcentagemLucro = custoBase == 0 ? 0 : (lucroDestaMoeda / custoBase) * 100;

                pnlTotalGeral += lucroDestaMoeda;
                totalCalculado += valorNoAtivo;
                totalPatrimonioOntem += valorNoAtivo / (1 + (variacao24h / 100));

                listaAtivos.add(new ResumoAtivoDTO(
                        m.getTicker(), m.getSaldo().doubleValue(), preco, valorNoAtivo, porcentagemLucro, variacao24h
                ));
            }
        }
        double varTotalCarteira = (totalPatrimonioOntem > 0) ? ((totalCalculado - totalPatrimonioOntem) / totalPatrimonioOntem) * 100 : 0;

        return new ResumoCarteiraDTO(totalCalculado, pnlTotalGeral, varTotalCarteira, listaAtivos);
    }

    public double calcularPatrimonioTotal() {
        List<Transacao> todasAsTransacoes = this.transacaoRepo.findAll();

        double total = 0;
        for (Transacao t : todasAsTransacoes) {
            double valorTrans = t.getQuantidade().multiply(t.getPrecoUnitario()).doubleValue();
            if(t.getTipo().equals("COMPRA")){
                total += valorTrans;
            }else if(t.getTipo().equals("VENDA")){
                total -= valorTrans;
            }
        }
        return total;
    }

    @Autowired
    private HttpService httpService;

    public ResumoCarteiraDTO obterResumoGeral(){
        Carteira carteira = new Carteira();//carteira vazia

        List<Transacao> historico = transacaoRepo.findAll();

        reconstruirCarteira(carteira, historico);//reconstroi a carteira

        return gerarResumoCompleto(carteira, this.httpService);
    }

    public Transacao registrarNovaTransacao(Transacao novaTransacao){
        Carteira carteiraTemporaria = new Carteira();
        List<Transacao> historico = transacaoRepo.findAll();
        reconstruirCarteira(carteiraTemporaria, historico);

        String ticker = novaTransacao.getTicker().toUpperCase();
        Moeda moedaDaOperacao = carteiraTemporaria.obterMoeda(ticker, ticker);

        processarTransacao(moedaDaOperacao, novaTransacao, true);

        return novaTransacao;
    }

}