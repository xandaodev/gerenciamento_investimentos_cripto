package br.com.criptovision.controller;

import br.com.criptovision.dto.TransacaoRequestDTO;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.service.CarteiraService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    @Autowired
    private CarteiraService carteiraService;

    @GetMapping
    public List<Transacao> listarTodas(
        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.listarTransacoes(usuario);
    }

    @PostMapping
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
