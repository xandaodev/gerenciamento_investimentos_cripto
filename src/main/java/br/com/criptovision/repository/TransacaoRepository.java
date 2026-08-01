package br.com.criptovision.repository;

import br.com.criptovision.dto.AportePorMoedaProjection;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findAllByUsuarioOrderByDataAscIdAsc(
        Usuario usuario
    );

    Optional<Transacao> findByIdAndUsuario(
        Long id,
        Usuario usuario
    );

    // calcula o total de dólares aportados, agrupado por moeda
    @Query("""
        SELECT
            t.ticker AS ticker,
            SUM(t.quantidade * t.precoUnitario) AS totalAportado
        FROM Transacao t
        WHERE t.tipo = :tipo
        GROUP BY t.ticker
        """)
    List<AportePorMoedaProjection> calcularTotalAportadoPorMoeda(
        @Param("tipo") TipoTransacao tipo
    );

}
