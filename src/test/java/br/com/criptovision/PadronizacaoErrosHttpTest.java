package br.com.criptovision;

import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import br.com.criptovision.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "api.security.token.secret="
            + "segredo-exclusivo-dos-testes-criptovision-2026"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PadronizacaoErrosHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private Usuario usuario;

    @BeforeEach
    void prepararBanco() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = usuarioRepository.saveAndFlush(
            new Usuario(
                "alexandre",
                passwordEncoder.encode("SenhaSegura@2026")
            )
        );
    }

    @Test
    void devePadronizar401SemToken() throws Exception {
        mockMvc.perform(get("/transacoes"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Não autorizado"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.instance").value("/transacoes"))
            .andExpect(jsonPath("$.dataHora").exists());
    }

    @Test
    void devePadronizar401ComTokenInvalido() throws Exception {
        mockMvc.perform(get("/transacoes")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer token-invalido"
                ))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Não autorizado"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void devePadronizarLoginInvalido() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": "alexandre",
                      "senha": "senha-errada"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.title").value("Não autorizado"))
            .andExpect(jsonPath("$.detail").value(
                "Login ou senha inválidos."
            ));
    }

    @Test
    void devePadronizarErrosDeValidacao() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "login": "ab",
                      "senha": "123"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.title").value("Dados inválidos"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.erros[*].campo").value(
                hasItem("login")
            ))
            .andExpect(jsonPath("$.erros[*].campo").value(
                hasItem("senha")
            ));
    }

    @Test
    void devePadronizarJsonMalformado() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"login\": \"alexandre\", "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value(
                "Corpo da requisição inválido"
            ))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void devePadronizarParametroComTipoInvalido() throws Exception {
        String token = tokenService.gerarToken(
            usuario.getLogin()
        );

        mockMvc.perform(get("/transacoes/abc")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value(
                "Parâmetro inválido"
            ))
            .andExpect(jsonPath("$.instance").value(
                "/transacoes/abc"
            ));
    }
}
