package br.com.criptovision.controller;

import br.com.criptovision.dto.DadosAutenticacao;
import br.com.criptovision.dto.DadosCadastroUsuario;
import br.com.criptovision.dto.ProblemaApiDTO;
import br.com.criptovision.dto.TokenJwtDTO;
import br.com.criptovision.dto.UsuarioCadastradoDTO;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.security.ProtecaoForcaBruta;
import br.com.criptovision.security.TokenService;
import br.com.criptovision.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(
    name = "Autenticação",
    description = "Cadastro de usuários e emissão de tokens JWT."
)
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;
    private final ProtecaoForcaBruta protecaoForcaBruta;

    public AutenticacaoController(
        AuthenticationManager manager,
        TokenService tokenService,
        UsuarioService usuarioService,
        ProtecaoForcaBruta protecaoForcaBruta
    ) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.usuarioService = usuarioService;
        this.protecaoForcaBruta = protecaoForcaBruta;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Autenticar usuário",
        description = "Valida as credenciais e retorna um token JWT para acesso às rotas protegidas.",
        operationId = "efetuarLogin",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Credenciais do usuário.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DadosAutenticacao.class),
                examples = @ExampleObject(
                    name = "Login válido",
                    value = """
                        {
                          "login": "alexandre",
                          "senha": "SenhaSegura@2026"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Autenticação realizada com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TokenJwtDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.exemplo.assinatura"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Campos obrigatórios ausentes ou inválidos.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Login ou senha inválidos.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Muitas tentativas a partir deste IP ou para este login. "
                + "O cabeçalho Retry-After informa em quantos segundos tentar de novo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<TokenJwtDTO> efetuarLogin(
        @Valid @RequestBody DadosAutenticacao dados,
        HttpServletRequest request
    ) {
        protecaoForcaBruta.verificarLogin(
            request.getRemoteAddr(),
            dados.login()
        );

        var authenticationToken =
            new UsernamePasswordAuthenticationToken(
                dados.login(),
                dados.senha()
            );

        Authentication authentication;

        try {
            authentication = manager.authenticate(authenticationToken);
        } catch (BadCredentialsException ex) {
            // login inexistente também chega aqui (o Spring esconde a diferença)
            protecaoForcaBruta.registrarFalhaDeLogin(dados.login());
            throw ex;
        }

        protecaoForcaBruta.registrarLoginComSucesso(dados.login());

        var usuario =
            (Usuario) authentication.getPrincipal();

        var tokenJWT =
            tokenService.gerarToken(usuario.getLogin());

        return ResponseEntity.ok(
            new TokenJwtDTO(tokenJWT)
        );
    }

    @PostMapping("/register")
    @Operation(
        summary = "Cadastrar usuário",
        description = "Cria uma conta com login único e senha armazenada de forma segura.",
        operationId = "cadastrarUsuario",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados da nova conta.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DadosCadastroUsuario.class),
                examples = @ExampleObject(
                    name = "Novo usuário",
                    value = """
                        {
                          "login": "alexandre.dev",
                          "senha": "SenhaSegura@2026"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Usuário criado com sucesso.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UsuarioCadastradoDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "id": 1,
                          "login": "alexandre.dev"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de cadastro inválidos.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "O login informado já está cadastrado.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Muitos cadastros a partir deste IP. "
                + "O cabeçalho Retry-After informa em quantos segundos tentar de novo.",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemaApiDTO.class)
            )
        )
    })
    public ResponseEntity<UsuarioCadastradoDTO> cadastrar(
        @Valid @RequestBody DadosCadastroUsuario dados,
        HttpServletRequest request
    ) {
        // conta também os cadastros recusados com 409, que revelariam
        // quais logins existem se pudessem ser feitos à vontade
        protecaoForcaBruta.verificarCadastro(request.getRemoteAddr());

        Usuario usuario = usuarioService.cadastrar(dados);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new UsuarioCadastradoDTO(usuario));
    }
}
