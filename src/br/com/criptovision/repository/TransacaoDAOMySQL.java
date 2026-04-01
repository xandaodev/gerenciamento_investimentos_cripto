package br.com.criptovision.repository;

import br.com.criptovision.database.ConexaoDB;
import br.com.criptovision.exception.BancoDeDadosException;
import br.com.criptovision.model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransacaoDAOMySQL implements TransacaoDAO {

    @Override
    public void salvar(Transacao transacao){
        // aqui escrevemos as querys sql, as interrogaçoes sao os espaços em branco que serão preenchidos com informaçoes
        // isso evita a famosa SQL injection
        String sql = "INSERT INTO transacoes (ticker, quantidade, preco_unitario, tipo, data_transacao) VALUES (?, ?, ?, ?, ?)";

        // preparando a query
        try (Connection conn = ConexaoDB.getConexao();
            //limpa os dados antes de inseri-los, evitando sql injection
            PreparedStatement stmt = conn.prepareStatement(sql)){

            // preenche os espaços em branco com os dados do objeto
            stmt.setString(1, transacao.getTicker());
            stmt.setDouble(2, transacao.getQuantidade());
            stmt.setDouble(3, transacao.getPrecoUnitario());
            stmt.setString(4, transacao.getTipo());
            
            // o java usa LocalDateTime, mas o mysql precisa de um Timestamp
            stmt.setTimestamp(5, Timestamp.valueOf(transacao.getData()));

            stmt.execute();
            System.out.println("Transação salva no MySQL !");

        }catch(SQLException e){
            throw new BancoDeDadosException("Erro ao salvar transação no banco: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transacao> lerTudo() {
        return new ArrayList<>(); 
    }
}