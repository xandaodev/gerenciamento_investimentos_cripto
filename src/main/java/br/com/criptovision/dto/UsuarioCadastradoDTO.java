package br.com.criptovision.dto;

import br.com.criptovision.model.Usuario;

public record UsuarioCadastradoDTO(
    Long id,
    String login
) {

    public UsuarioCadastradoDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getLogin());
    }
}
