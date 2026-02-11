package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.repository.TransacaoRepository;
import java.util.List;

public class TickerView {

    // adicionando cores pra personalizar os lucros e prejuizos
    public static final String RESET = "\u001B[0m";
    public static final String VERDE = "\u001B[32m";
    public static final String VERMELHO = "\u001B[31m";
    public static final String AMARELO = "\u001B[33m";

    public static void main(String[] args){

        System.out.println(" - RESUMO RÁPIDO DE MERCADO  -");

        CarteiraService service = new CarteiraService();
        TransacaoRepository repo = new TransacaoRepository();
        HttpService http = new HttpService();
        Carteira carteira = new Carteira();

        List<Transacao> historico = repo.lerTudo();
        for(Transacao t : historico){
            String nomeApi = http.converterTickerParaId(t.getTicker());
            Moeda m = carteira.obterMoeda(t.getTicker(), nomeApi);
            service.processarTransacao(m, t);
        }

        double pnlTotalGeral = 0;
        double valorTotalPatrimonio = 0;

        System.out.println("---------------------------------------");
        for(Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                double lucro = service.calcularLucroPotencial(m, precoAtual);
                double valorNoAtivo = m.getSaldo() * precoAtual;
                double porcentagem = (lucro / (m.getSaldo() * m.getPrecoMedio())) * 100;
                
                pnlTotalGeral += lucro;
                valorTotalPatrimonio += valorNoAtivo;

                String corPnl = (lucro >= 0) ? VERDE : VERMELHO;
                
                System.out.printf("%s: $ %.2f | PNL: %s$ %.2f (%.2f%%)%s\n", 
                    m.getTicker(), precoAtual, corPnl, lucro, porcentagem, RESET);
            }
        }
        System.out.println("---------------------------------------");
        
        String corStatus = (pnlTotalGeral >= 0) ? VERDE : VERMELHO;
        String status = (pnlTotalGeral >= 0) ? "LUCRO" : "PREJUÍZO";

        System.out.printf("PATRIMÔNIO TOTAL: $ %.2f\n", valorTotalPatrimonio);
        System.out.printf("PNL GERAL: %s$ %.2f (%s)%s\n", corStatus, pnlTotalGeral, status, RESET);
        System.out.println("---------------------------------------");

        System.out.println("\nPressione Enter para fechar");
        try{ 
            System.in.read(); 
        }catch(Exception e){}
    }
}