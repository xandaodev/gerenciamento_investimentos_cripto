package br.com.criptovision.controller;

import br.com.criptovision.config.OpenApiConfiguration;
import br.com.criptovision.dto.ProblemaApiDTO;
import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.service.CarteiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
@Tag(
    name = "Transações",
    description = "Cadastro, consulta, alteração e exclusão de transações."
)
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class TransacaoController {

    @Autowired
    private CarteiraService carteiraService;

    @GetMapping
    @Operation(
        summary = "Listar transações",
        description = "Retorna somente as transações pertencentes ao usuário autenticado.",
        operationId = "listarTransacoes"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Histórico retornado com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(implementation = Transacao.class)
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente, inválido ou expirado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public List<Transacao> listarTodas(
        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.listarTransacoes(usuario);
    }

    @PostMapping
    @Operation(
        summary = "Cadastrar transação",
        description = "Registra uma compra ou venda para o usuário autenticado.",
        operationId = "cadastrarTransacao",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados da operação.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransacaoRequestDTO.class),
                examples = {
                    @ExampleObject(
                        name = "Compra de Bitcoin",
                        value = """
                            {
                              "ticker": "BTC",
                              "quantidade": 0.015,
                              "precoUnitario": 64000.00,
                              "tipo": "COMPRA"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Venda de Ethereum",
                        value = """
                            {
                              "ticker": "ETH",
                              "quantidade": 0.25,
                              "precoUnitario": 3500.00,
                              "tipo": "VENDA"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Transação registrada com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Transacao.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos, ticker inexistente ou saldo insuficiente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente, inválido ou expirado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<Transacao> salvar(
        @AuthenticationPrincipal Usuario usuario,
        @Valid @RequestBody TransacaoRequestDTO dados
    ) {
        Transacao transacaoSalva =
            carteiraService.registrarNovaTransacao(
                dados.toEntity(),
                usuario
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(transacaoSalva);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar transação por ID",
        description = "Retorna uma transação do usuário autenticado.",
        operationId = "buscarTransacaoPorId"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transação encontrada.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Transacao.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente, inválido ou expirado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transação não encontrada para este usuário.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<Transacao> buscarPorId(
        @PathVariable("id") Long id,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
            carteiraService.buscarTransacaoPorId(
                id,
                usuario
            )
        );
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar transação",
        description = "Substitui os dados de uma transação sem permitir que o histórico fique inconsistente.",
        operationId = "atualizarTransacao",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Novos dados da operação.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransacaoRequestDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "ticker": "BTC",
                          "quantidade": 0.020,
                          "precoUnitario": 62500.00,
                          "tipo": "COMPRA"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transação atualizada com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Transacao.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou ticker inexistente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente, inválido ou expirado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transação não encontrada para este usuário.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "A alteração tornaria o histórico inconsistente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<Transacao> atualizar(
        @PathVariable("id") Long id,
        @AuthenticationPrincipal Usuario usuario,
        @Valid @RequestBody TransacaoRequestDTO dados
    ) {
        return ResponseEntity.ok(
            carteiraService.atualizarTransacao(
                id,
                dados,
                usuario
            )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Excluir transação",
        description = "Exclui uma transação quando a remoção não torna o histórico inconsistente.",
        operationId = "excluirTransacao"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Transação excluída com sucesso."
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente, inválido ou expirado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transação não encontrada para este usuário.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "A exclusão tornaria o histórico inconsistente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<Void> deletar(
        @PathVariable("id") Long id,
        @AuthenticationPrincipal Usuario usuario
    ) {
        carteiraService.excluirTransacao(
            id,
            usuario
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}
