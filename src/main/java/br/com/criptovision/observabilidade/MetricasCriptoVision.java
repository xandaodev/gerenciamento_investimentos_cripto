package br.com.criptovision.observabilidade;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas de negócio, além das que o Spring já publica (requisições HTTP, JVM,
 * pool de conexões). São os sinais para configurar alertas numa ferramenta de
 * monitoramento: aparecem em {@code /actuator/metrics} e {@code /actuator/prometheus}
 * na porta de gerenciamento.
 *
 * <p>As tags têm poucos valores possíveis (nunca IDs, tickers ou logins), para que
 * o número de séries não cresça sem limite.
 */
@Component
public class MetricasCriptoVision {

    /** Respostas 500, por tipo de exceção. Qualquer valor acima de zero merece atenção. */
    public static final String ERROS_INTERNOS = "criptovision.erros.internos";

    /** Tentativas recusadas pela proteção contra força bruta, por limite. */
    public static final String TENTATIVAS_BLOQUEADAS = "criptovision.tentativas.bloqueadas";

    /** Falhas da Binance, pelo que a API fez: usou uma cotação anterior ou ficou sem cotação. */
    public static final String PROVEDOR_DE_COTACOES_INDISPONIVEL = "criptovision.cotacoes.provedor.indisponivel";

    private final MeterRegistry registro;

    public MetricasCriptoVision(MeterRegistry registro) {
        this.registro = registro;
    }

    public void erroInterno(Throwable erro) {
        contador(ERROS_INTERNOS, "excecao", erro.getClass().getSimpleName()).increment();
    }

    /** @param limite "login-por-ip", "falhas-por-login" ou "cadastro-por-ip" */
    public void tentativaBloqueada(String limite) {
        contador(TENTATIVAS_BLOQUEADAS, "limite", limite).increment();
    }

    public void cotacaoAnteriorUsada() {
        contador(PROVEDOR_DE_COTACOES_INDISPONIVEL, "resultado", "cotacao_anterior").increment();
    }

    public void semCotacao() {
        contador(PROVEDOR_DE_COTACOES_INDISPONIVEL, "resultado", "sem_cotacao").increment();
    }

    private Counter contador(String nome, String tag, String valor) {
        return Counter.builder(nome).tag(tag, valor).register(registro);
    }
}
