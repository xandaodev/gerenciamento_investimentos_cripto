package br.com.criptovision.observabilidade;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Dá a cada requisição um identificador, que aparece em todos os logs dela (MDC),
 * no cabeçalho {@code X-Request-Id} da resposta e no corpo das respostas de erro.
 * Um usuário pode citar esse ID ao relatar um problema, e ele leva direto aos logs.
 *
 * <p>Roda antes de qualquer outro filtro, inclusive o do Spring Security, para que
 * também os logs de autenticação tenham o ID. Um {@code X-Request-Id} enviado pelo
 * cliente ou pelo proxy só é aproveitado se for curto e só tiver caracteres seguros;
 * caso contrário, é gerado um novo, para que ninguém injete texto nos logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentificadorRequisicaoFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Request-Id";
    public static final String CHAVE_MDC = "requestId";

    private static final Pattern FORMATO_SEGURO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    /** O ID da requisição atual, ou null fora de uma requisição. */
    public static String atual() {
        return MDC.get(CHAVE_MDC);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String recebido = request.getHeader(CABECALHO);
        String identificador = recebido != null && FORMATO_SEGURO.matcher(recebido).matches()
            ? recebido
            : UUID.randomUUID().toString();

        MDC.put(CHAVE_MDC, identificador);
        response.setHeader(CABECALHO, identificador);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }
}
