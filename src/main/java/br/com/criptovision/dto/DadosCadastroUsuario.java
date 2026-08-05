package br.com.criptovision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record DadosCadastroUsuario(
    @NotBlank(message = "O login é obrigatório.")
    @Size(
        min = 3,
        max = 100,
        message = "O login deve possuir entre 3 e 100 caracteres."
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "O login pode conter somente letras, números, ponto, hífen e underline."
    )
    String login,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(
        min = 8,
        max = 64,
        message = "A senha deve possuir entre 8 e 64 caracteres."
    )
    String senha
) {

    public DadosCadastroUsuario {
        if (login != null) {
            login = login.trim().toLowerCase(Locale.ROOT);
        }
    }
}
