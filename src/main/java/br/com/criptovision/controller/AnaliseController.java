package br.com.criptovision.controller;

import br.com.criptovision.dto.AportePorMoedaProjection;
import br.com.criptovision.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analise")
public class AnaliseController {

    @Autowired
    private TransacaoRepository repository;

    @GetMapping("/aportes-por-moeda")
    public List<AportePorMoedaProjection> obterAportesPorMoeda() {
        // Chamamos a query poderosa que acabamos de criar!
        return repository.calcularTotalAportadoPorMoeda();
    }
}