package br.com.criptovision.security;

import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Logout e troca de senha revogam os tokens pela versão guardada no usuário.
 * Limite baixo de falhas por login neste contexto; cada teste usa um login próprio,
 * porque os contadores vivem enquanto o contexto Spring existir.
 */
@SpringBootTest(
    properties = {
        "seguranca.tentativas.falhas-por-login.maximo=3",
        "seguranca.tentativas.falhas-por-login.janela=15m"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessaoHttpTest {

    private static final String SENHA = "SenhaSegura@2026";
    private static final String NOVA_SENHA = "OutraSenhaSegura@2027";

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
    void deveRevogarOsTokensDeTodosOsDispositivosNoLogout() throws Exception {
        criarUsuario("sessao.logout");
        String celular = login("sessao.logout", SENHA);
        String computador = login("sessao.logout", SENHA);

        acessar(celular).andExpect(status().isOk());
        acessar(computador).andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + celular))
            .andExpect(status().isNoContent());

        acessar(celular).andExpect(status().isUnauthorized());
        acessar(computador).andExpect(status().isUnauthorized());

        // um login novo volta a funcionar
        acessar(login("sessao.logout", SENHA)).andExpect(status().isOk());
    }

    @Test
    void deveExigirTokenParaOLogout() throws Exception {
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveTrocarASenhaERevogarOsTokensAntigos() throws Exception {
        criarUsuario("sessao.troca");
        String atual = login("sessao.troca", SENHA);
        String outroDispositivo = login("sessao.troca", SENHA);

        String novo = JsonPath.read(
            trocarSenha(atual, SENHA, NOVA_SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            "$.token"
        );

        acessar(atual).andExpect(status().isUnauthorized());
        acessar(outroDispositivo).andExpect(status().isUnauthorized());
        acessar(novo).andExpect(status().isOk());

        tentarLogin("sessao.troca", SENHA).andExpect(status().isUnauthorized());
        tentarLogin("sessao.troca", NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    void deveRecusarSenhaAtualIncorretaSemRevogarOToken() throws Exception {
        criarUsuario("sessao.errada");
        String token = login("sessao.errada", SENHA);

        trocarSenha(token, "senha-errada", NOVA_SENHA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Troca de senha inválida"))
            .andExpect(jsonPath("$.detail").value("A senha atual está incorreta."));

        acessar(token).andExpect(status().isOk());
        tentarLogin("sessao.errada", SENHA).andExpect(status().isOk());
    }

    @Test
    void deveRecusarNovaSenhaIgualAAtual() throws Exception {
        criarUsuario("sessao.igual");
        String token = login("sessao.igual", SENHA);

        trocarSenha(token, SENHA, SENHA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("A nova senha deve ser diferente da atual."));

        acessar(token).andExpect(status().isOk());
    }

    @Test
    void deveValidarANovaSenha() throws Exception {
        criarUsuario("sessao.curta");
        String token = login("sessao.curta", SENHA);

        trocarSenha(token, SENHA, "curta")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Dados inválidos"))
            .andExpect(jsonPath("$.erros[*].campo").value(hasItem("novaSenha")));
    }

    @Test
    void deveExigirTokenParaTrocarASenha() throws Exception {
        mockMvc.perform(put("/auth/senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoTrocaSenha(SENHA, NOVA_SENHA)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveBloquearATrocaDeSenhaDepoisDeFalhasSeguidas() throws Exception {
        // quem roubar um token não consegue testar senhas aqui à vontade
        criarUsuario("sessao.ataque");
        String token = login("sessao.ataque", SENHA);

        for (int i = 0; i < 3; i++) {
            trocarSenha(token, "chute-" + i, NOVA_SENHA)
                .andExpect(status().isBadRequest());
        }

        trocarSenha(token, SENHA, NOVA_SENHA)
            .andExpect(status().isTooManyRequests());

        // as falhas contam para o mesmo limite do login
        tentarLogin("sessao.ataque", SENHA).andExpect(status().isTooManyRequests());
    }

    private void criarUsuario(String login) {
        usuarioRepository.saveAndFlush(
            new Usuario(login, passwordEncoder.encode(SENHA))
        );
    }

    private ResultActions tentarLogin(String login, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"login": "%s", "senha": "%s"}
                """.formatted(login, senha)));
    }

    private String login(String login, String senha) throws Exception {
        String resposta = tentarLogin(login, senha)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(resposta, "$.token");
    }

    private ResultActions acessar(String token) throws Exception {
        return mockMvc.perform(get("/transacoes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions trocarSenha(String token, String atual, String nova)
        throws Exception {

        return mockMvc.perform(put("/auth/senha")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoTrocaSenha(atual, nova)));
    }

    private static String corpoTrocaSenha(String atual, String nova) {
        return """
            {"senhaAtual": "%s", "novaSenha": "%s"}
            """.formatted(atual, nova);
    }
}
