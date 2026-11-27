package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
    name = "DadosTrocaSenha",
    description = "Senha atual e nova senha do usuário autenticado."
)
public record DadosTrocaSenha(
    @Schema(
        description = "Senha atual do usuário.",
        example = "SenhaSegura@2026",
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "A senha atual é obrigatória.")
    String senhaAtual,

    // as mesmas regras da senha do cadastro
    @Schema(
        description = "Nova senha, com 8 a 64 caracteres e diferente da atual.",
        example = "OutraSenhaSegura@2027",
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(
        min = 8,
        max = 64,
        message = "A nova senha deve possuir entre 8 e 64 caracteres."
    )
    String novaSenha
) {
}
