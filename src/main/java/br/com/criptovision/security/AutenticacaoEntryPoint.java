package br.com.criptovision.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AutenticacaoEntryPoint
    implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public AutenticacaoEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(
            MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        Map<String, Object> problema =
            new LinkedHashMap<>();

        problema.put("type", "about:blank");
        problema.put("title", "Não autorizado");
        problema.put(
            "status",
            HttpStatus.UNAUTHORIZED.value()
        );
        problema.put(
            "detail",
            "É necessário enviar um token JWT válido para acessar este recurso."
        );
        problema.put("instance", request.getRequestURI());
        problema.put(
            "dataHora",
            OffsetDateTime.now(ZoneOffset.UTC)
        );

        jsonMapper.writeValue(
            response.getOutputStream(),
            problema
        );
    }
}
