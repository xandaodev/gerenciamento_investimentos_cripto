package br.com.criptovision.controller;

import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.dto.SimulacaoDCADTO;
import br.com.criptovision.dto.SimulacaoVendaDTO;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.service.CarteiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carteira")
public class CarteiraController {

    @Autowired
    private CarteiraService carteiraService;

    @GetMapping("/total")
    public String obterTotal(
        @AuthenticationPrincipal Usuario usuario
    ) {
        double total =
            carteiraService.calcularPatrimonioTotal(usuario);

        return "Patrimônio Total Investido: R$ " + total;
    }

    @GetMapping("/resumo")
    public ResumoCarteiraDTO obterResumo(
        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.obterResumoGeral(usuario);
    }

    @GetMapping("/simulador/dca")
    public SimulacaoDCADTO simularAporteDCA(
        @RequestParam String ticker,
        @RequestParam double aporte,
        @RequestParam double preco,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.executarSimulacaoDCA(
            ticker,
            aporte,
            preco,
            usuario
        );
    }

    @GetMapping("/simulador/venda")
    public SimulacaoVendaDTO simularVendaFutura(
        @RequestParam String ticker,
        @RequestParam double precoAlvo,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return carteiraService.executarSimulacaoVenda(
            ticker,
            precoAlvo,
            usuario
        );
    }
}
