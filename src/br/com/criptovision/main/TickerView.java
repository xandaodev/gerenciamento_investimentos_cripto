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

        System.out.println("\n==================================================================================");
        System.out.println("                      ---   RESUMO  DA  CARTEIRA   ---                    ");
        System.out.println("==================================================================================");

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
        double cotacaoDolarResumo = http.buscarCotacaoDolar();

        System.out.printf("  Cotação do Dólar: R$ %.2f\n", cotacaoDolarResumo);
        System.out.println("----------------------------------------------------------------------------------");
        
        System.out.println("  ATIVO      QUANTIDADE          PREÇO ATUAL      VALOR (USD)      PNL (%)");
        System.out.println("  ----------------------------------------------------------------------------------");

        for(Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo() > 0){
                double precoAtual = http.buscarPrecoAtual(m);
                double lucro = service.calcularLucroPotencial(m, precoAtual);
                double valorNoAtivo = m.getSaldo() * precoAtual;
                double porcentagem = (lucro / (m.getSaldo() * m.getPrecoMedio())) * 100;
                
                pnlTotalGeral += lucro;
                valorTotalPatrimonio += valorNoAtivo;

                System.out.printf("  %-10s %-18.8f $ %-12.2f $ %-14.2f %+.2f%%\n",m.getTicker(), m.getSaldo(), precoAtual, valorNoAtivo, porcentagem);
                System.out.print("\n");
            }
        }
        
        String statusGeral = (pnlTotalGeral >= 0) ? "LUCRO" : "PREJUÍZO";
        String sinalGeral = (pnlTotalGeral >= 0) ? "[+]" : "[-]";

        System.out.println("----------------------------------------------------------------------------------");
        System.out.println("\n  >>> RESUMO GERAL");
        System.out.printf("  PATRIMÔNIO TOTAL : $ %.2f  |  (R$ %.2f)\n", 
                valorTotalPatrimonio, (valorTotalPatrimonio * cotacaoDolarResumo));
        System.out.printf("  PNL GERAL        : %s $ %.2f  |  (R$ %.2f) [%s]\n", 
                sinalGeral, pnlTotalGeral, (pnlTotalGeral * cotacaoDolarResumo), statusGeral);
        System.out.println("==================================================================================");

        System.out.println("\nPressione Enter para sair...");
        try{ System.in.read(); }catch(Exception e){}
    }
}