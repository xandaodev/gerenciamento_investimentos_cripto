package br.com.criptovision.controller;

import br.com.criptovision.dto.DadosAutenticacao;
import br.com.criptovision.dto.TokenJwtDTO;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity efetuarLogin(@RequestBody DadosAutenticacao dados) {
        // encapsula o login e senha recebidos
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());

        // o Spring vai no banco e testa se a senha bate
        var authentication = manager.authenticate(authenticationToken);

        var usuario = (Usuario) authentication.getPrincipal();
        var tokenJWT = tokenService.gerarToken(usuario.getLogin());

        //devolve o JSON com o token dentro
        return ResponseEntity.ok(new TokenJwtDTO(tokenJWT));
    }
}