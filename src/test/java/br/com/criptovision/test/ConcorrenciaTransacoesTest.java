package br.com.criptovision.test;

import br.com.criptovision.cotacao.service.CotacaoService;
import br.com.criptovision.exception.SaldoInsuficienteException;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import br.com.criptovision.service.CarteiraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        // tempo de espera pelo bloqueio maior que o tempo que o teste o segura
        "spring.datasource.url=jdbc:h2:mem:criptovision-test;MODE=MySQL;"
            + "DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000"
    }
)
@ActiveProfiles("test")
public class ConcorrenciaTransacoesTest {

    @Autowired
    private CarteiraService carteiraService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CotacaoService cotacaoService;

    private Usuario usuario;

    @BeforeEach
    public void prepararCarteiraComUmBitcoin() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = usuarioRepository.saveAndFlush(
            new Usuario(
                "alexandre",
                "senha-criptografada"
            )
        );

        when(cotacaoService.normalizarTicker(anyString()))
            .thenAnswer(invocacao ->
                invocacao.<String>getArgument(0)
                    .trim()
                    .toUpperCase()
            );

        carteiraService.registrarNovaTransacao(
            criarTransacao(TipoTransacao.COMPRA),
            usuario
        );
    }

    @Test
    public void deveAguardarVendaEmAndamentoAntesDeValidarNovaVenda()
        throws Exception {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        CountDownLatch primeiraVendaSalva = new CountDownLatch(1);
        CountDownLatch liberarPrimeiraVenda = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // simula uma venda de 1 BTC que já bloqueou o histórico
            // e salvou a transação, mas ainda não fez commit
            Future<?> primeiraVenda = executor.submit(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    Usuario bloqueado = usuarioRepository
                        .bloquearPorId(usuario.getId())
                        .orElseThrow();

                    Transacao venda =
                        criarTransacao(TipoTransacao.VENDA);
                    venda.setUsuario(bloqueado);
                    transacaoRepository.saveAndFlush(venda);

                    primeiraVendaSalva.countDown();
                    aguardar(liberarPrimeiraVenda);
                })
            );

            assertTrue(
                primeiraVendaSalva.await(5, TimeUnit.SECONDS)
            );

            Future<Transacao> segundaVenda = executor.submit(() ->
                carteiraService.registrarNovaTransacao(
                    criarTransacao(TipoTransacao.VENDA),
                    usuario
                )
            );

            // enquanto a primeira venda não termina, a segunda espera o bloqueio
            assertThrows(
                TimeoutException.class,
                () -> segundaVenda.get(500, TimeUnit.MILLISECONDS)
            );

            liberarPrimeiraVenda.countDown();
            primeiraVenda.get(5, TimeUnit.SECONDS);

            // depois do commit, a segunda venda enxerga o saldo zerado
            ExecutionException excecao = assertThrows(
                ExecutionException.class,
                () -> segundaVenda.get(10, TimeUnit.SECONDS)
            );

            assertInstanceOf(
                SaldoInsuficienteException.class,
                excecao.getCause()
            );

            assertEquals(2, transacaoRepository.count());
        } finally {
            liberarPrimeiraVenda.countDown();
            executor.shutdownNow();
        }
    }

    private Transacao criarTransacao(TipoTransacao tipo) {
        return new Transacao(
            "BTC",
            BigDecimal.ONE,
            BigDecimal.valueOf(50000),
            tipo
        );
    }

    private void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                    "O teste não liberou a primeira venda a tempo."
                );
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
