package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import java.util.List;
import br.com.criptovision.repository.TransacaoDAO;
import br.com.criptovision.repository.TransacaoDAOMySQL;

public class TickerView {

    public static void main(String[] args){

        System.out.println("\n==================================================================================");
        System.out.println("                      ---   RESUMO  DA  CARTEIRA   ---                    ");
        System.out.println("==================================================================================");

        CarteiraService service = new CarteiraService();
        TransacaoDAO repo = new TransacaoDAOMySQL();
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
        double valorPatrimonioOntem = 0; // calcula a variação da carteira toda
        double cotacaoDolarResumo = http.buscarCotacaoDolar();

        System.out.printf("  Cotação do Dólar: R$ %.2f\n", cotacaoDolarResumo);
        System.out.println("---------------------------------------------------------------------------------------------");
        
        System.out.println("  ATIVO      QUANTIDADE          PREÇO ATUAL      VALOR (USD)      PNL (%)      VAR 24H");
        System.out.println("  ---------------------------------------------------------------------------------------------");

        for(Moeda m : carteira.getMoedas().values()){
            if(m.getSaldo() > 0){
                double[] dadosApi = http.buscarPrecoEVariacao(m);
                double precoAtual = dadosApi[0];
                double variacao24h = dadosApi[1];

                double lucro = service.calcularLucroPotencial(m, precoAtual);
                double valorNoAtivo = m.getSaldo() * precoAtual;
                double porcentagem = (lucro / (m.getSaldo() * m.getPrecoMedio())) * 100;
                
                pnlTotalGeral += lucro;
                valorTotalPatrimonio += valorNoAtivo;
                
                // regra de 3 para descobrir quanto essa moeda valia ontem na sua carteira
                valorPatrimonioOntem += valorNoAtivo / (1 + (variacao24h / 100));

                String icone24h = (variacao24h >= 0) ? "[+]" : "[-]";

                System.out.printf("  %-10s %-18.8f $ %-12.2f $ %-14.2f %-11.2f%% %s %+.2f%%\n",m.getTicker(), m.getSaldo(), precoAtual, valorNoAtivo, porcentagem, icone24h, variacao24h);
                System.out.print("\n");
            }
        }
        
        String statusGeral = (pnlTotalGeral >= 0) ? "LUCRO" : "PREJUÍZO";
        String sinalGeral = (pnlTotalGeral >= 0) ? "[+]" : "[-]";

        System.out.println("---------------------------------------------------------------------------------------------");
        System.out.println("\n  >>> RESUMO GERAL");
        System.out.printf("  PATRIMÔNIO TOTAL : $ %.2f  |  (R$ %.2f)\n", 
                valorTotalPatrimonio, (valorTotalPatrimonio * cotacaoDolarResumo));
        
        double variacaoTotalCarteira = (valorPatrimonioOntem > 0) ? ((valorTotalPatrimonio - valorPatrimonioOntem) / valorPatrimonioOntem) * 100 : 0;
        
        String iconeCarteira = (variacaoTotalCarteira >= 0) ? "[+]" : "[-]";
        System.out.printf("  VARIAÇÃO 24H     : %s %+.2f%%\n", iconeCarteira, variacaoTotalCarteira);

        System.out.printf("  PNL GERAL        : %s $ %.2f  |  (R$ %.2f) [%s]\n", 
                sinalGeral, pnlTotalGeral, (pnlTotalGeral * cotacaoDolarResumo), statusGeral);
        System.out.println("==================================================================================");

        System.out.println("\nPressione Enter para sair...");
        try{ System.in.read(); }catch(Exception e){}
    }
}