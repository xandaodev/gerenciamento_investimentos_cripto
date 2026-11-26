package br.com.criptovision.security;

import br.com.criptovision.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService implements UserDetailsService {

    private final UsuarioRepository repository;

    public AutenticacaoService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // O Spring chama este método a cada tentativa de login.
        // Lançar UsernameNotFoundException (e nunca devolver null) faz o
        // DaoAuthenticationProvider comparar a senha com um hash fictício antes de
        // recusar. Assim, um login inexistente leva o mesmo tempo que uma senha
        // errada e recebe o mesmo 401 genérico, sem revelar quais logins existem.
        return repository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
