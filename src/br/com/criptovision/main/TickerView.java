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

        System.out.println(" ---   RESUMO RÁPIDO DE MERCADO   ---");
        System.out.println(" Atualizando preços em tempo real...\n");

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

        System.out.println("----------------------------------------------------------");
        System.out.println("\n");
        for(Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                double lucro = service.calcularLucroPotencial(m, precoAtual);
                double valorNoAtivo = m.getSaldo() * precoAtual;
                double porcentagem = (lucro / (m.getSaldo() * m.getPrecoMedio())) * 100;
                
                pnlTotalGeral += lucro;
                valorTotalPatrimonio += valorNoAtivo;

                String indicador = (lucro >= 0) ? "▲" : "▼";

                System.out.printf("          %s %s: $ %.2f | PNL: $ %.2f (%.2f%%)\n",indicador, m.getTicker(), precoAtual, lucro, porcentagem);
                System.out.print("\n");
                System.out.print("          ***********************************************");
                System.out.println("\n");
            }
        }
        System.out.println("----------------------------------------------------------");
        
        String statusGeral = (pnlTotalGeral >= 0) ? "LUCRO" : "PREJUÍZO";
        String sinalGeral = (pnlTotalGeral >= 0) ? "[+]" : "[-]";

        double cotacaoDolarResumo = http.buscarCotacaoDolar();
        
        System.out.printf("PATRIMÔNIO TOTAL: $ %.2f (R$ %.2f)\n", valorTotalPatrimonio, (valorTotalPatrimonio * cotacaoDolarResumo));
        System.out.printf("PNL GERAL: %s $ %.2f (R$ .2f) (%s)\n", sinalGeral, pnlTotalGeral,(pnlTotalGeral * cotacaoDolarResumo), statusGeral);
        System.out.println("----------------------------------------------------------");

        System.out.println("\nPressione Enter para fechar");
        try{ 
            System.in.read(); 
        }catch(Exception e){}
    }
}