package br.com.criptovision.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errosPorCampo = new LinkedHashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(erro -> errosPorCampo.putIfAbsent(
                erro.getField(),
                erro.getDefaultMessage()
            ));

        Map<String, Object> erroJson = new LinkedHashMap<>();
        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put("status", HttpStatus.BAD_REQUEST.value());
        erroJson.put("erro", "Dados inválidos");
        erroJson.put(
            "mensagem",
            "Um ou mais campos possuem valores inválidos."
        );
        erroJson.put("campos", errosPorCampo);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(erroJson);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoInvalido(
        HttpMessageNotReadableException ex
    ) {
        Map<String, Object> erroJson = new LinkedHashMap<>();

        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put("status", HttpStatus.BAD_REQUEST.value());
        erroJson.put("erro", "Corpo da requisição inválido");
        erroJson.put("mensagem", extrairMensagemDaCausa(ex));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(erroJson);
    }

    private String extrairMensagemDaCausa(Throwable throwable) {
        Throwable causaAtual = throwable;

        while (causaAtual != null) {
            if (causaAtual instanceof IllegalArgumentException
                && causaAtual.getMessage() != null
                && !causaAtual.getMessage().isBlank()) {

                return causaAtual.getMessage();
            }

            causaAtual = causaAtual.getCause();
        }

        return "Verifique os tipos e valores informados no corpo da requisição.";
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

    @ExceptionHandler(TransacaoNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> tratarTransacaoNaoEncontrada(
        TransacaoNaoEncontradaException ex
    ) {
        Map<String, Object> erroJson = new LinkedHashMap<>();

        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put(
            "status",
            HttpStatus.NOT_FOUND.value()
        );
        erroJson.put("erro", "Transação não encontrada");
        erroJson.put("mensagem", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(erroJson);
    }

    @ExceptionHandler(AlteracaoHistoricoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> tratarAlteracaoHistoricoInvalida(
        AlteracaoHistoricoInvalidaException ex
    ) {
        Map<String, Object> erroJson = new LinkedHashMap<>();

        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put(
            "status",
            HttpStatus.CONFLICT.value()
        );
        erroJson.put("erro", "Conflito no histórico");
        erroJson.put("mensagem", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(erroJson);
    }

    @ExceptionHandler(HistoricoInconsistenteException.class)
    public ResponseEntity<Map<String, Object>> tratarHistoricoInconsistente(
        HistoricoInconsistenteException ex
    ) {
        Map<String, Object> erroJson = new LinkedHashMap<>();

        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put(
            "status",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        erroJson.put("erro", "Histórico inconsistente");
        erroJson.put("mensagem", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(erroJson);
    }


    @ExceptionHandler(LoginJaCadastradoException.class)
    public ResponseEntity<Map<String, Object>> tratarLoginJaCadastrado(
        LoginJaCadastradoException ex
    ) {
        Map<String, Object> erroJson = new LinkedHashMap<>();

        erroJson.put("timestamp", LocalDateTime.now());
        erroJson.put("status", HttpStatus.CONFLICT.value());
        erroJson.put("erro", "Login já cadastrado");
        erroJson.put("mensagem", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(erroJson);
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
