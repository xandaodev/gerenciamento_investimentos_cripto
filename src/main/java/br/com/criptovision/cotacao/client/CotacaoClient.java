package br.com.criptovision.cotacao.client;

import br.com.criptovision.cotacao.model.CotacaoMercado;

import java.util.Map;
import java.util.Set;

public interface CotacaoClient {

    Map<String, CotacaoMercado> buscarCotacoes(
        Set<String> tickers
    );

    CotacaoMercado buscarCotacao(String ticker);
}
