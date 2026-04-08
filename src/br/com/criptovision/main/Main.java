package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.service.RelatorioService;
import br.com.criptovision.repository.TransacaoDAO;
import br.com.criptovision.repository.TransacaoDAOMySQL;
import br.com.criptovision.util.InputUtils;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // criando os objetos que comandam a logica e os dados
        CarteiraService carteira = new CarteiraService();
        TransacaoDAO repositorio = new TransacaoDAOMySQL();

        RelatorioService relatorioService = new RelatorioService();

        //repositorio.realizarBackup(); // retirando pois o backup nao é de responsabilidade da aplicação, agr que temos o banco de dados isso nao faz mais sentido
        
        // reconstroi e reprocessa todas as operações para gerar o saldo atual
        List<Transacao> historico = repositorio.lerTudo();
        Carteira minhaCarteira = new Carteira();
        HttpService httpTradutor = new HttpService();

        for(Transacao tAntiga : historico){
            String tickerOriginal = tAntiga.getTicker().toUpperCase();
            // obterMoeda garante que não teremos objetos de moedas duplicados, exemplo : dois objetos para btc
            Moeda m = minhaCarteira.obterMoeda(tickerOriginal, tickerOriginal);
            carteira.processarTransacao(m, tAntiga);
        }

        // o lopp while mantém o programa rodando até voce escolher "10" para sair
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
                    // NOVA COMPRA
                    case 1:
                        try {
                            String ticker = InputUtils.lerString("Qual o Ticker da moeda (ex: BTC)? ");
                            
                            HttpService httpService = new HttpService();
                            // antes de aceitar, verifica se a binance reconhece esse ticker
                            System.out.println("Validando '" + ticker + "' na Binance...");
            
                            if(!httpService.validarTicker(ticker)){
                                System.out.println("ERRO: Ativo não encontrado na Binance (par " + ticker + "USDT não existe).");
                                break; 
                            }

                            Moeda moedaSelecionada = minhaCarteira.obterMoeda(ticker, ticker); 

                            double qtd = InputUtils.lerDouble("Quantidade comprada: ");

                            double preco = InputUtils.lerDouble("Preço unitário pago: ");
                            
                            // cria a transacao, processa o preço medio e salva no historico e no csv
                            Transacao t = new Transacao(ticker, qtd, preco, "COMPRA");
                            carteira.processarTransacao(moedaSelecionada, t);
                            historico.add(t);
                            repositorio.salvar(t);
                            System.out.println("Compra registrada com sucesso!");
                        }catch(Exception e){
                            System.out.println("Erro na compra: " + e.getMessage());
                        }
                        break;

                    case 2:
                        try{
                            String tickerVenda = InputUtils.lerString("Qual o Ticker da moeda para venda? ");

                            // na binance, o identificador é o ticker
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
                            
                            historico.add(tVenda); 
                            repositorio.salvar(tVenda);

                            System.out.println("\nVenda registrada com sucesso no banco de dados!");

                        }catch(br.com.criptovision.exception.SaldoInsuficienteException e){
                            System.out.println("\nERRO DE OPERAÇÃO: " + e.getMessage());

                        }catch(IllegalArgumentException e){
                            System.out.println("\nERRO DE VALOR: " + e.getMessage());

                        }catch(Exception e){
                            System.out.println("\nERRO GERAL: " + e.getMessage());
                        }
                        break;
                        
                    // RESUMO DA CARTEIRA REATORADO
                    case 3: 
                        System.out.println("\n--- RESUMO DA CARTEIRA ---");
                        HttpService serviceHttp = new HttpService();
                        System.out.println("conectando a binance e fazando os calculos...");
                        
                        double cotacaoDolar = serviceHttp.buscarCotacaoDolar();

                        br.com.criptovision.dto.ResumoCarteiraDTO resumo = carteira.gerarResumoCompleto(minhaCarteira, serviceHttp);

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

                            for(br.com.criptovision.dto.ResumoAtivoDTO ativo : resumo.getAtivos()){
                                double percentagem = (ativo.getValorTotalUSD() / resumo.getValorTotalCarteira()) * 100;
                                String icone24h = (ativo.getVariacao24h() >= 0) ? "[+]" : "[-]";

                                System.out.printf("   %s: $ %.2f (%.1f%%) | VAR 24H: %s %+.2f%%\n", ativo.getTicker(), ativo.getValorTotalUSD(), percentagem, icone24h, ativo.getVariacao24h());
                            }
                        }
                        System.out.println("---------------------------------------");
                        break;

                    // Ver Saldo e Preço Médio
                    case 4:
                        System.out.println("\n--- MINHAS MOEDAS ---");
                        for(Moeda m : minhaCarteira.getMoedas().values()){ // o ".values()" pega todos os objetos Moedas que estao guardados
                            if(m.getSaldo() > 0){
                                System.out.printf("Ativo: %s | Saldo: %.8f | Preço Médio: $ %.2f\n",m.getTicker(), m.getSaldo(), m.getPrecoMedio());
                            }
                        }
                        break;

                    // Simular venda futura
                    // Simular venda futura
                    case 5:
                        System.out.println("\n--- SIMULADOR DE VENDA FUTURA ---");
                        String tickerSim = InputUtils.lerString("Digite o Ticker da moeda que você possui (ex: BTC): ");

                        // primeiro verifica se a moeda existe na carteira
                        if(minhaCarteira.getMoedas().containsKey(tickerSim)){
                            Moeda mSim = minhaCarteira.getMoedas().get(tickerSim);
                            
                            if(mSim.getSaldo() > 0){
                                double precoFicticio = InputUtils.lerDouble("Digite o preço fictício de venda ($): ");

                                double precoAtualMercado = httpTradutor.consultarPrecoPorTicker(tickerSim);
                                double precoDolar = httpTradutor.buscarCotacaoDolar();

                                br.com.criptovision.dto.SimulacaoVendaDTO simulacao = carteira.simularVendaFutura(mSim, precoFicticio, precoAtualMercado);

                                System.out.println("\n------------------------------------------------------");
                                System.out.printf("  Simulação para %s:\n \n", tickerSim);
                                System.out.printf("  Saldo que você possui: %.8f\n", mSim.getSaldo());
                                
                                System.out.printf("  Valor que você possui em " + tickerSim + ": $ %.2f (R$ %.2f)\n \n", simulacao.getValorTotalAtual(), simulacao.getValorTotalAtual() * precoDolar);
                                System.out.printf("******************************************************* \n");
                                System.out.printf("  Se vender a: $ %.2f\n \n", precoFicticio);
                                System.out.printf("  LUCRO ESTIMADO: $ %.2f (R$ %.2f) -> [%.2f%%]\n \n", simulacao.getLucroEstimado(), simulacao.getLucroEstimado() * precoDolar, simulacao.getPorcentagemLucro());
                                System.out.printf("  SALDO TOTAL ESTIMADO: $ %.2f (R$ %.2f)\n", simulacao.getValorTotalFicticio(), simulacao.getValorTotalFicticio() * precoDolar);
                                System.out.println("------------------------------------------------------");
                            }else{
                                System.out.println("Você não possui saldo desta moeda para simular.");
                            }
                        }else{
                            System.out.println("Moeda não encontrada na sua carteira.");
                        }
                        break;

                    // Gerar Relatório
                    case 6:
                        System.out.println("\n Gerando relatório...");
                        relatorioService.gerarRelatorio(new ArrayList<>(minhaCarteira.getMoedas().values()));
                        break;
                    
                    // Ver lucro total realizado
                    case 7:
                        System.out.println("\n--- total de lucros realizados até hoje ---");
                        br.com.criptovision.repository.LucroDAO repoLucro = new br.com.criptovision.repository.LucroDAOMySQL();
                        double totalRealizado = repoLucro.lerLucroTotal();
                        
                        if(totalRealizado == 0){
                            System.out.println("Voce ainda nao realizou nenhum lucro");
                        }else{
                            String status = (totalRealizado >= 0) ? "POSITIVO" : "NEGATIVO";
                            System.out.printf("TOTAL ACUMULADO EM VENDAS: $ %.2f (%s)\n", totalRealizado, status);
                            System.out.println("valor que representa o lucro real que já realizou ao vender seus ativos.");
                        }
                        System.out.println("-----------------------------------------------------");
                        break;

                    // Consultar preço em tempo real
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

                    // Simular aporte (DCA)
                    case 9:
                        System.out.println("\n--- SIMULADOR DE APORTE (DCA) ---");
                        String tickerDCA = InputUtils.lerString("Digite o Ticker da moeda para simular (ex: BTC): ");

                        // busca a moeda na carteira
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

                            //novo metodo que ta em carteiraService:
                            carteira.simularDCA(moedaDCA, valorAporte, precoMercado);
                        }else{
                            System.out.println("Erro ao buscar preço. Tente novamente.");
                        }
                        break;

                    // Sair do sistema
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