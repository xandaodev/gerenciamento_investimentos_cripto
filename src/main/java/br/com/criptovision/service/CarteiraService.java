package br.com.criptovision.service;

import br.com.criptovision.cotacao.model.CotacaoMercado;
import br.com.criptovision.cotacao.model.ResultadoCotacoes;
import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.cotacao.util.TickerNormalizer;
import br.com.criptovision.dto.*;
import br.com.criptovision.exception.AlteracaoHistoricoInvalidaException;
import br.com.criptovision.exception.HistoricoInconsistenteException;
import br.com.criptovision.exception.SaldoInsuficienteException;
import br.com.criptovision.exception.TransacaoNaoEncontradaException;
import br.com.criptovision.model.*;
import br.com.criptovision.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

// uma das classes mais importantes, aqui são feitos todos os calculos usando os dados que as outras classes fornecem

@Service
public class CarteiraService {


    private static final Comparator<Transacao> ORDEM_CRONOLOGICA =
        Comparator.comparing(
            Transacao::getData,
            Comparator.nullsLast(Comparator.naturalOrder())
        ).thenComparing(
            Transacao::getId,
            Comparator.nullsLast(Comparator.naturalOrder())
        );



    private static final BigDecimal CEM =
        BigDecimal.valueOf(100);

    private final TransacaoRepository transacaoRepo;
    private final CotacaoService cotacaoService;

    public CarteiraService(
        TransacaoRepository transacaoRepo,
        CotacaoService cotacaoService
    ) {
        this.transacaoRepo = transacaoRepo;
        this.cotacaoService = cotacaoService;
    }

    // nesse metodo é atualizado o estado de uma moeda baseado numa transacao
    // ele é chamado e rechamado varias vezes quando o programa é iniciado para reconstruir seu saldo

    // metodo 1 que é usado na main
    public void processarTransacao(Moeda moeda, Transacao transacao) throws SaldoInsuficienteException {
        processarTransacao(moeda, transacao, true); // redireciona para o método de baixo
    }

    // metodo 2, o motor
    public void processarTransacao(Moeda moeda, Transacao transacao, boolean salvarNoBanco) throws SaldoInsuficienteException {

        if (transacao.getTipo() == TipoTransacao.COMPRA) {
            BigDecimal custoTotalAntigo = moeda.getSaldo().multiply(moeda.getPrecoMedio());

            // custoNovaCompra = quantidade * precoUnitario
            BigDecimal custoNovaCompra = transacao.getQuantidade().multiply(transacao.getPrecoUnitario());

            // novoSaldo = saldo + quantidade
            BigDecimal novoSaldo = moeda.getSaldo().add(transacao.getQuantidade());

            // novoPrecoMedio = (custoTotalAntigo + custoNovaCompra) / novoSaldo
            BigDecimal novoPrecoMedio = custoTotalAntigo.add(custoNovaCompra).divide(novoSaldo, 8, RoundingMode.HALF_UP);

            moeda.setSaldo(novoSaldo);
            moeda.setPrecoMedio(novoPrecoMedio);

        } else if (transacao.getTipo() == TipoTransacao.VENDA) {
            if (transacao.getQuantidade().compareTo(BigDecimal.ZERO) <= 0){
                throw new IllegalArgumentException("A quantidade de venda deve ser maior que zero.");
            }

            if (transacao.getQuantidade().compareTo(moeda.getSaldo()) > 0){
                throw new SaldoInsuficienteException(
                        "Saldo insuficiente para a venda! Você tentou vender " + transacao.getQuantidade() +
                                ", mas possui apenas " + moeda.getSaldo() + " de " + moeda.getTicker()
                );
            }

            BigDecimal custoParteVendida = transacao.getQuantidade().multiply(moeda.getPrecoMedio());
            BigDecimal valorRecebidoNaVenda = transacao.getQuantidade().multiply(transacao.getPrecoUnitario());
            BigDecimal lucroOperacao = valorRecebidoNaVenda.subtract(custoParteVendida);

            moeda.setSaldo(moeda.getSaldo().subtract(transacao.getQuantidade()));
        }

        if (salvarNoBanco){
            this.transacaoRepo.save(transacao);
        }
    }

    public List<Transacao> carregarHistoricoDeTransacoes(Usuario usuario){
        return transacaoRepo
            .findAllByUsuarioOrderByDataAscIdAsc(usuario);
    }

