package br.com.criptovision.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A conexão entre a API e o MySQL usa TLS, como no docker-compose: o servidor
 * exige transporte seguro ({@code require_secure_transport=ON}) e a API conecta
 * com {@code sslMode=REQUIRED}, sem {@code allowPublicKeyRetrieval}.
 */
@SpringBootTest
@ActiveProfiles("integracao")
@Import(MysqlContainerConfiguration.class)
class ConexaoSeguraMysqlIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MySQLContainer mysql;

    @Test
    void deveConectarComTlsPeloPoolDaAplicacao() {
        // a conexão vem do pool da aplicação (Hikari), a mesma de produção
        assertThat(statusDaSessao("Ssl_cipher")).isNotBlank();
        assertThat(statusDaSessao("Ssl_version")).startsWith("TLSv1.");
    }

    @Test
    void deveRecusarConexaoSemTls() {
        // allowPublicKeyRetrieval=true aqui só para que a recusa venha da exigência
        // de TLS do servidor, e não da falta da chave pública
        String urlSemTls = "jdbc:mysql://" + mysql.getHost() + ":"
            + mysql.getMappedPort(MySQLContainer.MYSQL_PORT) + "/"
            + mysql.getDatabaseName()
            + "?sslMode=DISABLED&allowPublicKeyRetrieval=true";

        assertThatThrownBy(() ->
            DriverManager.getConnection(
                urlSemTls,
                mysql.getUsername(),
                mysql.getPassword()
            ).close()
        )
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("insecure transport");
    }

    private String statusDaSessao(String variavel) {
        return jdbcTemplate.queryForObject(
            "SHOW SESSION STATUS LIKE ?",
            (resultado, linha) -> resultado.getString("Value"),
            variavel
        );
    }
}
