package br.com.criptovision.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum TipoTransacao {

    COMPRA,
    VENDA;

    @JsonCreator
    public static TipoTransacao fromValue(String valor) {
        if (valor == null) {
            return null;
        }

        try {
            return TipoTransacao.valueOf(
                valor.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Tipo de transação inválido. Use COMPRA ou VENDA."
            );
        }
    }
}
