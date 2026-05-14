package br.com.criptovision.repository;

import br.com.criptovision.model.Transacao;
import java.util.List;

// classe de contrato, qualquer classe que quiser lidar com Transações terá obrigatoriamente que implementar esses métodos
public interface TransacaoDAO {
    
    void salvar(Transacao transacao);
    
    List<Transacao> lerTudo();
    
    //  quando a interface web tiver pronta, vai precisar de atualizar e apagar registos:
    // void atualizar(Transacao transacao);
    // void apagar(Long id);
}