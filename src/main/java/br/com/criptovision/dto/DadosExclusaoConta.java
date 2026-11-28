package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    name = "DadosExclusaoConta",
    description = "Senha atual, para confirmar a exclusão definitiva da conta."
)
public record DadosExclusaoConta(
    @Schema(
        description = "Senha atual do usuário.",
        example = "SenhaSegura@2026",
        accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "A senha atual é obrigatória.")
    String senhaAtual
) {
}
