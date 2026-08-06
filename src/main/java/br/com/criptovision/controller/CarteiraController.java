package br.com.criptovision.controller;

import br.com.criptovision.config.OpenApiConfiguration;
import br.com.criptovision.dto.ProblemaApiDTO;
import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.dto.SimulacaoDCADTO;
import br.com.criptovision.dto.SimulacaoVendaDTO;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.service.CarteiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carteira")
@Tag(
    name = "Carteira",
    description = "Resumo, patrimônio e simulações da carteira autenticada."
)
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class CarteiraController {

    @Autowired
    private CarteiraService carteiraService;

    @GetMapping("/total")
    @Operation(
        summary = "Consultar patrimônio total investido",
        description = "Calcula o total líquido das compras e vendas registradas.",
        operationId = "obterPatrimonioTotal"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Patrimônio calculado com sucesso.",
            content = @Content(
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                examples = @ExampleObject(
                    value = "Patrimônio Total Investido: R$ 12500.0"
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
    public String obterTotal(
        @AuthenticationPrincipal Usuario usuario
    ) {
        double total =
            carteiraService.calcularPatrimonioTotal(usuario);

        return "Patrimônio Total Investido: R$ " + total;
    }

    @GetMapping("/resumo")
    @Operation(
        summary = "Obter resumo da carteira",
        description = "Reconstrói a carteira e retorna valores atuais, PNL e variação em 24 horas. Quando uma cotação falha, preserva os demais ativos e informa dados parciais.",
        operationId = "obterResumoCarteira"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resumo calculado com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ResumoCarteiraDTO.class)
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
            responseCode = "500",
            description = "Não foi possível reconstruir ou calcular a carteira.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResumoCarteiraDTO obterResumo(
        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.obterResumoGeral(usuario);
    }

    @GetMapping("/simulador/dca")
    @Operation(
        summary = "Simular aporte DCA",
        description = "Estima a quantidade comprada e o novo preço médio após um aporte hipotético.",
        operationId = "simularAporteDca"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Simulação realizada com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SimulacaoDCADTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros ausentes ou com tipos inválidos.",
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
            responseCode = "500",
            description = "O histórico da carteira está inconsistente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public SimulacaoDCADTO simularAporteDCA(
        @Parameter(
            description = "Ticker do ativo.",
            example = "BTC",
            required = true
        )
        @RequestParam String ticker,

        @Parameter(
            description = "Valor hipotético do novo aporte em USD.",
            example = "500.00",
            required = true
        )
        @RequestParam double aporte,

        @Parameter(
            description = "Preço de mercado utilizado na simulação.",
            example = "65000.00",
            required = true
        )
        @RequestParam double preco,

        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.executarSimulacaoDCA(
            ticker,
            aporte,
            preco,
            usuario
        );
    }

    @GetMapping("/simulador/venda")
    @Operation(
        summary = "Simular venda futura",
        description = "Estima o resultado de vender toda a posição de um ativo em um preço-alvo.",
        operationId = "simularVendaFutura"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Simulação realizada com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SimulacaoVendaDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros ausentes ou com tipos inválidos.",
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
            responseCode = "500",
            description = "O histórico da carteira está inconsistente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "O serviço de cotações está temporariamente indisponível.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public SimulacaoVendaDTO simularVendaFutura(
        @Parameter(
            description = "Ticker do ativo.",
            example = "BTC",
            required = true
        )
        @RequestParam String ticker,

        @Parameter(
            description = "Preço-alvo hipotético em USD.",
            example = "72000.00",
            required = true
        )
        @RequestParam double precoAlvo,

        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.executarSimulacaoVenda(
            ticker,
            precoAlvo,
            usuario
        );
    }
}
