package br.com.criptovision.security;

import br.com.criptovision.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository repository;

    public SecurityFilter(
        TokenService tokenService,
        UsuarioRepository repository
    ) {
        this.tokenService = tokenService;
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String tokenJWT = recuperarToken(request);

        if (tokenJWT != null
            && SecurityContextHolder
            .getContext()
            .getAuthentication() == null) {

            // o token só autentica se a versão dele for a atual do usuário:
            // logout e troca de senha incrementam a versão e revogam os tokens antigos
            tokenService.validarToken(tokenJWT)
                .flatMap(token -> repository.findByLogin(token.login())
                    .filter(usuario -> usuario.getVersaoToken() == token.versao()))
                .ifPresent(usuario -> {
                    var authentication =
                        new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            usuario.getAuthorities()
                        );

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
                });
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(
        HttpServletRequest request
    ) {
        String authorizationHeader =
            request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
            || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader
            .substring(7)
            .trim();

        return token.isBlank() ? null : token;
    }
}
