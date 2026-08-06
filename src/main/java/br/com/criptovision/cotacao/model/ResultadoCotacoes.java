package br.com.criptovision.cotacao.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ResultadoCotacoes(
    Map<String, CotacaoMercado> cotacoes,
    Set<String> indisponiveis
) {

    public ResultadoCotacoes {
        cotacoes = Collections.unmodifiableMap(
            new LinkedHashMap<>(cotacoes)
        );
        indisponiveis = Collections.unmodifiableSet(
            new LinkedHashSet<>(indisponiveis)
        );
    }

    public Optional<CotacaoMercado> obter(String ticker) {
        return Optional.ofNullable(cotacoes.get(ticker));
    }

    public boolean possuiDadosParciais() {
        return !indisponiveis.isEmpty()
            || cotacoes.values().stream()
                .anyMatch(CotacaoMercado::desatualizada);
    }
}
