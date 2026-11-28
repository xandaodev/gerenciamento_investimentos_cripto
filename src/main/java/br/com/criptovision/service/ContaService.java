package br.com.criptovision.service;

import br.com.criptovision.dto.DadosExclusaoConta;
import br.com.criptovision.dto.ExportacaoDadosDTO;
import br.com.criptovision.dto.ExportacaoDadosDTO.ContaExportada;
import br.com.criptovision.dto.ExportacaoDadosDTO.TransacaoExportada;
import br.com.criptovision.exception.ContaInexistenteException;
import br.com.criptovision.exception.SenhaDeConfirmacaoIncorretaException;
import br.com.criptovision.model.Usuario;
import br.com.criptovision.repository.TransacaoRepository;
import br.com.criptovision.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Os direitos do titular sobre a própria conta (LGPD, art. 18): exportar todos os
 * dados e excluir a conta. A política de privacidade está em
 * docs/POLITICA_DE_PRIVACIDADE.md.
 */
@Service
public class ContaService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ContaService.class);

    private final UsuarioRepository usuarioRepository;
    private final TransacaoRepository transacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ContaService(
        UsuarioRepository usuarioRepository,
        TransacaoRepository transacaoRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.transacaoRepository = transacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExportacaoDadosDTO exportarDados(Usuario usuario) {
        var transacoes = transacaoRepository
            .findAllByUsuarioOrderByDataAscIdAsc(usuario)
            .stream()
            .map(transacao -> new TransacaoExportada(
                transacao.getId(),
                transacao.getTicker(),
                transacao.getTipo(),
                transacao.getQuantidade(),
                transacao.getPrecoUnitario(),
                // as datas são gravadas em UTC: no arquivo, com o fuso explícito
                transacao.getData() == null
                    ? null
                    : transacao.getData().atOffset(ZoneOffset.UTC)
            ))
            .toList();

        return new ExportacaoDadosDTO(
            OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC),
            new ContaExportada(usuario.getLogin()),
            transacoes
        );
    }

    /**
     * Exclui a conta e todo o histórico, de forma definitiva. Pede a senha atual:
     * um token roubado não basta para apagar a conta.
     *
     * <p>Bloqueia o usuário primeiro (SELECT ... FOR UPDATE), como a criação, a
     * alteração e a exclusão de transações. Uma transação em andamento termina
     * antes, e uma que chegue depois encontra a conta apagada (401).
     */
    @Transactional
    public void excluirConta(Usuario usuario, DadosExclusaoConta dados) {
        Usuario atual = usuarioRepository.bloquearPorId(usuario.getId())
            .orElseThrow(ContaInexistenteException::new);

        if (!passwordEncoder.matches(dados.senhaAtual(), atual.getSenha())) {
            throw new SenhaDeConfirmacaoIncorretaException();
        }

        // as transações saem antes do usuário. A FK também tem ON DELETE CASCADE
        // (migration V5), mas a exclusão não depende disso
        int transacoes = transacaoRepository.excluirTodasDoUsuario(atual);
        usuarioRepository.delete(atual);

        // só o ID: o login é um dado pessoal, e a conta não existe mais
        LOGGER.info("Conta {} excluída a pedido do titular, com {} transações.", atual.getId(), transacoes);
    }
}
