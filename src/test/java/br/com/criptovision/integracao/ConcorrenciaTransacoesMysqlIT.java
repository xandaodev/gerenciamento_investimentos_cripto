package br.com.criptovision.integracao;

import br.com.criptovision.test.CenarioConcorrenciaTransacoes;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Executa o {@link CenarioConcorrenciaTransacoes} contra o MySQL real, sob o
 * isolamento padrão do InnoDB (REPEATABLE READ). Se alguma leitura comum passar
 * a acontecer antes do {@code SELECT ... FOR UPDATE}, a segunda venda enxerga o
 * snapshot antigo, também é aceita, e este teste falha.
 */
@SpringBootTest
@ActiveProfiles("integracao")
@Import(MysqlContainerConfiguration.class)
class ConcorrenciaTransacoesMysqlIT extends CenarioConcorrenciaTransacoes {
}
