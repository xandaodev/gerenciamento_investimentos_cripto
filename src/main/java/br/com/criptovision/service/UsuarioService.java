package br.com.criptovision.service;

import br.com.criptovision.dto.DadosCadastroUsuario;
import br.com.criptovision.dto.DadosTrocaSenha;
import br.com.criptovision.exception.LoginJaCadastradoException;
import br.com.criptovision.exception.NovaSenhaInvalidaException;
import br.com.criptovision.exception.SenhaAtualIncorretaException;
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

    /** Logout: revoga todos os tokens do usuário, em todos os dispositivos. */
    @Transactional
    public void encerrarSessoes(Usuario usuario) {
        bloquear(usuario).revogarTokens();
    }

    /**
     * Troca a senha e revoga todos os tokens emitidos antes. Devolve o usuário com a
     * nova versão de tokens, para que o chamador emita um token válido.
     */
    @Transactional
    public Usuario trocarSenha(Usuario usuario, DadosTrocaSenha dados) {
        Usuario atual = bloquear(usuario);

        if (!passwordEncoder.matches(dados.senhaAtual(), atual.getSenha())) {
            throw new SenhaAtualIncorretaException();
        }

        if (passwordEncoder.matches(dados.novaSenha(), atual.getSenha())) {
            throw new NovaSenhaInvalidaException();
        }

        atual.alterarSenha(passwordEncoder.encode(dados.novaSenha()));
        return atual;
    }

    // lê o usuário com SELECT ... FOR UPDATE: um logout e uma troca de senha
    // simultâneos não perdem o incremento um do outro na versão dos tokens
    private Usuario bloquear(Usuario usuario) {
        return usuarioRepository.bloquearPorId(usuario.getId())
            .orElseThrow(() -> new IllegalStateException(
                "Usuário autenticado não encontrado."
            ));
    }
}
