package br.com.criptovision;

import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CadastroUsuarioHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparBanco() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveCadastrarUsuarioComSenhaCriptografada() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": " Novo.Usuario ",
                      "senha": "SenhaSegura@2026"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.login").value("novo.usuario"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());

        Usuario usuario = (Usuario) usuarioRepository.findByLogin(
            "novo.usuario"
        );

        assertThat(usuario).isNotNull();
        assertThat(usuario.getSenha()).isNotEqualTo("SenhaSegura@2026");
        assertThat(
            passwordEncoder.matches(
                "SenhaSegura@2026",
                usuario.getSenha()
            )
        ).isTrue();
    }

    @Test
    void deveRetornarConflitoQuandoLoginJaExistir() throws Exception {
        cadastrarUsuario("alexandre", "SenhaSegura@2026");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": " ALEXANDRE ",
                      "senha": "OutraSenha@2026"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.erro").value("Login já cadastrado"));
    }

    @Test
    void deveRejeitarLoginInvalido() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": "ab",
                      "senha": "SenhaSegura@2026"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.login").exists());
    }

    @Test
    void deveRejeitarSenhaCurta() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": "alexandre",
                      "senha": "1234567"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void devePermitirLoginDepoisDoCadastro() throws Exception {
        cadastrarUsuario("alexandre", "SenhaSegura@2026");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": "alexandre",
                      "senha": "SenhaSegura@2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private void cadastrarUsuario(
        String login,
        String senha
    ) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": "%s",
                      "senha": "%s"
                    }
                    """.formatted(login, senha)))
            .andExpect(status().isCreated());
    }
}
