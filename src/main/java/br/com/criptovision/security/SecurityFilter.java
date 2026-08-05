package br.com.criptovision.security;

import br.com.criptovision.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository repository;


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

            String subject =
                tokenService.validarToken(tokenJWT);

            if (subject != null && !subject.isBlank()) {
                var usuario = repository.findByLogin(subject);

                if (usuario != null) {
                    var authentication =
                        new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            usuario.getAuthorities()
                        );

                    SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
                }
            }
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
