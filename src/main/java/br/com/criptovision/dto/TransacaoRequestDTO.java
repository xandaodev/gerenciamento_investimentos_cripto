package br.com.criptovision.dto;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;

public record TransacaoRequestDTO(

    @NotBlank(message = "O ticker é obrigatório.")
    @Size(
        max = 20,
        message = "O ticker deve possuir no máximo 20 caracteres."
    )
    @Pattern(
        regexp = "^[A-Z0-9]+$",
        message = "O ticker deve conter apenas letras e números."
    )
    String ticker,

    @NotNull(message = "A quantidade é obrigatória.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    BigDecimal quantidade,

    @NotNull(message = "O preço unitário é obrigatório.")
    @Positive(message = "O preço unitário deve ser maior que zero.")
    BigDecimal precoUnitario,

    @NotNull(message = "O tipo da transação é obrigatório.")
    TipoTransacao tipo
) {

    public TransacaoRequestDTO {
        if (ticker != null) {
            ticker = ticker
                .trim()
                .toUpperCase(Locale.ROOT);
        }
    }

    public Transacao toEntity() {
        return new Transacao(
            ticker,
            quantidade,
            precoUnitario,
            tipo
        );
    }
}
