package br.com.criptovision.security;

import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limites baixos só neste contexto: 6 logins por IP, 3 falhas por login e
 * 2 cadastros por IP. Os contadores vivem enquanto o contexto Spring existir,
 * então cada teste usa IPs e logins próprios.
 */
@SpringBootTest(
    properties = {
        "seguranca.tentativas.login-por-ip.maximo=6",
        "seguranca.tentativas.login-por-ip.janela=1m",
        "seguranca.tentativas.falhas-por-login.maximo=3",
        "seguranca.tentativas.falhas-por-login.janela=15m",
        "seguranca.tentativas.cadastro-por-ip.maximo=2",
        "seguranca.tentativas.cadastro-por-ip.janela=1h"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProtecaoForcaBrutaHttpTest {

    private static final String SENHA = "SenhaSegura@2026";

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
    void deveBloquearLoginDepoisDeFalhasSeguidasMesmoComSenhaCorreta() throws Exception {
        criarUsuario("conta.atacada");

        for (int i = 0; i < 3; i++) {
            login("10.0.0.1", "conta.atacada", "senha-errada")
                .andExpect(status().isUnauthorized());
        }

        esperarBloqueio(login("10.0.0.1", "conta.atacada", SENHA));
    }

    @Test
    void deveBloquearLoginInexistenteDoMesmoJeito() throws Exception {
        // a resposta não pode revelar se a conta existe
        for (int i = 0; i < 3; i++) {
            login("10.0.0.2", "conta.inexistente", "senha-errada")
                .andExpect(status().isUnauthorized());
        }

        esperarBloqueio(login("10.0.0.2", "conta.inexistente", "senha-errada"));
    }

    @Test
    void deveContarVariacoesDeMaiusculasEEspacosComoOMesmoLogin() throws Exception {
        criarUsuario("conta.variada");

        login("10.0.0.3", "conta.variada", "senha-errada")
            .andExpect(status().isUnauthorized());
        login("10.0.0.3", " CONTA.VARIADA ", "senha-errada")
            .andExpect(status().isUnauthorized());
        login("10.0.0.3", "Conta.Variada", "senha-errada")
            .andExpect(status().isUnauthorized());

        esperarBloqueio(login("10.0.0.3", "conta.variada", SENHA));
    }

    @Test
    void deveZerarFalhasDepoisDeLoginComSucesso() throws Exception {
        criarUsuario("conta.reset");

        login("10.0.0.4", "conta.reset", "senha-errada")
            .andExpect(status().isUnauthorized());
        login("10.0.0.4", "conta.reset", "senha-errada")
            .andExpect(status().isUnauthorized());
        login("10.0.0.4", "conta.reset", SENHA)
            .andExpect(status().isOk());

        // sem o reset, a segunda falha abaixo seria a quarta e já estaria bloqueada
        login("10.0.0.4", "conta.reset", "senha-errada")
            .andExpect(status().isUnauthorized());
        login("10.0.0.4", "conta.reset", "senha-errada")
            .andExpect(status().isUnauthorized());
    }

    @Test
    void naoDeveBloquearOutrasContasDoMesmoIp() throws Exception {
        criarUsuario("conta.bloqueada");
        criarUsuario("conta.vizinha");

        for (int i = 0; i < 3; i++) {
            login("10.0.0.5", "conta.bloqueada", "senha-errada")
                .andExpect(status().isUnauthorized());
        }

        login("10.0.0.5", "conta.vizinha", SENHA)
            .andExpect(status().isOk());
    }

    @Test
    void deveLimitarTentativasDeLoginPorIp() throws Exception {
        // logins diferentes, para que só o limite por IP seja atingido
        for (int i = 1; i <= 6; i++) {
            login("10.0.0.6", "alvo" + i, "senha-errada")
                .andExpect(status().isUnauthorized());
        }

        esperarBloqueio(login("10.0.0.6", "alvo7", "senha-errada"));

        login("10.0.0.7", "alvo7", "senha-errada")
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveLimitarCadastrosPorIp() throws Exception {
        cadastrar("10.0.0.8", "novo.um").andExpect(status().isCreated());
        cadastrar("10.0.0.8", "novo.dois").andExpect(status().isCreated());

        esperarBloqueio(cadastrar("10.0.0.8", "novo.tres"));

        cadastrar("10.0.0.9", "novo.tres").andExpect(status().isCreated());
    }

    private void esperarBloqueio(ResultActions resposta) throws Exception {
        resposta
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string(
                HttpHeaders.RETRY_AFTER,
                matchesPattern("[1-9][0-9]*")
            ))
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.title").value("Muitas tentativas"))
            .andExpect(jsonPath("$.detail").value(
                containsString("Tente novamente em")
            ));
    }

    private void criarUsuario(String login) {
        usuarioRepository.saveAndFlush(
            new Usuario(login, passwordEncoder.encode(SENHA))
        );
    }

    private ResultActions login(String ip, String login, String senha)
        throws Exception {

        return mockMvc.perform(post("/auth/login")
            .with(requisicao -> {
                requisicao.setRemoteAddr(ip);
                return requisicao;
            })
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"login": "%s", "senha": "%s"}
                """.formatted(login, senha)));
    }

    private ResultActions cadastrar(String ip, String login) throws Exception {
        return mockMvc.perform(post("/auth/register")
            .with(requisicao -> {
                requisicao.setRemoteAddr(ip);
                return requisicao;
            })
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"login": "%s", "senha": "%s"}
                """.formatted(login, SENHA)));
    }
}
