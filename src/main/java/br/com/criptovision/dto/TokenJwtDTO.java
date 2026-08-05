package br.com.criptovision.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "TokenJwt",
    description = "Token JWT utilizado no cabeçalho Authorization das rotas protegidas."
)
public record TokenJwtDTO(
    @Schema(
        description = "Token de autenticação no formato JWT.",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.exemplo.assinatura"
    )
    String token
) {
}
