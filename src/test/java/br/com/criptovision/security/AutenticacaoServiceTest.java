package br.com.criptovision.security;

import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository repository;

    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void prepararServico() {
        autenticacaoService = new AutenticacaoService(repository);
    }

    @Test
    void deveRetornarUsuarioQuandoLoginExistir() {
        Usuario usuario = new Usuario("alexandre", "hash-bcrypt");
        when(repository.findByLogin("alexandre"))
            .thenReturn(Optional.of(usuario));

        assertThat(autenticacaoService.loadUserByUsername("alexandre"))
            .isSameAs(usuario);
    }

    @Test
    void deveLancarUsernameNotFoundQuandoLoginNaoExistir() {
        when(repository.findByLogin("inexistente"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            autenticacaoService.loadUserByUsername("inexistente")
        ).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void deveCompararSenhaMesmoQuandoLoginNaoExistir() {
        // Sem a comparação com um hash fictício, um login inexistente
        // responderia mais rápido que um existente, e o tempo de resposta
        // revelaria quais logins estão cadastrados.
        PasswordEncoder passwordEncoder = mock(
            PasswordEncoder.class,
            delegatesTo(new BCryptPasswordEncoder(4))
        );

        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(autenticacaoService);
        provider.setPasswordEncoder(passwordEncoder);

        when(repository.findByLogin("inexistente"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                "inexistente",
                "SenhaQualquer@2026"
            )
        )).isExactlyInstanceOf(BadCredentialsException.class);

        verify(passwordEncoder).matches(
            eq("SenhaQualquer@2026"),
            anyString()
        );
    }
}
