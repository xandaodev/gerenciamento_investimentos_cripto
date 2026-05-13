package br.com.criptovision.controller;

import br.com.criptovision.service.CarteiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}