package br.com.criptovision.exception;

import br.com.criptovision.dto.ErroCampoDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> errosUnicos =
            new LinkedHashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(erro -> errosUnicos.putIfAbsent(
                erro.getField(),
                erro.getDefaultMessage()
            ));

        List<ErroCampoDTO> erros = new ArrayList<>();

        errosUnicos.forEach((campo, mensagem) ->
            erros.add(new ErroCampoDTO(campo, mensagem))
        );

        ProblemDetail problema = criarProblema(
            HttpStatus.BAD_REQUEST,
            "Dados inválidos",
            "Um ou mais campos possuem valores inválidos.",
            request
        );

        problema.setProperty("erros", erros);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problema);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> tratarCorpoInvalido(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.BAD_REQUEST,
            "Corpo da requisição inválido",
            extrairMensagemDaCausa(ex),
            request
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problema);
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> tratarParametroInvalido(
        Exception ex,
        HttpServletRequest request
    ) {
        String detalhe =
            "Verifique os parâmetros enviados na requisição.";

        if (ex instanceof MissingServletRequestParameterException missing) {
            detalhe = "O parâmetro '" + missing.getParameterName()
                + "' é obrigatório.";
        }

        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            detalhe = "O parâmetro '" + mismatch.getName()
                + "' possui um valor inválido.";
        }

        ProblemDetail problema = criarProblema(
            HttpStatus.BAD_REQUEST,
            "Parâmetro inválido",
            detalhe,
            request
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problema);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> tratarArgumentoInvalido(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.BAD_REQUEST,
            "Dados inválidos",
            mensagemSegura(
                ex,
                "Os dados informados são inválidos."
            ),
            request
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problema);
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ProblemDetail> tratarSaldoInsuficiente(
        SaldoInsuficienteException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.BAD_REQUEST,
            "Saldo insuficiente",
            ex.getMessage(),
            request
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problema);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> tratarFalhaDeAutenticacao(
        AuthenticationException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.UNAUTHORIZED,
            "Não autorizado",
            "Login ou senha inválidos.",
            request
        );

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(problema);
    }

    @ExceptionHandler({
        TransacaoNaoEncontradaException.class,
        ArquivoNaoEncontradoException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<ProblemDetail> tratarRecursoNaoEncontrado(
        Exception ex,
        HttpServletRequest request
    ) {
        String detalhe = ex instanceof NoResourceFoundException
            ? "O recurso solicitado não foi encontrado."
            : mensagemSegura(
                ex,
                "O recurso solicitado não foi encontrado."
            );

        ProblemDetail problema = criarProblema(
            HttpStatus.NOT_FOUND,
            "Recurso não encontrado",
            detalhe,
            request
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(problema);
    }

    @ExceptionHandler(LoginJaCadastradoException.class)
    public ResponseEntity<ProblemDetail> tratarLoginJaCadastrado(
        LoginJaCadastradoException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.CONFLICT,
            "Login já cadastrado",
            ex.getMessage(),
            request
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problema);
    }

    @ExceptionHandler(AlteracaoHistoricoInvalidaException.class)
    public ResponseEntity<ProblemDetail> tratarConflitoNoHistorico(
        AlteracaoHistoricoInvalidaException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.CONFLICT,
            "Conflito no histórico",
            ex.getMessage(),
            request
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problema);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> tratarMetodoNaoPermitido(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Método não permitido",
            "O método HTTP utilizado não é aceito para este recurso.",
            request
        );

        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(problema);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> tratarTipoDeConteudoInvalido(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problema = criarProblema(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Tipo de conteúdo não suportado",
            "Envie a requisição utilizando um tipo de conteúdo suportado.",
            request
        );

        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(problema);
    }

    @ExceptionHandler({
        HistoricoInconsistenteException.class,
        BancoDeDadosException.class
    })
    public ResponseEntity<ProblemDetail> tratarErroInternoConhecido(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Erro interno ao processar a requisição {}",
            request.getRequestURI(),
            ex
        );

        ProblemDetail problema = criarProblema(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno do servidor",
            "Não foi possível concluir a operação.",
            request
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarErroGenerico(
        Exception ex,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Erro inesperado ao processar a requisição {}",
            request.getRequestURI(),
            ex
        );

        ProblemDetail problema = criarProblema(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno do servidor",
            "Ocorreu um erro inesperado. Tente novamente mais tarde.",
            request
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(problema);
    }

    private ProblemDetail criarProblema(
        HttpStatus status,
        String titulo,
        String detalhe,
        HttpServletRequest request
    ) {
        ProblemDetail problema =
            ProblemDetail.forStatusAndDetail(status, detalhe);

        problema.setType(URI.create("about:blank"));
        problema.setTitle(titulo);
        problema.setInstance(
            URI.create(request.getRequestURI())
        );
        problema.setProperty(
            "dataHora",
            OffsetDateTime.now(ZoneOffset.UTC)
        );

        return problema;
    }

    private String extrairMensagemDaCausa(
        Throwable throwable
    ) {
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

    private String mensagemSegura(
        Throwable throwable,
        String mensagemPadrao
    ) {
        if (throwable.getMessage() == null
            || throwable.getMessage().isBlank()) {
            return mensagemPadrao;
        }

        return throwable.getMessage();
    }
}
