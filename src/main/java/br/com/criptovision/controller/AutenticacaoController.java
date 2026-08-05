package br.com.criptovision.controller;

import br.com.criptovision.dto.DadosAutenticacao;
import br.com.criptovision.dto.DadosCadastroUsuario;
import br.com.criptovision.dto.TokenJwtDTO;
import br.com.criptovision.dto.UsuarioCadastradoDTO;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.security.TokenService;
import br.com.criptovision.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;

    public AutenticacaoController(
        AuthenticationManager manager,
        TokenService tokenService,
        UsuarioService usuarioService
    ) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenJwtDTO> efetuarLogin(
        @RequestBody DadosAutenticacao dados
    ) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(
            dados.login(),
            dados.senha()
        );

        var authentication = manager.authenticate(authenticationToken);
        var usuario = (Usuario) authentication.getPrincipal();
        var tokenJWT = tokenService.gerarToken(usuario.getLogin());

        return ResponseEntity.ok(new TokenJwtDTO(tokenJWT));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioCadastradoDTO> cadastrar(
        @Valid @RequestBody DadosCadastroUsuario dados
    ) {
        Usuario usuario = usuarioService.cadastrar(dados);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new UsuarioCadastradoDTO(usuario));
    }
}
