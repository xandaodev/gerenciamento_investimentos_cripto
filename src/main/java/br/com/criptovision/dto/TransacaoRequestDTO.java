package br.com.criptovision.dto;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Schema(
    name = "TransacaoRequest",
    description = "Dados utilizados para cadastrar ou atualizar uma transação."
)
public record TransacaoRequestDTO(

    @Schema(
        description = "Símbolo do ativo. É normalizado para letras maiúsculas.",
        example = "BTC",
        maxLength = 20,
        pattern = "^[A-Z0-9]+$"
    )
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

    @Schema(
        description = "Quantidade negociada, com até 18 casas decimais.",
        example = "0.001",
        minimum = "0",
        exclusiveMinimum = true
    )
    @NotNull(message = "A quantidade é obrigatória.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Digits(
        integer = 20,
        fraction = 18,
        message = "A quantidade deve possuir no máximo 20 dígitos inteiros e 18 casas decimais."
    )
    BigDecimal quantidade,

    @Schema(
        description = "Preço unitário do ativo, com até 18 casas decimais.",
        example = "64000.00",
        minimum = "0",
        exclusiveMinimum = true
    )
    @NotNull(message = "O preço unitário é obrigatório.")
    @Positive(message = "O preço unitário deve ser maior que zero.")
    @Digits(
        integer = 20,
        fraction = 18,
        message = "O preço unitário deve possuir no máximo 20 dígitos inteiros e 18 casas decimais."
    )
    BigDecimal precoUnitario,

    @Schema(
        description = "Natureza da operação.",
        example = "COMPRA",
        allowableValues = {"COMPRA", "VENDA"}
    )
    @NotNull(message = "O tipo da transação é obrigatório.")
    TipoTransacao tipo,

    // com o fuso obrigatório: sem ele, a mesma data valeria horários diferentes
    // conforme o lugar. O CarteiraService valida o intervalo (passado, desde 2009)
    @Schema(
        description = "Data e hora da operação, em ISO 8601 com fuso, como "
            + "\"2025-03-10T14:00:00-03:00\" ou \"2025-03-10T17:00:00Z\". Opcional: sem ela, "
            + "vale o momento do cadastro e, na alteração, a data atual da transação. "
            + "Não pode estar no futuro nem ser anterior a 2009-01-03, quando o Bitcoin foi criado.",
        example = "2025-03-10T14:00:00-03:00",
        nullable = true
    )
    OffsetDateTime data
) {

    public TransacaoRequestDTO {
        if (ticker != null) {
            ticker = ticker
                .trim()
                .toUpperCase(Locale.ROOT);
        }
    }

    /** Transação sem data informada. */
    public TransacaoRequestDTO(
        String ticker,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        TipoTransacao tipo
    ) {
        this(ticker, quantidade, precoUnitario, tipo, null);
    }

    /**
     * Cria a transação com a data convertida para UTC, ou sem data (null) se nenhuma
     * foi informada. O CarteiraService completa e valida a data.
     */
    public Transacao toEntity() {
        Transacao transacao = new Transacao(
            ticker,
            quantidade,
            precoUnitario,
            tipo
        );

        transacao.setData(data == null
            ? null
            : data.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());

        return transacao;
    }
}
