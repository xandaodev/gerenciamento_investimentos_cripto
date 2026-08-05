package br.com.criptovision.controller;

import br.com.criptovision.dto.AportePorMoedaProjection;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public List<AportePorMoedaProjection> obterAportesPorMoeda(
        @AuthenticationPrincipal Usuario usuario
    ) {
        return repository.calcularTotalAportadoPorMoeda(
            TipoTransacao.COMPRA,
            usuario
        );
    }
}