    // metodo muito importante e funcional, ele calcula quanto voce ganharia se vendesse tudo agora
    // "Lucro nao realizado"
    public double calcularLucroPotencial(Moeda moeda, double precoAtual){
        if(moeda.getSaldo().compareTo(BigDecimal.ZERO) <= 0){
            return 0;
        }
        double valorInvestido = moeda.getSaldo().multiply(moeda.getPrecoMedio()).doubleValue();
        double valorAtual = moeda.getSaldo().multiply(BigDecimal.valueOf(precoAtual)).doubleValue();
        return valorAtual - valorInvestido;
    }

    // agr o metodo puro, sem prints, apenas regra de negócio
    public SimulacaoDCADTO simularDCA(Moeda moeda, double valorAporteUSD, double precoMercado){
        double saldoAtual = moeda.getSaldo().doubleValue();
        double pmAtual = moeda.getPrecoMedio().doubleValue();
        double custoTotalAtual = saldoAtual * pmAtual;

        // quantidade que o novo aporte compraria
        double qtdComprada = valorAporteUSD / precoMercado;

        // novos valores totais simulados
        double novoSaldoTotal = saldoAtual + qtdComprada;
        double novoCustoTotal = custoTotalAtual + valorAporteUSD;
        double novoPM = novoCustoTotal / novoSaldoTotal;

        // diferenças em porcentagem do preço medio
        double diferencaPM = ((novoPM - pmAtual) / pmAtual) * 100;

        //mostra quanto a moeda precisa valorizar para começar a ter lucro
        double valorizacaoNecessaria = ((novoPM / precoMercado) - 1) * 100;

        // empacota tudo na caixa e devolve
        return new SimulacaoDCADTO(
                qtdComprada, saldoAtual, novoSaldoTotal, pmAtual, novoPM, diferencaPM, valorizacaoNecessaria
        );
    }

    // agora a reconstrução da carteira nao fica mais na main, fica aqui no service
    public void reconstruirCarteira(
        Carteira carteira,
        List<Transacao> historico
    ) {
        List<Transacao> historicoOrdenado = new ArrayList<>(historico);
        historicoOrdenado.sort(ORDEM_CRONOLOGICA);

        for (Transacao transacao : historicoOrdenado) {
            try {
                String ticker = TickerNormalizer.normalizar(
                    transacao.getTicker()
                );
                Moeda moeda = carteira.obterMoeda(ticker, ticker);

                processarTransacao(moeda, transacao, false);
            } catch (RuntimeException e) {
                String tipo = transacao.getTipo() == null
                    ? null
                    : transacao.getTipo().name();

                throw new HistoricoInconsistenteException(
                    transacao.getId(),
                    transacao.getTicker(),
                    tipo,
                    e
                );
            }
        }
    }


    // METODOS DTO

    public SimulacaoVendaDTO simularVendaFutura(Moeda moeda, double precoFicticio, double precoAtualMercado){

        // calcula o lucro e a porcentagem
        double lucroSimulado = calcularLucroPotencial(moeda, precoFicticio);
        double custoBase = moeda.getSaldo().multiply(moeda.getPrecoMedio()).doubleValue();
        double porcSimulada = (lucroSimulado / custoBase) * 100;

        // calcula os totais
        double valorTotalFicticio = moeda.getSaldo().doubleValue() * precoFicticio;
        double valorTotalAtual = moeda.getSaldo().doubleValue() * precoAtualMercado;

        //empacota tudo na caixa (DTO) e devolve para quem chamou
        return new SimulacaoVendaDTO(lucroSimulado, porcSimulada, valorTotalFicticio, valorTotalAtual);
    }

