package br.com.criptovision.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> tratarSaldoInsuficiente(SaldoInsuficienteException ex) {

        Map<String, Object> erroJson = new LinkedHashMap<>();
        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put("status", HttpStatus.BAD_REQUEST.value());
        erroJson.put("erro", "Operação Negada");
        erroJson.put("mensagem", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroJson);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarArgumentoInvalido(IllegalArgumentException ex) {

        Map<String, Object> erroJson = new LinkedHashMap<>();
        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put("status", HttpStatus.BAD_REQUEST.value());
        erroJson.put("erro", "Dados Inválidos");
        erroJson.put("mensagem", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroJson);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroGenerico(Exception ex) {

        Map<String, Object> erroJson = new LinkedHashMap<>();
        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        erroJson.put("erro", "Erro Interno do Servidor");
        erroJson.put("mensagem", "Ocorreu um erro inesperado. Por favor, contacta o suporte.");
        // erroJson.put("detalhe_tecnico", ex.getMessage()); // pode descomentar para ver o erro tecnico

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erroJson);
    }
}