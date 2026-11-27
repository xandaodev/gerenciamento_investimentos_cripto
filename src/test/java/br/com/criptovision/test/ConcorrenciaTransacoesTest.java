package br.com.criptovision.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Executa o {@link CenarioConcorrenciaTransacoes} no H2, em todo {@code mvn test}.
 * A versão contra o MySQL real é o {@code ConcorrenciaTransacoesMysqlIT}.
 */
@SpringBootTest(
    properties = {
        // tempo de espera pelo bloqueio maior que o tempo que o teste o segura
        "spring.datasource.url=jdbc:h2:mem:criptovision-test;MODE=MySQL;"
            + "DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000"
    }
)
@ActiveProfiles("test")
public class ConcorrenciaTransacoesTest extends CenarioConcorrenciaTransacoes {
}
