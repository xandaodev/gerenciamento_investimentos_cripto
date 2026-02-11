package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.repository.TransacaoRepository;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);
        CarteiraService carteira = new CarteiraService();
        TransacaoRepository repositorio = new TransacaoRepository();

        //chamando funcao pra realizar backup
        repositorio.realizarBackup();
        
        List<Transacao> historico = repositorio.lerTudo();
        Carteira minhaCarteira = new Carteira();
        HttpService httpTradutor = new HttpService();

        for(Transacao tAntiga : historico){
            String tickerOriginal = tAntiga.getTicker().toUpperCase();
            Moeda m = minhaCarteira.obterMoeda(tickerOriginal, tickerOriginal);
            carteira.processarTransacao(m, tAntiga);
        }

        int opcao = 0;
        while (opcao != 10){
            try{
                System.out.println("\n--- GERENCIAMENTO DE INVESTIMENTOS CRIPTO ---");
                System.out.println("1. Nova compra");
                System.out.println("2. Nova venda");
                System.out.println("3. Gerar Dashboard de Patrimônio");
                System.out.println("4. Ver Saldo e Preço Médio");
                System.out.println("5. Simular lucro");
                System.out.println("6. Gerar Relatório");
                System.out.println("7. Ver lucro total realizado");
                System.out.println("8. Consultar preço em tempo real");
                System.out.println("9. Simular aporte (DCA)");
                System.out.println("10. Sair do sistema");
                System.out.print("Escolha uma opção: ");
                
                opcao = leitor.nextInt();

                switch (opcao){
                    case 1:
                        try {
                            System.out.print("Qual o Ticker da moeda (ex: BTC)? ");
                            String ticker = leitor.next().toUpperCase();
                            leitor.nextLine(); 
                            
                            HttpService httpService = new HttpService();
                            
                            System.out.println("Validando '" + ticker + "' na Binance...");
            
                            if (!httpService.validarTicker(ticker)) {
                                System.out.println("ERRO: Ativo não encontrado na Binance (par " + ticker + "USDT não existe).");
                                break; 
                            }

                            Moeda moedaSelecionada = minhaCarteira.obterMoeda(ticker, ticker); 

                            System.out.print("Quantidade comprada: ");
                            double qtd = Double.parseDouble(leitor.nextLine().replace(",", "."));

                            System.out.print("Preço unitário pago: ");
                            double preco = Double.parseDouble(leitor.nextLine().replace(",", "."));
                            
                            Transacao t = new Transacao(ticker, qtd, preco, "COMPRA");
                            carteira.processarTransacao(moedaSelecionada, t);
                            historico.add(t);
                            repositorio.salvar(t);
                            System.out.println("Compra registrada com sucesso!");
                        } catch (Exception e) {
                            System.out.println("Erro na compra: " + e.getMessage());
                        }
                        break;

                    case 2:
                        try {
                            System.out.print("Qual o Ticker da moeda para venda? ");
                            String tickerVenda = leitor.next().toUpperCase();
                            leitor.nextLine();

                            // na binance, o identificador é o ticker
                            Moeda moedaVenda = minhaCarteira.obterMoeda(tickerVenda, tickerVenda);

                            if(moedaVenda.getSaldo() <= 0){
                                System.out.println("Não tem saldo de " + tickerVenda + " para vender.");
                                break;
                            }

                            System.out.printf("Saldo disponível: %.8f\n", moedaVenda.getSaldo());
                            System.out.print("Quantidade a vender: ");
                            double qtdVenda = Double.parseDouble(leitor.nextLine().replace(",", "."));
                            
                            if (qtdVenda > moedaVenda.getSaldo()) {
                                System.out.println("Erro: Saldo insuficiente!");
                                break; 
                            }

                            System.out.print("Preço unitário de venda: ");
                            double precoVenda = Double.parseDouble(leitor.nextLine().replace(",", "."));
                            
                            Transacao tVenda = new Transacao(tickerVenda, qtdVenda, precoVenda, "VENDA");
                            carteira.processarTransacao(moedaVenda, tVenda);
                            historico.add(tVenda); 
                            repositorio.salvar(tVenda);
                            
                            System.out.println("Venda registrada!");
                        } catch(Exception e) {
                            System.out.println("Erro na venda: " + e.getMessage());
                        }
                        break;
                        
                    case 3:
                        System.out.println("\n--- DASHBOARD DE PATRIMONIO ---");
                        HttpService serviceHttp = new HttpService();
                        
                        double totalCalculado = 0;
                        double pnlTotalGeral = 0;
                        java.util.Map<String, Double> valoresPorMoeda = new java.util.HashMap<>();

                        System.out.println("Atualizando preços na Binance...");
                        
                        double cotacaoDolar = serviceHttp.buscarCotacaoDolar();

                        for(Moeda m : minhaCarteira.getMoedas().values()){
                            if (m.getSaldo() > 0) {
                                double preco = serviceHttp.buscarPrecoAtual(m); 
                                double valorNoAtivo = m.getSaldo() * preco;
                                double lucroDestaMoeda = carteira.calcularLucroPotencial(m, preco);
                                pnlTotalGeral += lucroDestaMoeda;
                                valoresPorMoeda.put(m.getTicker(), valorNoAtivo);
                                totalCalculado += valorNoAtivo;
                            }
                        }

                        if(totalCalculado == 0){
                            System.out.println("Erro: Não foi possível obter preços ou carteira vazia.");
                        }else{
                            System.out.printf("VALOR TOTAL DO PATRIMONIO: $ %.2f (R$ %.2f)\n", totalCalculado, (totalCalculado * cotacaoDolar));
                            String status = (pnlTotalGeral >= 0) ? "LUCRO" : "PREJUIZO";
                            System.out.printf("PNL GERAL DA CARTEIRA: $ %.2f  (R$ %.2f) (%s)\n", pnlTotalGeral,(pnlTotalGeral * cotacaoDolar), status);
                            System.out.println("---------------------------------------");
                            System.out.println("Distribuição por Ativo:");

                            for(Moeda m : minhaCarteira.getMoedas().values()){
                                if(m.getSaldo() > 0){
                                    double valorAtivo = valoresPorMoeda.get(m.getTicker());
                                    double percentagem = (valorAtivo / totalCalculado) * 100;

                                    System.out.printf("   %s: $ %.2f (%.1f%%)\n", m.getTicker(), valorAtivo, percentagem);
                                }
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
                        System.out.print("Digite o Ticker da moeda que você possui (ex: BTC): ");
                        String tickerSim = leitor.next().toUpperCase();
                        leitor.nextLine();

                        if(minhaCarteira.getMoedas().containsKey(tickerSim)){
                            Moeda mSim = minhaCarteira.getMoedas().get(tickerSim);
                            
                            if(mSim.getSaldo() > 0){
                                System.out.print("Digite o preço fictício de venda ($): ");
                                double precoFicticio = Double.parseDouble(leitor.nextLine().replace(",", "."));

                                double lucroSimulado = carteira.calcularLucroPotencial(mSim, precoFicticio);
                                double porcSimulada = (lucroSimulado / (mSim.getSaldo() * mSim.getPrecoMedio())) * 100;

                                System.out.println("\n---------------------------------------");
                                System.out.printf("Simulação para %s:\n", tickerSim);
                                System.out.printf("  Saldo que você possui: %.8f\n", mSim.getSaldo());
                                System.out.printf("  Se vender a: $ %.2f\n", precoFicticio);
                                System.out.printf("  RESULTADO ESTIMADO: $ %.2f (%.2f%%)\n", lucroSimulado, porcSimulada);
                                System.out.println("---------------------------------------");
                            }else{
                                System.out.println("Você não possui saldo desta moeda para simular.");
                            }
                        }else{
                            System.out.println("Moeda não encontrada na sua carteira.");
                        }
                        break;

                    case 6:
                        System.out.println("Gerando relatório...");
                        repositorio.gerarRelatorio(new ArrayList<>(minhaCarteira.getMoedas().values()));
                        break;
                    
                    case 7:
                        System.out.println("\n--- total de lucros realizados até hoje ---");
                        br.com.criptovision.repository.LucroRepository repoLucro = new br.com.criptovision.repository.LucroRepository();
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

                    case 8:
                        System.out.print("\nQual o Ticker da moeda para consulta rápida (ex: BTC, SOL, LNK)? ");
                        String tickerBusca = leitor.next().toUpperCase();
                        leitor.nextLine();
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
                        System.out.println("\n---  SIMULADOR DE APORTE (DCA) ---");
                        System.out.print("Digite o Ticker da moeda para simular (ex: BTC): ");
                        String tickerDCA = leitor.next().toUpperCase();
                        leitor.nextLine();

                        HttpService httpDCA = new HttpService();
                        // verifica se você já tem a moeda na carteira
                        Moeda moedaDCA = minhaCarteira.getMoedas().get(tickerDCA);
                        
                        // busca o preço atual da binance para a simulação
                        double precoMercado = httpDCA.consultarPrecoPorTicker(tickerDCA);
                        
                        if(precoMercado <= 0){
                            System.out.println("Erro: Não foi possível obter o preço atual de " + tickerDCA);
                            break;
                        }

                        System.out.printf("Preço atual de mercado: $ %.2f\n", precoMercado);
                        System.out.print("Quanto você pretende investir agora (em USD)? ");
                        double valorInvestimento = Double.parseDouble(leitor.nextLine().replace(",", "."));

                        double qtdComprada = valorInvestimento / precoMercado;
                        
                        if (moedaDCA != null && moedaDCA.getSaldo() > 0) {
                            double saldoAtual = moedaDCA.getSaldo();
                            double pmAtual = moedaDCA.getPrecoMedio();
                            double custoTotalAtual = saldoAtual * pmAtual;
                            
                            double novoSaldoTotal = saldoAtual + qtdComprada;
                            double novoPM = (custoTotalAtual + valorInvestimento) / novoSaldoTotal;
                            double diferencaPM = ((novoPM - pmAtual) / pmAtual) * 100;

                            System.out.println("\n================ RESULTADO DA SIMULAÇÃO ================");
                            System.out.printf(" Aporte: $ %.2f  ->  Compraria: %.8f %s\n", valorInvestimento, qtdComprada, tickerDCA);
                            System.out.printf(" Saldo: %.8f  ->  Novo Saldo: %.8f\n", saldoAtual, novoSaldoTotal);
                            System.out.printf(" PM Atual: $ %.2f  ->  Novo PM: $ %.2f\n", pmAtual, novoPM);
                            
                            if(novoPM < pmAtual){
                                System.out.printf(" EXCELENTE! Isso reduziria seu Preço Médio em %.2f%%\n", Math.abs(diferencaPM));
                            }else{
                                System.out.printf(" ATENÇÃO: Esse aporte aumentaria seu Preço Médio em %.2f%%\n", diferencaPM);
                            }
                        }else{
                            System.out.println("\n================ RESULTADO DA SIMULAÇÃO ================");
                            System.out.printf(" Com $ %.2f, você iniciaria sua posição com %.8f %s\n", valorInvestimento, qtdComprada, tickerDCA);
                            System.out.printf(" Seu Preço Médio inicial seria: $ %.2f\n", precoMercado);
                        }
                        System.out.println("========================================================");
                        break;

                    case 10:
                        System.out.println("Saindo...");
                        break;

                    default:
                        System.out.println("Opção inválida!");
                }
            }catch(NumberFormatException e){ 
                System.out.println(" ERRO: Por favor, digite apenas números inteiros para as opções do menu.");
            }catch(Exception e){ 
                System.out.println(" Ocorreu um erro inesperado: " + e.getMessage());
                if(leitor.hasNextLine()){
                    leitor.nextLine();
                }
            }
        }
        leitor.close();
    }
}