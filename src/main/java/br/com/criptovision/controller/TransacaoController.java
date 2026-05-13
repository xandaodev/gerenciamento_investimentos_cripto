package br.com.criptovision.controller;

import br.com.criptovision.model.Transacao;
import br.com.criptovision.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoRepository repository;

    @GetMapping
    public List<Transacao> listarTodas() {
        return repository.findAll();
    }

    @PostMapping
    public Transacao salvar(@RequestBody Transacao novaTransacao) {

        return repository.save(novaTransacao);
    }
}