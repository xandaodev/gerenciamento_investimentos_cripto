package br.com.criptovision.service;

import br.com.criptovision.dto.DadosCadastroUsuario;
import br.com.criptovision.exception.LoginJaCadastradoException;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrar(DadosCadastroUsuario dados) {
        if (usuarioRepository.existsByLogin(dados.login())) {
            throw new LoginJaCadastradoException();
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());
        Usuario usuario = new Usuario(dados.login(), senhaCriptografada);

        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new LoginJaCadastradoException();
        }
    }
}
