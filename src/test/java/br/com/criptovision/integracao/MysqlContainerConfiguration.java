package br.com.criptovision.integracao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * MySQL real em container para os testes de integração (*IT.java).
 *
 * <p>O {@code @ServiceConnection} entrega a URL, o usuário e a senha do container
 * ao DataSource e ao Flyway, sem nenhuma configuração nos properties.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MysqlContainerConfiguration {

    // mesma imagem do docker-compose.yml, para testar o banco que vai para produção
    private static final String IMAGEM_MYSQL = "mysql:8.0";

    // um único container para a JVM inteira: classes de teste com contextos
    // Spring diferentes compartilham o mesmo banco em vez de subir um MySQL cada
    private static final MySQLContainer MYSQL =
        new MySQLContainer(IMAGEM_MYSQL)
            .withDatabaseName("criptovision");

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return MYSQL;
    }
}
