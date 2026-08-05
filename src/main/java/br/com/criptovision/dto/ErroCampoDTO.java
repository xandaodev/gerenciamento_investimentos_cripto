package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "ErroCampo",
    description = "Erro de validação associado a um campo da requisição."
)
public record ErroCampoDTO(
    @Schema(description = "Nome do campo inválido.", example = "senha")
    String campo,

    @Schema(
        description = "Mensagem explicando a restrição violada.",
        example = "A senha deve possuir entre 8 e 64 caracteres."
    )
    String mensagem
) {
}
