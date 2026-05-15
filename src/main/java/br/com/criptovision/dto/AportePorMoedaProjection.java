package br.com.criptovision.dto;

import java.math.BigDecimal;

public interface AportePorMoedaProjection {

    String getTicker();
    BigDecimal getTotalAportado();

}