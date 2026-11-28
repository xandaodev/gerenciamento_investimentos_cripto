package br.com.criptovision.controller;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
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
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os direitos do titular (LGPD): exportar todos os dados e excluir a conta.
 * Limite baixo de falhas por login neste contexto, porque a senha errada na
 * exclusão conta como falha de login. Cada teste usa logins próprios.
 */
@SpringBootTest(
    properties = {
        "seguranca.tentativas.falhas-por-login.maximo=3",
        "seguranca.tentativas.falhas-por-login.janela=15m"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaHttpTest {

    private static final String SENHA = "SenhaSegura@2026";

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

    @BeforeEach
    void limparBanco() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // --- exportação -------------------------------------------------------------

    @Test
    void exportacaoDeveTrazerAContaETodasAsTransacoesEmOrdem() throws Exception {
        Usuario titular = criarUsuario("conta.exporta");
        salvar(titular, "ETH", "2", "3000", TipoTransacao.COMPRA, LocalDateTime.parse("2025-06-01T10:00:00"));
        salvar(titular, "BTC", "0.5", "65000.125", TipoTransacao.COMPRA, LocalDateTime.parse("2025-03-10T17:00:00"));
        salvar(titular, "BTC", "0.1", "70000", TipoTransacao.VENDA, LocalDateTime.parse("2025-09-01T08:30:00"));

        exportar(titular)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            // baixado como arquivo, com a data no nome
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                matchesPattern("attachment; filename=\"criptovision-dados-\\d{4}-\\d{2}-\\d{2}\\.json\"")
            ))
            .andExpect(jsonPath("$.exportadoEm").value(matchesPattern(".*Z$")))
            .andExpect(jsonPath("$.conta.login").value("conta.exporta"))
            .andExpect(jsonPath("$.transacoes", hasSize(3)))
            .andExpect(jsonPath("$.transacoes[0].ticker").value("BTC"))
            .andExpect(jsonPath("$.transacoes[0].tipo").value("COMPRA"))
            .andExpect(jsonPath("$.transacoes[0].quantidade").value(0.5))
            .andExpect(jsonPath("$.transacoes[0].precoUnitario").value(65000.125))
            .andExpect(jsonPath("$.transacoes[0].data").value("2025-03-10T17:00:00Z"))
            .andExpect(jsonPath("$.transacoes[1].ticker").value("ETH"))
            .andExpect(jsonPath("$.transacoes[2].tipo").value("VENDA"));
    }

    @Test
    void exportacaoNaoDeveTrazerDadosDeOutraContaNemSegredos() throws Exception {
        Usuario titular = criarUsuario("conta.titular");
        Usuario outro = criarUsuario("conta.outra");
        salvar(titular, "BTC", "1", "65000", TipoTransacao.COMPRA, LocalDateTime.parse("2025-03-10T17:00:00"));
        salvar(outro, "SOL", "10", "150", TipoTransacao.COMPRA, LocalDateTime.parse("2025-03-11T17:00:00"));

        String json = exportar(titular)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transacoes", hasSize(1)))
            .andExpect(jsonPath("$.transacoes[0].ticker").value("BTC"))
            .andReturn().getResponse().getContentAsString();

        // nem o hash da senha nem a versão interna dos tokens saem no arquivo
        assertThat(json)
            .doesNotContain("SOL", "conta.outra", "senha", "$2a$", "versao");
    }

    @Test
    void exportacaoDeContaSemTransacoesDeveTrazerUmaListaVazia() throws Exception {
        exportar(criarUsuario("conta.vazia"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conta.login").value("conta.vazia"))
            .andExpect(jsonPath("$.transacoes", hasSize(0)));
    }

    @Test
    void exportacaoDeveExigirToken() throws Exception {
        mockMvc.perform(get("/conta/exportacao"))
            .andExpect(status().isUnauthorized());
    }

    // --- exclusão ---------------------------------------------------------------

    @Test
    void exclusaoDeveApagarAContaETodasAsTransacoes() throws Exception {
        Usuario titular = criarUsuario("conta.exclui");
        Usuario outro = criarUsuario("conta.fica");
        salvar(titular, "BTC", "1", "65000", TipoTransacao.COMPRA, LocalDateTime.parse("2025-03-10T17:00:00"));
        salvar(titular, "BTC", "0.5", "70000", TipoTransacao.VENDA, LocalDateTime.parse("2025-04-10T17:00:00"));
        salvar(outro, "ETH", "2", "3000", TipoTransacao.COMPRA, LocalDateTime.parse("2025-03-10T17:00:00"));
        String token = tokenService.gerarToken(titular);

        excluir(token, SENHA).andExpect(status().isNoContent());

        assertThat(usuarioRepository.findByLogin("conta.exclui")).isEmpty();
        assertThat(transacaoRepository.findAllByUsuarioOrderByDataAscIdAsc(titular)).isEmpty();
        // a outra conta não é afetada
        assertThat(transacaoRepository.findAllByUsuarioOrderByDataAscIdAsc(outro)).hasSize(1);

        // o token da conta excluída não vale mais nada
        mockMvc.perform(get("/transacoes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
        excluir(token, SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeUmaContaExcluidaDeveFicarLivreParaUmNovoCadastro() throws Exception {
        Usuario titular = criarUsuario("conta.reusa");
        excluir(tokenService.gerarToken(titular), SENHA).andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login": "conta.reusa", "senha": "%s"}
                    """.formatted(SENHA)))
            .andExpect(status().isCreated());
    }

    @Test
    void exclusaoComSenhaErradaNaoDeveApagarNada() throws Exception {
        Usuario titular = criarUsuario("conta.senha.errada");
        salvar(titular, "BTC", "1", "65000", TipoTransacao.COMPRA, LocalDateTime.parse("2025-03-10T17:00:00"));
        String token = tokenService.gerarToken(titular);

        excluir(token, "senha-errada")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Exclusão de conta recusada"))
            .andExpect(jsonPath("$.detail").value(containsString("não foi excluída")));

        assertThat(usuarioRepository.findByLogin("conta.senha.errada")).isPresent();
        assertThat(transacaoRepository.findAllByUsuarioOrderByDataAscIdAsc(titular)).hasSize(1);
    }

    @Test
    void senhaErradaNaExclusaoDeveContarComoFalhaDeLogin() throws Exception {
        // quem roubou um token não pode testar senhas aqui à vontade
        Usuario titular = criarUsuario("conta.forca.bruta");
        String token = tokenService.gerarToken(titular);

        for (int i = 0; i < 3; i++) {
            excluir(token, "senha-errada").andExpect(status().isBadRequest());
        }

        excluir(token, SENHA)
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        assertThat(usuarioRepository.findByLogin("conta.forca.bruta")).isPresent();
    }

    @Test
    void exclusaoSemSenhaDeveSerRecusada() throws Exception {
        String token = tokenService.gerarToken(criarUsuario("conta.sem.senha"));

        mockMvc.perform(post("/conta/exclusao")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erros[0].campo").value("senhaAtual"));

        assertThat(usuarioRepository.findByLogin("conta.sem.senha")).isPresent();
    }

    @Test
    void exclusaoDeveExigirToken() throws Exception {
        mockMvc.perform(post("/conta/exclusao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"senhaAtual": "%s"}
                    """.formatted(SENHA)))
            .andExpect(status().isUnauthorized());
    }

    // --- apoio ------------------------------------------------------------------

    private Usuario criarUsuario(String login) {
        return usuarioRepository.saveAndFlush(
            new Usuario(login, passwordEncoder.encode(SENHA))
        );
    }

    private void salvar(
        Usuario usuario,
        String ticker,
        String quantidade,
        String preco,
        TipoTransacao tipo,
        LocalDateTime data
    ) {
        Transacao transacao = new Transacao(ticker, new BigDecimal(quantidade), new BigDecimal(preco), tipo);
        transacao.setUsuario(usuario);
        transacao.setData(data);
        transacaoRepository.saveAndFlush(transacao);
    }

    private ResultActions exportar(Usuario usuario) throws Exception {
        return mockMvc.perform(get("/conta/exportacao")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.gerarToken(usuario)));
    }

    private ResultActions excluir(String token, String senha) throws Exception {
        return mockMvc.perform(post("/conta/exclusao")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"senhaAtual": "%s"}
                """.formatted(senha)));
    }
}
