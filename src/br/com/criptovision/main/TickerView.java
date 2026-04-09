package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.dto.ResumoAtivoDTO;
import java.util.List;

public class TickerView {

    public static void main(String[] args){

        System.out.println("\n==================================================================================");
        System.out.println("                      ---   RESUMO  DA  CARTEIRA   ---                    ");
        System.out.println("==================================================================================");

        CarteiraService carteiraService = new CarteiraService();
        HttpService httpTradutor = new HttpService();
        Carteira minhaCarteira = new Carteira();

        List<Transacao> historico = carteiraService.carregarHistoricoDeTransacoes();
        carteiraService.reconstruirCarteira(minhaCarteira, historico);

        double cotacaoDolar = httpTradutor.buscarCotacaoDolar();
        ResumoCarteiraDTO resumo = carteiraService.gerarResumoCompleto(minhaCarteira, httpTradutor);

        if(resumo.getValorTotalCarteira() == 0){
            System.out.println("Erro: Não foi possível obter preços ou a carteira está vazia.");
        }else{
            System.out.printf("  Cotação do Dólar: R$ %.2f\n", cotacaoDolar);
            System.out.println("---------------------------------------------------------------------------------------------");
            System.out.println("  ATIVO      QUANTIDADE          PREÇO ATUAL      VALOR (USD)      PNL (%)      VAR 24H");
            System.out.println("  ---------------------------------------------------------------------------------------------");

            for(ResumoAtivoDTO ativo : resumo.getAtivos()){
                String icone24h = (ativo.getVariacao24h() >= 0) ? "[+]" : "[-]";
                
                System.out.printf("  %-10s %-18.8f $ %-12.2f $ %-14.2f %-11.2f%% %s %+.2f%%\n",
                    ativo.getTicker(), ativo.getSaldo(), ativo.getPrecoAtual(), ativo.getValorTotalUSD(), ativo.getPorcentagemPNL(), icone24h, ativo.getVariacao24h());
            }

            String statusGeral = (resumo.getPnlGeral() >= 0) ? "LUCRO" : "PREJUÍZO";
            String sinalGeral = (resumo.getPnlGeral() >= 0) ? "[+]" : "[-]";

            System.out.println("---------------------------------------------------------------------------------------------");
            System.out.println("\n  >>> RESUMO GERAL");
            System.out.printf("  PATRIMÔNIO TOTAL : $ %.2f  |  (R$ %.2f)\n", 
                    resumo.getValorTotalCarteira(), (resumo.getValorTotalCarteira() * cotacaoDolar));
            
            String iconeCarteira = (resumo.getVariacao24hCarteira() >= 0) ? "[+]" : "[-]";
            System.out.printf("  VARIAÇÃO 24H     : %s %+.2f%%\n", iconeCarteira, resumo.getVariacao24hCarteira());

            System.out.printf("  PNL GERAL        : %s $ %.2f  |  (R$ %.2f) [%s]\n", 
                    sinalGeral, resumo.getPnlGeral(), (resumo.getPnlGeral() * cotacaoDolar), statusGeral);
            System.out.println("==================================================================================");
        }

        System.out.println("\nPressione Enter para sair...");
        try{ System.in.read(); }catch(Exception e){}
    }
}