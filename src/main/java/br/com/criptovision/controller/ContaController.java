package br.com.criptovision.controller;

import br.com.criptovision.config.OpenApiConfiguration;
import br.com.criptovision.dto.DadosExclusaoConta;
import br.com.criptovision.dto.ExportacaoDadosDTO;
import br.com.criptovision.dto.ProblemaApiDTO;
import br.com.criptovision.exception.SenhaDeConfirmacaoIncorretaException;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.security.ProtecaoForcaBruta;
import br.com.criptovision.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conta")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(
    name = "Conta",
    description = "Direitos do titular dos dados (LGPD): exportação dos dados e exclusão da conta."
)
public class ContaController {

    private final ContaService contaService;
    private final ProtecaoForcaBruta protecaoForcaBruta;

    public ContaController(
        ContaService contaService,
        ProtecaoForcaBruta protecaoForcaBruta
    ) {
        this.contaService = contaService;
        this.protecaoForcaBruta = protecaoForcaBruta;
    }

    @GetMapping("/exportacao")
    @Operation(
        summary = "Exportar os dados",
        description = "Devolve, num arquivo JSON, todos os dados guardados sobre o usuário: "
            + "a conta e o histórico completo de transações (acesso e portabilidade, LGPD art. 18).",
        operationId = "exportarDados"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Arquivo JSON com todos os dados do usuário.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ExportacaoDadosDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente, inválido, expirado ou já revogado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<ExportacaoDadosDTO> exportarDados(
        @AuthenticationPrincipal Usuario usuario
    ) {
        ExportacaoDadosDTO dados = contaService.exportarDados(usuario);

        // o navegador baixa como arquivo, em vez de abrir o JSON na aba
        String arquivo = "criptovision-dados-" + dados.exportadoEm().toLocalDate() + ".json";

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(arquivo).build().toString()
            )
            .contentType(MediaType.APPLICATION_JSON)
            .body(dados);
    }

    @PostMapping("/exclusao")
    @Operation(
        summary = "Excluir a conta",
        description = "Exclui a conta e todo o histórico de transações, de forma definitiva, "
            + "depois de conferir a senha atual. Nenhum token continua válido. Os backups "
            + "cifrados que ainda contêm a conta são apagados no prazo da política de privacidade.",
        operationId = "excluirConta"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Conta e transações excluídas."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Senha ausente ou incorreta. Nada foi excluído.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente, inválido, expirado ou já revogado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Muitas tentativas com a senha errada. "
                + "O cabeçalho Retry-After informa em quantos segundos tentar de novo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<Void> excluirConta(
        @AuthenticationPrincipal Usuario usuario,
        @Valid @RequestBody DadosExclusaoConta dados,
        HttpServletRequest request
    ) {
        // a senha errada conta como falha de login, como na troca de senha
        protecaoForcaBruta.verificarLogin(request.getRemoteAddr(), usuario.getLogin());

        try {
            contaService.excluirConta(usuario, dados);
        } catch (SenhaDeConfirmacaoIncorretaException ex) {
            protecaoForcaBruta.registrarFalhaDeLogin(usuario.getLogin());
            throw ex;
        }

        protecaoForcaBruta.registrarLoginComSucesso(usuario.getLogin());
        return ResponseEntity.noContent().build();
    }
}
