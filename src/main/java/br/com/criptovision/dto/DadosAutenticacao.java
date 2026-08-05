package br.com.criptovision.dto;

import jakarta.validation.constraints.NotBlank;

public record DadosAutenticacao(
    @NotBlank(message = "O login é obrigatório.")
    String login,

    @NotBlank(message = "A senha é obrigatória.")
    String senha
) {
}
