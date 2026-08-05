package br.com.criptovision.dto;

public record ErroCampoDTO(
    String campo,
    String mensagem
) {
}
