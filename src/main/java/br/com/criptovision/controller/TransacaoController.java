package br.com.criptovision.controller;

import br.com.criptovision.model.Transacao;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.service.CarteiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoRepository repository;

    @Autowired
    private CarteiraService carteiraService;

    @GetMapping
    public List<Transacao> listarTodas() {
        return repository.findAll();
    }

    @PostMapping
    public Transacao salvar(@RequestBody Transacao novaTransacao) {

        return carteiraService.registrarNovaTransacao(novaTransacao);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transacao> buscarPorId(@PathVariable Long id) {

        Optional<Transacao> transacao = repository.findById(id);

        if (transacao.isPresent()) {
            return ResponseEntity.ok(transacao.get());
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transacao> atualizar(@PathVariable Long id, @RequestBody Transacao transacaoAtualizada) {
        Optional<Transacao> transacaoExistente = repository.findById(id);

        if (transacaoExistente.isPresent()) {
            Transacao transacaoSalva = transacaoExistente.get();

            transacaoSalva.setTicker(transacaoAtualizada.getTicker());
            transacaoSalva.setQuantidade(transacaoAtualizada.getQuantidade());
            transacaoSalva.setPrecoUnitario(transacaoAtualizada.getPrecoUnitario());
            transacaoSalva.setTipo(transacaoAtualizada.getTipo());


            repository.save(transacaoSalva);

            return ResponseEntity.ok(transacaoSalva);
        }

        return ResponseEntity.notFound().build();
    }
}