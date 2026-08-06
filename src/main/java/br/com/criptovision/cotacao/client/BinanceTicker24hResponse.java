package br.com.criptovision.cotacao.client;

record BinanceTicker24hResponse(
    String symbol,
    String lastPrice,
    String priceChangePercent,
    Long closeTime
) {
}
