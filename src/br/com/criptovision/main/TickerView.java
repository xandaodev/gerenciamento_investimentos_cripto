package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.repository.TransacaoRepository;
import java.util.List;


public class TickerView {

    public static void main(String[] args){

        System.out.println("- RESUMO RÁPIDO DE MERCADO -");
        System.out.println("atualizando preços da API...\n");

        CarteiraService service = new CarteiraService();
        TransacaoRepository repo = new TransacaoRepository();
        HttpService http = new HttpService();
        Carteira carteira = new Carteira();


        // carrega o historico para montar a carteira
        List<Transacao> historico = repo.lerTudo();
        for(Transacao t : historico){
            String nomeApi = http.converterTickerParaId(t.getTicker());
            Moeda m = carteira.obterMoeda(t.getTicker(), nomeApi);
            service.processarTransacao(m, t);
        }

        // calculos gerais
        double totalGeral = service.calcularValorTotalCarteira(carteira.getMoedas(),http);
        double pnlTotalGeral = 0;

        System.out.println("---------------------------------------");
        for(Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                double lucro = service.calcularLucroPotencial(m, precoAtual);
                double porcentagem = (lucro / (m.getSaldo() * m.getPrecoMedio())) * 100;
                pnlTotalGeral += lucro;

                System.out.printf("%s: $ %.2f | PNL: $ %.2f (%.2f%%)\n", 
                    m.getTicker(), precoAtual, lucro, porcentagem);
            }
        }
        System.out.println("---------------------------------------");
        
        String status = (pnlTotalGeral >= 0) ? "LUCRO" : "PREJUÍZO";
        System.out.printf("PATRIMÔNIO TOTAL: $ %.2f\n", totalGeral);
        System.out.printf("PNL GERAL: $ %.2f (%s)\n", pnlTotalGeral, status);
        System.out.println("---------------------------------------");

        //pausa o terminal para ele não fechar sozinho 
        System.out.println("\nPressione Enter para fechar");
        try{ 
            System.in.read(); 
        }catch(Exception e){

        }
    }
}