    // gera o resumo completo da carteira e empacota tudo num DTO
    public ResumoCarteiraDTO gerarResumoCompleto(
        Carteira carteira,
        ResultadoCotacoes resultadoCotacoes
    ) {
        BigDecimal totalCalculado = BigDecimal.ZERO;
        BigDecimal pnlTotalGeral = BigDecimal.ZERO;
        BigDecimal totalPatrimonioOntem = BigDecimal.ZERO;
        List<ResumoAtivoDTO> listaAtivos = new ArrayList<>();
        Instant cotacoesAtualizadasEm = null;

        for (Moeda moeda : carteira.getMoedas().values()) {
            if (moeda.getSaldo().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<CotacaoMercado> cotacaoOptional =
                resultadoCotacoes.obter(moeda.getTicker());

            if (cotacaoOptional.isEmpty()) {
                listaAtivos.add(
                    ResumoAtivoDTO.semCotacao(
                        moeda.getTicker(),
                        moeda.getSaldo(),
                        moeda.getPrecoMedio()
                    )
                );
                continue;
            }

            CotacaoMercado cotacao = cotacaoOptional.get();
            BigDecimal preco = cotacao.preco();
            BigDecimal variacao24h = cotacao.variacao24h();

            BigDecimal valorNoAtivo = moeda.getSaldo()
                .multiply(preco);

            BigDecimal custoBase = moeda.getSaldo()
                .multiply(moeda.getPrecoMedio());

            BigDecimal lucroDestaMoeda =
                valorNoAtivo.subtract(custoBase);

            BigDecimal porcentagemLucro =
                custoBase.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : lucroDestaMoeda
                        .multiply(CEM)
                        .divide(
                            custoBase,
                            8,
                            RoundingMode.HALF_UP
                        );

            totalCalculado = totalCalculado.add(valorNoAtivo);
            pnlTotalGeral = pnlTotalGeral.add(
                lucroDestaMoeda
            );

            BigDecimal fatorVariacao = BigDecimal.ONE.add(
                variacao24h.divide(
                    CEM,
                    12,
                    RoundingMode.HALF_UP
                )
            );

            if (fatorVariacao.compareTo(BigDecimal.ZERO) > 0) {
                totalPatrimonioOntem =
                    totalPatrimonioOntem.add(
                        valorNoAtivo.divide(
                            fatorVariacao,
                            12,
                            RoundingMode.HALF_UP
                        )
                    );
            }

            if (cotacoesAtualizadasEm == null
                || cotacao.atualizadaEm()
                    .isBefore(cotacoesAtualizadasEm)) {
                cotacoesAtualizadasEm =
                    cotacao.atualizadaEm();
            }

            listaAtivos.add(new ResumoAtivoDTO(
                moeda.getTicker(),
                moeda.getSaldo(),
                preco,
                moeda.getPrecoMedio(),
                valorNoAtivo,
                porcentagemLucro,
                variacao24h,
                true,
                cotacao.desatualizada(),
                cotacao.atualizadaEm()
            ));
        }

        BigDecimal variacaoCarteira =
            totalPatrimonioOntem.compareTo(BigDecimal.ZERO) > 0
                ? totalCalculado
                    .subtract(totalPatrimonioOntem)
                    .multiply(CEM)
                    .divide(
                        totalPatrimonioOntem,
                        8,
                        RoundingMode.HALF_UP
                    )
                : BigDecimal.ZERO;

        return new ResumoCarteiraDTO(
            totalCalculado,
            pnlTotalGeral,
            variacaoCarteira,
            listaAtivos,
            cotacoesAtualizadasEm,
            resultadoCotacoes.possuiDadosParciais(),
            new ArrayList<>(
                resultadoCotacoes.indisponiveis()
            )
        );
    }

    public double calcularPatrimonioTotal(Usuario usuario){
        List<Transacao> todasAsTransacoes = carregarHistoricoDeTransacoes(usuario);

        double total = 0;
        for (Transacao t : todasAsTransacoes) {
            double valorTrans = t.getQuantidade().multiply(t.getPrecoUnitario()).doubleValue();
            if (t.getTipo() == TipoTransacao.COMPRA) {
                total += valorTrans;
            } else if (t.getTipo() == TipoTransacao.VENDA) {
                total -= valorTrans;
            }
        }
        return total;
    }

    public ResumoCarteiraDTO obterResumoGeral(Usuario usuario){
        Carteira carteira = new Carteira();

        List<Transacao> historico =
            carregarHistoricoDeTransacoes(usuario);

        reconstruirCarteira(carteira, historico);

        Set<String> tickers = carteira.getMoedas()
            .values()
            .stream()
            .filter(moeda ->
                moeda.getSaldo().compareTo(BigDecimal.ZERO) > 0
            )
            .map(Moeda::getTicker)
            .collect(
                java.util.stream.Collectors.toCollection(
                    LinkedHashSet::new
                )
            );

        ResultadoCotacoes resultadoCotacoes =
            cotacaoService.buscarCotacoes(tickers);

        return gerarResumoCompleto(
            carteira,
            resultadoCotacoes
        );
    }

    public List<Transacao> listarTransacoes(Usuario usuario){
        return carregarHistoricoDeTransacoes(usuario);
    }

    public Transacao buscarTransacaoPorId(Long id, Usuario usuario){
        return transacaoRepo.findByIdAndUsuario(id, usuario).orElseThrow(() -> new TransacaoNaoEncontradaException(id));
    }

    private String validarTicker(String ticker) {
        String normalizado =
            cotacaoService.normalizarTicker(ticker);

        cotacaoService.validarTicker(normalizado);
        return normalizado;
    }

    private void validarHistoricoAposAlteracao(List<Transacao> historico, String mensagemDeErro){
        try{
            Carteira carteiraTemporaria = new Carteira();

            reconstruirCarteira(carteiraTemporaria, historico);
        }catch(HistoricoInconsistenteException ex){
            throw new AlteracaoHistoricoInvalidaException(mensagemDeErro, ex);
        }
    }


    @Transactional
    public Transacao atualizarTransacao(Long id, TransacaoRequestDTO dados, Usuario usuario){
        Transacao transacaoExistente = buscarTransacaoPorId(id, usuario);

        String tickerNormalizado =
            validarTicker(dados.ticker());

        Transacao transacaoCandidata = dados.toEntity();
        transacaoCandidata.setTicker(tickerNormalizado);

        transacaoCandidata.setId(
            transacaoExistente.getId()
        );

        transacaoCandidata.setData(
            transacaoExistente.getData()
        );

        transacaoCandidata.setUsuario(
            transacaoExistente.getUsuario()
        );

        List<Transacao> historicoSimulado =
            new ArrayList<>(carregarHistoricoDeTransacoes(usuario));

        boolean transacaoSubstituida = false;

        for (int indice = 0; indice < historicoSimulado.size(); indice++) {

            Transacao transacaoDoHistorico = historicoSimulado.get(indice);

            if (Objects.equals(transacaoDoHistorico.getId(), id)) {
                historicoSimulado.set(
                    indice,
                    transacaoCandidata
                );

                transacaoSubstituida = true;
                break;
            }
        }

        if (!transacaoSubstituida) {
            throw new TransacaoNaoEncontradaException(id);
        }

        validarHistoricoAposAlteracao(
            historicoSimulado,
            "A alteração da transação de ID "
                + id
                + " tornaria o histórico inconsistente."
        );

        transacaoExistente.setTicker(
            transacaoCandidata.getTicker()
        );
        transacaoExistente.setQuantidade(
            transacaoCandidata.getQuantidade()
        );
        transacaoExistente.setPrecoUnitario(
            transacaoCandidata.getPrecoUnitario()
        );
        transacaoExistente.setTipo(
            transacaoCandidata.getTipo()
        );

        return transacaoRepo.save(transacaoExistente);
    }


    @Transactional
    public void excluirTransacao(Long id, Usuario usuario){
        Transacao transacaoExistente = buscarTransacaoPorId(id, usuario);

        List<Transacao> historicoSemTransacao =
            carregarHistoricoDeTransacoes(usuario)
                .stream()
                .filter(transacao ->
                    !Objects.equals(
                        transacao.getId(),
                        id
                    )
                )
                .toList();

        validarHistoricoAposAlteracao(
            historicoSemTransacao,
            "A exclusão da transação de ID "
                + id
                + " tornaria o histórico inconsistente."
        );

        transacaoRepo.delete(transacaoExistente);
    }



    public Transacao registrarNovaTransacao(Transacao novaTransacao, Usuario usuario){
        novaTransacao.setUsuario(usuario);

        String tickerNormalizado =
            validarTicker(novaTransacao.getTicker());
        novaTransacao.setTicker(tickerNormalizado);
        Carteira carteiraTemporaria = new Carteira();
        List<Transacao> historico = carregarHistoricoDeTransacoes(usuario);
        reconstruirCarteira(carteiraTemporaria, historico);

        Moeda moedaDaOperacao = carteiraTemporaria.obterMoeda(
            tickerNormalizado,
            tickerNormalizado
        );

        processarTransacao(moedaDaOperacao, novaTransacao, true);

        return novaTransacao;
    }

    public SimulacaoDCADTO executarSimulacaoDCA(String ticker, double valorAporte, double precoMercado, Usuario usuario){
        Carteira carteira = new Carteira();
        List<Transacao> historico = carregarHistoricoDeTransacoes(usuario);
        reconstruirCarteira(carteira, historico);

        String tickerNormalizado =
            TickerNormalizer.normalizar(ticker);

        Moeda moeda = carteira.obterMoeda(
            tickerNormalizado,
            tickerNormalizado
        );
        return simularDCA(moeda, valorAporte, precoMercado);
    }

    public SimulacaoVendaDTO executarSimulacaoVenda(String ticker, double precoFicticio, Usuario usuario){
        Carteira carteira = new Carteira();
        List<Transacao> historico = carregarHistoricoDeTransacoes(usuario);
        reconstruirCarteira(carteira, historico);

        String tickerNormalizado =
            cotacaoService.normalizarTicker(ticker);

        Moeda moeda = carteira.obterMoeda(
            tickerNormalizado,
            tickerNormalizado
        );

        double precoAtualMercado =
            cotacaoService.buscarCotacao(
                tickerNormalizado
            ).preco().doubleValue();

        return simularVendaFutura(
            moeda,
            precoFicticio,
            precoAtualMercado
        );
    }

}
