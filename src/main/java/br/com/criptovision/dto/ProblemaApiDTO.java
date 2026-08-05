package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(
    name = "ProblemaApi",
    description = "Resposta padronizada de erro seguindo o formato Problem Details."
)
public record ProblemaApiDTO(
    @Schema(
        description = "Identificador do tipo de problema.",
        example = "about:blank"
    )
    URI type,

    @Schema(
        description = "Título resumido da categoria do erro.",
        example = "Dados inválidos"
    )
    String title,

    @Schema(
        description = "Código HTTP da resposta.",
        example = "400"
    )
    int status,

    @Schema(
        description = "Explicação legível do problema.",
        example = "Um ou mais campos possuem valores inválidos."
    )
    String detail,

    @Schema(
        description = "Caminho da requisição que gerou o erro.",
        example = "/auth/register"
    )
    URI instance,

    @Schema(
        description = "Data e hora UTC em que o erro foi produzido.",
        example = "2026-08-05T20:00:00Z"
    )
    OffsetDateTime dataHora,

    @Schema(
        description = "Erros associados a campos específicos. Presente em falhas de validação.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    List<ErroCampoDTO> erros
) {
}
