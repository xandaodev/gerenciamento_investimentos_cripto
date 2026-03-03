package br.com.criptovision.main;

import br.com.criptovision.model.Carteira;
import br.com.criptovision.model.Moeda;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.service.CarteiraService;
import br.com.criptovision.service.HttpService;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.util.InputUtils;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // criando os objetos que comandam a logica e os dados
        CarteiraService carteira = new CarteiraService();
        TransacaoRepository repositorio = new TransacaoRepository();

        // chama funcao pra realizar backup, criando uma copia do arquivo atual
        repositorio.realizarBackup();
        
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
                System.out.println("3. Gerar Dashboard de Patrimônio");
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
                                System.out.println("Não tem saldo de " + tickerVenda + " para vender.");
                                break;
                            }

                            System.out.printf("Saldo disponível: %.8f\n", moedaVenda.getSaldo());
                            double qtdVenda = InputUtils.lerDouble("Quantidade a vender: ");
                            
                            if (qtdVenda > moedaVenda.getSaldo()) {
                                System.out.println("Erro: Saldo insuficiente!");
                                break; 
                            }

                            double precoVenda = InputUtils.lerDouble("Preço unitário de venda: ");
                            
                            Transacao tVenda = new Transacao(tickerVenda, qtdVenda, precoVenda, "VENDA");
                            carteira.processarTransacao(moedaVenda, tVenda);
                            historico.add(tVenda); 
                            repositorio.salvar(tVenda);

                            double custoParteVendida = tVenda.getQuantidade() * moedaVenda.getPrecoMedio();
                            double lucroOperacao = (tVenda.getQuantidade() * tVenda.getPrecoUnitario()) - custoParteVendida;
                            System.out.printf("\n   Venda registrada com sucesso! PNL da operação: $ %.2f\n", lucroOperacao);
                            
                            System.out.println("Venda registrada!");
                        } catch(Exception e) {
                            System.out.println("Erro na venda: " + e.getMessage());
                        }
                        break;
                        
                    // DASHBOARD DE PATRIMONIO
                    case 3: 
                        System.out.println("\n--- DASHBOARD DE PATRIMONIO ---");
                        HttpService serviceHttp = new HttpService();
                        
                        double totalCalculado = 0;
                        double pnlTotalGeral = 0;
                        java.util.Map<String, Double> valoresPorMoeda = new java.util.HashMap<>();

                        System.out.println("Atualizando preços na Binance...");
                        
                        double cotacaoDolar = serviceHttp.buscarCotacaoDolar();

                        for(Moeda m : minhaCarteira.getMoedas().values()){
                            if(m.getSaldo() > 0){
                                // busca o preço pra ver quanto vale seu saldo agora
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
                    case 5:
                        System.out.println("\n--- SIMULADOR DE VENDA FUTURA ---");
                        String tickerSim = InputUtils.lerString("Digite o Ticker da moeda que você possui (ex: BTC): ");

                        // primeiro verifica se a moeda existe na carteira
                        if(minhaCarteira.getMoedas().containsKey(tickerSim)){
                            Moeda mSim = minhaCarteira.getMoedas().get(tickerSim);
                            
                            if(mSim.getSaldo() > 0){
                                double precoFicticio = InputUtils.lerDouble("Digite o preço fictício de venda ($): ");

                                // chama o calculo de lucro potencial do carteiraService
                                double lucroSimulado = carteira.calcularLucroPotencial(mSim, precoFicticio);
                                // calcula a porcentagem 
                                double porcSimulada = (lucroSimulado / (mSim.getSaldo() * mSim.getPrecoMedio())) * 100;

                                // variavel pra armazenar o valor total do usuario em uma cripto se ela chegasse a aquele preço
                                double valorTotalFicticio = mSim.getSaldo() * precoFicticio;

                                // calcula o valor total atual da carteira nesse ativo
                                double precoAtual = httpTradutor.consultarPrecoPorTicker(tickerSim);
                                double valorTotalAtual = mSim.getSaldo() * precoAtual;

                                double precoDolar = httpTradutor.buscarCotacaoDolar();//busca o preço do dolar


                                System.out.println("\n------------------------------------------------------");
                                System.out.printf("  Simulação para %s:\n \n", tickerSim);
                                System.out.printf("  Saldo que você possui: %.8f\n", mSim.getSaldo());
                                System.out.printf("  Valor que você possui em " + tickerSim + ": $ %.2f (R$ %.2f)\n \n", valorTotalAtual, valorTotalAtual * precoDolar);
                                System.out.printf("******************************************************* \n");
                                System.out.printf("  Se vender a: $ %.2f\n \n", precoFicticio);
                                System.out.printf("  LUCRO ESTIMADO: $ %.2f (R$ %.2f) -> [%.2f%%]\n \n", lucroSimulado,lucroSimulado * precoDolar, porcSimulada);
                                System.out.printf("  SALDO TOTAL ESTIMADO: $ %.2f (R$ %.2f)\n", valorTotalFicticio, valorTotalFicticio*precoDolar);
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
                        System.out.println("Gerando relatório...");
                        repositorio.gerarRelatorio(new ArrayList<>(minhaCarteira.getMoedas().values()));
                        break;
                    
                    // Ver lucro total realizado
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