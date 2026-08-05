package br.com.criptovision.dto;

import br.com.criptovision.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "UsuarioCadastrado",
    description = "Dados públicos retornados após a criação de uma conta."
)
public record UsuarioCadastradoDTO(
    @Schema(description = "Identificador do usuário.", example = "1")
    Long id,

    @Schema(description = "Login normalizado do usuário.", example = "alexandre.dev")
    String login
) {

    public UsuarioCadastradoDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getLogin());
    }
}
