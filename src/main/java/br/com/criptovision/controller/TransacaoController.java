package br.com.criptovision.controller;

import br.com.criptovision.model.Transacao;
import br.com.criptovision.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoRepository repository;

    @GetMapping
    public List<Transacao> listarTodas() {
        // vai no banco de dados, faz um select *, transforma tudo em lista e devolve
        return repository.findAll();
    }
}