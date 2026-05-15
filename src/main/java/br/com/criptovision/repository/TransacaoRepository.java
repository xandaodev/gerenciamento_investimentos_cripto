package br.com.criptovision.repository;

import br.com.criptovision.model.Transacao;
import br.com.criptovision.dto.AportePorMoedaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    // calcula o total de dólares aportados, agrupado por moeda
    @Query("SELECT t.ticker as ticker, SUM(t.quantidade * t.precoUnitario) as totalAportado " +
            "FROM Transacao t " +
            "WHERE t.tipo = 'COMPRA' " +
            "GROUP BY t.ticker")
    List<AportePorMoedaProjection> calcularTotalAportadoPorMoeda();

}