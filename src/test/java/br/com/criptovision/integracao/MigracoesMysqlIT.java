package br.com.criptovision.integracao;

import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a aplicação contra um MySQL 8 real, com o schema criado pelas migrations
 * do Flyway e o Hibernate em {@code ddl-auto=validate}, como em produção.
 *
 * <p>Se uma migration divergir das entidades, o contexto não sobe e todos os
 * testes desta classe falham, em vez de o erro aparecer só no deploy.
 */
@SpringBootTest
@ActiveProfiles("integracao")
@Import(MysqlContainerConfiguration.class)
class MigracoesMysqlIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @BeforeEach
    void limparBanco() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() {
        MigrationInfo[] todas = flyway.info().all();
        MigrationInfo[] aplicadas = flyway.info().applied();

        assertThat(todas).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(aplicadas).hasSameSizeAs(todas);
        assertThat(aplicadas)
            .allSatisfy(migration ->
                assertThat(migration.getState().isFailed()).isFalse()
            );
    }

    @Test
    void deveCriarColunasDeValoresComDezoitoCasasDecimais() {
        // o ddl-auto=validate do Hibernate não confere precisão nem escala,
        // então a garantia do V3 (DECIMAL(38, 18)) é verificada aqui
        List<Map<String, Object>> colunas = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME AS coluna,
                   NUMERIC_PRECISION AS precisao,
                   NUMERIC_SCALE AS escala
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'transacoes'
              AND COLUMN_NAME IN ('quantidade', 'preco_unitario')
            """);

        assertThat(colunas).hasSize(2);
        assertThat(colunas).allSatisfy(coluna -> {
            assertThat(((Number) coluna.get("precisao")).intValue())
                .as("precisão de %s", coluna.get("coluna"))
                .isEqualTo(38);
            assertThat(((Number) coluna.get("escala")).intValue())
                .as("escala de %s", coluna.get("coluna"))
                .isEqualTo(18);
        });
    }

    @Test
    void deveGuardarValoresNoLimiteDaValidacaoSemPerderPrecisao() {
        // 20 dígitos inteiros e 18 casas decimais: o máximo aceito pelo TransacaoRequestDTO
        BigDecimal quantidadeMaxima =
            new BigDecimal("12345678901234567890.123456789012345678");
        BigDecimal menorPreco = new BigDecimal("0.000000000000000001");

        Usuario usuario = usuarioRepository.saveAndFlush(
            new Usuario("alexandre", "senha-criptografada")
        );

        Transacao transacao = new Transacao(
            "PEPE",
            quantidadeMaxima,
            menorPreco,
            TipoTransacao.COMPRA
        );
        transacao.setUsuario(usuario);
        Long id = transacaoRepository.saveAndFlush(transacao).getId();

        // a busca roda em outra transação, então os valores vêm do MySQL
        Transacao lida = transacaoRepository.findById(id).orElseThrow();

        assertThat(lida.getQuantidade()).isEqualByComparingTo(quantidadeMaxima);
        assertThat(lida.getPrecoUnitario()).isEqualByComparingTo(menorPreco);
    }
}
