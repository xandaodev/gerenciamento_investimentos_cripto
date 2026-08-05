package br.com.criptovision.controller;

import br.com.criptovision.config.OpenApiConfiguration;
import br.com.criptovision.dto.AportePorMoedaProjection;
import br.com.criptovision.dto.ProblemaApiDTO;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analise")
@Tag(
    name = "Análises",
    description = "Indicadores agregados das transações do usuário autenticado."
)
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AnaliseController {

    @Autowired
    private TransacaoRepository repository;

    @GetMapping("/aportes-por-moeda")
    @Operation(
        summary = "Calcular aportes por moeda",
        description = "Agrupa as compras do usuário e soma o valor aportado em cada ativo.",
        operationId = "obterAportesPorMoeda"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Aportes agrupados com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(
                        implementation = AportePorMoedaProjection.class
                    )
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
    public List<AportePorMoedaProjection> obterAportesPorMoeda(
        @AuthenticationPrincipal Usuario usuario
    ) {
        return repository.calcularTotalAportadoPorMoeda(
            TipoTransacao.COMPRA,
            usuario
        );
    }
}
