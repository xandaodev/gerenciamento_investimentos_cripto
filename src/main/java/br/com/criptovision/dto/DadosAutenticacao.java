package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    name = "DadosAutenticacao",
    description = "Credenciais utilizadas para autenticar um usuário."
)
public record DadosAutenticacao(
    @Schema(
        description = "Login do usuário cadastrado.",
        example = "alexandre"
    )
    @NotBlank(message = "O login é obrigatório.")
    String login,

    @Schema(
        description = "Senha do usuário.",
        example = "SenhaSegura@2026",
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "A senha é obrigatória.")
    String senha
) {
}
