package br.com.criptovision.controller;

import br.com.criptovision.dto.ResumoCarteiraDTO;
import br.com.criptovision.service.CarteiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.criptovision.dto.SimulacaoDCADTO;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.criptovision.dto.SimulacaoVendaDTO;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/carteira")
public class CarteiraController {

    @Autowired
    private CarteiraService carteiraService;

    @GetMapping("/total")
    public String obterTotal() {
        double total = carteiraService.calcularPatrimonioTotal();
        return "Patrimônio Total Investido: R$ " + total;
    }

    @GetMapping("/resumo")
    public ResumoCarteiraDTO obterResumo(){
        return carteiraService.obterResumoGeral();
    }

    @GetMapping("/simulador/dca")
    public SimulacaoDCADTO simularAporteDCA(
            @RequestParam String ticker,
            @RequestParam double aporte,
            @RequestParam double preco) {
        return carteiraService.executarSimulacaoDCA(ticker, aporte, preco);
    }

    @GetMapping("/simulador/venda")
    public SimulacaoVendaDTO simularVendaFutura(
            @RequestParam String ticker,
            @RequestParam double precoAlvo) {
        return carteiraService.executarSimulacaoVenda(ticker, precoAlvo);
    }
}