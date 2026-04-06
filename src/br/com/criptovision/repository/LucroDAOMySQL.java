package br.com.criptovision.repository;

import br.com.criptovision.database.ConexaoDB;
import br.com.criptovision.exception.BancoDeDadosException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LucroDAOMySQL implements LucroDAO {

    @Override
    public void salvarLucroRealizado(String ticker, double valorLucro){
        String sql = "INSERT INTO lucros (ticker, valor_lucro, data_registro) VALUES (?, ?, ?)";
        try (Connection conn = ConexaoDB.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, ticker);
            stmt.setDouble(2, valorLucro);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            stmt.execute();
        }catch(SQLException e){
            throw new BancoDeDadosException("Erro ao salvar lucro no banco: " + e.getMessage(), e);
        }
    }

    @Override
    public double lerLucroTotal(){
        String sql = "SELECT SUM(valor_lucro) as total FROM lucros";
        double somaTotal = 0;

        try(Connection conn = ConexaoDB.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            if (rs.next()){
                somaTotal = rs.getDouble("total");
            }
        }catch(SQLException e){
            throw new BancoDeDadosException("Erro ao calcular lucro total: " + e.getMessage(), e);
        }
        return somaTotal;
    }
}