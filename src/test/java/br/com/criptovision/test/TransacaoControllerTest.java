package br.com.criptovision.test;

import br.com.criptovision.controller.TransacaoController;
import br.com.criptovision.exception.GlobalExceptionHandler;
import br.com.criptovision.model.TipoTransacao;
import br.com.criptovision.model.Transacao;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.service.CarteiraService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TransacaoControllerTest {

    private Usuario usuario;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @Mock
    private CarteiraService carteiraService;

    @InjectMocks
    private TransacaoController controller;

    @BeforeEach
    public void configurarMockMvc() {

        usuario = new Usuario(
            "alexandre",
            "senha-criptografada"
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
            );

        SecurityContext context =
            SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator).setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver()
            )
            .build();
    }

    @AfterEach
    public void encerrarValidador() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    public void deveCriarTransacaoValidaERetornarStatus201()
        throws Exception {

        Transacao transacaoSalva = new Transacao(
            "BTC",
            BigDecimal.valueOf(0.5),
            BigDecimal.valueOf(60000),
            TipoTransacao.COMPRA
        );

        transacaoSalva.setId(1L);

        when(carteiraService.registrarNovaTransacao(
            any(Transacao.class),
            same(usuario)
        )).thenReturn(transacaoSalva);

        mockMvc.perform(
                post("/transacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                        {
                                          "ticker": " btc ",
                                          "quantidade": 0.5,
                                          "precoUnitario": 60000,
                                          "tipo": "compra"
                                        }
                                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.ticker").value("BTC"))
            .andExpect(jsonPath("$.tipo").value("COMPRA"));

        ArgumentCaptor<Transacao> captor =
            ArgumentCaptor.forClass(Transacao.class);

        verify(carteiraService)
            .registrarNovaTransacao(
                captor.capture(),
                same(usuario)
            );

        Transacao recebidaPeloService = captor.getValue();

        assertEquals("BTC", recebidaPeloService.getTicker());
        assertEquals(
            TipoTransacao.COMPRA,
            recebidaPeloService.getTipo()
        );
    }

    @Test
    public void deveRetornar400QuandoQuantidadeForZero()
        throws Exception {

        mockMvc.perform(
                post("/transacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 0,
                                          "precoUnitario": 60000,
                                          "tipo": "COMPRA"
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.title")
                    .value("Dados inválidos")
            )
            .andExpect(
                jsonPath("$.erros[0].mensagem")
                    .value(
                        "A quantidade deve ser maior que zero."
                    )
            );

        verifyNoInteractions(carteiraService);
    }

    @Test
    public void deveRetornar400QuandoTipoForInvalido()
        throws Exception {

        mockMvc.perform(
                post("/transacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                        {
                                          "ticker": "BTC",
                                          "quantidade": 1,
                                          "precoUnitario": 60000,
                                          "tipo": "TROCA"
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.title")
                    .value("Corpo da requisição inválido")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        containsString(
                            "Tipo de transação inválido"
                        )
                    )
            );

        verifyNoInteractions(carteiraService);
    }
}
