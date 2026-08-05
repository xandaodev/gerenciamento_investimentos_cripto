package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(
    name = "DadosCadastroUsuario",
    description = "Dados necessários para criar uma conta no CriptoVision."
)
public record DadosCadastroUsuario(
    @Schema(
        description = "Login único. É normalizado para letras minúsculas.",
        example = "alexandre.dev",
        minLength = 3,
        maxLength = 100,
        pattern = "^[a-zA-Z0-9._-]+$"
    )
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

    @Schema(
        description = "Senha com 8 a 64 caracteres.",
        example = "SenhaSegura@2026",
        minLength = 8,
        maxLength = 64,
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
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
