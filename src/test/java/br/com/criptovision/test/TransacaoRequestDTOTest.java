package br.com.criptovision.test;

import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.model.TipoTransacao;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransacaoRequestDTOTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidador() {
        validatorFactory = Validation
            .buildDefaultValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void encerrarValidador() {
        validatorFactory.close();
    }

    @Test
    public void deveNormalizarTickerEManterDadosValidos() {
        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            " btc ",
            BigDecimal.valueOf(0.5),
            BigDecimal.valueOf(60000),
            TipoTransacao.COMPRA
        );

        assertTrue(validator.validate(dados).isEmpty());
        assertEquals("BTC", dados.ticker());
        assertEquals(
            TipoTransacao.COMPRA,
            dados.toEntity().getTipo()
        );
    }

    @Test
    public void deveRejeitarTickerEmBranco() {
        TransacaoRequestDTO dados = criarDadosValidosComTicker("   ");

        assertTrue(
            camposInvalidos(dados).contains("ticker")
        );
    }

    @Test
    public void deveRejeitarQuantidadeZero() {
        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            "BTC",
            BigDecimal.ZERO,
            BigDecimal.valueOf(60000),
            TipoTransacao.COMPRA
        );

        assertTrue(
            camposInvalidos(dados).contains("quantidade")
        );
    }

    @Test
    public void deveRejeitarPrecoNegativo() {
        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(-60000),
            TipoTransacao.COMPRA
        );

        assertTrue(
            camposInvalidos(dados).contains("precoUnitario")
        );
    }

    @Test
    public void deveRejeitarTipoNulo() {
        TransacaoRequestDTO dados = new TransacaoRequestDTO(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(60000),
            null
        );

        assertTrue(
            camposInvalidos(dados).contains("tipo")
        );
    }

    private TransacaoRequestDTO criarDadosValidosComTicker(
        String ticker
    ) {
        return new TransacaoRequestDTO(
            ticker,
            BigDecimal.ONE,
            BigDecimal.valueOf(60000),
            TipoTransacao.COMPRA
        );
    }

    private Set<String> camposInvalidos(
        TransacaoRequestDTO dados
    ) {
        return validator
            .validate(dados)
            .stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }
}
