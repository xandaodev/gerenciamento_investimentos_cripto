package br.com.criptovision.security;

import br.com.criptovision.exception.TentativasExcedidasException;
import br.com.criptovision.observabilidade.MetricasCriptoVision;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Limita tentativas em {@code /auth/login} e {@code /auth/register}:
 *
 * <ul>
 *   <li>por IP, todas as tentativas de login e de cadastro (freia quem testa
 *       muitas contas ou cria contas em massa);</li>
 *   <li>por login, só as falhas: depois de N senhas erradas, a conta fica
 *       bloqueada até a janela fechar, mesmo com a senha certa (freia quem testa
 *       muitas senhas numa conta, mesmo distribuindo o ataque entre IPs).
 *       Um login com sucesso zera a contagem.</li>
 * </ul>
 *
 * <p>Os contadores são locais à instância. Atrás de um proxy reverso, o IP só é
 * o do cliente se os cabeçalhos de encaminhamento estiverem configurados
 * ({@code server.forward-headers-strategy}); sem isso, todos os clientes
 * compartilhariam o limite do IP do proxy.
 */
@Component
public class ProtecaoForcaBruta {

    // nenhum login válido passa disso (limite do cadastro); limita a memória por chave
    static final int TAMANHO_MAXIMO_DA_CHAVE = 100;

    private final LimiteDeTentativas loginPorIp;
    private final LimiteDeTentativas falhasPorLogin;
    private final LimiteDeTentativas cadastroPorIp;
    private final MetricasCriptoVision metricas;

    public ProtecaoForcaBruta(
        TentativasProperties propriedades,
        Clock clock,
        MetricasCriptoVision metricas
    ) {
        this.loginPorIp = criar("login por IP", propriedades.loginPorIp(), clock);
        this.falhasPorLogin = criar("falhas por login", propriedades.falhasPorLogin(), clock);
        this.cadastroPorIp = criar("cadastro por IP", propriedades.cadastroPorIp(), clock);
        this.metricas = metricas;
    }

    /** Chamado antes de autenticar. Lança {@link TentativasExcedidasException} se bloqueado. */
    public void verificarLogin(String ip, String login) {
        recusarSeBloqueado(loginPorIp.tentar(ip), "login-por-ip");
        recusarSeBloqueado(falhasPorLogin.bloqueio(chaveDoLogin(login)), "falhas-por-login");
    }

    public void registrarFalhaDeLogin(String login) {
        falhasPorLogin.registrar(chaveDoLogin(login));
    }

    public void registrarLoginComSucesso(String login) {
        falhasPorLogin.limpar(chaveDoLogin(login));
    }

    /** Chamado antes de cadastrar. Lança {@link TentativasExcedidasException} se bloqueado. */
    public void verificarCadastro(String ip) {
        recusarSeBloqueado(cadastroPorIp.tentar(ip), "cadastro-por-ip");
    }

    // "Alexandre" e " alexandre " são a mesma conta (o cadastro normaliza o login)
    static String chaveDoLogin(String login) {
        String chave = login.trim().toLowerCase(Locale.ROOT);

        return chave.length() > TAMANHO_MAXIMO_DA_CHAVE
            ? chave.substring(0, TAMANHO_MAXIMO_DA_CHAVE)
            : chave;
    }

    // a métrica conta cada recusa: um pico indica um ataque em andamento
    private void recusarSeBloqueado(Optional<Duration> bloqueio, String limite) {
        if (bloqueio.isPresent()) {
            metricas.tentativaBloqueada(limite);
            throw new TentativasExcedidasException(bloqueio.get());
        }
    }

    private static LimiteDeTentativas criar(
        String nome,
        TentativasProperties.Limite limite,
        Clock clock
    ) {
        return new LimiteDeTentativas(nome, limite.maximo(), limite.janela(), clock);
    }
}
