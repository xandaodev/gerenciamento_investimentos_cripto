package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.service.RelatorioService;
import br.com.criptovision.exception.*;
import br.com.criptovision.util.InputUtils;
import java.util.ArrayList;
import java.util.List;

import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.dto.ResumoAtivoDTO;
import br.com.criptovision.dto.SimulacaoVendaDTO;
import br.com.criptovision.dto.SimulacaoDCADTO;



public class Main {
    public static void main(String[] args) {
        
        CarteiraService carteira = new CarteiraService();
        RelatorioService relatorioService = new RelatorioService();
        HttpService httpTradutor = new HttpService();
        Carteira minhaCarteira = new Carteira();

        List<Transacao> historico = carteira.carregarHistoricoDeTransacoes();
        carteira.reconstruirCarteira(minhaCarteira, historico);

        int opcao = 0;
        while (opcao != 10){
            try{
                System.out.println("\n--- GERENCIAMENTO DE INVESTIMENTOS CRIPTO ---");
                System.out.println("1. Nova compra");
                System.out.println("2. Nova venda");
                System.out.println("3. Gerar Resumo da Carteira");
                System.out.println("4. Ver Saldo e Preço Médio");
                System.out.println("5. Simular venda futura");
                System.out.println("6. Gerar Relatório");
                System.out.println("7. Ver lucro total realizado");
                System.out.println("8. Consultar preço em tempo real");
                System.out.println("9. Simular aporte (DCA)");
                System.out.println("10. Sair do sistema");
                
                opcao = InputUtils.lerInt("Escolha uma opção: ");

                switch (opcao){
                    case 1:
                        try {
                            String ticker = InputUtils.lerString("Qual o Ticker da moeda (ex: BTC)? ");
                            
                            System.out.println("Validando '" + ticker + "' na Binance...");
            
                            if(!httpTradutor.validarTicker(ticker)){
                                System.out.println("ERRO: Ativo não encontrado na Binance (par " + ticker + "USDT não existe).");
                                break; 
                            }

                            Moeda moedaSelecionada = minhaCarteira.obterMoeda(ticker, ticker); 
                            double qtd = InputUtils.lerDouble("Quantidade comprada: ");
                            double preco = InputUtils.lerDouble("Preço unitário pago: ");
                            
                            Transacao t = new Transacao(ticker, qtd, preco, "COMPRA");
                            
                            carteira.processarTransacao(moedaSelecionada, t);
                            
                            System.out.println("Compra registrada com sucesso no banco de dados!");
                        }catch(Exception e){
                            System.out.println("Erro na compra: " + e.getMessage());
                        }
                        break;

                    case 2:
                        try{
                            String tickerVenda = InputUtils.lerString("Qual o Ticker da moeda para venda? ");
                            Moeda moedaVenda = minhaCarteira.obterMoeda(tickerVenda, tickerVenda);

                            if(moedaVenda.getSaldo() <= 0){
                                System.out.println("Você não tem saldo de " + tickerVenda + " para vender.");
                                break;
                            }

                            System.out.printf("Saldo disponível: %.8f\n", moedaVenda.getSaldo());
                            double qtdVenda = InputUtils.lerDouble("Quantidade a vender: ");
                            double precoVenda = InputUtils.lerDouble("Preço unitário de venda: ");
                            
                            Transacao tVenda = new Transacao(tickerVenda, qtdVenda, precoVenda, "VENDA");
                            
                            carteira.processarTransacao(moedaVenda, tVenda);

                            System.out.println("\nVenda registrada com sucesso no banco de dados!");

                        }catch(SaldoInsuficienteException e){
                            System.out.println("\nERRO DE OPERAÇÃO: " + e.getMessage());
                        }catch(IllegalArgumentException e){
                            System.out.println("\nERRO DE VALOR: " + e.getMessage());
                        }catch(Exception e){
                            System.out.println("\nERRO GERAL: " + e.getMessage());
                        }
                        break;
                        
                    case 3: 
                        System.out.println("\n--- RESUMO DA CARTEIRA ---");
                        System.out.println("Conectando à Binance e fazendo os cálculos...");
                        
                        double cotacaoDolar = httpTradutor.buscarCotacaoDolar();
                        ResumoCarteiraDTO resumo = carteira.gerarResumoCompleto(minhaCarteira, httpTradutor);

                        if(resumo.getValorTotalCarteira() == 0){
                            System.out.println("Erro: Não foi possível obter preços ou a carteira está vazia.");
                        }else{
                            System.out.printf("VALOR TOTAL DA CARTEIRA: $ %.2f (R$ %.2f)\n", resumo.getValorTotalCarteira(), (resumo.getValorTotalCarteira() * cotacaoDolar));
                            
                            String iconeCarteira = (resumo.getVariacao24hCarteira() >= 0) ? "[+]" : "[-]";
                            System.out.printf("VARIAÇÃO 24H DA CARTEIRA: %s %+.2f%%\n", iconeCarteira, resumo.getVariacao24hCarteira());

                            String status = (resumo.getPnlGeral() >= 0) ? "LUCRO" : "PREJUÍZO";
                            System.out.printf("PNL GERAL DA CARTEIRA: $ %.2f  (R$ %.2f) (%s)\n", resumo.getPnlGeral(),(resumo.getPnlGeral() * cotacaoDolar), status);
                            System.out.println("---------------------------------------");
                            System.out.println("Distribuição por Ativo:");

                            for(ResumoAtivoDTO ativo : resumo.getAtivos()){
                                double percentagem = (ativo.getValorTotalUSD() / resumo.getValorTotalCarteira()) * 100;
                                String icone24h = (ativo.getVariacao24h() >= 0) ? "[+]" : "[-]";

                                System.out.printf("   %s: $ %.2f (%.1f%%) | VAR 24H: %s %+.2f%%\n", ativo.getTicker(), ativo.getValorTotalUSD(), percentagem, icone24h, ativo.getVariacao24h());
                            }
                        }
                        System.out.println("---------------------------------------");
                        break;

                    case 4:
                        System.out.println("\n--- MINHAS MOEDAS ---");
                        for(Moeda m : minhaCarteira.getMoedas().values()){ 
                            if(m.getSaldo() > 0){
                                System.out.printf("Ativo: %s | Saldo: %.8f | Preço Médio: $ %.2f\n",m.getTicker(), m.getSaldo(), m.getPrecoMedio());
                            }
                        }
                        break;

                    case 5:
                        System.out.println("\n--- SIMULADOR DE VENDA FUTURA ---");
                        String tickerSim = InputUtils.lerString("Digite o Ticker da moeda que você possui (ex: BTC): ");

                        if(minhaCarteira.getMoedas().containsKey(tickerSim)){
                            Moeda mSim = minhaCarteira.getMoedas().get(tickerSim);
                            
                            if(mSim.getSaldo() > 0){
                                double precoFicticio = InputUtils.lerDouble("Digite o preço fictício de venda ($): ");
                                double precoAtualMercado = httpTradutor.consultarPrecoPorTicker(tickerSim);
                                double precoDolarCotacao = httpTradutor.buscarCotacaoDolar();

                                SimulacaoVendaDTO simulacao = carteira.simularVendaFutura(mSim, precoFicticio, precoAtualMercado);

                                System.out.println("\n------------------------------------------------------");
                                System.out.printf("  Simulação para %s:\n \n", tickerSim);
                                System.out.printf("  Saldo que você possui: %.8f\n", mSim.getSaldo());
                                
                                System.out.printf("  Valor que você possui em " + tickerSim + ": $ %.2f (R$ %.2f)\n \n", simulacao.getValorTotalAtual(), simulacao.getValorTotalAtual() * precoDolarCotacao);
                                System.out.printf("******************************************************* \n");
                                System.out.printf("  Se vender a: $ %.2f\n \n", precoFicticio);
                                System.out.printf("  LUCRO ESTIMADO: $ %.2f (R$ %.2f) -> [%.2f%%]\n \n", simulacao.getLucroEstimado(), simulacao.getLucroEstimado() * precoDolarCotacao, simulacao.getPorcentagemLucro());
                                System.out.printf("  SALDO TOTAL ESTIMADO: $ %.2f (R$ %.2f)\n", simulacao.getValorTotalFicticio(), simulacao.getValorTotalFicticio() * precoDolarCotacao);
                                System.out.println("------------------------------------------------------");
                            }else{
                                System.out.println("Você não possui saldo desta moeda para simular.");
                            }
                        }else{
                            System.out.println("Moeda não encontrada na sua carteira.");
                        }
                        break;

                    case 6:
                        System.out.println("\n Gerando relatório...");
                        relatorioService.gerarRelatorio(new ArrayList<>(minhaCarteira.getMoedas().values()));
                        break;
                    
                    case 7:
                        System.out.println("\n--- total de lucros realizados até hoje ---");
                        double totalRealizado = carteira.obterLucroTotalRealizado();
                        
                        if(totalRealizado == 0){
                            System.out.println("Voce ainda nao realizou nenhum lucro");
                        }else{
                            String status = (totalRealizado >= 0) ? "POSITIVO" : "NEGATIVO";
                            System.out.printf("TOTAL ACUMULADO EM VENDAS: $ %.2f (%s)\n", totalRealizado, status);
                            System.out.println("valor que representa o lucro real que já realizou ao vender seus ativos.");
                        }
                        System.out.println("-----------------------------------------------------");
                        break;

                    case 8:
                        String tickerBusca = InputUtils.lerString("\nQual o Ticker da moeda para consulta rápida (ex: BTC, SOL, LNK)? ");
                        System.out.println("Consultando Binance...");
                        double precoBuscado = httpTradutor.consultarPrecoPorTicker(tickerBusca);
                        if(precoBuscado > 0){
                            System.out.println("\n=======================================");
                            System.out.printf("COTAÇÃO ATUAL (%s): $ %.2f\n", tickerBusca, precoBuscado);
                            System.out.println("=======================================");
                        }else{
                            System.out.println("\nNão foi possível obter o preço.");
                        }
                        break;

                    case 9:
                        System.out.println("\n--- SIMULADOR DE APORTE (DCA) ---");
                        String tickerDCA = InputUtils.lerString("Digite o Ticker da moeda para simular (ex: BTC): ");

                        Moeda moedaDCA = minhaCarteira.getMoedas().get(tickerDCA);
                        
                        if(moedaDCA == null || moedaDCA.getSaldo() <= 0){
                            System.out.println("Você ainda não tem saldo de " + tickerDCA + ". O primeiro aporte definirá seu Preço Médio inicial.");
                            break;
                        }

                        System.out.println("Consultando preço atual...");
                        double precoMercado = httpTradutor.consultarPrecoPorTicker(tickerDCA);

                        if (precoMercado > 0){
                            System.out.printf("Preço atual de mercado: $ %.2f\n", precoMercado);
                            double valorAporte = InputUtils.lerDouble("Quanto pretende investir agora (USD)? ");

                            SimulacaoDCADTO simulacaoDCA = carteira.simularDCA(moedaDCA, valorAporte, precoMercado);

                            System.out.println("\n================ RESULTADO DA SIMULAÇÃO ================");
                            System.out.printf(" Se você investir: $ %.2f com %s a $ %.2f\n", valorAporte, tickerDCA, precoMercado);
                            System.out.printf(" Você irá comprar:    %.8f %s\n", simulacaoDCA.getQtdComprada(), tickerDCA);
                            System.out.println("--------------------------------------------------------");
                            System.out.printf(" Saldo:   %.8f  ->  Novo Saldo: %.8f\n", simulacaoDCA.getSaldoAtual(), simulacaoDCA.getNovoSaldoTotal());
                            System.out.printf(" Preço Médio:      $ %.2f      ->  Novo Preço Médio:    $ %.2f\n", simulacaoDCA.getPmAtual(), simulacaoDCA.getNovoPM());

                            if (simulacaoDCA.getNovoPM() < simulacaoDCA.getPmAtual()){
                                System.out.printf(" Seu Preço Médio cairia %.2f%%!\n", Math.abs(simulacaoDCA.getDiferencaPM()));
                            }else{
                                System.out.printf(" Seu Preço Médio subiria %.2f%%.\n", simulacaoDCA.getDiferencaPM());
                            }
                            
                            if(simulacaoDCA.getValorizacaoNecessaria() > 0){
                                System.out.printf(" A moeda precisa subir %.2f%% para atingir o novo Preço Médio.\n", simulacaoDCA.getValorizacaoNecessaria());
                            }
                            System.out.println("===================================================================");

                        }else{
                            System.out.println("Erro ao buscar preço. Tente novamente.");
                        }
                        break;

                    case 10:
                        System.out.println("Saindo...");
                        break;

                    default:
                        System.out.println("Opção inválida!");
                }
            }catch(Exception e){ 
                System.out.println(" Ocorreu um erro inesperado: " + e.getMessage());
            }
        }
    }
}